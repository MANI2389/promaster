package com.example.promaster.domain.model

enum class MessageSender {
    USER,
    AI_FRIEND,
    LANGUAGE_COACH,
    SYSTEM
}

data class AiMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String,
    val translationHint: String? = null,
    val grammarTip: String? = null,
    val audioDurationSec: Int? = null
)
