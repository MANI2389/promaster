package com.example.promaster.domain.engine

import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus
import org.junit.Assert.*
import org.junit.Test

class LearningLockSystemTest {

    private fun createSampleTasks(): List<DailyTask> {
        return listOf(
            DailyTask(
                taskId = "t1",
                dayNumber = 1,
                title = "Vocabulary",
                description = "Learn words",
                category = TaskCategory.VOCABULARY,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.AVAILABLE
            ),
            DailyTask(
                taskId = "t2",
                dayNumber = 1,
                title = "Grammar",
                description = "Learn rules",
                category = TaskCategory.GRAMMAR,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.LOCKED
            ),
            DailyTask(
                taskId = "t3",
                dayNumber = 1,
                title = "Speaking",
                description = "Pronounce words",
                category = TaskCategory.SPEAKING,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.LOCKED,
                minPassingScore = 70
            )
        )
    }

    @Test
    fun canStartTask_allowsFirstTask_andBlocksLockedTasks() {
        val tasks = createSampleTasks()

        val (canStartT1, _) = LearningLockSystem.canStartTask(tasks, "t1")
        assertTrue(canStartT1)

        val (canStartT2, reasonT2) = LearningLockSystem.canStartTask(tasks, "t2")
        assertFalse(canStartT2)
        assertNotNull(reasonT2)
        assertTrue(reasonT2!!.contains("Vocabulary"))
    }

    @Test
    fun submitTaskScore_passingScore_unlocksNextTask() {
        val tasks = createSampleTasks()

        val result = LearningLockSystem.submitTaskScore(tasks, "t1", 90)
        assertTrue(result is LearningLockSystem.TaskValidationResult.Success)

        val success = result as LearningLockSystem.TaskValidationResult.Success
        assertEquals(TaskCompletionStatus.COMPLETED, success.completedTask.completionStatus)
        assertTrue(success.nextTaskUnlocked)

        // Verify task 2 in updated list is now AVAILABLE
        val updatedT2 = success.updatedTasks.find { it.taskId == "t2" }
        assertNotNull(updatedT2)
        assertEquals(TaskCompletionStatus.AVAILABLE, updatedT2!!.completionStatus)
    }

    @Test
    fun submitTaskScore_antiSkipping_rejectsSubmittingLockedTask() {
        val tasks = createSampleTasks()

        // Attempting to submit T2 without completing T1
        val result = LearningLockSystem.submitTaskScore(tasks, "t2", 95)
        assertTrue(result is LearningLockSystem.TaskValidationResult.Locked)

        val locked = result as LearningLockSystem.TaskValidationResult.Locked
        assertTrue(locked.reason.contains("Cannot skip"))
    }

    @Test
    fun submitTaskScore_speakingTask_requires70PercentToPass() {
        // Complete T1 and T2 first
        val tasks = listOf(
            DailyTask(
                taskId = "t1",
                dayNumber = 1,
                title = "Vocab",
                description = "",
                category = TaskCategory.VOCABULARY,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.COMPLETED
            ),
            DailyTask(
                taskId = "t2",
                dayNumber = 1,
                title = "Speaking",
                description = "",
                category = TaskCategory.SPEAKING,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.AVAILABLE,
                minPassingScore = 70
            ),
            DailyTask(
                taskId = "t3",
                dayNumber = 1,
                title = "Quiz",
                description = "",
                category = TaskCategory.QUIZ,
                estimatedMinutes = 5,
                completionStatus = TaskCompletionStatus.LOCKED,
                minPassingScore = 70
            )
        )

        // Submit failing score of 60% on Speaking task
        val failResult = LearningLockSystem.submitTaskScore(tasks, "t2", 60)
        assertTrue(failResult is LearningLockSystem.TaskValidationResult.FailedPassingScore)

        val failed = failResult as LearningLockSystem.TaskValidationResult.FailedPassingScore
        assertEquals(1, failed.retryCount)
        assertEquals(TaskCompletionStatus.IN_PROGRESS, failed.task.completionStatus)

        // Ensure T3 remains LOCKED
        val t3AfterFail = failed.updatedTasks.find { it.taskId == "t3" }
        assertEquals(TaskCompletionStatus.LOCKED, t3AfterFail!!.completionStatus)

        // Submit passing score of 85% on Speaking task retry
        val passResult = LearningLockSystem.submitTaskScore(failed.updatedTasks, "t2", 85)
        assertTrue(passResult is LearningLockSystem.TaskValidationResult.Success)

        val passed = passResult as LearningLockSystem.TaskValidationResult.Success
        assertEquals(TaskCompletionStatus.COMPLETED, passed.completedTask.completionStatus)
        assertEquals(85, passed.completedTask.score)

        // T3 should now be unlocked to AVAILABLE
        val t3AfterPass = passed.updatedTasks.find { it.taskId == "t3" }
        assertEquals(TaskCompletionStatus.AVAILABLE, t3AfterPass!!.completionStatus)
    }

    @Test
    fun calculatePercentage_tracksProgressAccurately() {
        val tasks = listOf(
            DailyTask(taskId = "t1", dayNumber = 1, title = "T1", description = "", category = TaskCategory.VOCABULARY, estimatedMinutes = 5, completionStatus = TaskCompletionStatus.COMPLETED),
            DailyTask(taskId = "t2", dayNumber = 1, title = "T2", description = "", category = TaskCategory.GRAMMAR, estimatedMinutes = 5, completionStatus = TaskCompletionStatus.COMPLETED),
            DailyTask(taskId = "t3", dayNumber = 1, title = "T3", description = "", category = TaskCategory.SPEAKING, estimatedMinutes = 5, completionStatus = TaskCompletionStatus.AVAILABLE),
            DailyTask(taskId = "t4", dayNumber = 1, title = "T4", description = "", category = TaskCategory.QUIZ, estimatedMinutes = 5, completionStatus = TaskCompletionStatus.LOCKED)
        )

        val percentage = LearningLockSystem.calculatePercentage(tasks)
        assertEquals(0.5f, percentage, 0.001f)
    }
}
