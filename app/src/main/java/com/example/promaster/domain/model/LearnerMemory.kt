package com.example.promaster.domain.model

/**
 * Persists useful learning telemetry to personalize the AI Friend & Language Coach experience.
 *
 * PRIVACY & SECURITY:
 * Strictly stores learning context only (languages, level, progress, weak areas, completed lessons).
 * Never stores unnecessary sensitive personal information (passwords, tokens, phone numbers, PII).
 */
data class LearnerMemory(
    val userId: String = "",
    val targetLanguage: String = "Spanish",
    val motherTongue: String = "English",
    val level: LanguageLevel = LanguageLevel.BEGINNER,
    val currentDay: Int = 1,
    val progressPercentage: Int = 0,
    val streakDays: Int = 1,
    val weakAreas: List<String> = emptyList(),
    val completedLessons: List<String> = emptyList(),
    val lastEncouragementType: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
