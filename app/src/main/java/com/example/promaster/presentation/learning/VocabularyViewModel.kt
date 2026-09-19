package com.example.promaster.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.FirebaseLearningRepositoryImpl
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class VocabularyMode(val title: String) {
    LEARN("Learn"),
    PRACTICE("Practice"),
    QUIZ("Quiz"),
    REVIEW("Review")
}

data class VocabularyUiState(
    val words: List<VocabularyItem> = emptyList(),
    val filteredWords: List<VocabularyItem> = emptyList(),
    val selectedMode: VocabularyMode = VocabularyMode.LEARN,
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val lastQuizResult: QuizResult? = null,
    val feedbackMessage: String? = null
)

class VocabularyViewModel(
    private val learningRepo: LearningRepository = FirebaseLearningRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        loadVocabulary()
    }

    private fun loadVocabulary() {
        viewModelScope.launch {
            learningRepo.getVocabulary().collect { list ->
                val words = if (list.isNotEmpty()) list else MockDataProvider.vocabularyList
                _uiState.update { current ->
                    current.copy(
                        words = words,
                        filteredWords = getFilteredForMode(words, current.selectedMode),
                        quizQuestions = generateQuizFromVocabulary(words)
                    )
                }
            }
        }
    }

    fun setMode(mode: VocabularyMode) {
        _uiState.update { current ->
            val filtered = getFilteredForMode(current.words, mode)
            current.copy(
                selectedMode = mode,
                filteredWords = filtered,
                currentCardIndex = 0,
                isCardFlipped = false
            )
        }
    }

    fun toggleCardFlip() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun nextCard() {
        _uiState.update { current ->
            val maxIndex = current.filteredWords.lastIndex.coerceAtLeast(0)
            if (current.currentCardIndex < maxIndex) {
                current.copy(currentCardIndex = current.currentCardIndex + 1, isCardFlipped = false)
            } else current
        }
    }

    fun previousCard() {
        _uiState.update { current ->
            if (current.currentCardIndex > 0) {
                current.copy(currentCardIndex = current.currentCardIndex - 1, isCardFlipped = false)
            } else current
        }
    }

    fun markNeedsPractice(wordId: String) {
        viewModelScope.launch {
            val target = _uiState.value.words.find { it.id == wordId } ?: return@launch
            val newMastery = (target.masteryLevel - 15).coerceIn(10, 100)
            learningRepo.updateVocabularyStatus(wordId, ReviewStatus.LEARNING, newMastery)
            updateLocalWord(wordId, ReviewStatus.LEARNING, newMastery)
            _uiState.update { it.copy(feedbackMessage = "Added '${target.word}' to your review queue.") }
        }
    }

    fun markKnowThis(wordId: String) {
        viewModelScope.launch {
            val target = _uiState.value.words.find { it.id == wordId } ?: return@launch
            val newMastery = (target.masteryLevel + 25).coerceIn(0, 100)
            val newStatus = if (newMastery >= 90) ReviewStatus.MASTERED else ReviewStatus.LEARNING
            learningRepo.updateVocabularyStatus(wordId, newStatus, newMastery)
            updateLocalWord(wordId, newStatus, newMastery)
            _uiState.update { it.copy(feedbackMessage = "Great! '${target.word}' mastery increased to $newMastery%.") }
        }
    }

    fun submitQuiz(result: QuizResult) {
        viewModelScope.launch {
            val awarded = learningRepo.submitQuizResult(result)
            awarded.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        lastQuizResult = res,
                        feedbackMessage = if (res.completion) "Quiz completed! +${res.earnedXp} XP awarded." else "Quiz completed with ${res.score}%. Score 70%+ to earn XP."
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun updateLocalWord(wordId: String, status: ReviewStatus, mastery: Int) {
        _uiState.update { current ->
            val updated = current.words.map { w ->
                if (w.id == wordId) w.copy(reviewStatus = status, masteryLevel = mastery) else w
            }
            current.copy(
                words = updated,
                filteredWords = getFilteredForMode(updated, current.selectedMode)
            )
        }
    }

    private fun getFilteredForMode(words: List<VocabularyItem>, mode: VocabularyMode): List<VocabularyItem> {
        return when (mode) {
            VocabularyMode.LEARN -> words
            VocabularyMode.PRACTICE -> words
            VocabularyMode.QUIZ -> words
            VocabularyMode.REVIEW -> {
                val reviewQueue = words.filter {
                    it.reviewStatus == ReviewStatus.NEEDS_REVIEW ||
                            it.reviewStatus == ReviewStatus.LEARNING ||
                            it.masteryLevel < 80
                }
                if (reviewQueue.isNotEmpty()) reviewQueue else words
            }
        }
    }

    private fun generateQuizFromVocabulary(words: List<VocabularyItem>): List<QuizQuestion> {
        if (words.isEmpty()) return emptyList()
        val questions = mutableListOf<QuizQuestion>()

        words.take(6).forEachIndexed { idx, item ->
            when (idx % 4) {
                0 -> {
                    // Multiple Choice
                    val wrongOptions = words.filter { it.id != item.id }.map { it.translation }.shuffled().take(3)
                    val allOptions = (wrongOptions + item.translation).shuffled()
                    questions.add(
                        QuizQuestion(
                            id = "vocab_q_${item.id}",
                            type = QuizQuestionType.MULTIPLE_CHOICE,
                            prompt = "What is the translation of \"${item.word}\"?",
                            options = allOptions,
                            correctAnswer = item.translation,
                            explanation = "${item.word} (${item.phonetic}) means ${item.translation}.",
                            motherTongueHint = item.meaning
                        )
                    )
                }
                1 -> {
                    // Fill in the Blank
                    val example = item.exampleSentence
                    val blankedPrompt = if (example.contains(item.word, ignoreCase = true)) {
                        example.replace(Regex("(?i)\\b${Regex.escape(item.word)}\\b"), "_____")
                    } else {
                        "The Spanish word for \"${item.translation}\" is _____."
                    }
                    questions.add(
                        QuizQuestion(
                            id = "vocab_q_${item.id}",
                            type = QuizQuestionType.FILL_IN_THE_BLANK,
                            prompt = blankedPrompt,
                            options = listOf(item.word) + words.filter { it.id != item.id }.map { it.word }.take(2),
                            correctAnswer = item.word,
                            explanation = "Example: $example (${item.exampleTranslation})",
                            motherTongueHint = item.translation
                        )
                    )
                }
                2 -> {
                    // Translation
                    questions.add(
                        QuizQuestion(
                            id = "vocab_q_${item.id}",
                            type = QuizQuestionType.TRANSLATION,
                            prompt = "Translate \"${item.translation}\" into the target language:",
                            options = listOf(item.word) + words.filter { it.id != item.id }.map { it.word }.take(3).shuffled(),
                            correctAnswer = item.word,
                            explanation = "${item.word} translates to ${item.translation}.",
                            motherTongueHint = item.meaning
                        )
                    )
                }
                3 -> {
                    // Sentence Correction
                    val wrongExample = item.exampleSentence.replace(item.word, "xyzWrong", ignoreCase = true)
                    questions.add(
                        QuizQuestion(
                            id = "vocab_q_${item.id}",
                            type = QuizQuestionType.SENTENCE_CORRECTION,
                            prompt = "Select the word that correctly completes: \"$wrongExample\"",
                            options = listOf(item.word) + words.filter { it.id != item.id }.map { it.word }.take(2),
                            correctAnswer = item.word,
                            explanation = "Correct sentence: \"${item.exampleSentence}\"",
                            motherTongueHint = item.translation
                        )
                    )
                }
            }
        }
        return questions
    }
}
