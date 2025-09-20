package com.example.studybuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Random
import java.io.IOException
import java.io.File
import android.media.AudioAttributes
import android.media.AudioManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.common.util.Util
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.util.UnstableApi

@UnstableApi
class LofiMusicService : Service() {
    private var exoPlayer: ExoPlayer? = null
    private var currentTrackIndex = 0
    private var isPlayingState = false
    private var isPaused = false
    private val binder = MusicBinder()
    private var onTrackChangedListener: ((LofiTrack) -> Unit)? = null
    private var onPlaybackStateChangedListener: ((Boolean) -> Unit)? = null
    private var onBufferingUpdateListener: ((Int) -> Unit)? = null
    
    // Caching and network monitoring
    private var simpleCache: SimpleCache? = null
    private var networkConnectivityManager: NetworkConnectivityManager? = null
    private var isNetworkAvailable = true
    private var currentRetryCount = 0
    private var retryJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val musicApiService = MusicApiService()
    
    companion object {
        private const val TAG = "LofiMusicService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "lofi_music_channel"
        
        // Actions for notification controls
        const val ACTION_PLAY_PAUSE = "com.example.studybuddy.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.studybuddy.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.studybuddy.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.studybuddy.ACTION_STOP"
        
        // Cache and retry constants
        private const val CACHE_SIZE = 50L * 1024 * 1024 // 50 MB
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L // 2 seconds
        private const val CACHE_FOLDER = "music_cache"
    }
    
    private var lofiTracks: List<LofiTrack> = emptyList()
    
