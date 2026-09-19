package com.example.promaster.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.FirebaseLearningRepositoryImpl
import com.example.promaster.data.service.AndroidSpeechRecognitionService
import com.example.promaster.data.service.DefaultSpeakingEvaluationService
import com.example.promaster.domain.engine.ActivityXpPolicy
import com.example.promaster.domain.model.GrammarAnalysisResult
import com.example.promaster.domain.model.PronunciationResult
import com.example.promaster.domain.model.SpeakingPrompt
import com.example.promaster.domain.model.SpeakingSessionState
import com.example.promaster.domain.repository.LearningRepository
import com.example.promaster.domain.service.SpeakingEvaluationService
import com.example.promaster.domain.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeakingPracticeUiState(
    val sessionState: SpeakingSessionState = SpeakingSessionState.IDLE,
    val currentPrompt: SpeakingPrompt,
    val motherTongue: String = "Tamil",
    val targetLanguage: String = "English",
    val userSpokenInitial: String = "",
    val userSpokenRepeat: String = "",
    val grammarAnalysis: GrammarAnalysisResult? = null,
    val pronunciationResult: PronunciationResult? = null,
    val isVerified: Boolean = false,
    val verificationScore: Int = 0,
    val earnedXp: Int = 0,
    val attempts: Int = 1,
    val isTaskCompleted: Boolean = false,
    val errorMessage: String? = null
)

