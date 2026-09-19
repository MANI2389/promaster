package com.example.promaster.data.repository

import com.example.promaster.ai.AiBackendClient
import com.example.promaster.ai.MockAiBackendClient
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockLearningRepositoryImpl(
    private val aiBackend: AiBackendClient = MockAiBackendClient()
) : LearningRepository {

    private val _tasks = MutableStateFlow(MockDataProvider.dailyTasks)
    private val _vocab = MutableStateFlow(MockDataProvider.vocabularyList)

    override fun getAvailableLanguages(): Flow<List<Language>> =
        MutableStateFlow(MockDataProvider.languages).asStateFlow()

    override fun get30DayPlan(): Flow<List<LearningPlan>> =
        MutableStateFlow(MockDataProvider.thirtyDayPlan).asStateFlow()

    override fun getTasksForDay(dayNumber: Int): Flow<List<DailyTask>> = _tasks.asStateFlow()

    override fun getTodayTasks(): Flow<List<DailyTask>> = _tasks.asStateFlow()

    override fun getVocabulary(): Flow<List<VocabularyItem>> = _vocab.asStateFlow()

    override fun getGrammarRules(): Flow<List<GrammarRule>> =
        MutableStateFlow(MockDataProvider.grammarRules).asStateFlow()

    override suspend fun completeTask(taskId: String): Result<Boolean> {
        val validation = com.example.promaster.domain.engine.LearningLockSystem.submitTaskScore(
            tasks = _tasks.value,
            taskId = taskId,
            score = 100
        )
        return when (validation) {
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.Success -> {
                _tasks.value = validation.updatedTasks
                Result.success(true)
            }
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.FailedPassingScore -> {
                _tasks.value = validation.updatedTasks
                Result.success(false)
            }
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.Locked -> {
                Result.failure(Exception(validation.reason))
            }
        }
    }

    override suspend fun submitTaskScore(
        taskId: String,
        score: Int
    ): Result<com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult> {
        val validation = com.example.promaster.domain.engine.LearningLockSystem.submitTaskScore(
            tasks = _tasks.value,
            taskId = taskId,
            score = score
        )
        when (validation) {
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.Success -> {
                _tasks.value = validation.updatedTasks
            }
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.FailedPassingScore -> {
                _tasks.value = validation.updatedTasks
            }
            is com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult.Locked -> {}
        }
        return Result.success(validation)
    }

    override suspend fun markWordMastered(wordId: String): Result<Boolean> {
        return updateVocabularyStatus(wordId, ReviewStatus.MASTERED, 100)
    }

    override suspend fun updateVocabularyStatus(
        wordId: String,
        status: ReviewStatus,
        mastery: Int
    ): Result<Boolean> {
        val updated = _vocab.value.map { word ->
            if (word.id == wordId) word.copy(reviewStatus = status, masteryLevel = mastery) else word
        }
        _vocab.value = updated
        return Result.success(true)
    }

    override suspend fun submitQuizResult(quizResult: QuizResult): Result<QuizResult> {
        val xpAward = com.example.promaster.domain.engine.ActivityXpPolicy.calculateXp(
            activityType = quizResult.title,
            score = quizResult.score,
            attemptCount = quizResult.attempts
        )
        val awardedResult = quizResult.copy(
            earnedXp = xpAward.earnedXp,
            completion = xpAward.passedGate
        )
        return Result.success(awardedResult)
    }

    override suspend fun analyzeGrammar(inputSentence: String): Result<GrammarCorrection> {
        return aiBackend.analyzeGrammar(inputSentence, "Spanish")
    }
}
