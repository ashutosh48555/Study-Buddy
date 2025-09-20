package com.example.studybuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.media.RingtoneManager
import android.media.Ringtone
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.app.AlarmManager
import android.app.PendingIntent

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Task Reminder"
        val taskId = intent.getStringExtra("taskId") ?: "0"
        val notificationId = taskId.hashCode()
        val ringtoneUriString = intent.getStringExtra("ringtoneUri")
        val ringtoneUri = if (!ringtoneUriString.isNullOrEmpty()) Uri.parse(ringtoneUriString) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone: Ringtone? = RingtoneManager.getRingtone(context, ringtoneUri)
        // Stop any currently playing ringtone
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone?.play()
        // Stop ringtone after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({ ringtone?.stop() }, 10000)
        // Get ringtone name
        val ringtoneTitle = try { RingtoneManager.getRingtone(context, ringtoneUri)?.getTitle(context) ?: "Default" } catch (_: Exception) { "Default" }
        // Snooze and Dismiss actions
        val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("taskTitle", taskTitle)
            putExtra("taskId", taskId)
            putExtra("ringtoneUri", ringtoneUriString)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, notificationId + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismissIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("taskId", taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(context, notificationId + 2, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Handle snooze/dismiss actions
        when (intent.action) {
            "SNOOZE" -> {
                // Snooze for 5 minutes
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
                val snoozeAlarmIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    putExtra("taskTitle", taskTitle)
                    putExtra("taskId", taskId)
                    putExtra("ringtoneUri", ringtoneUriString)
                }
                val snoozeAlarmPendingIntent = PendingIntent.getBroadcast(context, notificationId, snoozeAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, snoozeAlarmPendingIntent)
                return
            }
            "DISMISS" -> {
                // Just stop ringtone and return
                ringtone?.stop()
                return
            }
        }
        val builder = NotificationCompat.Builder(context, "task_reminder_channel")
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentTitle("Task Reminder: $ringtoneTitle")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_pause, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_delete_task, "Dismiss", dismissPendingIntent)
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
} 