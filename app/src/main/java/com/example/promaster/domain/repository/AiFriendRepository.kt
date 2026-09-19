package com.example.promaster.domain.repository

import com.example.promaster.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiFriendRepository {
    fun getFriendMessages(): Flow<List<AiMessage>>
    fun getCoachMessages(): Flow<List<AiMessage>>
    suspend fun sendFriendMessage(text: String): Result<AiMessage>
    suspend fun sendCoachMessage(text: String): Result<AiMessage>
    suspend fun clearHistory(isCoach: Boolean)
}
