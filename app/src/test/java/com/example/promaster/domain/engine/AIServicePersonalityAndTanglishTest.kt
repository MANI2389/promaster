package com.example.promaster.domain.engine

import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AIServicePersonalityAndTanglishTest {

    private val aiService = DefaultPromasterAiService()

    @Test
    fun grammarCorrection_withTamilMotherTongue_returnsFriendlyTanglishExplanation() = runBlocking {
        val request = AIRequest(
            prompt = "I am go to college yesterday.",
            targetLanguage = "English",
            motherTongue = "Tamil",
            forcedIntent = AIIntent.GRAMMAR_CORRECTION
        )

        val result = aiService.processRequest(request)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        // Structured fields
        assertEquals(AIIntent.GRAMMAR_CORRECTION, response.intent)
        assertNotNull(response.correction)
        assertEquals("I went to college yesterday.", response.correction)
        assertNotNull(response.explanation)
        assertNotNull(response.nextAction)
        assertTrue(response.confidence > 0.9f)

        // Personality & Tanglish verification
        assertNotNull(response.motherTongueExplanation)
        val tanglish = response.motherTongueExplanation!!
        assertTrue(
            "Should contain helpful and encouraging Tanglish phrasing",
            tanglish.contains("Romba nalla attempt", ignoreCase = true) || tanglish.contains("went")
        )
    }

    @Test
    fun conversation_withTamilMotherTongue_greetsWarmlyInTanglish() = runBlocking {
        val request = AIRequest(
            prompt = "Hello friend",
            targetLanguage = "English",
            motherTongue = "Tamil",
            forcedIntent = AIIntent.GENERAL_CONVERSATION
        )

        val result = aiService.processRequest(request)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        assertEquals(AIIntent.GENERAL_CONVERSATION, response.intent)
        assertTrue(
            "Response should be warm, friendly, and include Vanakkam or Tanglish",
            response.message.contains("Vanakkam") || response.message.contains("PROMASTER")
        )
    }

    @Test
    fun motivation_providesEncouragingStreakCoachingInTanglish() = runBlocking {
        val request = AIRequest(
            prompt = "I feel like giving up on my streak",
            targetLanguage = "English",
            motherTongue = "Tamil",
            forcedIntent = AIIntent.MOTIVATION
        )

        val result = aiService.processRequest(request)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        assertEquals(AIIntent.MOTIVATION, response.intent)
        assertTrue(response.message.contains("streak") || response.message.contains("Super"))
        assertNotNull(response.motherTongueExplanation)
    }
}
