package com.example.promaster.data.repository

import com.example.promaster.ai.AiBackendClient
import com.example.promaster.ai.MockAiBackendClient
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.MessageSender
import com.example.promaster.domain.repository.AiFriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MockAiFriendRepositoryImpl(
    private val aiClient: AiBackendClient = MockAiBackendClient()
) : AiFriendRepository {

    private val _friendMessages = MutableStateFlow(MockDataProvider.friendMessages.toList())
    private val _coachMessages = MutableStateFlow(MockDataProvider.coachMessages.toList())

    override fun getFriendMessages(): Flow<List<AiMessage>> = _friendMessages.asStateFlow()

    override fun getCoachMessages(): Flow<List<AiMessage>> = _coachMessages.asStateFlow()

    private fun currentTime(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    override suspend fun sendFriendMessage(text: String): Result<AiMessage> {
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = text,
            timestamp = currentTime()
        )
        val updatedList = _friendMessages.value + userMsg
        _friendMessages.value = updatedList

        val aiResult = aiClient.generateChatReply(text, updatedList)
        val replyText = aiResult.getOrDefault("¡Me encanta platicar contigo! Cuéntame más sobre lo que te gusta hacer.")
        val aiMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.AI_FRIEND,
            text = replyText,
            timestamp = currentTime()
        )
        _friendMessages.value = _friendMessages.value + aiMsg
        return Result.success(aiMsg)
    }

    override suspend fun sendCoachMessage(text: String): Result<AiMessage> {
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = text,
            timestamp = currentTime()
        )
        val updatedList = _coachMessages.value + userMsg
        _coachMessages.value = updatedList

        val aiResult = aiClient.analyzeGrammar(text, "Spanish")
        val correction = aiResult.getOrNull()

        val coachMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.LANGUAGE_COACH,
            text = correction?.explanation ?: "Buen trabajo formulando la oración. Sigue practicando estructuras similares.",
            timestamp = currentTime(),
            grammarTip = if (correction != null && correction.rulesApplied.isNotEmpty())
                "Rule: " + correction.rulesApplied.joinToString(", ") else null
        )
        _coachMessages.value = _coachMessages.value + coachMsg
        return Result.success(coachMsg)
    }

    override suspend fun clearHistory(isCoach: Boolean) {
        if (isCoach) {
            _coachMessages.value = emptyList()
        } else {
            _friendMessages.value = emptyList()
        }
    }
}
