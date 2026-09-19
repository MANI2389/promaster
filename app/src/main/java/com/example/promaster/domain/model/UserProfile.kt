package com.example.promaster.domain.model

data class UserProfile(
    val id: String = "user_default",
    val name: String,
    val motherTongue: Language,
    val targetLanguage: Language,
    val level: LanguageLevel = LanguageLevel.BEGINNER,
    val preferences: LearningPreferences = LearningPreferences(),
    val goal: LearningGoal = LearningGoal(),
    val isSetupComplete: Boolean = true,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
