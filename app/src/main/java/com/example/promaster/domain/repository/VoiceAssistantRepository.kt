package com.example.promaster.domain.repository

import kotlinx.coroutines.flow.Flow

interface VoiceAssistantRepository {
    val isListening: Flow<Boolean>
    val isWakeWordActive: Flow<Boolean>
    val recognizedText: Flow<String>
    val assistantResponse: Flow<String>
    suspend fun startListening()
    suspend fun stopListening()
    suspend fun toggleWakeWord(enabled: Boolean)
    suspend fun processVoiceCommand(command: String): Result<String>
}
