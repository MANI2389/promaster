package com.example.promaster.domain.model

enum class PreferredLearningTime(
    val label: String,
    val timeSlot: String,
    val iconEmoji: String,
    val subtitle: String
) {
    MORNING("Morning", "08:00 AM", "🌅", "Start your day with sharp mental focus"),
    AFTERNOON("Afternoon", "01:00 PM", "☀️", "Quick practice during midday break"),
    EVENING("Evening", "06:00 PM", "🌇", "Relax and unwind with post-work study"),
    NIGHT("Night", "09:00 PM", "🌙", "Peaceful review right before bedtime")
}

data class LearningPreferences(
    val dailyDurationMinutes: Int = 15, // 10, 15, 20, 30, 45, 60
    val preferredLearningTime: PreferredLearningTime = PreferredLearningTime.MORNING,
    val reminderNotificationEnabled: Boolean = true
) {
    companion object {
        val DURATION_OPTIONS = listOf(10, 15, 20, 30, 45, 60)
    }
}
