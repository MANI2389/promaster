package com.example.promaster.domain.service

import com.example.promaster.domain.model.CommandExecutionResult
import com.example.promaster.domain.model.VoiceAssistantState
import kotlinx.coroutines.flow.StateFlow

/**
 * End-to-end Voice Assistant orchestrator interface.
 * Coordinates Voice Input -> Speech-to-Text -> IntentRouter -> CommandProcessor -> Result -> TTS.
 */
interface VoiceAssistantService {
    val state: StateFlow<VoiceAssistantState>

    fun startVoiceSession(language: String = "en")
    fun stopVoiceSession()

    suspend fun processTextCommand(
        commandText: String,
        confirmed: Boolean = false
    ): CommandExecutionResult

    fun confirmPendingAction()
    fun cancelPendingAction()
    fun dismissPermissionExplanation()
    fun shutdown()
}
