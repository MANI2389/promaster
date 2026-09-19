package com.example.promaster.domain.repository

import com.example.promaster.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun getAvailableLanguages(): Flow<List<Language>>
    fun get30DayPlan(): Flow<List<LearningPlan>>
    fun getTodayTasks(): Flow<List<DailyTask>>
    fun getTasksForDay(dayNumber: Int): Flow<List<DailyTask>>
    fun getVocabulary(): Flow<List<VocabularyItem>>
    fun getGrammarRules(): Flow<List<GrammarRule>>
    suspend fun completeTask(taskId: String): Result<Boolean>
    suspend fun submitTaskScore(taskId: String, score: Int): Result<com.example.promaster.domain.engine.LearningLockSystem.TaskValidationResult>
    suspend fun markWordMastered(wordId: String): Result<Boolean>
    suspend fun updateVocabularyStatus(wordId: String, status: ReviewStatus, mastery: Int): Result<Boolean>
    suspend fun submitQuizResult(quizResult: QuizResult): Result<QuizResult>
    suspend fun analyzeGrammar(inputSentence: String): Result<GrammarCorrection>
}
