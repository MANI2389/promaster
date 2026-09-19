package com.example.promaster.ai

import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.GrammarCorrection
import com.example.promaster.domain.model.MessageSender

interface AiBackendClient {
    suspend fun generateChatReply(prompt: String, conversationContext: List<AiMessage>): Result<String>
    suspend fun analyzeGrammar(text: String, targetLanguage: String): Result<GrammarCorrection>
    suspend fun evaluatePronunciation(audioBytes: ByteArray, expectedText: String): Result<Float>
}

class MockAiBackendClient : AiBackendClient {
    override suspend fun generateChatReply(prompt: String, conversationContext: List<AiMessage>): Result<String> {
        val reply = when {
            prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                "¡Hola! ¿Cómo estás hoy? Ready to practice some conversational Spanish?"
            prompt.contains("help", ignoreCase = true) ->
                "I'm here to support you! We can practice greetings, ordering food, or common daily phrases. What would you like to try?"
            prompt.contains("weather", ignoreCase = true) ->
                "Hace un día hermoso para aprender. En español decimos: 'El clima está agradable hoy'."
            else ->
                "¡Excelente intento! That sentence is very natural. Notice how you placed the adjective after the noun. What else would you like to share?"
        }
        return Result.success(reply)
    }

    override suspend fun analyzeGrammar(text: String, targetLanguage: String): Result<GrammarCorrection> {
        val correction = if (text.contains("yo querer", ignoreCase = true)) {
            GrammarCorrection(
                originalText = text,
                correctedText = text.replace("yo querer", "yo quiero", ignoreCase = true),
                explanation = "In Spanish, the verb 'querer' must be conjugated in the present tense for the first-person singular (yo quiero), rather than left in the infinitive.",
                improvedVersion = "Me gustaría ordenar un café, por favor.",
                confidenceScore = 0.98f,
                rulesApplied = listOf("Present Tense Conjugation", "Polite Expressions")
            )
        } else {
            GrammarCorrection(
                originalText = text,
                correctedText = text,
                explanation = "Your sentence is grammatically sound! Word order and subject-verb agreement are correct.",
                improvedVersion = "$text (Native phrasing: ¡Qué bien dicho!)",
                confidenceScore = 0.95f,
                rulesApplied = listOf("Subject-Verb Concord", "Article Agreement")
            )
        }
        return Result.success(correction)
    }

    override suspend fun evaluatePronunciation(audioBytes: ByteArray, expectedText: String): Result<Float> {
        // Mock accuracy score between 85% and 98%
        return Result.success(0.92f)
    }
}
