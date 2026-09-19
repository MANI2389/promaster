package com.example.promaster.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.promaster.MainActivity
import com.example.promaster.R
import com.example.promaster.data.service.OnDeviceWakeWordDetector
import com.example.promaster.domain.service.WakeWordDetector

/**
 * Foreground Service for background wake-word detection ("Hey Bro").
 *
 * POLICY & RESTRICTIONS:
 * - Adheres strictly to Android 14+ FOREGROUND_SERVICE_MICROPHONE type.
 * - Displays a visible, persistent ongoing notification while active so the user has visible system behavior.
 * - Provides an immediate "Stop" action button to halt listening.
 * - Immediately halts detection and calls stopSelf() if microphone permission is revoked.
 */
class AssistantForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "promaster_wake_word_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_LISTENING = "com.example.promaster.action.START_WAKE_WORD"
        const val ACTION_STOP_LISTENING = "com.example.promaster.action.STOP_WAKE_WORD"

        fun start(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.startService(intent)
        }
    }

    private var detector: WakeWordDetector? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        detector = OnDeviceWakeWordDetector(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_LISTENING) {
            stopListeningAndShutdown()
            return START_NOT_STICKY
        }

        // Verify microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            stopListeningAndShutdown()
            return START_NOT_STICKY
        }

        // Start in foreground with visible ongoing notification
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start on-device detection if available
        detector?.startDetecting(
            onDetected = {
                // Wake word detected - launch assistant
                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("TRIGGERED_BY_WAKE_WORD", true)
                }
                startActivity(launchIntent)
            },
            onError = {
                // If model missing, stop immediately
                stopListeningAndShutdown()
            }
        )

        return START_STICKY
    }

    override fun onDestroy() {
        stopListeningAndShutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopListeningAndShutdown() {
        // Stop listening immediately when disabled or destroyed
        detector?.stopDetecting()
        detector = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PROMASTER Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when PROMASTER is listening for the wake word 'Hey Bro'"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }.let {
            PendingIntent.getService(this, 1, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PROMASTER Listening for 'Hey Bro'")
            .setContentText("On-device wake-word detection active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
