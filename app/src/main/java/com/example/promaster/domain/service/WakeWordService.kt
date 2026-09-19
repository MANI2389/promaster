package com.example.promaster.domain.service

import com.example.promaster.domain.model.AssistantSettings
import com.example.promaster.domain.model.WakeWordStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Service orchestrating wake-word lifecycle, settings, background execution, and privacy rules.
 */
interface WakeWordService {
    val settings: StateFlow<AssistantSettings>
    val status: StateFlow<WakeWordStatus>
    val isListening: StateFlow<Boolean>

    /**
     * Enables wake-word detection for "Hey Bro".
     * Validates microphone permissions and engine availability.
     */
    fun enableWakeWord(runInBackground: Boolean = false): Result<Unit>

    /**
     * Disables wake-word detection.
     * POLICY: Stops listening immediately and cancels any foreground service.
     */
    fun disableWakeWord()

    /**
     * Updates user privacy preferences regarding audio recording retention and consent.
     */
    fun updatePrivacySettings(storeAudio: Boolean, consent: Boolean)

    fun setWakeWordListener(onTriggered: () -> Unit)
    fun shutdown()
}
