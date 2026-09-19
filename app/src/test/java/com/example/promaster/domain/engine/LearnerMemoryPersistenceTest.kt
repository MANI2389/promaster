package com.example.promaster.domain.engine

import com.example.promaster.data.repository.MockConversationRepositoryImpl
import com.example.promaster.domain.model.LanguageLevel
import com.example.promaster.domain.model.LearnerMemory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LearnerMemoryPersistenceTest {

    @Test
    fun learnerMemory_updatesAndTracksLearningTelemetryAccurately() = runBlocking {
        val repo = MockConversationRepositoryImpl(
            initialMemory = LearnerMemory(
                userId = "learner_007",
                targetLanguage = "Spanish",
                motherTongue = "English",
                level = LanguageLevel.BEGINNER,
                currentDay = 2,
                progressPercentage = 15,
                streakDays = 3,
                weakAreas = listOf("Ser vs Estar"),
                completedLessons = listOf("Greetings")
            )
        )

        // Verify initial retrieval
        val initial = repo.getLearnerMemory("learner_007").first()
        assertEquals("learner_007", initial.userId)
        assertEquals("Spanish", initial.targetLanguage)
        assertEquals(3, initial.streakDays)
        assertEquals(listOf("Ser vs Estar"), initial.weakAreas)

        // Record a new weak area
        repo.recordWeakArea("learner_007", "Direct Object Pronouns")
        val afterWeakArea = repo.getLearnerMemory("learner_007").first()
        assertEquals(2, afterWeakArea.weakAreas.size)
        assertTrue(afterWeakArea.weakAreas.contains("Direct Object Pronouns"))

        // Duplicate weak area should not duplicate entry
        repo.recordWeakArea("learner_007", "Direct Object Pronouns")
        val afterDup = repo.getLearnerMemory("learner_007").first()
        assertEquals(2, afterDup.weakAreas.size)

        // Record a completed lesson
        repo.recordLessonCompleted("learner_007", "Ordering Food in Spanish")
        val afterLesson = repo.getLearnerMemory("learner_007").first()
        assertEquals(2, afterLesson.completedLessons.size)
        assertTrue(afterLesson.completedLessons.contains("Ordering Food in Spanish"))

        // Update memory overall
        val updated = afterLesson.copy(
            currentDay = 3,
            progressPercentage = 30,
            streakDays = 4,
            lastEncouragementType = "MILESTONE_CELEBRATION"
        )
        repo.updateLearnerMemory("learner_007", updated)

        val latest = repo.getLearnerMemory("learner_007").first()
        assertEquals(3, latest.currentDay)
        assertEquals(30, latest.progressPercentage)
        assertEquals(4, latest.streakDays)
        assertEquals("MILESTONE_CELEBRATION", latest.lastEncouragementType)
    }

    @Test
    fun learnerMemory_strictlyStoresZeroSensitiveInformation() {
        val forbiddenSubstrings = listOf(
            "password",
            "token",
            "secret",
            "apikey",
            "credential",
            "creditcard",
            "ssn",
            "pin",
            "auth"
        )

        val propertyNames = LearnerMemory::class.java.declaredFields.map { it.name.lowercase() }

        for (prop in propertyNames) {
            for (forbidden in forbiddenSubstrings) {
                assertFalse(
                    "LearnerMemory must never store sensitive property: $prop (matched: $forbidden)",
                    prop.contains(forbidden)
                )
            }
        }

        // Check required safe telemetry fields exist
        assertTrue(propertyNames.contains("targetlanguage"))
        assertTrue(propertyNames.contains("mothertongue"))
        assertTrue(propertyNames.contains("level"))
        assertTrue(propertyNames.contains("currentday"))
        assertTrue(propertyNames.contains("progresspercentage"))
        assertTrue(propertyNames.contains("streakdays"))
        assertTrue(propertyNames.contains("weakareas"))
        assertTrue(propertyNames.contains("completedlessons"))
    }
}
