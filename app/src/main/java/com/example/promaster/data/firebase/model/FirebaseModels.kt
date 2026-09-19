package com.example.promaster.data.firebase.model

import com.example.promaster.domain.model.*

/**
 * Firestore DTO for collection: users/{userId}
 */
data class UserDocument(
    val name: String = "",
    val email: String = "",
    val motherTongue: String = "English",
    val targetLanguage: String = "Spanish",
    val level: String = "Beginner",
    val dailyDuration: Int = 15,
    val preferredTime: String = "Morning",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(userId: String): User {
        return User(
            id = userId,
            name = name.ifBlank { "Alex Vance" },
            email = email.ifBlank { "alex@promaster.ai" },
            targetLanguage = targetLanguage.ifBlank { "Spanish" },
            nativeLanguage = motherTongue.ifBlank { "English" },
            streakDays = 1,
            totalXp = 0,
            level = 1,
            currentDay = 1,
            dailyGoalMinutes = dailyDuration
        )
    }

    companion object {
        fun fromDomain(user: User, motherTongue: String? = null, level: String? = null, preferredTime: String? = null): UserDocument {
            return UserDocument(
                name = user.name,
                email = user.email,
                motherTongue = motherTongue ?: user.nativeLanguage,
                targetLanguage = user.targetLanguage,
                level = level ?: "Beginner",
                dailyDuration = user.dailyGoalMinutes,
                preferredTime = preferredTime ?: "Morning",
                createdAt = System.currentTimeMillis()
            )
        }

        fun fromUserProfile(profile: UserProfile): UserDocument {
            return UserDocument(
                name = profile.name,
                email = "user@promaster.ai",
                motherTongue = profile.motherTongue.name,
                targetLanguage = profile.targetLanguage.name,
                level = profile.level.title,
                dailyDuration = profile.preferences.dailyDurationMinutes,
                preferredTime = profile.preferences.preferredLearningTime.label,
                createdAt = profile.createdAtTimestamp
            )
        }
    }
}

/**
 * Firestore DTO for collection: progress/{userId}
 */
data class ProgressDocument(
    val xp: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val currentDay: Int = 1,
    val completedTasks: Int = 0,
    val grammarScore: Int = 0,
    val vocabularyScore: Int = 0,
    val speakingScore: Int = 0,
    val overallProgress: Double = 0.0,
    val lastActivityDate: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProgressState {
        val effectiveLongest = maxOf(longestStreak, streak)
        return ProgressState(
            currentStreak = streak,
            bestStreak = effectiveLongest,
            longestStreak = effectiveLongest,
            totalXp = xp,
            wordsLearned = maxOf(completedTasks * 5, 20),
            rulesMastered = maxOf(grammarScore / 10, 3),
            speakingMinutes = maxOf(speakingScore / 5, 10),
            listeningMinutes = 30,
            level = maxOf(1, xp / 250 + 1),
            weeklyXp = listOf(xp / 7, xp / 6, xp / 5, xp / 4, xp / 3, xp / 2, xp),
            badges = listOf("🔥 Active Learner", "🎯 Milestone Starter"),
            lastActivityDate = lastActivityDate
        )
    }

    companion object {
        fun fromDomain(state: ProgressState): ProgressDocument {
            return ProgressDocument(
                xp = state.totalXp,
                streak = state.currentStreak,
                longestStreak = state.longestStreak,
                currentDay = state.level,
                completedTasks = state.wordsLearned / 5,
                grammarScore = state.rulesMastered * 10,
                vocabularyScore = state.wordsLearned,
                speakingScore = state.speakingMinutes * 5,
                overallProgress = (state.wordsLearned / 300.0).coerceIn(0.0, 1.0),
                lastActivityDate = if (state.lastActivityDate > 0L) state.lastActivityDate else System.currentTimeMillis()
            )
        }
    }
}

/**
 * Nested Day Item for LearningPlanDocument
 */
data class DayPlanItem(
    val day: Int = 1,
    val title: String = "",
    val estimatedMinutes: Int = 15,
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false
) {
    fun toDomain(): LearningPlan {
        return LearningPlan(
            day = day,
            title = title,
            description = "Master core concepts for Day $day",
            estimatedMinutes = estimatedMinutes,
            isCompleted = isCompleted,
            isCurrent = isCurrent,
            taskCount = 4
        )
    }

    companion object {
        fun fromDomain(plan: LearningPlan): DayPlanItem {
            return DayPlanItem(
                day = plan.day,
                title = plan.title,
                estimatedMinutes = plan.estimatedMinutes,
                isCompleted = plan.isCompleted,
                isCurrent = plan.isCurrent
            )
        }
    }
}

