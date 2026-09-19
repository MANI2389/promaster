package com.example.promaster.domain.engine

import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AIServiceIntentDetectionTest {

    private val aiService = DefaultPromasterAiService()

    @Test
    fun detectIntent_classifiesAllTenCapabilitiesAccurately() = runBlocking {
        // 1. Grammar correction
        val grammar = aiService.detectIntent("Can you correct my grammar mistake in this sentence?")
        assertEquals(AIIntent.GRAMMAR_CORRECTION, grammar)

        // 2. Translation
        val translation = aiService.detectIntent("Please translate this phrase in Spanish")
        assertEquals(AIIntent.TRANSLATION, translation)

        // 3. Vocabulary explanation
        val vocab = aiService.detectIntent("What is the vocabulary meaning of 'desayuno'?")
        assertEquals(AIIntent.VOCABULARY_EXPLANATION, vocab)

        // 4. Speaking practice
        val speaking = aiService.detectIntent("How do I pronounce this word with a native accent?")
        assertEquals(AIIntent.SPEAKING_PRACTICE, speaking)

        // 5. Language teaching
        val teaching = aiService.detectIntent("Please teach me the rule for regular verbs")
        assertEquals(AIIntent.LANGUAGE_TEACHING, teaching)

        // 6. Personalized lessons
        val customLesson = aiService.detectIntent("Can you create a personalized custom lesson for me?")
        assertEquals(AIIntent.PERSONALIZED_LESSONS, customLesson)

        // 7. Motivation
        val motivation = aiService.detectIntent("I need motivation to keep my streak going")
        assertEquals(AIIntent.MOTIVATION, motivation)

        // 8. Learning-plan assistance
        val plan = aiService.detectIntent("How is my Day 5 curriculum schedule looking in the plan?")
        assertEquals(AIIntent.LEARNING_PLAN_ASSISTANCE, plan)

        // 9. Command intent detection
        val command = aiService.detectIntent("open vocabulary flashcards")
        assertEquals(AIIntent.COMMAND_INTENT_DETECTION, command)

        // 10. General conversation
        val convo = aiService.detectIntent("Hello, how are you doing today?")
        assertEquals(AIIntent.GENERAL_CONVERSATION, convo)
    }
}
