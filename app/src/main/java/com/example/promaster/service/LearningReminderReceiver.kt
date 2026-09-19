package com.example.promaster.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.promaster.MainActivity
import com.example.promaster.R
import com.example.promaster.domain.engine.StreakCalculationEngine
import com.example.promaster.data.mock.MockDataProvider

/**
 * Lightweight BroadcastReceiver for handling scheduled PROMASTER learning alarms.
 *
 * ARCHITECTURAL SPECIFICATION:
 * - Operates transiently for fractions of a second: NO always-running background service.
 * - Dispatches notifications on channel 'promaster_learning_reminders'.
 * - Checks notification permissions before posting.
 */
class LearningReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "promaster_learning_reminders"
        const val NOTIFICATION_ID_DAILY = 3001
        const val NOTIFICATION_ID_MISSED = 3002
        const val NOTIFICATION_ID_STREAK = 3003

        const val ACTION_DAILY_LESSON_REMINDER = "com.example.promaster.action.DAILY_LESSON_REMINDER"
        const val ACTION_MISSED_TASK_REMINDER = "com.example.promaster.action.MISSED_TASK_REMINDER"
        const val ACTION_STREAK_REMINDER = "com.example.promaster.action.STREAK_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // 1. Verify notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        // 2. Ensure notification channel exists
        createNotificationChannel(context)

        // 3. Dispatch specific reminder based on action
        when (action) {
            ACTION_DAILY_LESSON_REMINDER -> {
                showNotification(
                    context = context,
                    notificationId = NOTIFICATION_ID_DAILY,
                    title = "PROMASTER Daily Practice 🎯",
                    message = "Time for your daily language lesson! Complete today's activities to keep your streak alive.",
                    destination = "home"
                )
            }
            ACTION_MISSED_TASK_REMINDER -> {
                showNotification(
                    context = context,
                    notificationId = NOTIFICATION_ID_MISSED,
                    title = "Incomplete Daily Tasks 📚",
                    message = "You still have unfinished learning tasks today. A quick 10-minute session will wrap it up!",
                    destination = "today_tasks"
                )
            }
            ACTION_STREAK_REMINDER -> {
                val lastActivity = MockDataProvider.progressState.lastActivityDate
                val isSecured = StreakCalculationEngine.isStreakActiveToday(lastActivity)
                // Only alert if the streak has not yet been secured today
                if (!isSecured) {
                    showNotification(
                        context = context,
                        notificationId = NOTIFICATION_ID_STREAK,
                        title = "Your Learning Streak is at Risk! 🔥",
                        message = "Only a few hours left today! Complete an exercise now to save your streak from breaking.",
                        destination = "today_tasks"
                    )
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        destination: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", destination)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PROMASTER Learning Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily lesson reminders, missed tasks, and streak protection notifications"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
