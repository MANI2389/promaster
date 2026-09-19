package com.example.promaster.domain.repository

import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<User>
    fun getProgressState(): Flow<ProgressState>
    suspend fun updateTargetLanguage(languageCode: String)
    suspend fun updateDailyGoal(minutes: Int)
    suspend fun addXp(amount: Int)
}
