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
class SpeakingPracticeFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestSpeechRecognitionService : SpeechRecognitionService {
        override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
        override fun isAvailable(): Boolean = true
        override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {}
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
    fun completeSpeakingFlow_userSpeaksError_correctionGenerated_repeatVerified_taskCompleted() = runTest {
        val grammarService = DefaultGrammarService(available = true)
        val pronunciationService = DefaultPronunciationService(available = true)
        val translationService = DefaultTranslationService(available = true)
        val evaluationService = DefaultSpeakingEvaluationService(
            grammarService = grammarService,
            pronunciationService = pronunciationService,
            translationService = translationService
        )
        val learningRepo = MockLearningRepositoryImpl()
        val speechService = TestSpeechRecognitionService()

        val viewModel = SpeakingPracticeViewModel(
            speechService = speechService,
            evaluationService = evaluationService,
            learningRepo = learningRepo
        )

        viewModel.setMotherTongue("Tamil")
        viewModel.setTargetLanguage("English")

        // 1. Initial speech with error: "I am go to college yesterday."
        viewModel.processSpokenText("I am go to college yesterday.")
        testScheduler.advanceUntilIdle()

        val stateAfterInitial = viewModel.uiState.value
        assertEquals(SpeakingSessionState.CORRECTION_REQUIRED, stateAfterInitial.sessionState)
        assertNotNull(stateAfterInitial.grammarAnalysis)
        assertFalse(stateAfterInitial.grammarAnalysis!!.isCorrect)
        assertEquals("I went to college yesterday.", stateAfterInitial.grammarAnalysis!!.correctedSentence)

        // Verify Tamil mother-tongue explanation
        assertTrue(
            "Tamil explanation should be present",
            stateAfterInitial.grammarAnalysis!!.motherTongueExplanation.contains("நேற்று")
        )

        // 2. User repeats with poor accuracy (e.g. gibberish or wrong words)
        viewModel.processSpokenText("I am still go")
        testScheduler.advanceUntilIdle()

        val stateAfterBadRepeat = viewModel.uiState.value
        assertFalse("Task should NOT be completed on poor repetition", stateAfterBadRepeat.isTaskCompleted)
        assertFalse("Verification must fail on poor repetition", stateAfterBadRepeat.isVerified)
        assertTrue(stateAfterBadRepeat.verificationScore < 70)

        // 3. User repeats the corrected sentence accurately
        viewModel.processSpokenText("I went to college yesterday.")
        testScheduler.advanceUntilIdle()

        val stateAfterGoodRepeat = viewModel.uiState.value
        assertEquals(SpeakingSessionState.VERIFIED_SUCCESS, stateAfterGoodRepeat.sessionState)
        assertTrue("Task must be marked completed after verified repetition", stateAfterGoodRepeat.isTaskCompleted)
        assertTrue("Verification must pass", stateAfterGoodRepeat.isVerified)
        assertTrue("Accuracy score must be >= 70%", stateAfterGoodRepeat.verificationScore >= 70)
        assertTrue("XP must be awarded upon verified completion", stateAfterGoodRepeat.earnedXp > 0)
    }
}
