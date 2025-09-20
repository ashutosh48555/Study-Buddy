package com.example.studybuddy

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class splashscreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    
    // Permission launcher for background permission requests
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions requested in background - no UI feedback needed
        // Continue with app flow regardless of permission status
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splashscreen)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        
        // Get views
        val splashLayout = findViewById<ConstraintLayout>(R.id.splash)
        
        // Apply fade-in animation to the entire splash layout
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein)
        splashLayout.startAnimation(fadeIn)
        
        // Request permissions in background immediately
        requestPermissionsInBackground()
        
        // Navigate after animation completes
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, 2000) // Optimized timing for better UX
    }
    
    private fun requestPermissionsInBackground() {
        // Get all required permissions
        val requiredPermissions = mutableListOf<String>().apply {
            // Camera permission
            add(Manifest.permission.CAMERA)
            
            // Storage permissions based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            
            // Notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Filter out already granted permissions
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        // Request permissions silently in background
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun checkAuthenticationAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, go to home page
            startActivity(Intent(this, HomePage::class.java))
        } else {
            // User is not signed in, go to login/signup
            startActivity(Intent(this, loginsignup::class.java))
        }
        finish()
    }
}
