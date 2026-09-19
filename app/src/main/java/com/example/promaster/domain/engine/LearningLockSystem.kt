package com.example.promaster.domain.engine

import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus

/**
 * Enforces sequential progression, anti-skipping safeguards,
 * and passing-score threshold gates for speaking, conversation, and quiz tasks.
 */
object LearningLockSystem {

    sealed class TaskValidationResult {
        data class Success(
            val updatedTasks: List<DailyTask>,
            val completedTask: DailyTask,
            val nextTaskUnlocked: Boolean,
            val isDayCompleted: Boolean,
            val message: String
        ) : TaskValidationResult()

        data class FailedPassingScore(
            val updatedTasks: List<DailyTask>,
            val task: DailyTask,
            val currentScore: Int,
            val requiredScore: Int,
            val retryCount: Int,
            val message: String
        ) : TaskValidationResult()

        data class Locked(
            val reason: String,
            val prerequisiteTaskTitle: String?
        ) : TaskValidationResult()
    }

    /**
     * Checks if a user is allowed to start or open a task.
     * Prevents skipping prerequisites.
     */
    fun canStartTask(tasks: List<DailyTask>, taskId: String): Pair<Boolean, String?> {
        val index = tasks.indexOfFirst { it.taskId == taskId }
        if (index == -1) return Pair(false, "Task not found.")

        val task = tasks[index]
        if (task.completionStatus == TaskCompletionStatus.COMPLETED) {
            return Pair(true, null) // Allow reviewing completed tasks
        }

        if (index == 0) {
            return Pair(true, null)
        }

        val previousIncompleteTask = tasks.take(index).firstOrNull { it.completionStatus != TaskCompletionStatus.COMPLETED }
        return if (previousIncompleteTask == null) {
            Pair(true, null)
        } else {
            Pair(
                false,
                "Locked: Please complete \"${previousIncompleteTask.title}\" first to unlock this exercise."
            )
        }
    }

    /**
     * Submits an exercise score and evaluates locking / progression rules.
     */
    fun submitTaskScore(
        tasks: List<DailyTask>,
        taskId: String,
        score: Int
    ): TaskValidationResult {
        val index = tasks.indexOfFirst { it.taskId == taskId }
        if (index == -1) {
            return TaskValidationResult.Locked("Task not found.", null)
        }

        val task = tasks[index]

        // Anti-skipping check: Ensure all preceding tasks are completed
        val precedingIncomplete = tasks.take(index).firstOrNull { it.completionStatus != TaskCompletionStatus.COMPLETED }
        if (precedingIncomplete != null) {
            return TaskValidationResult.Locked(
                reason = "Cannot skip required exercises. Complete \"${precedingIncomplete.title}\" first.",
                prerequisiteTaskTitle = precedingIncomplete.title
            )
        }

        val normalizedScore = score.coerceIn(0, 100)
        val requiresStrictPassing = task.category in listOf(
            TaskCategory.SPEAKING,
            TaskCategory.CONVERSATION,
            TaskCategory.QUIZ
        )
        val passingThreshold = if (requiresStrictPassing) maxOf(task.minPassingScore, 70) else 50

        // Check passing threshold gate
        if (requiresStrictPassing && normalizedScore < passingThreshold) {
            val newRetry = task.retryCount + 1
            val updatedTask = task.copy(
                score = normalizedScore,
                retryCount = newRetry,
                completionStatus = TaskCompletionStatus.IN_PROGRESS
            )
            val updatedList = tasks.toMutableList().apply {
                this[index] = updatedTask
            }
            return TaskValidationResult.FailedPassingScore(
                updatedTasks = updatedList,
                task = updatedTask,
                currentScore = normalizedScore,
                requiredScore = passingThreshold,
                retryCount = newRetry,
                message = "${task.category.displayName} score was $normalizedScore%. A minimum of $passingThreshold% is required to pass and unlock the next stage. (Attempt #$newRetry)"
            )
        }

        // Successfully passed
        val completedTask = task.copy(
            score = normalizedScore,
            completionStatus = TaskCompletionStatus.COMPLETED
        )

        val updatedList = tasks.toMutableList()
        updatedList[index] = completedTask

        // Unlock next task if one exists and was locked
        var nextTaskUnlocked = false
        if (index + 1 < updatedList.size) {
            val nextTask = updatedList[index + 1]
            if (nextTask.completionStatus == TaskCompletionStatus.LOCKED) {
                updatedList[index + 1] = nextTask.copy(completionStatus = TaskCompletionStatus.AVAILABLE)
                nextTaskUnlocked = true
            }
        }

        val allDayTasksCompleted = updatedList.all { it.completionStatus == TaskCompletionStatus.COMPLETED }

        return TaskValidationResult.Success(
            updatedTasks = updatedList,
            completedTask = completedTask,
            nextTaskUnlocked = nextTaskUnlocked,
            isDayCompleted = allDayTasksCompleted,
            message = "Exercise completed with $normalizedScore%! +${completedTask.xpReward} XP earned."
        )
    }

    /**
     * Enforces that lock states across a task list are structurally valid.
     */
    fun refreshLockStates(tasks: List<DailyTask>): List<DailyTask> {
        var previousWasCompleted = true
        return tasks.mapIndexed { index, task ->
            if (index == 0) {
                if (task.completionStatus == TaskCompletionStatus.LOCKED) {
                    task.copy(completionStatus = TaskCompletionStatus.AVAILABLE)
                } else {
                    task
                }
            } else {
                when {
                    task.completionStatus == TaskCompletionStatus.COMPLETED -> {
                        previousWasCompleted = true
                        task
                    }
                    previousWasCompleted -> {
                        previousWasCompleted = false
                        if (task.completionStatus == TaskCompletionStatus.LOCKED) {
                            task.copy(completionStatus = TaskCompletionStatus.AVAILABLE)
                        } else {
                            task
                        }
                    }
                    else -> {
                        task.copy(completionStatus = TaskCompletionStatus.LOCKED)
                    }
                }
            }
        }
    }

    /**
     * Calculates completion percentage for a given set of tasks.
     */
    fun calculatePercentage(tasks: List<DailyTask>): Float {
        if (tasks.isEmpty()) return 0f
        val completed = tasks.count { it.completionStatus == TaskCompletionStatus.COMPLETED }
        return (completed.toFloat() / tasks.size.toFloat()).coerceIn(0f, 1f)
    }
}
