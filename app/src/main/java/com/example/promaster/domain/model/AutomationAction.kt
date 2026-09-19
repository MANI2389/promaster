package com.example.promaster.domain.model

enum class SafetyLevel {
    SAFE,
    REQUIRES_CONFIRMATION,
    RESTRICTED
}

data class AutomationAction(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val safetyLevel: SafetyLevel,
    val isEnabled: Boolean,
    val triggers: String
)
