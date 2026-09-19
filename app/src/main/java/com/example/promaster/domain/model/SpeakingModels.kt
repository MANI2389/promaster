package com.example.promaster.domain.model

enum class SpeakingSessionState {
    IDLE,
    LISTENING_INITIAL,
    ANALYZING,
    CORRECTION_REQUIRED,
    LISTENING_REPEAT,
    VERIFYING,
    VERIFIED_SUCCESS,
    SERVICE_UNAVAILABLE
}

data class GrammarAnalysisResult(
    val originalSentence: String,
    val isCorrect: Boolean,
    val correctedSentence: String,
    val explanation: String,
    val motherTongueExplanation: String,
    val rulesViolated: List<String> = emptyList()
)

data class PronunciationResult(
    val accuracyScore: Int, // 0..100
    val isMatch: Boolean,
    val feedback: String,
    val wordAccuracies: Map<String, Int> = emptyMap()
)

data class SpeakingPrompt(
    val id: String,
    val promptContext: String,
    val expectedPhrase: String,
    val translation: String,
    val motherTongueHint: String = "",
    val targetLanguage: String = "English"
)

data class SpeakingSessionResult(
    val promptId: String,
    val originalSpoken: String,
    val correctedSentence: String,
    val isVerified: Boolean,
    val verificationScore: Int,
    val earnedXp: Int = 0,
    val attempts: Int = 1
)
