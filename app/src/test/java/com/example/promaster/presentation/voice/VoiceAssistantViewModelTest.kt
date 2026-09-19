package com.example.promaster.presentation.voice

import com.example.promaster.data.service.DefaultVoiceAssistantService
import com.example.promaster.data.service.MockTextToSpeechService
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.domain.service.SpeechRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceAssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockSpeechService: MockSpeechRecognitionService
    private lateinit var mockTtsService: MockTextToSpeechService
    private lateinit var assistantService: DefaultVoiceAssistantService
    private lateinit var viewModel: VoiceAssistantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSpeechService = MockSpeechRecognitionService()
        mockTtsService = MockTextToSpeechService()
        assistantService = DefaultVoiceAssistantService(
            speechService = mockSpeechService,
            ttsService = mockTtsService,
            coroutineScope = testScope
        )
        viewModel = VoiceAssistantViewModel(assistantService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdleWithGreeting() {
        val state = viewModel.uiState.value
        assertFalse(state.isListening)
        assertFalse(state.isProcessing)
        assertFalse(state.isSpeaking)
        assertTrue(state.assistantReply.isNotBlank())
    }

    @Test
    fun toggleListening_startsAndStopsSession() {
        viewModel.toggleListening()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.toggleListening()
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun executeCommand_processesCommandAndUpdatesUi() {
        viewModel.executeCommand("Open settings.")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Open settings.", state.recognizedText)
        assertEquals(CommandResultState.SUCCESS, state.lastResultState)
        assertEquals("settings", state.targetDestination)
        assertTrue(state.assistantReply.contains("Opening settings", ignoreCase = true))
    }

    @Test
    fun dismissPermissionExplanation_clearsPermissionState() {
        viewModel.executeCommand("Start speaking practice.")
        testDispatcher.scheduler.advanceUntilIdle()

        // By default DefaultCommandProcessor checks PermissionChecker. If permission was needed, dismiss clears it
        viewModel.dismissPermissionExplanation()
        assertNull(viewModel.uiState.value.requiredPermission)
    }

    private class MockSpeechRecognitionService : SpeechRecognitionService {
        private val _isListening = MutableStateFlow(false)
        override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
        override fun isAvailable(): Boolean = true
        override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
            _isListening.value = true
        }
        override fun stopListening() {
            _isListening.value = false
        }
    }
}