    private fun loadTracksFromAPI() {
        serviceScope.launch {
            try {
                val tracks = musicApiService.fetchSongs()
                lofiTracks = tracks
                Log.d(TAG, "Successfully loaded ${tracks.size} tracks from API")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tracks from API", e)
                // Fallback to default tracks if API loading fails
                lofiTracks = listOf(
                    LofiTrack("Chill Vibes", "StudyBeats", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://picsum.photos/200", "3:45"),
                    LofiTrack("Study Focus", "LofiGirl", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://picsum.photos/201", "4:12")
                )
            }
        }
    }
    
    inner class MusicBinder : Binder() {
        fun getService(): LofiMusicService = this@LofiMusicService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeCache()
        initializeNetworkMonitoring()
        loadTracksFromAPI()
        Log.d(TAG, "LofiMusicService created with caching and network monitoring")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stopMusic()
        }
    return START_STICKY
}

private fun initializeCache() {
    val cacheDir = File(cacheDir, CACHE_FOLDER)
    simpleCache = SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
        StandaloneDatabaseProvider(this)
    )
    Log.d(TAG, "Cache initialized in folder: ${cacheDir.absolutePath}")
}

private fun initializeNetworkMonitoring() {
    networkConnectivityManager = NetworkConnectivityManager(this)
    networkConnectivityManager?.startListening { isAvailable ->
        isNetworkAvailable = isAvailable
        if (!isAvailable) {
            Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun handleTrackPlayback(streamUrl: String) {
    if (isNetworkAvailable) {
        exoPlayer?.run {
            stop()
            clearMediaItems()
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            setMediaItem(mediaItem)
            prepare()
            play()
            currentRetryCount = 0
        }
    } else {
        Toast.makeText(this, "Track unavailable – check connection", Toast.LENGTH_SHORT).show()
    }
}
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lofi Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for lofi music playback"
                setShowBadge(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, HomePage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val playPauseAction = NotificationCompat.Action(
            if (isPlayingState) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlayingState) "Pause" else "Play",
            PendingIntent.getService(
                this, 0,
                Intent(this, LofiMusicService::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        
        val nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next,
            "Next",
            PendingIntent.getService(
                this, 0,
                Intent(this, LofiMusicService::class.java).apply {
                    action = ACTION_NEXT
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        
        val prevAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous,
            "Previous",
            PendingIntent.getService(
                this, 0,
                Intent(this, LofiMusicService::class.java).apply {
                    action = ACTION_PREVIOUS
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lofi Music Player")
            .setContentText(getCurrentTrack().title)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    fun playMusic() {
        if (lofiTracks.isEmpty()) {
            Log.e(TAG, "No tracks available to play")
            return
        }
        
        if (isPaused) {
            resumeMusic()
            return
        }

        if (exoPlayer == null) {
            createExoPlayerWithCache()
        }
        
        retryJob?.cancel()
        playTrackWithRetry(lofiTracks[currentTrackIndex].streamUrl)
    }
    
    private fun createExoPlayerWithCache() {
        // Create HTTP data source factory for better streaming support
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(
            Util.getUserAgent(this, "LofiMusicService")
        )
        
        // Create cache data source factory
        val cacheDataSourceFactory = simpleCache?.let { cache ->
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } ?: DefaultDataSource.Factory(this, httpDataSourceFactory)
        
        // Create media source factory with caching support
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
        
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                // Set audio attributes for music playback
                val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttributes, true)
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_ENDED -> playNext()
                            Player.STATE_READY -> {
                                if (playWhenReady) {
                                    startForeground(NOTIFICATION_ID, createNotification())
                                    Log.d(TAG, "Started playing: ${getCurrentTrack().title}")
                                    isPlayingState = true
                                    isPaused = false
                                    onPlaybackStateChangedListener?.invoke(true)
                                    onTrackChangedListener?.invoke(getCurrentTrack())
                                    currentRetryCount = 0 // Reset retry count on successful play
                                }
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Buffering track: ${getCurrentTrack().title}")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer error: ", error)
                        handlePlaybackError(error)
                    }
                    
                    override fun onLoadingChanged(isLoading: Boolean) {
                        Log.d(TAG, "Loading state changed: $isLoading")
                    }
                })
            }
    }
    
    private fun playTrackWithRetry(streamUrl: String) {
        if (!isNetworkAvailable) {
            showNetworkUnavailableToast()
            return
        }
        
        serviceScope.launch {
            try {
                // Test URL availability first
                if (isUrlReachable(streamUrl)) {
                    withContext(Dispatchers.Main) {
                        handleTrackPlayback(streamUrl)
                    }
                } else {
                    throw IOException("URL not reachable: $streamUrl")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing track: ", e)
                handlePlaybackError(e)
            }
        }
    }
    
    private suspend fun isUrlReachable(url: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .build()
                
                val response: Response = okHttpClient.newCall(request).execute()
                val isReachable = response.isSuccessful
                response.close()
                isReachable
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL reachability: ", e)
            false
        }
    }
    
    private fun handlePlaybackError(error: Any) {
        Log.e(TAG, "Playback error: $error")
        onPlaybackStateChangedListener?.invoke(false)
        
        if (currentRetryCount < MAX_RETRY_ATTEMPTS) {
            currentRetryCount++
            Log.d(TAG, "Retrying playback (attempt $currentRetryCount/$MAX_RETRY_ATTEMPTS)")
            
            retryJob = serviceScope.launch {
                delay(RETRY_DELAY_MS)
                withContext(Dispatchers.Main) {
                    playTrackWithRetry(lofiTracks[currentTrackIndex].streamUrl)
                }
            }
        } else {
            Log.e(TAG, "Max retry attempts reached, skipping to next track")
            currentRetryCount = 0
            mainHandler.post {
                showNetworkUnavailableToast()
                playNext()
            }
        }
    }
    
    private fun showNetworkUnavailableToast() {
        mainHandler.post {
            Toast.makeText(this, "Track unavailable – check connection", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun pauseMusic() {
        exoPlayer?.pause()
        isPlayingState = false
        isPaused = true
        onPlaybackStateChangedListener?.invoke(false)
        updateNotification()
        Log.d(TAG, "Music paused")
    }
    
    fun resumeMusic() {
        exoPlayer?.play()
        isPlayingState = true
        isPaused = false
        onPlaybackStateChangedListener?.invoke(true)
        updateNotification()
        Log.d(TAG, "Music resumed")
    }
    
    fun stopMusic() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        isPlayingState = false
        isPaused = false
        onPlaybackStateChangedListener?.invoke(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Music stopped")
    }
    
    fun togglePlayback() {
        if (isPlayingState) {
            pauseMusic()
        } else if (isPaused) {
            resumeMusic()
        } else {
            playMusic()
        }
    }
    
    fun playNext() {
        if (lofiTracks.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % lofiTracks.size
            playMusic()
        }
    }
    
    fun playPrevious() {
        if (lofiTracks.isNotEmpty()) {
            currentTrackIndex = if (currentTrackIndex == 0) {
                lofiTracks.size - 1
            } else {
                currentTrackIndex - 1
            }
            playMusic()
        }
    }
    
    fun playTrack(index: Int) {
        if (index in lofiTracks.indices) {
            currentTrackIndex = index
            playMusic()
        }
    }
    
    fun shuffleTrack() {
        if (lofiTracks.isNotEmpty()) {
            val random = Random()
            var newIndex = random.nextInt(lofiTracks.size)
            while (newIndex == currentTrackIndex && lofiTracks.size > 1) {
                newIndex = random.nextInt(lofiTracks.size)
            }
            currentTrackIndex = newIndex
            playMusic()
        }
    }
    
    fun getCurrentTrack(): LofiTrack {
        return if (lofiTracks.isNotEmpty()) {
            lofiTracks[currentTrackIndex.coerceIn(0, lofiTracks.size - 1)]
        } else {
            LofiTrack("No Track", "Unknown", "", "", "0:00")
        }
    }
    
    fun getAllTracks(): List<LofiTrack> = lofiTracks
    
    fun isPlaying(): Boolean = isPlayingState
    
    fun isPaused(): Boolean = isPaused
    
    fun getCurrentPosition(): Int = exoPlayer?.currentPosition?.toInt() ?: 0
    
    fun getDuration(): Int = exoPlayer?.duration?.toInt() ?: 0
    
    fun seekTo(position: Int) {
        exoPlayer?.seekTo(position.toLong())
    }
    
    fun setOnTrackChangedListener(listener: (LofiTrack) -> Unit) {
        onTrackChangedListener = listener
    }
    
    fun setOnPlaybackStateChangedListener(listener: (Boolean) -> Unit) {
        onPlaybackStateChangedListener = listener
    }
    
    fun setOnBufferingUpdateListener(listener: (Int) -> Unit) {
        onBufferingUpdateListener = listener
    }
    
    fun getCacheSize(): Long {
        return simpleCache?.cacheSpace ?: 0L
    }
    
    fun getCacheUsage(): String {
        val cacheSize = getCacheSize()
        return if (cacheSize > 0) {
            val sizeInMB = cacheSize / (1024 * 1024)
            "Cache: ${sizeInMB}MB / ${CACHE_SIZE / (1024 * 1024)}MB"
        } else {
            "Cache: Not initialized"
        }
    }
    
    fun getBufferedPercentage(): Int {
        return exoPlayer?.bufferedPercentage ?: 0
    }
    
    fun getNetworkAvailable(): Boolean {
        return isNetworkAvailable
    }
    
    fun clearCache() {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    simpleCache?.let { cache ->
                        val keys = cache.keys
                        for (key in keys) {
                            cache.removeResource(key)
                        }
                    }
                    Log.d(TAG, "Cache cleared successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing cache: ", e)
                }
            }
        }
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel any pending retry jobs
        retryJob?.cancel()
        
        // Clean up ExoPlayer
        exoPlayer?.release()
        exoPlayer = null
        
        // Clean up network monitoring
        networkConnectivityManager?.stopListening()
        networkConnectivityManager = null
        
        // Clean up cache
        try {
            simpleCache?.release()
            simpleCache = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing cache: ", e)
        }
        
        Log.d(TAG, "LofiMusicService destroyed with cleanup")
    }
}

data class LofiTrack(
    val title: String,
    val artist: String,
    val url: String,
    val coverArtUrl: String,
    val duration: String
) {
    val streamUrl: String get() = url
}
