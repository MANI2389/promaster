package com.example.promaster.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Text-to-Speech service abstraction for audible voice feedback.
 */
interface TextToSpeechService {
    val isSpeaking: StateFlow<Boolean>
    fun isAvailable(): Boolean
    fun speak(text: String, language: String = "en", onComplete: (() -> Unit)? = null)
    fun stop()
    fun shutdown()
}
