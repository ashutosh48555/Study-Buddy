package com.example.studybuddy

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class StudyBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Enable network for Firestore (if using)
        try {
            FirebaseFirestore.getInstance().enableNetwork()
        } catch (e: Exception) {
            Log.e("Firebase", "Error enabling network: ${e.message}")
        }
        // App Check: Use Play Integrity for release, Debug for debug builds
        val appCheck = FirebaseAppCheck.getInstance()
        val isDebug = applicationContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            // Debug logging for authentication state
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    Log.d("FirebaseAuth", "User signed in: ${user.uid}")
                } else {
                    Log.d("FirebaseAuth", "User signed out")
                }
            }
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
        createTaskReminderChannel()
    }

    private fun createTaskReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_reminder_channel",
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
} 