class SpeakingPracticeViewModel(
    private val speechService: SpeechRecognitionService = AndroidSpeechRecognitionService(),
    private val evaluationService: SpeakingEvaluationService = DefaultSpeakingEvaluationService(),
    private val learningRepo: LearningRepository = FirebaseLearningRepositoryImpl(),
    initialPrompt: SpeakingPrompt = defaultPrompt
) : ViewModel() {

    companion object {
        val defaultPrompt = SpeakingPrompt(
            id = "prompt_college_past",
            promptContext = "Tell me where you went yesterday:",
            expectedPhrase = "I went to college yesterday.",
            translation = "நான் நேற்று கல்லூரிக்குச் சென்றேன்.",
            motherTongueHint = "நேற்று நடந்த நிகழ்வு (Use past simple 'went')",
            targetLanguage = "English"
        )
    }

    private val _uiState = MutableStateFlow(
        SpeakingPracticeUiState(
            currentPrompt = initialPrompt,
            motherTongue = MockDataProvider.currentUser.nativeLanguage.ifBlank { "Tamil" },
            targetLanguage = MockDataProvider.currentUser.targetLanguage.ifBlank { "English" }
        )
    )
    val uiState: StateFlow<SpeakingPracticeUiState> = _uiState.asStateFlow()

    fun setMotherTongue(lang: String) {
        _uiState.update { it.copy(motherTongue = lang) }
    }

    fun setTargetLanguage(lang: String) {
        _uiState.update { it.copy(targetLanguage = lang) }
    }

    fun startListening() {
        val currentState = _uiState.value.sessionState

        // Service availability gate
        if (!evaluationService.isAvailable()) {
            _uiState.update {
                it.copy(
                    sessionState = SpeakingSessionState.SERVICE_UNAVAILABLE,
                    errorMessage = "Speaking analysis is currently unavailable."
                )
            }
            return
        }

        if (!speechService.isAvailable()) {
            _uiState.update {
                it.copy(
                    sessionState = SpeakingSessionState.SERVICE_UNAVAILABLE,
                    errorMessage = "Speaking analysis is currently unavailable."
                )
            }
            return
        }

        val targetState = if (currentState == SpeakingSessionState.CORRECTION_REQUIRED) {
            SpeakingSessionState.LISTENING_REPEAT
        } else {
            SpeakingSessionState.LISTENING_INITIAL
        }

        _uiState.update { it.copy(sessionState = targetState, errorMessage = null) }

        speechService.startListening(
            language = _uiState.value.targetLanguage,
            onResult = { spokenText ->
                processSpokenText(spokenText)
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        sessionState = if (error.contains("unavailable", ignoreCase = true)) {
                            SpeakingSessionState.SERVICE_UNAVAILABLE
                        } else currentState,
                        errorMessage = error
                    )
                }
            }
        )
    }

    fun stopListening() {
        speechService.stopListening()
        if (_uiState.value.sessionState == SpeakingSessionState.LISTENING_INITIAL ||
            _uiState.value.sessionState == SpeakingSessionState.LISTENING_REPEAT
        ) {
            _uiState.update { it.copy(sessionState = SpeakingSessionState.IDLE) }
        }
    }

    fun processSpokenText(spokenText: String) {
        val currentState = _uiState.value.sessionState

        viewModelScope.launch {
            if (currentState == SpeakingSessionState.LISTENING_REPEAT ||
                currentState == SpeakingSessionState.CORRECTION_REQUIRED
            ) {
                // Verification Phase (User repeats)
                _uiState.update {
                    it.copy(
                        sessionState = SpeakingSessionState.VERIFYING,
                        userSpokenRepeat = spokenText
                    )
                }

                val expectedTarget = _uiState.value.grammarAnalysis?.correctedSentence
                    ?: _uiState.value.currentPrompt.expectedPhrase

                val verificationResult = evaluationService.verifyRepeatedSpeech(spokenText, expectedTarget)

                verificationResult.fold(
                    onSuccess = { pronResult ->
                        if (pronResult.isMatch && pronResult.accuracyScore >= ActivityXpPolicy.MIN_PASSING_SCORE) {
                            // Successful verification
                            val xpAward = ActivityXpPolicy.calculateXp(
                                activityType = "SPEAKING",
                                score = pronResult.accuracyScore,
                                attemptCount = _uiState.value.attempts
                            )

                            // Complete daily task in repository
                            learningRepo.completeTask("task_04")

                            _uiState.update {
                                it.copy(
                                    sessionState = SpeakingSessionState.VERIFIED_SUCCESS,
                                    pronunciationResult = pronResult,
                                    isVerified = true,
                                    verificationScore = pronResult.accuracyScore,
                                    earnedXp = xpAward.earnedXp,
                                    isTaskCompleted = true,
                                    errorMessage = null
                                )
                            }
                        } else {
                            // Inaccurate repetition - ask to repeat again
                            _uiState.update {
                                it.copy(
                                    sessionState = SpeakingSessionState.CORRECTION_REQUIRED,
                                    pronunciationResult = pronResult,
                                    isVerified = false,
                                    verificationScore = pronResult.accuracyScore,
                                    errorMessage = "Pronunciation match was ${pronResult.accuracyScore}%. Score 70%+ to verify. Please repeat again."
                                )
                            }
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                sessionState = SpeakingSessionState.SERVICE_UNAVAILABLE,
                                errorMessage = "Speaking analysis is currently unavailable."
                            )
                        }
                    }
                )
            } else {
                // Initial Speech Phase (Sentence analysis & grammar checking)
                _uiState.update {
                    it.copy(
                        sessionState = SpeakingSessionState.ANALYZING,
                        userSpokenInitial = spokenText
                    )
                }

                val analysisResult = evaluationService.evaluateInitialSpeech(
                    spokenText = spokenText,
                    targetLanguage = _uiState.value.targetLanguage,
                    motherTongue = _uiState.value.motherTongue
                )

                analysisResult.fold(
                    onSuccess = { grammarRes ->
                        if (!grammarRes.isCorrect) {
                            // Grammar error detected -> Show correction + mother-tongue explanation
                            _uiState.update {
                                it.copy(
                                    sessionState = SpeakingSessionState.CORRECTION_REQUIRED,
                                    grammarAnalysis = grammarRes,
                                    errorMessage = null
                                )
                            }
                        } else {
                            // Already correct -> Prompt pronunciation verification
                            _uiState.update {
                                it.copy(
                                    sessionState = SpeakingSessionState.CORRECTION_REQUIRED,
                                    grammarAnalysis = grammarRes,
                                    errorMessage = null
                                )
                            }
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                sessionState = SpeakingSessionState.SERVICE_UNAVAILABLE,
                                errorMessage = "Speaking analysis is currently unavailable."
                            )
                        }
                    }
                )
            }
        }
    }

    fun retry() {
        _uiState.update {
            it.copy(
                sessionState = SpeakingSessionState.IDLE,
                userSpokenInitial = "",
                userSpokenRepeat = "",
                grammarAnalysis = null,
                pronunciationResult = null,
                isVerified = false,
                verificationScore = 0,
                errorMessage = null,
                attempts = it.attempts + 1
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
