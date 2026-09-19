package com.example.promaster.data.service

import com.example.promaster.domain.model.GrammarAnalysisResult
import com.example.promaster.domain.model.PronunciationResult
import com.example.promaster.domain.service.GrammarService
import com.example.promaster.domain.service.PronunciationService
import com.example.promaster.domain.service.SpeakingEvaluationService
import com.example.promaster.domain.service.TranslationService

class DefaultSpeakingEvaluationService(
    private val grammarService: GrammarService = DefaultGrammarService(),
    private val pronunciationService: PronunciationService = DefaultPronunciationService(),
    private val translationService: TranslationService = DefaultTranslationService()
) : SpeakingEvaluationService {

    override fun isAvailable(): Boolean {
        return grammarService.isAvailable() &&
                pronunciationService.isAvailable() &&
                translationService.isAvailable()
    }

    override suspend fun evaluateInitialSpeech(
        spokenText: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<GrammarAnalysisResult> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }
        return grammarService.analyzeSentence(spokenText, targetLanguage, motherTongue)
    }

    override suspend fun verifyRepeatedSpeech(
        repeatedText: String,
        expectedSentence: String
    ): Result<PronunciationResult> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }
        return pronunciationService.evaluatePronunciation(repeatedText, expectedSentence)
    }
}
