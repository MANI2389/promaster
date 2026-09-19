package com.example.promaster.data.service

import com.example.promaster.domain.service.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe Mock TTS implementation for unit tests and fallback.
 */
class MockTextToSpeechService(
    private val available: Boolean = true
) : TextToSpeechService {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var lastSpokenText: String? = null
        private set
    var lastLanguage: String? = null
        private set
    var speakCount: Int = 0
        private set

    override fun isAvailable(): Boolean = available

    override fun speak(text: String, language: String, onComplete: (() -> Unit)?) {
        if (!available) return
        lastSpokenText = text
        lastLanguage = language
        speakCount++
        _isSpeaking.value = true
        _isSpeaking.value = false
        onComplete?.invoke()
    }

    override fun stop() {
        _isSpeaking.value = false
    }

    override fun shutdown() {
        _isSpeaking.value = false
        lastSpokenText = null
    }

    fun setSpeaking(speaking: Boolean) {
        _isSpeaking.value = speaking
    }
}
