package com.example.promaster.domain.engine

import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.data.service.DefaultGrammarService
import com.example.promaster.data.service.DefaultPronunciationService
import com.example.promaster.data.service.DefaultSpeakingEvaluationService
import com.example.promaster.data.service.DefaultTranslationService
import com.example.promaster.domain.model.SpeakingSessionState
import com.example.promaster.domain.service.SpeechRecognitionService
import com.example.promaster.presentation.learning.SpeakingPracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeakingServiceAvailabilityTest {

    private val testDispatcher = StandardTestDispatcher()

    private class UnavailableSpeechRecognitionService : SpeechRecognitionService {
        override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
        override fun isAvailable(): Boolean = false
        override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
            onError("Speaking analysis is currently unavailable.")
        }
        override fun stopListening() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startListening_whenSpeechRecognitionUnavailable_showsUnavailableAndDoesNotCompleteTask() = runTest {
        val speechService = UnavailableSpeechRecognitionService()
        val evaluationService = DefaultSpeakingEvaluationService()
        val learningRepo = MockLearningRepositoryImpl()

        val viewModel = SpeakingPracticeViewModel(
            speechService = speechService,
            evaluationService = evaluationService,
            learningRepo = learningRepo
        )

        viewModel.startListening()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SpeakingSessionState.SERVICE_UNAVAILABLE, state.sessionState)
        assertEquals("Speaking analysis is currently unavailable.", state.errorMessage)
        assertFalse("Task must not be completed when service is unavailable", state.isTaskCompleted)
        assertFalse("Verification must be false", state.isVerified)
    }

    @Test
    fun processSpeech_whenEvaluationServiceUnavailable_showsUnavailableAndDoesNotCompleteTask() = runTest {
        val speechService = object : SpeechRecognitionService {
            override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
            override fun isAvailable(): Boolean = true
            override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {}
            override fun stopListening() {}
        }

        // Grammar service marked offline
        val grammarService = DefaultGrammarService(available = false)
        val evaluationService = DefaultSpeakingEvaluationService(grammarService = grammarService)
        val learningRepo = MockLearningRepositoryImpl()

        val viewModel = SpeakingPracticeViewModel(
            speechService = speechService,
            evaluationService = evaluationService,
            learningRepo = learningRepo
        )

        viewModel.processSpokenText("I am go to college yesterday.")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SpeakingSessionState.SERVICE_UNAVAILABLE, state.sessionState)
        assertEquals("Speaking analysis is currently unavailable.", state.errorMessage)
        assertFalse(state.isTaskCompleted)
        assertFalse(state.isVerified)
    }

    @Test
    fun repetitionVerification_whenPronunciationServiceUnavailable_doesNotClaimCorrect() = runTest {
        val grammarService = DefaultGrammarService(available = true)
        val pronunciationService = DefaultPronunciationService(available = true)
        val translationService = DefaultTranslationService(available = true)
        val evaluationService = DefaultSpeakingEvaluationService(
            grammarService = grammarService,
            pronunciationService = pronunciationService,
            translationService = translationService
        )
        val speechService = object : SpeechRecognitionService {
            override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
            override fun isAvailable(): Boolean = true
            override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {}
            override fun stopListening() {}
        }

        val viewModel = SpeakingPracticeViewModel(
            speechService = speechService,
            evaluationService = evaluationService,
            learningRepo = MockLearningRepositoryImpl()
        )

        // Initial speech -> correction required
        viewModel.processSpokenText("I am go to college yesterday.")
        testScheduler.advanceUntilIdle()
        assertEquals(SpeakingSessionState.CORRECTION_REQUIRED, viewModel.uiState.value.sessionState)

        // Pronunciation service goes offline before repetition
        pronunciationService.setAvailable(false)

        // User repeats
        viewModel.processSpokenText("I went to college yesterday.")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SpeakingSessionState.SERVICE_UNAVAILABLE, state.sessionState)
        assertEquals("Speaking analysis is currently unavailable.", state.errorMessage)
        assertFalse("Must never claim correct if evaluation service is unavailable", state.isVerified)
        assertFalse("Must not complete task silently", state.isTaskCompleted)
    }
}