/**
 * Firestore DTO for collection: learningPlans/{userId}
 */
data class LearningPlanDocument(
    val planId: String = "",
    val userId: String = "",
    val targetLanguage: String = "Spanish",
    val level: String = "Beginner",
    val duration: Int = 15,
    val startDate: Long = System.currentTimeMillis(),
    val currentDay: Int = 1,
    val days: List<DayPlanItem> = emptyList()
)

/**
 * Firestore DTO for collection: dailyTasks/{userId}/tasks/{taskId}
 */
data class DailyTaskDocument(
    val taskId: String = "",
    val dayNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val category: String = "VOCABULARY",
    val estimatedMinutes: Int = 5,
    val status: String = "AVAILABLE", // LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED
    val score: Int = 0,
    val retryCount: Int = 0,
    val completedAt: Long? = null
) {
    fun toDomain(): DailyTask {
        val taskCat = try {
            TaskCategory.valueOf(category)
        } catch (_: Exception) {
            TaskCategory.VOCABULARY
        }
        val compStatus = when (status) {
            "COMPLETED" -> TaskCompletionStatus.COMPLETED
            "IN_PROGRESS" -> TaskCompletionStatus.IN_PROGRESS
            "LOCKED" -> TaskCompletionStatus.LOCKED
            else -> TaskCompletionStatus.AVAILABLE
        }
        return DailyTask(
            taskId = taskId,
            dayNumber = dayNumber,
            title = title,
            description = description,
            category = taskCat,
            estimatedMinutes = estimatedMinutes,
            completionStatus = compStatus,
            score = score,
            retryCount = retryCount,
            minPassingScore = if (taskCat in listOf(TaskCategory.SPEAKING, TaskCategory.CONVERSATION, TaskCategory.QUIZ)) 70 else 60,
            xpReward = 30 + (score / 10) * 5,
            isRequired = true
        )
    }

    companion object {
        fun fromDomain(task: DailyTask, dayNumber: Int = task.dayNumber): DailyTaskDocument {
            return DailyTaskDocument(
                taskId = task.taskId,
                dayNumber = dayNumber,
                title = task.title,
                description = task.description,
                category = task.category.name,
                estimatedMinutes = task.estimatedMinutes,
                status = task.completionStatus.name,
                score = task.score,
                retryCount = task.retryCount,
                completedAt = if (task.completionStatus == TaskCompletionStatus.COMPLETED) System.currentTimeMillis() else null
            )
        }
    }
}

/**
 * Firestore DTO for collection: conversations/{userId}/messages/{messageId}
 */
data class ConversationMessageDocument(
    val messageId: String = "",
    val role: String = "USER", // USER, AI_FRIEND, LANGUAGE_COACH, SYSTEM
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String = "CHAT" // CHAT, GRAMMAR_CHECK, PRACTICE, QUESTION
) {
    fun toDomain(): AiMessage {
        val sender = when (role) {
            "AI_FRIEND" -> MessageSender.AI_FRIEND
            "LANGUAGE_COACH" -> MessageSender.LANGUAGE_COACH
            else -> MessageSender.USER
        }
        return AiMessage(
            id = messageId,
            sender = sender,
            text = message,
            timestamp = "Just now"
        )
    }

    companion object {
        fun fromDomain(msg: AiMessage, intent: String = "CHAT"): ConversationMessageDocument {
            return ConversationMessageDocument(
                messageId = msg.id,
                role = msg.sender.name,
                message = msg.text,
                timestamp = System.currentTimeMillis(),
                intent = intent
            )
        }
    }
}

/**
 * Firestore DTO for collection: settings/{userId}
 */
data class SettingsDocument(
    val notificationsEnabled: Boolean = true,
    val dailyReminderTime: String = "08:00 AM",
    val theme: String = "SYSTEM",
    val hapticFeedback: Boolean = true,
    val studyModeDnd: Boolean = true,
    val offlineSyncEnabled: Boolean = true
)
