package com.example.promaster.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val targetLanguage: String,
    val nativeLanguage: String,
    val streakDays: Int,
    val totalXp: Int,
    val level: Int,
    val currentDay: Int,
    val dailyGoalMinutes: Int = 15,
    val avatarEmoji: String = "🦉"
)
