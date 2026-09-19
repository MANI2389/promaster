package com.example.promaster.data.repository

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.LanguageLevel
import com.example.promaster.domain.model.LearnerMemory
import com.example.promaster.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockConversationRepositoryImpl(
    initialMessages: List<AiMessage> = MockDataProvider.friendMessages,
    initialMemory: LearnerMemory = defaultTestMemory
) : ConversationRepository {

    companion object {
        val defaultTestMemory = LearnerMemory(
            userId = "test_user_001",
            targetLanguage = "Spanish",
            motherTongue = "Tamil",
            level = LanguageLevel.BEGINNER,
            currentDay = 3,
            progressPercentage = 25,
            streakDays = 5,
            weakAreas = listOf("Past Tense Conjugation", "Articles el/la"),
            completedLessons = listOf("Present Tense -AR Verbs", "Day 1 Greetings")
        )
    }

    private val _messages = MutableStateFlow(initialMessages)
    private val _memory = MutableStateFlow(initialMemory)

    override fun getConversationMessages(userId: String): Flow<List<AiMessage>> = _messages.asStateFlow()

    override suspend fun saveMessage(userId: String, message: AiMessage): Result<Unit> {
        _messages.update { it + message }
        return Result.success(Unit)
    }

    override suspend fun clearHistory(userId: String): Result<Unit> {
        _messages.value = emptyList()
        return Result.success(Unit)
    }

    override fun getLearnerMemory(userId: String): Flow<LearnerMemory> = _memory.asStateFlow()

    override suspend fun updateLearnerMemory(userId: String, memory: LearnerMemory): Result<Unit> {
        _memory.value = memory
        return Result.success(Unit)
    }

    override suspend fun recordWeakArea(userId: String, weakArea: String): Result<Unit> {
        _memory.update { current ->
            if (!current.weakAreas.contains(weakArea)) {
                current.copy(weakAreas = current.weakAreas + weakArea)
            } else current
        }
        return Result.success(Unit)
    }

    override suspend fun recordLessonCompleted(userId: String, lessonTitle: String): Result<Unit> {
        _memory.update { current ->
            if (!current.completedLessons.contains(lessonTitle)) {
                current.copy(completedLessons = current.completedLessons + lessonTitle)
            } else current
        }
        return Result.success(Unit)
    }
}
