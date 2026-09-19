package com.example.promaster.domain.model

data class ProgressState(
    val currentStreak: Int,
    val bestStreak: Int,
    val longestStreak: Int = bestStreak,
    val totalXp: Int,
    val wordsLearned: Int,
    val rulesMastered: Int,
    val speakingMinutes: Int,
    val listeningMinutes: Int,
    val level: Int,
    val weeklyXp: List<Int>,
    val badges: List<String>,
    val lastActivityDate: Long = 0L
)
