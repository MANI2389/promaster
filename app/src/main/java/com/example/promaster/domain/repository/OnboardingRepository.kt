package com.example.promaster.domain.repository

import com.example.promaster.domain.model.Language
import com.example.promaster.domain.model.LearningPlan
import com.example.promaster.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun getAvailableLanguages(): List<Language>
    suspend fun isSetupComplete(): Boolean
    suspend fun generatePersonalizedPlan(profile: UserProfile): List<LearningPlan>
}
