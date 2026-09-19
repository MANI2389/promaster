package com.example.promaster.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.FirebaseLearningRepositoryImpl
import com.example.promaster.domain.engine.ActivityXpPolicy
import com.example.promaster.domain.model.GrammarExercise
import com.example.promaster.domain.model.GrammarRule
import com.example.promaster.domain.model.QuizResult
import com.example.promaster.domain.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GrammarTab(val title: String) {
    LESSON("Lesson"),
    EXERCISES("Exercises"),
    QUIZ("Quiz")
}

data class GrammarUiState(
    val rules: List<GrammarRule> = emptyList(),
    val selectedRuleIndex: Int = 0,
    val selectedMotherTongue: String = "English",
    val activeTab: GrammarTab = GrammarTab.LESSON,
    val exerciseAnswers: Map<String, String> = emptyMap(),
    val exerciseChecked: Map<String, Boolean> = emptyMap(),
    val exerciseFeedback: Map<String, String> = emptyMap(),
    val lastQuizResult: QuizResult? = null,
    val feedbackMessage: String? = null
) {
    val currentRule: GrammarRule?
        get() = rules.getOrNull(selectedRuleIndex) ?: rules.firstOrNull()
}

class GrammarViewModel(
    private val learningRepo: LearningRepository = FirebaseLearningRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrammarUiState())
    val uiState: StateFlow<GrammarUiState> = _uiState.asStateFlow()

    init {
        loadGrammarRules()
    }

    private fun loadGrammarRules() {
        viewModelScope.launch {
            learningRepo.getGrammarRules().collect { rulesList ->
                val rules = if (rulesList.isNotEmpty()) rulesList else MockDataProvider.grammarRules
                _uiState.update { current ->
                    current.copy(
                        rules = rules,
                        selectedMotherTongue = MockDataProvider.currentUser.nativeLanguage.ifBlank { "English" }
                    )
                }
            }
        }
    }

    fun selectRule(index: Int) {
        _uiState.update {
            it.copy(
                selectedRuleIndex = index,
                exerciseAnswers = emptyMap(),
                exerciseChecked = emptyMap(),
                exerciseFeedback = emptyMap()
            )
        }
    }

    fun setMotherTongue(language: String) {
        _uiState.update { it.copy(selectedMotherTongue = language) }
    }

    fun setActiveTab(tab: GrammarTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onAnswerChanged(exerciseId: String, answer: String) {
        _uiState.update { current ->
            val updated = current.exerciseAnswers.toMutableMap()
            updated[exerciseId] = answer
            current.copy(exerciseAnswers = updated)
        }
    }

    fun checkExercise(exercise: GrammarExercise) {
        val givenAnswer = _uiState.value.exerciseAnswers[exercise.id]?.trim() ?: ""
        val isCorrect = givenAnswer.equals(exercise.correctAnswer.trim(), ignoreCase = true)

        val xpResult = if (isCorrect) {
            ActivityXpPolicy.calculateXp(
                activityType = "GRAMMAR_EXERCISE",
                score = 100,
                attemptCount = 1
            )
        } else null

        _uiState.update { current ->
            val checked = current.exerciseChecked.toMutableMap()
            checked[exercise.id] = isCorrect

            val feedback = current.exerciseFeedback.toMutableMap()
            feedback[exercise.id] = if (isCorrect) {
                "Correct! ${exercise.explanation}"
            } else {
                "Incorrect. Expected: \"${exercise.correctAnswer}\". ${exercise.explanation}"
            }

            current.copy(
                exerciseChecked = checked,
                exerciseFeedback = feedback,
                feedbackMessage = if (isCorrect && xpResult != null) {
                    "+${xpResult.earnedXp} XP awarded for exercise completion!"
                } else null
            )
        }
    }

    fun submitQuiz(result: QuizResult) {
        viewModelScope.launch {
            val awarded = learningRepo.submitQuizResult(result)
            awarded.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        lastQuizResult = res,
                        feedbackMessage = if (res.completion) "Grammar quiz passed! +${res.earnedXp} XP awarded." else "Quiz completed with ${res.score}%. Score 70%+ to pass."
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
