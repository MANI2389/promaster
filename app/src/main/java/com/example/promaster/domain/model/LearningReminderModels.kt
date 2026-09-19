package com.example.promaster.domain.model

/**
 * Types of learning reminders supported by PROMASTER.
 */
enum class ReminderType(val channelName: String, val description: String) {
    DAILY_LESSON("Daily Lesson Reminder", "Reminds learner at their chosen study time to begin their daily lesson"),
    MISSED_TASK("Missed Task Reminder", "Evening prompt when daily curriculum tasks remain incomplete"),
    STREAK_PRESERVATION("Streak Preservation", "High-priority alert before day ends if streak is at risk")
}

/**
 * Configuration and user preferences for learning reminders.
 */
data class LearningReminderSettings(
    val preferredTime: String = "08:00 AM",
    val dailyLessonReminderEnabled: Boolean = true,
    val missedTaskReminderEnabled: Boolean = true,
    val streakReminderEnabled: Boolean = true
)

/**
 * Outcome of a streak calculation following learning activity completion.
 */
data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: Long,
    val streakIncremented: Boolean,
    val streakBroken: Boolean
)
