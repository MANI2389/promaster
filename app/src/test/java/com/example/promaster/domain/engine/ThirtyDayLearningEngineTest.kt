package com.example.promaster.domain.engine

import com.example.promaster.domain.model.LanguageLevel
import com.example.promaster.domain.model.LearningGoalType
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus
import org.junit.Assert.*
import org.junit.Test

class ThirtyDayLearningEngineTest {

    @Test
    fun engine_generatesCompleteThirtyDayCurriculum() {
        val curriculum = ThirtyDayLearningEngine.generateCompleteCurriculum(
            motherTongue = "Tamil",
            targetLanguage = "Spanish",
            level = LanguageLevel.BEGINNER,
            dailyDuration = 30,
            goal = LearningGoalType.TRAVEL
        )

        assertEquals(30, curriculum.size)
        assertEquals(1, curriculum.first().dayNumber)
        assertEquals(30, curriculum.last().dayNumber)

        // Day 1 should have foundations and greetings
        assertTrue(curriculum.first().title.contains("Foundations") || curriculum.first().title.contains("Greetings"))
        // Day 30 should be graduation milestone
        assertTrue(curriculum.last().title.contains("Graduation") || curriculum.last().title.contains("Certificate"))
    }

    @Test
    fun engine_taskModel_containsAllRequiredFields() {
        val tasks = ThirtyDayLearningEngine.generateTasksForDay(
            dayNumber = 1,
            targetLanguage = "Spanish",
            motherTongue = "English",
            level = LanguageLevel.BEGINNER,
            dailyDuration = 30,
            goal = LearningGoalType.TRAVEL
        )

        assertTrue(tasks.isNotEmpty())
        val task = tasks.first()

        assertNotNull(task.taskId)
        assertEquals(1, task.dayNumber)
        assertNotNull(task.title)
        assertNotNull(task.description)
        assertNotNull(task.category)
        assertTrue(task.estimatedMinutes > 0)
        assertEquals(TaskCompletionStatus.AVAILABLE, task.completionStatus)
        assertEquals(0, task.score)
        assertEquals(0, task.retryCount)
    }

    @Test
    fun engine_scalesTasksAcrossAllDurations() {
        val durations = listOf(10, 15, 20, 30, 45, 60)

        for (duration in durations) {
            val tasks = ThirtyDayLearningEngine.generateTasksForDay(
                dayNumber = 3,
                targetLanguage = "German",
                motherTongue = "English",
                level = LanguageLevel.INTERMEDIATE,
                dailyDuration = duration,
                goal = LearningGoalType.CAREER
            )

            when (duration) {
                10 -> assertEquals(2, tasks.size)
                15, 20 -> assertEquals(3, tasks.size)
                30 -> assertEquals(5, tasks.size)
                45 -> assertEquals(6, tasks.size)
                60 -> assertEquals(7, tasks.size)
            }
        }
    }

    @Test
    fun engine_containsAllRequiredCategoriesAcrossCurriculum() {
        val tasks60Min = ThirtyDayLearningEngine.generateTasksForDay(
            dayNumber = 5,
            targetLanguage = "French",
            motherTongue = "English",
            level = LanguageLevel.ELEMENTARY,
            dailyDuration = 60,
            goal = LearningGoalType.SOCIAL
        )

        val categoriesPresent = tasks60Min.map { it.category }.toSet()

        assertTrue(categoriesPresent.contains(TaskCategory.VOCABULARY))
        assertTrue(categoriesPresent.contains(TaskCategory.GRAMMAR))
        assertTrue(categoriesPresent.contains(TaskCategory.LISTENING))
        assertTrue(categoriesPresent.contains(TaskCategory.SPEAKING))
        assertTrue(categoriesPresent.contains(TaskCategory.CONVERSATION))
        assertTrue(categoriesPresent.contains(TaskCategory.REVIEW))
        assertTrue(categoriesPresent.contains(TaskCategory.QUIZ))
    }

    @Test
    fun engine_tailorsContentToGoal() {
        val travelCurriculum = ThirtyDayLearningEngine.generateCompleteCurriculum(
            motherTongue = "English",
            targetLanguage = "Japanese",
            level = LanguageLevel.BEGINNER,
            dailyDuration = 30,
            goal = LearningGoalType.TRAVEL
        )

        val careerCurriculum = ThirtyDayLearningEngine.generateCompleteCurriculum(
            motherTongue = "English",
            targetLanguage = "Japanese",
            level = LanguageLevel.BEGINNER,
            dailyDuration = 30,
            goal = LearningGoalType.CAREER
        )

        val day16Travel = travelCurriculum[15].title
        val day16Career = careerCurriculum[15].title

        assertTrue(day16Travel.contains("Airport") || day16Travel.contains("Travel"))
        assertTrue(day16Career.contains("Professional") || day16Career.contains("Business") || day16Career.contains("Email"))
    }
}
