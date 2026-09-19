package com.example.promaster.domain.model

data class LearningPlan(
    val day: Int,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val taskCount: Int = 4,
    val category: String = "Comprehensive"
)
