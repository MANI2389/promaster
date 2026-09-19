package com.example.promaster.domain.engine

import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.LearnerMemory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersonalizedEncouragementTest {

    private val aiService = DefaultPromasterAiService()

    @Test
    fun selectNextEncouragementType_rotatesWithoutRepeatingPreviousStyle() {
        val styles = listOf(
            "CURIOSITY_GROWTH",
            "MILESTONE_CELEBRATION",
            "WEAK_AREA_EMPOWERMENT",
            "HABIT_STACKING",
            "AUTHENTIC_EFFORT"
        )

        var currentStyle: String? = null
        for (i in 0 until 15) {
            val nextStyle = aiService.selectNextEncouragementType(currentStyle)
            assertTrue("Selected style must be one of the 5 canonical styles", styles.contains(nextStyle))
            if (currentStyle != null) {
                assertNotEquals("Next style must not repeat previous style consecutively", currentStyle, nextStyle)
            }
            currentStyle = nextStyle
        }
    }

    @Test
    fun generatePersonalizedEncouragement_incorporatesWeakAreasAndStreak() {
        val memory = LearnerMemory(
            userId = "learner_1",
            streakDays = 9,
            currentDay = 12,
            weakAreas = listOf("Subjunctive Mood"),
            motherTongue = "Tamil"
        )

        // Test Weak Area Empowerment
        val (msgWeak, mtWeak) = aiService.generatePersonalizedEncouragement(
            memory = memory,
            motherTongue = "Tamil",
            encouragementType = "WEAK_AREA_EMPOWERMENT"
        )
        assertTrue("Message should mention the weak area", msgWeak.contains("Subjunctive Mood"))
        assertNotNull("Should provide Tanglish explanation for Tamil mother tongue", mtWeak)
        assertTrue(mtWeak!!.contains("Subjunctive Mood"))

        // Test Milestone Celebration
        val (msgMilestone, mtMilestone) = aiService.generatePersonalizedEncouragement(
            memory = memory,
            motherTongue = "Tamil",
            encouragementType = "MILESTONE_CELEBRATION"
        )
        assertTrue("Message should mention Day 12", msgMilestone.contains("Day 12"))
        assertTrue("Message should mention 9-day streak", msgMilestone.contains("9-day streak"))
        assertNotNull("Should provide Tanglish explanation", mtMilestone)
        assertTrue(mtMilestone!!.contains("9 days streak"))
    }

    @Test
    fun processRequest_personalizesReplyWithWeakAreasAndEncouragementRotation() = runBlocking {
        val memory = LearnerMemory(
            userId = "learner_2",
            streakDays = 5,
            currentDay = 3,
            weakAreas = listOf("Irregular Past Verbs"),
            motherTongue = "Tamil",
            targetLanguage = "English",
            lastEncouragementType = "CURIOSITY_GROWTH"
        )

        val request = AIRequest(
            prompt = "Hello coach, how are you?",
            forcedIntent = AIIntent.GENERAL_CONVERSATION,
            targetLanguage = "English",
            motherTongue = "Tamil",
            learnerMemory = memory
        )

        val result = aiService.processRequest(request)
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!

        // Verify that encouragement rotated away from CURIOSITY_GROWTH
        assertNotEquals("CURIOSITY_GROWTH", response.encouragementType)
        assertEquals("MILESTONE_CELEBRATION", response.encouragementType)

        // Verify personalized tip includes the weak area
        assertTrue("Response should include weak area coach tip", response.message.contains("Irregular Past Verbs"))

        // Verify Tanglish explanation provided
        assertNotNull("Tanglish explanation should be provided", response.motherTongueExplanation)
    }
}
