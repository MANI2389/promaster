package com.example.promaster.data.repository

import com.example.promaster.ai.AiBackendClient
import com.example.promaster.ai.MockAiBackendClient
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.DailyTaskDocument
import com.example.promaster.data.firebase.repository.FirestoreDailyTaskRepository
import com.example.promaster.data.firebase.repository.FirestoreLearningPlanRepository
import com.example.promaster.data.firebase.repository.FirestoreProgressRepository
import com.example.promaster.data.firebase.repository.FirestoreUserRepository
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.engine.LearningLockSystem
import com.example.promaster.domain.engine.ThirtyDayLearningEngine
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.LearningRepository
import kotlinx.coroutines.flow.*

class FirebaseLearningRepositoryImpl(
    private val firestoreTasksRepo: FirestoreDailyTaskRepository = FirestoreDailyTaskRepository(),
    private val firestorePlanRepo: FirestoreLearningPlanRepository = FirestoreLearningPlanRepository(),
    private val firestoreProgressRepo: FirestoreProgressRepository = FirestoreProgressRepository(),
    private val firestoreUserRepo: FirestoreUserRepository = FirestoreUserRepository(),
    private val aiBackend: AiBackendClient = MockAiBackendClient()
) : LearningRepository {

    private val currentUserId: String
        get() = FirebaseManager.currentUserId

    override fun getAvailableLanguages(): Flow<List<Language>> =
        flowOf(MockDataProvider.languages)

    override fun get30DayPlan(): Flow<List<LearningPlan>> {
        return firestorePlanRepo.getPlanFlow(currentUserId).map { result ->
            when (result) {
                is FirebaseResult.Success -> {
                    result.data.days.map { it.toDomain() }
                }
                else -> MockDataProvider.thirtyDayPlan
            }
        }
    }

    override fun getTasksForDay(dayNumber: Int): Flow<List<DailyTask>> {
        return firestoreTasksRepo.getTasksFlow(currentUserId, dayNumber).map { taskDocs ->
            if (taskDocs.isEmpty()) {
                val generated = ThirtyDayLearningEngine.generateTasksForDay(
                    dayNumber = dayNumber,
                    targetLanguage = MockDataProvider.currentUser.targetLanguage,
                    motherTongue = MockDataProvider.currentUser.nativeLanguage,
                    level = LanguageLevel.BEGINNER,
                    dailyDuration = MockDataProvider.currentUser.dailyGoalMinutes,
                    goal = LearningGoalType.TRAVEL,
                    isCurrentOrPastDay = true
                )
                generated
            } else {
                val domainTasks = taskDocs.map { it.toDomain() }
                LearningLockSystem.refreshLockStates(domainTasks)
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getTodayTasks(): Flow<List<DailyTask>> {
        return firestoreProgressRepo.getProgressFlow(currentUserId).flatMapLatest { progResult ->
            val currentDay = when (progResult) {
                is FirebaseResult.Success -> progResult.data.currentDay
                else -> 1
            }
            getTasksForDay(currentDay)
        }
    }

    override fun getVocabulary(): Flow<List<VocabularyItem>> =
        flowOf(MockDataProvider.vocabularyList)

    override fun getGrammarRules(): Flow<List<GrammarRule>> =
        flowOf(MockDataProvider.grammarRules)

    override suspend fun completeTask(taskId: String): Result<Boolean> {
        val result = submitTaskScore(taskId, 100)
        return result.map { it is LearningLockSystem.TaskValidationResult.Success }
    }

    override suspend fun submitTaskScore(
        taskId: String,
        score: Int
    ): Result<LearningLockSystem.TaskValidationResult> {
        val currentTasks = getTodayTasks().first()
        val validation = LearningLockSystem.submitTaskScore(currentTasks, taskId, score)

        when (validation) {
            is LearningLockSystem.TaskValidationResult.Success -> {
                val completed = validation.completedTask
                // Persist completed task
                firestoreTasksRepo.saveTask(
                    currentUserId,
                    DailyTaskDocument.fromDomain(completed, completed.dayNumber)
                )

                // Persist next unlocked task if applicable
                val updatedNext = validation.updatedTasks.getOrNull(
                    validation.updatedTasks.indexOfFirst { it.taskId == taskId } + 1
                )
                if (updatedNext != null && updatedNext.completionStatus == TaskCompletionStatus.AVAILABLE) {
                    firestoreTasksRepo.saveTask(
                        currentUserId,
                        DailyTaskDocument.fromDomain(updatedNext, updatedNext.dayNumber)
                    )
                }

                // Award XP and record progress
                firestoreProgressRepo.recordActivity(
                    userId = currentUserId,
                    xpEarned = completed.xpReward,
                    category = completed.category.name,
                    score = score
                )
                firestoreUserRepo.addXp(completed.xpReward)

                // If entire day is completed, advance current day in plan and progress
                if (validation.isDayCompleted) {
                    val nextDay = minOf(completed.dayNumber + 1, 30)
                    firestorePlanRepo.updateCurrentDay(currentUserId, nextDay)
                }
            }

            is LearningLockSystem.TaskValidationResult.FailedPassingScore -> {
                // Update retry count and in-progress status
                val retried = validation.task
                firestoreTasksRepo.saveTask(
                    currentUserId,
                    DailyTaskDocument.fromDomain(retried, retried.dayNumber)
                )
            }

            is LearningLockSystem.TaskValidationResult.Locked -> {
                // Do not update data if locked
            }
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
        return Result.success(true)
    }

    override suspend fun submitQuizResult(quizResult: QuizResult): Result<QuizResult> {
        val xpAward = com.example.promaster.domain.engine.ActivityXpPolicy.calculateXp(
            activityType = quizResult.title,
            score = quizResult.score,
            attemptCount = quizResult.attempts
        )
        if (xpAward.earnedXp > 0) {
            firestoreUserRepo.addXp(xpAward.earnedXp)
            firestoreProgressRepo.recordActivity(
                userId = currentUserId,
                xpEarned = xpAward.earnedXp,
                category = "QUIZ",
                score = quizResult.score
            )
        }
        val awardedResult = quizResult.copy(
            earnedXp = xpAward.earnedXp,
            completion = xpAward.passedGate
        )
        return Result.success(awardedResult)
    }

    override suspend fun analyzeGrammar(inputSentence: String): Result<GrammarCorrection> {
        return aiBackend.analyzeGrammar(inputSentence, MockDataProvider.currentUser.targetLanguage)
    }
}
