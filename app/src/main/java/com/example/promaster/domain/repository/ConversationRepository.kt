package com.example.promaster.domain.repository

import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.LearnerMemory
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getConversationMessages(userId: String): Flow<List<AiMessage>>
    suspend fun saveMessage(userId: String, message: AiMessage): Result<Unit>
    suspend fun clearHistory(userId: String): Result<Unit>
    fun getLearnerMemory(userId: String): Flow<LearnerMemory>
    suspend fun updateLearnerMemory(userId: String, memory: LearnerMemory): Result<Unit>
    suspend fun recordWeakArea(userId: String, weakArea: String): Result<Unit>
    suspend fun recordLessonCompleted(userId: String, lessonTitle: String): Result<Unit>
}
