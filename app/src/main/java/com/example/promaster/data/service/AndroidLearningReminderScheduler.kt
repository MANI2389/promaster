package com.example.promaster.data.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.promaster.domain.model.LearningReminderSettings
import com.example.promaster.domain.model.ReminderType
import com.example.promaster.domain.service.LearningReminderScheduler
import com.example.promaster.service.LearningReminderReceiver
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale

/**
 * Android AlarmManager-backed implementation of [LearningReminderScheduler].
 *
 * ARCHITECTURAL SPECIFICATION:
 * - Uses standard Android-supported scheduling mechanisms (AlarmManager + PendingIntent).
 * - Target is a transient BroadcastReceiver ([LearningReminderReceiver]).
 * - Does NOT create or rely on any always-running background service.
 */
class AndroidLearningReminderScheduler(
    private val context: Context
) : LearningReminderScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val RC_DAILY_LESSON = 4001
        const val RC_MISSED_TASK = 4002
        const val RC_STREAK_REMINDER = 4003

        fun parseTimeString(timeStr: String): Pair<Int, Int> {
            val trimmed = timeStr.trim()
            return try {
                // Try 12-hour format e.g. "08:00 AM", "8:30 PM", "8:00 am"
                val formatter = DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("[h:mm a][hh:mm a][H:mm][HH:mm]")
                    .toFormatter(Locale.US)
                val localTime = LocalTime.parse(trimmed, formatter)
                Pair(localTime.hour, localTime.minute)
            } catch (e: Exception) {
                // Fallback heuristic parsing
                fallbackParse(trimmed)
            }
        }

        private fun fallbackParse(trimmed: String): Pair<Int, Int> {
            val isPm = trimmed.contains("PM", ignoreCase = true)
            val isAm = trimmed.contains("AM", ignoreCase = true)
            val clean = trimmed.replace("AM", "", ignoreCase = true)
                .replace("PM", "", ignoreCase = true)
                .trim()
            val parts = clean.split(":")
            var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }
    }

    override fun scheduleDailyLessonReminder(timeStr: String) {
        val (hour, minute) = parseTimeString(timeStr)
        val triggerMillis = calculateNextTriggerMillis(hour, minute)

        val intent = Intent(context, LearningReminderReceiver::class.java).apply {
            action = LearningReminderReceiver.ACTION_DAILY_LESSON_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_DAILY_LESSON,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun scheduleMissedTaskReminder(hour: Int, minute: Int) {
        val triggerMillis = calculateNextTriggerMillis(hour, minute)

        val intent = Intent(context, LearningReminderReceiver::class.java).apply {
            action = LearningReminderReceiver.ACTION_MISSED_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_MISSED_TASK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun scheduleStreakReminder(hour: Int, minute: Int) {
        val triggerMillis = calculateNextTriggerMillis(hour, minute)

        val intent = Intent(context, LearningReminderReceiver::class.java).apply {
            action = LearningReminderReceiver.ACTION_STREAK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_STREAK_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun updateSettings(settings: LearningReminderSettings) {
        if (settings.dailyLessonReminderEnabled) {
            scheduleDailyLessonReminder(settings.preferredTime)
        } else {
            cancelReminder(ReminderType.DAILY_LESSON)
        }

        if (settings.missedTaskReminderEnabled) {
            scheduleMissedTaskReminder()
        } else {
            cancelReminder(ReminderType.MISSED_TASK)
        }

        if (settings.streakReminderEnabled) {
            scheduleStreakReminder()
        } else {
            cancelReminder(ReminderType.STREAK_PRESERVATION)
        }
    }

    override fun cancelReminder(type: ReminderType) {
        val (requestCode, action) = when (type) {
            ReminderType.DAILY_LESSON -> Pair(RC_DAILY_LESSON, LearningReminderReceiver.ACTION_DAILY_LESSON_REMINDER)
            ReminderType.MISSED_TASK -> Pair(RC_MISSED_TASK, LearningReminderReceiver.ACTION_MISSED_TASK_REMINDER)
            ReminderType.STREAK_PRESERVATION -> Pair(RC_STREAK_REMINDER, LearningReminderReceiver.ACTION_STREAK_REMINDER)
        }

        val intent = Intent(context, LearningReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun cancelAllReminders() {
        cancelReminder(ReminderType.DAILY_LESSON)
        cancelReminder(ReminderType.MISSED_TASK)
        cancelReminder(ReminderType.STREAK_PRESERVATION)
    }

    override fun isReminderScheduled(type: ReminderType): Boolean {
        val (requestCode, action) = when (type) {
            ReminderType.DAILY_LESSON -> Pair(RC_DAILY_LESSON, LearningReminderReceiver.ACTION_DAILY_LESSON_REMINDER)
            ReminderType.MISSED_TASK -> Pair(RC_MISSED_TASK, LearningReminderReceiver.ACTION_MISSED_TASK_REMINDER)
            ReminderType.STREAK_PRESERVATION -> Pair(RC_STREAK_REMINDER, LearningReminderReceiver.ACTION_STREAK_REMINDER)
        }

        val intent = Intent(context, LearningReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
}
