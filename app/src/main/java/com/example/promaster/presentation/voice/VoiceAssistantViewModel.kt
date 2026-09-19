package com.example.promaster.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.service.DefaultVoiceAssistantService
import com.example.promaster.data.service.DefaultWakeWordService
import com.example.promaster.domain.model.AssistantSettings
import com.example.promaster.domain.model.VoiceAssistantState
import com.example.promaster.domain.model.WakeWordStatus
import com.example.promaster.domain.service.VoiceAssistantService
import com.example.promaster.domain.service.WakeWordService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel governing the reactive PROMASTER Voice Assistant UI state, speech lifecycle,
 * and "Hey Bro" wake-word detection service.
 */
class VoiceAssistantViewModel(
    private val voiceAssistantService: VoiceAssistantService = DefaultVoiceAssistantService(),
    private val wakeWordService: WakeWordService = DefaultWakeWordService()
) : ViewModel() {

    val uiState: StateFlow<VoiceAssistantState> = voiceAssistantService.state
    val wakeWordSettings: StateFlow<AssistantSettings> = wakeWordService.settings
    val wakeWordStatus: StateFlow<WakeWordStatus> = wakeWordService.status

    init {
        // Automatically start listening session when "Hey Bro" is detected
        wakeWordService.setWakeWordListener {
            startListening()
        }
    }

    fun toggleListening(language: String = "en") {
        if (uiState.value.isListening) {
            voiceAssistantService.stopVoiceSession()
        } else {
            voiceAssistantService.startVoiceSession(language)
        }
    }

    fun startListening(language: String = "en") {
        voiceAssistantService.startVoiceSession(language)
    }

    fun stopListening() {
        voiceAssistantService.stopVoiceSession()
    }

    fun executeCommand(commandText: String) {
        viewModelScope.launch {
            voiceAssistantService.processTextCommand(commandText)
        }
    }

    fun toggleWakeWord(enabled: Boolean, runInBackground: Boolean = false): Result<Unit> {
        return if (enabled) {
            wakeWordService.enableWakeWord(runInBackground)
        } else {
            wakeWordService.disableWakeWord()
            Result.success(Unit)
        }
    }

    fun updatePrivacySettings(storeAudio: Boolean, consent: Boolean) {
        wakeWordService.updatePrivacySettings(storeAudio, consent)
    }

    fun confirmPendingAction() {
        voiceAssistantService.confirmPendingAction()
    }

    fun cancelPendingAction() {
        voiceAssistantService.cancelPendingAction()
    }

    fun dismissPermissionExplanation() {
        voiceAssistantService.dismissPermissionExplanation()
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantService.shutdown()
        wakeWordService.shutdown()
    }
}
