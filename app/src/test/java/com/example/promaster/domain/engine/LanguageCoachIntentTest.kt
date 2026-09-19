package com.example.promaster.domain.engine

import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.LearnerMemory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LanguageCoachIntentTest {

    private val aiService = DefaultPromasterAiService()

    @Test
    fun detectIntent_accuratelyDetectsAllSevenCoachModes() = runBlocking {
        // 1. Normal conversation
        assertEquals(AIIntent.GENERAL_CONVERSATION, aiService.detectIntent("Hello, how was your weekend?"))
        assertEquals(AIIntent.GENERAL_CONVERSATION, aiService.detectIntent("Good morning!"))

        // 2. English / Target practice
        assertEquals(AIIntent.LANGUAGE_TEACHING, aiService.detectIntent("I want to do English practice today"))
        assertEquals(AIIntent.LANGUAGE_TEACHING, aiService.detectIntent("Can we do conversation practice in English?"))

        // 3. Grammar help
        assertEquals(AIIntent.GRAMMAR_CORRECTION, aiService.detectIntent("I need grammar help with this sentence"))
        assertEquals(AIIntent.GRAMMAR_CORRECTION, aiService.detectIntent("Can you correct my grammar mistake?"))

        // 4. Vocabulary
        assertEquals(AIIntent.VOCABULARY_EXPLANATION, aiService.detectIntent("Explain vocabulary for 'desayuno'"))
        assertEquals(AIIntent.VOCABULARY_EXPLANATION, aiService.detectIntent("What is the vocab definition of 'hablar'?"))

        // 5. Translation
        assertEquals(AIIntent.TRANSLATION, aiService.detectIntent("Please do a translation for 'I went to college yesterday'"))
        assertEquals(AIIntent.TRANSLATION, aiService.detectIntent("Translate this into Spanish"))

        // 6. Speaking practice
        assertEquals(AIIntent.SPEAKING_PRACTICE, aiService.detectIntent("Let's do speaking practice for my accent"))
        assertEquals(AIIntent.SPEAKING_PRACTICE, aiService.detectIntent("How do I pronounce this correctly?"))

        // 7. Daily lesson help
        assertEquals(AIIntent.LEARNING_PLAN_ASSISTANCE, aiService.detectIntent("Can you give me daily lesson help?"))
        assertEquals(AIIntent.LEARNING_PLAN_ASSISTANCE, aiService.detectIntent("What is today's lesson plan on Day 4?"))
    }

    @Test
    fun processRequest_executesAllSevenCoachModesWithStructuredResponse() = runBlocking {
        val testMemory = LearnerMemory(
            userId = "test_learner",
            targetLanguage = "English",
            motherTongue = "Tamil",
            streakDays = 6,
            currentDay = 4,
            weakAreas = listOf("Past Tense")
        )

        val coachModes = listOf(
            AIIntent.GENERAL_CONVERSATION to "Hello coach!",
            AIIntent.LANGUAGE_TEACHING to "Let's do English practice",
            AIIntent.GRAMMAR_CORRECTION to "I am go to college yesterday",
            AIIntent.VOCABULARY_EXPLANATION to "Meaning of breakfast",
            AIIntent.TRANSLATION to "Translate I went to college yesterday",
            AIIntent.SPEAKING_PRACTICE to "Check my pronunciation of library",
            AIIntent.LEARNING_PLAN_ASSISTANCE to "Help me with today's lesson"
        )

        for ((mode, prompt) in coachModes) {
            val request = AIRequest(
                prompt = prompt,
                targetLanguage = "English",
                motherTongue = "Tamil",
                forcedIntent = mode,
                learnerMemory = testMemory
            )
            val result = aiService.processRequest(request)
            assertTrue("Expected success for mode $mode", result.isSuccess)
            val response = result.getOrNull()!!
            assertEquals("Response intent should match requested coach mode", mode, response.intent)
            assertTrue("Response message must not be blank", response.message.isNotBlank())
            assertNotNull("Encouragement type should be selected", response.encouragementType)
        }
    }
}
