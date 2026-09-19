package com.example.promaster.domain.engine

import com.example.promaster.data.service.DefaultVoiceAssistantService
import com.example.promaster.data.service.MockTextToSpeechService
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.domain.service.SpeechRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceAssistantServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockSpeechService: MockSpeechRecognitionService
    private lateinit var mockTtsService: MockTextToSpeechService
    private lateinit var service: DefaultVoiceAssistantService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSpeechService = MockSpeechRecognitionService(available = true)
        mockTtsService = MockTextToSpeechService(available = true)
        service = DefaultVoiceAssistantService(
            speechService = mockSpeechService,
            intentRouter = DefaultIntentRouter(),
            commandProcessor = DefaultCommandProcessor(),
            ttsService = mockTtsService,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun endToEnd_processTextCommand_executesPipelineAndSpeaksFeedback() = runBlocking {
        val result = service.processTextCommand("Start my English lesson.")
        assertEquals(CommandResultState.SUCCESS, result.state)

        val state = service.state.value
        assertEquals("Start my English lesson.", state.recognizedText)
        assertEquals(CommandResultState.SUCCESS, state.lastResultState)
        assertEquals("ai_conversation", state.targetDestination)

        // Verify TTS spoken feedback was triggered
        assertNotNull(mockTtsService.lastSpokenText)
        assertTrue(mockTtsService.lastSpokenText!!.contains("English lesson", ignoreCase = true))
        assertEquals(1, mockTtsService.speakCount)
    }

    @Test
    fun startVoiceSession_handlesUnavailableSpeechServiceGracefully() {
        val unavailableSpeech = MockSpeechRecognitionService(available = false)
        val unavailableAssistant = DefaultVoiceAssistantService(
            speechService = unavailableSpeech,
            ttsService = mockTtsService,
            coroutineScope = testScope
        )

        unavailableAssistant.startVoiceSession()
        val state = unavailableAssistant.state.value
        assertFalse(state.isListening)
        assertEquals(CommandResultState.FAILED, state.lastResultState)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun startVoiceSession_onRecognizedSpeech_triggersProcessingAndTts() {
        service.startVoiceSession()
        assertTrue(service.state.value.isListening)

        // Simulate speech recognized callback
        mockSpeechService.simulateResult("Open settings.")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = service.state.value
        assertFalse(state.isListening)
        assertFalse(state.isProcessing)
        assertEquals(CommandResultState.SUCCESS, state.lastResultState)
        assertEquals("settings", state.targetDestination)
        assertEquals("Opening settings.", mockTtsService.lastSpokenText)
    }

    @Test
    fun confirmationFlow_confirmPendingAction_executesAction() = runBlocking {
        // Run command requiring confirmation (alarm with no time specified)
        service.processTextCommand("Set an alarm.")
        val state = service.state.value
        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, state.lastResultState)
        assertNotNull(state.pendingConfirmation)

        // Confirm
        service.confirmPendingAction()
        testDispatcher.scheduler.advanceUntilIdle()

        val confirmedState = service.state.value
        assertEquals(CommandResultState.SUCCESS, confirmedState.lastResultState)
        assertNull(confirmedState.pendingConfirmation)
    }

    @Test
    fun cancelPendingAction_clearsPendingState() = runBlocking {
        service.processTextCommand("Set an alarm.")
        assertTrue(service.state.value.pendingConfirmation != null)

        service.cancelPendingAction()
        assertNull(service.state.value.pendingConfirmation)
        assertEquals("Action cancelled.", mockTtsService.lastSpokenText)
    }

    @Test
    fun handleSpeechError_permissionDenied_updatesStateToNeedsPermission() {
        service.startVoiceSession()
        mockSpeechService.simulateError("Microphone permission is required for speaking practice.")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = service.state.value
        assertEquals(CommandResultState.NEEDS_PERMISSION, state.lastResultState)
        assertEquals("android.permission.RECORD_AUDIO", state.requiredPermission)
        assertFalse(state.isListening)
    }

    @Test
    fun handleSpeechError_noSpeech_updatesStateToFailed() {
        service.startVoiceSession()
        mockSpeechService.simulateError("No speech detected. Please try again.")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = service.state.value
        assertEquals(CommandResultState.FAILED, state.lastResultState)
        assertNull(state.requiredPermission)
        assertFalse(state.isListening)
    }

    @Test
    fun ttsState_speakingFlow_reflectsInVoiceAssistantState() {
        assertFalse(service.state.value.isSpeaking)
        mockTtsService.setSpeaking(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(service.state.value.isSpeaking)

        mockTtsService.setSpeaking(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(service.state.value.isSpeaking)
    }

    @Test
    fun dismissPermissionExplanation_clearsPermissionState() {
        service.startVoiceSession()
        mockSpeechService.simulateError("Microphone permission is required for speaking practice.")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("android.permission.RECORD_AUDIO", service.state.value.requiredPermission)

        service.dismissPermissionExplanation()
        assertNull(service.state.value.requiredPermission)
    }

    // Helper mock for speech recognition
    private class MockSpeechRecognitionService(
        private val available: Boolean = true
    ) : SpeechRecognitionService {
        private val _isListening = MutableStateFlow(false)
        override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

        private var onResultCallback: ((String) -> Unit)? = null
        private var onErrorCallback: ((String) -> Unit)? = null

        override fun isAvailable(): Boolean = available

        override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
            if (!available) {
                onError("Speech service unavailable")
                return
            }
            _isListening.value = true
            onResultCallback = onResult
            onErrorCallback = onError
        }

        override fun stopListening() {
            _isListening.value = false
        }

        fun simulateResult(text: String) {
            _isListening.value = false
            onResultCallback?.invoke(text)
        }

        fun simulateError(errorMessage: String) {
            _isListening.value = false
            onErrorCallback?.invoke(errorMessage)
        }
    }
}
