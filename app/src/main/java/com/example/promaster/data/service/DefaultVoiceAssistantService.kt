package com.example.promaster.data.service

import com.example.promaster.domain.engine.CommandProcessor
import com.example.promaster.domain.engine.DefaultCommandProcessor
import com.example.promaster.domain.engine.DefaultIntentRouter
import com.example.promaster.domain.engine.IntentRouter
import com.example.promaster.domain.model.CommandExecutionResult
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.domain.model.VoiceAssistantState
import com.example.promaster.domain.service.SpeechRecognitionService
import com.example.promaster.domain.service.TextToSpeechService
import com.example.promaster.domain.service.VoiceAssistantService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * End-to-end Voice Assistant orchestrator:
 * Voice input -> Speech-to-Text -> IntentRouter -> CommandProcessor -> Service/Automation -> Result -> Text-to-Speech
 */
class DefaultVoiceAssistantService(
    private val speechService: SpeechRecognitionService = AndroidSpeechRecognitionService(),
    private val intentRouter: IntentRouter = DefaultIntentRouter(),
    private val commandProcessor: CommandProcessor = DefaultCommandProcessor(),
    private val ttsService: TextToSpeechService = AndroidTextToSpeechService(com.example.promaster.PromasterApp.applicationContextSafe),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
) : VoiceAssistantService {

    private val _state = MutableStateFlow(VoiceAssistantState())
    override val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private var lastRoutedIntentText: String? = null

    init {
        // Observe speech recognition listening flow
        coroutineScope.launch {
            speechService.isListening.collect { listening ->
                if (!listening && _state.value.isListening && !_state.value.isProcessing) {
                    _state.update { it.copy(isListening = false) }
                }
            }
        }

        // Observe TTS speaking flow
        coroutineScope.launch {
            ttsService.isSpeaking.collect { speaking ->
                _state.update { it.copy(isSpeaking = speaking) }
            }
        }
    }

    override fun startVoiceSession(language: String) {
        if (!speechService.isAvailable()) {
            val errorMsg = "Speech recognition is currently unavailable on this device."
            _state.update {
                it.copy(
                    isListening = false,
                    isProcessing = false,
                    lastResultState = CommandResultState.FAILED,
                    errorMessage = errorMsg,
                    assistantReply = errorMsg
                )
            }
            ttsService.speak(errorMsg)
            return
        }

        ttsService.stop()
        _state.update {
            it.copy(
                isListening = true,
                isProcessing = false,
                errorMessage = null,
                recognizedText = "",
                lastResultState = null
            )
        }

        speechService.startListening(
            language = language,
            onResult = { spokenText ->
                handleSpeechResult(spokenText)
            },
            onError = { errorMessage ->
                handleSpeechError(errorMessage)
            }
        )
    }

    override fun stopVoiceSession() {
        speechService.stopListening()
        ttsService.stop()
        _state.update { it.copy(isListening = false) }
    }

    override suspend fun processTextCommand(
        commandText: String,
        confirmed: Boolean
    ): CommandExecutionResult {
        _state.update {
            it.copy(
                isListening = false,
                isProcessing = true,
                recognizedText = commandText,
                errorMessage = null
            )
        }

        lastRoutedIntentText = commandText
        val intent = intentRouter.routeIntent(commandText)
        val result = commandProcessor.processCommand(intent, confirmed)

        _state.update {
            it.copy(
                isProcessing = false,
                assistantReply = result.spokenFeedback,
                lastResultState = result.state,
                requiredPermission = result.requiredPermission,
                pendingConfirmation = result.pendingConfirmationAction,
                targetDestination = result.targetDestination
            )
        }

        // Speak the feedback aloud via TTS
        ttsService.speak(result.spokenFeedback)
        return result
    }

    override fun confirmPendingAction() {
        val pending = _state.value.pendingConfirmation ?: return
        val originalText = lastRoutedIntentText ?: pending
        coroutineScope.launch {
            processTextCommand(originalText, confirmed = true)
        }
    }

    override fun cancelPendingAction() {
        _state.update {
            it.copy(
                pendingConfirmation = null,
                lastResultState = null,
                assistantReply = "Action cancelled. How else can I assist your learning today?"
            )
        }
        ttsService.speak("Action cancelled.")
    }

    override fun dismissPermissionExplanation() {
        _state.update {
            it.copy(
                requiredPermission = null,
                lastResultState = null
            )
        }
    }

    override fun shutdown() {
        stopVoiceSession()
        ttsService.shutdown()
    }

    private fun handleSpeechResult(spokenText: String) {
        coroutineScope.launch {
            processTextCommand(spokenText)
        }
    }

    private fun handleSpeechError(errorMessage: String) {
        val isPermission = errorMessage.contains("permission", ignoreCase = true)
        val resultState = if (isPermission) CommandResultState.NEEDS_PERMISSION else CommandResultState.FAILED
        val permissionName = if (isPermission) "android.permission.RECORD_AUDIO" else null

        _state.update {
            it.copy(
                isListening = false,
                isProcessing = false,
                errorMessage = errorMessage,
                lastResultState = resultState,
                requiredPermission = permissionName,
                assistantReply = errorMessage
            )
        }
        ttsService.speak(errorMessage)
    }
}
