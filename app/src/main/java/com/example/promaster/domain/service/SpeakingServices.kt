package com.example.promaster.domain.service

import com.example.promaster.domain.model.GrammarAnalysisResult
import com.example.promaster.domain.model.PronunciationResult
import kotlinx.coroutines.flow.StateFlow

interface SpeechRecognitionService {
    val isListening: StateFlow<Boolean>
    fun isAvailable(): Boolean
    fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
}

interface GrammarService {
    fun isAvailable(): Boolean
    suspend fun analyzeSentence(
        sentence: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<GrammarAnalysisResult>
}

interface PronunciationService {
    fun isAvailable(): Boolean
    suspend fun evaluatePronunciation(
        spokenText: String,
        expectedSentence: String
    ): Result<PronunciationResult>
}

interface TranslationService {
    fun isAvailable(): Boolean
    suspend fun translate(
        text: String,
        fromLanguage: String,
        toLanguage: String
    ): Result<String>
    suspend fun getMotherTongueExplanation(
        errorContext: String,
        motherTongue: String
    ): Result<String>
}

interface SpeakingEvaluationService {
    fun isAvailable(): Boolean
    suspend fun evaluateInitialSpeech(
        spokenText: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<GrammarAnalysisResult>
    suspend fun verifyRepeatedSpeech(
        repeatedText: String,
        expectedSentence: String
    ): Result<PronunciationResult>
}
