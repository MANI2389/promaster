package com.example.promaster.data.repository

import com.example.promaster.domain.repository.VoiceAssistantRepository
import com.example.promaster.voice.MockVoiceAssistantManager
import com.example.promaster.voice.VoiceAssistantManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockVoiceAssistantRepositoryImpl(
    private val voiceManager: VoiceAssistantManager = MockVoiceAssistantManager()
) : VoiceAssistantRepository {

    private val _isListening = MutableStateFlow(false)
    private val _isWakeWordActive = MutableStateFlow(false)
    private val _recognizedText = MutableStateFlow("")
    private val _assistantResponse = MutableStateFlow("Tap the microphone or say 'Hey Promaster' to start voice coaching.")

    override val isListening: Flow<Boolean> = _isListening.asStateFlow()
    override val isWakeWordActive: Flow<Boolean> = _isWakeWordActive.asStateFlow()
    override val recognizedText: Flow<String> = _recognizedText.asStateFlow()
    override val assistantResponse: Flow<String> = _assistantResponse.asStateFlow()

    override suspend fun startListening() {
        _isListening.value = true
        _recognizedText.value = "Listening to your Spanish pronunciation..."
        voiceManager.startListening("es") { result ->
            _recognizedText.value = result
        }
    }

    override suspend fun stopListening() {
        _isListening.value = false
        voiceManager.stopListening()
        if (_recognizedText.value.isNotEmpty()) {
            processVoiceCommand(_recognizedText.value)
        }
    }

    override suspend fun toggleWakeWord(enabled: Boolean) {
        _isWakeWordActive.value = enabled
    }

    override suspend fun processVoiceCommand(command: String): Result<String> {
        val response = when {
            command.contains("practice", ignoreCase = true) ->
                "Great! Let's practice speaking. Say: 'El sol brilla en la mañana'."
            command.contains("translate", ignoreCase = true) ->
                "Translation: 'Good morning, how are you?' is 'Buenos días, ¿cómo estás?'"
            command.contains("streak", ignoreCase = true) ->
                "Your streak is currently 7 days! Keep going to reach your next milestone badge."
            else ->
                "Heard: \"$command\". Your pronunciation was clear and accurate (94% match)!"
        }
        _assistantResponse.value = response
        return Result.success(response)
    }
}
