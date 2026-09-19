package com.example.promaster.data.service

import com.example.promaster.domain.model.LearningReminderSettings
import com.example.promaster.domain.model.ReminderType
import com.example.promaster.domain.service.LearningReminderScheduler

/**
 * In-memory testable mock implementation of [LearningReminderScheduler].
 */
class MockLearningReminderScheduler(
    private var initialSettings: LearningReminderSettings = LearningReminderSettings()
) : LearningReminderScheduler {

    private val scheduledReminders = mutableMapOf<ReminderType, Boolean>()
    var lastScheduledTimeStr: String? = null
    var lastMissedTaskTime: Pair<Int, Int>? = null
    var lastStreakReminderTime: Pair<Int, Int>? = null
    var currentSettings: LearningReminderSettings = initialSettings

    init {
        updateSettings(initialSettings)
    }

    override fun scheduleDailyLessonReminder(timeStr: String) {
        lastScheduledTimeStr = timeStr
        scheduledReminders[ReminderType.DAILY_LESSON] = true
    }

    override fun scheduleMissedTaskReminder(hour: Int, minute: Int) {
        lastMissedTaskTime = Pair(hour, minute)
        scheduledReminders[ReminderType.MISSED_TASK] = true
    }

    override fun scheduleStreakReminder(hour: Int, minute: Int) {
        lastStreakReminderTime = Pair(hour, minute)
        scheduledReminders[ReminderType.STREAK_PRESERVATION] = true
    }

    override fun updateSettings(settings: LearningReminderSettings) {
        currentSettings = settings
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
        scheduledReminders[type] = false
    }

    override fun cancelAllReminders() {
        ReminderType.values().forEach {
            scheduledReminders[it] = false
        }
    }

    override fun isReminderScheduled(type: ReminderType): Boolean {
        return scheduledReminders[type] == true
    }
}
