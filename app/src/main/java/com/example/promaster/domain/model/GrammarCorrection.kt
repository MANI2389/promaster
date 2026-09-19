package com.example.promaster.domain.model

data class GrammarCorrection(
    val originalText: String,
    val correctedText: String,
    val explanation: String,
    val improvedVersion: String,
    val confidenceScore: Float,
    val rulesApplied: List<String>
)
