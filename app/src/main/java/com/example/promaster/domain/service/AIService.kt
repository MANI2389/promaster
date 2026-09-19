package com.example.promaster.domain.service

import com.example.promaster.domain.model.AIConversation
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.AIResponse
import com.example.promaster.domain.model.LanguageLevel

interface AIService {
    suspend fun processRequest(request: AIRequest): Result<AIResponse>
    suspend fun detectIntent(input: String): AIIntent
    suspend fun generateConversationReply(
        conversation: AIConversation,
        newMessage: String,
        motherTongue: String,
        targetLanguage: String
    ): Result<AIResponse>
    suspend fun correctGrammar(
        text: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun explainVocabulary(
        word: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun translate(
        text: String,
        fromLang: String,
        toLang: String,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun evaluateSpeaking(
        spokenText: String,
        expectedPhrase: String,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun provideMotivation(
        streakDays: Int,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun assistLearningPlan(
        currentDay: Int,
        goal: String,
        motherTongue: String
    ): Result<AIResponse>
    suspend fun createPersonalizedLesson(
        topic: String,
        level: LanguageLevel,
        motherTongue: String
    ): Result<AIResponse>
}
