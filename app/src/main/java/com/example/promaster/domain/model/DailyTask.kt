package com.example.promaster.domain.model

enum class TaskCategory(val displayName: String, val icon: String) {
    VOCABULARY("Vocabulary", "📖"),
    GRAMMAR("Grammar", "🧠"),
    LISTENING("Listening", "🎧"),
    SPEAKING("Speaking", "🎙️"),
    CONVERSATION("Conversation", "💬"),
    REVIEW("Review", "🔄"),
    QUIZ("Quiz", "🏆"),
    AI_PRACTICE("AI Practice", "🤖")
}

enum class TaskCompletionStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED
}

data class DailyTask(
    val taskId: String,
    val dayNumber: Int = 1,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val estimatedMinutes: Int = 5,
    val completionStatus: TaskCompletionStatus = TaskCompletionStatus.AVAILABLE,
    val score: Int = 0,
    val retryCount: Int = 0,
    val minPassingScore: Int = 70,
    val xpReward: Int = 30,
    val isRequired: Boolean = true
) {
    val id: String get() = taskId
    val isCompleted: Boolean get() = (completionStatus == TaskCompletionStatus.COMPLETED)
    val durationMinutes: Int get() = estimatedMinutes

    constructor(
        id: String,
        title: String,
        description: String,
        category: TaskCategory,
        xpReward: Int,
        isCompleted: Boolean,
        durationMinutes: Int
    ) : this(
        taskId = id,
        dayNumber = 1,
        title = title,
        description = description,
        category = category,
        estimatedMinutes = durationMinutes,
        completionStatus = if (isCompleted) TaskCompletionStatus.COMPLETED else TaskCompletionStatus.AVAILABLE,
        score = if (isCompleted) 100 else 0,
        retryCount = 0,
        minPassingScore = 70,
        xpReward = xpReward,
        isRequired = true
    )
}
