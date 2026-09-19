package com.example.promaster.voice

interface VoiceAssistantManager {
    fun isWakeWordEngineReady(): Boolean
    suspend fun startListening(languageCode: String, onResult: (String) -> Unit)
    suspend fun stopListening()
    suspend fun synthesizeSpeech(text: String, languageCode: String)
}

class MockVoiceAssistantManager : VoiceAssistantManager {
    private var isListening = false

    override fun isWakeWordEngineReady(): Boolean = false // Placeholders for future offline Porcupine / Picovoice

    override suspend fun startListening(languageCode: String, onResult: (String) -> Unit) {
        isListening = true
        onResult("Practice speaking: 'Buenos días, me gustaría aprender'")
    }

    override suspend fun stopListening() {
        isListening = false
    }

    override suspend fun synthesizeSpeech(text: String, languageCode: String) {
        // Mock TTS player stub
    }
}
