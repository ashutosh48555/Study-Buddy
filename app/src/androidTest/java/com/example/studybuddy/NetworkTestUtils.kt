package com.example.studybuddy

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility class for network testing scenarios
 * Provides helper methods for network state manipulation and monitoring
 */
class NetworkTestUtils {
    
    companion object {
        private const val TAG = "NetworkTestUtils"
        private const val NETWORK_CHANGE_TIMEOUT = 10000L // 10 seconds
        private const val NETWORK_STABILIZATION_DELAY = 2000L // 2 seconds
        
        /**
         * Monitor network state changes
         */
        fun monitorNetworkChanges(
            context: Context,
            onNetworkAvailable: () -> Unit,
            onNetworkLost: () -> Unit
        ): ConnectivityManager.NetworkCallback {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available: $network")
                    onNetworkAvailable()
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost: $network")
                    onNetworkLost()
                }
            }
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            return networkCallback
        }
        
        /**
         * Wait for network state to change
         */
        suspend fun waitForNetworkState(
            context: Context,
            expectedState: NetworkState,
            timeoutMs: Long = NETWORK_CHANGE_TIMEOUT
        ): Boolean {
            return withTimeoutOrNull(timeoutMs) {
                while (getCurrentNetworkState(context) != expectedState) {
                    delay(500)
                }
                true
            } ?: false
        }
        
        /**
         * Get current network state
         */
        fun getCurrentNetworkState(context: Context): NetworkState {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork == null) {
                    return NetworkState.DISCONNECTED
                }
                
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities == null) {
                    return NetworkState.DISCONNECTED
                }
                
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.MOBILE
                    else -> NetworkState.OTHER
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
                    return NetworkState.DISCONNECTED
                }
                
                return when (activeNetworkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> NetworkState.WIFI
                    ConnectivityManager.TYPE_MOBILE -> NetworkState.MOBILE
                    else -> NetworkState.OTHER
                }
            }
        }
        
        /**
         * Check if network is available
         */
        fun isNetworkAvailable(context: Context): Boolean {
            return getCurrentNetworkState(context) != NetworkState.DISCONNECTED
        }
        
        /**
         * Check if Wi-Fi is connected
         */
        fun isWifiConnected(context: Context): Boolean {
            return getCurrentNetworkState(context) == NetworkState.WIFI
        }
        
        /**
         * Check if mobile data is connected
         */
        fun isMobileConnected(context: Context): Boolean {
            return getCurrentNetworkState(context) == NetworkState.MOBILE
        }
        
        /**
         * Toggle Wi-Fi state (requires appropriate permissions)
         */
        fun toggleWifi(context: Context, enabled: Boolean) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ requires user interaction for Wi-Fi toggle
                Log.w(TAG, "Wi-Fi toggle requires user interaction on Android 10+")
                return
            }
            
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
            Log.d(TAG, "Wi-Fi toggled: $enabled")
        }
        
        /**
         * Wait for Wi-Fi to stabilize after toggle
         */
        suspend fun waitForWifiStabilization(context: Context, expectedState: Boolean) {
            val startTime = System.currentTimeMillis()
            val timeout = 15000L // 15 seconds
            
            while (System.currentTimeMillis() - startTime < timeout) {
                val currentState = isWifiConnected(context)
                if (currentState == expectedState) {
                    delay(NETWORK_STABILIZATION_DELAY) // Wait for stabilization
                    return
                }
                delay(1000)
            }
            
            Log.w(TAG, "Wi-Fi stabilization timeout")
        }
        
        /**
         * Simulate airplane mode by disabling all network connections
         */
        fun simulateAirplaneMode(context: Context, enabled: Boolean) {
            if (enabled) {
                Log.d(TAG, "Simulating airplane mode ON")
                toggleWifi(context, false)
                // Note: Mobile data cannot be programmatically disabled in tests
                // This limitation is documented in the manual testing guide
            } else {
                Log.d(TAG, "Simulating airplane mode OFF")
                toggleWifi(context, true)
            }
        }
        
        /**
         * Test network speed (basic latency test)
         */
        suspend fun testNetworkLatency(context: Context): Long {
            val startTime = System.currentTimeMillis()
            
            // Simulate network test by checking connectivity
            val isConnected = isNetworkAvailable(context)
            val endTime = System.currentTimeMillis()
            
            return if (isConnected) {
                endTime - startTime
            } else {
                -1L // Network unavailable
            }
        }
        
        /**
         * Create a network state listener with latch for testing
         */
        fun createNetworkStateListener(
            context: Context,
            expectedState: NetworkState,
            latch: CountDownLatch
        ): ConnectivityManager.NetworkCallback {
            return monitorNetworkChanges(
                context,
                onNetworkAvailable = {
                    if (expectedState != NetworkState.DISCONNECTED) {
                        latch.countDown()
                    }
                },
                onNetworkLost = {
                    if (expectedState == NetworkState.DISCONNECTED) {
                        latch.countDown()
                    }
                }
            )
        }
        
        /**
         * Wait for network state change with latch
         */
        fun waitForNetworkStateChange(
            context: Context,
            expectedState: NetworkState,
            timeoutSeconds: Long = 10
        ): Boolean {
            val latch = CountDownLatch(1)
            val callback = createNetworkStateListener(context, expectedState, latch)
            
            try {
                return latch.await(timeoutSeconds, TimeUnit.SECONDS)
            } finally {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
        
        /**
         * Get network type string for logging
         */
        fun getNetworkTypeString(context: Context): String {
            return when (getCurrentNetworkState(context)) {
                NetworkState.WIFI -> "Wi-Fi"
                NetworkState.MOBILE -> "Mobile Data"
                NetworkState.OTHER -> "Other"
                NetworkState.DISCONNECTED -> "Disconnected"
            }
        }
        
        /**
         * Get detailed network info for debugging
         */
        fun getNetworkInfo(context: Context): NetworkInfo {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val state = getCurrentNetworkState(context)
            
            var capabilities: NetworkCapabilities? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            }
            
            return NetworkInfo(
                state = state,
                isConnected = state != NetworkState.DISCONNECTED,
                hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false,
                signalStrength = getSignalStrength(capabilities),
                networkType = getNetworkTypeString(context)
            )
        }
        
        private fun getSignalStrength(capabilities: NetworkCapabilities?): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                capabilities?.signalStrength ?: 0
            } else {
                0 // Signal strength not available in older APIs
            }
        }
        
        /**
         * Log network state for debugging
         */
        fun logNetworkState(context: Context, tag: String = TAG) {
            val info = getNetworkInfo(context)
            Log.d(tag, "Network State: ${info.state}, Connected: ${info.isConnected}, " +
                    "Internet: ${info.hasInternet}, Type: ${info.networkType}")
        }
    }
    
    /**
     * Network state enumeration
     */
    enum class NetworkState {
        WIFI,
        MOBILE,
        OTHER,
        DISCONNECTED
    }
    
    /**
     * Network information data class
     */
    data class NetworkInfo(
        val state: NetworkState,
        val isConnected: Boolean,
        val hasInternet: Boolean,
        val signalStrength: Int,
        val networkType: String
    )
}
