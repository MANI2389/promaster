package com.example.promaster.domain.service

import com.example.promaster.domain.model.LearningReminderSettings
import com.example.promaster.domain.model.ReminderType

/**
 * Contract for scheduling and cancelling PROMASTER learning reminders.
 * Adheres strictly to Android-supported scheduling mechanisms without always-running background services.
 */
interface LearningReminderScheduler {
    fun scheduleDailyLessonReminder(timeStr: String)
    fun scheduleMissedTaskReminder(hour: Int = 19, minute: Int = 0)
    fun scheduleStreakReminder(hour: Int = 22, minute: Int = 0)
    fun updateSettings(settings: LearningReminderSettings)
    fun cancelReminder(type: ReminderType)
    fun cancelAllReminders()
    fun isReminderScheduled(type: ReminderType): Boolean
}
