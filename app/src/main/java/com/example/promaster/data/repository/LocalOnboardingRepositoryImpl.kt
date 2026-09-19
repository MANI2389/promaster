package com.example.promaster.data.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.model.UserDocument
import com.example.promaster.data.firebase.repository.FirestoreUserRepository
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.preferences.UserPreferencesRepository
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Onboarding Repository that combines fast in-memory access with
 * persistent SharedPreferences and cloud Firestore synchronization.
 */
class LocalOnboardingRepositoryImpl(
    private val preferencesRepositoryProvider: () -> UserPreferencesRepository? = {
        UserPreferencesRepository.getInstanceOrNull()
    },
    private val firestoreUserRepo: FirestoreUserRepository = FirestoreUserRepository()
) : OnboardingRepository {

    companion object {
        private val _userProfileState = MutableStateFlow<UserProfile?>(null)
    }

    init {
        restoreProfileFromPreferences()
    }

    private fun restoreProfileFromPreferences() {
        val prefs = preferencesRepositoryProvider() ?: return
        if (prefs.isOnboardingCompleted && _userProfileState.value == null) {
            val restored = UserProfile(
                id = FirebaseManager.auth?.currentUser?.uid ?: "user_persisted",
                name = prefs.userDisplayName.ifBlank { "Learner" },
                motherTongue = MockDataProvider.languages.find { it.code == prefs.motherTongueCode }
                    ?: Language(prefs.motherTongueCode, prefs.motherTongueName, prefs.motherTongueName, "🌐", "Beginner", "1M learners"),
                targetLanguage = MockDataProvider.languages.find { it.code == prefs.targetLanguageCode }
                    ?: Language(prefs.targetLanguageCode, prefs.targetLanguageName, prefs.targetLanguageName, "🎯", "Beginner", "1M learners"),
                level = try { LanguageLevel.valueOf(prefs.userLevel) } catch (_: Exception) { LanguageLevel.BEGINNER },
                preferences = LearningPreferences(
                    dailyDurationMinutes = prefs.dailyGoalMinutes,
                    preferredLearningTime = PreferredLearningTime.MORNING,
                    reminderNotificationEnabled = prefs.reminderEnabled
                ),
                goal = LearningGoal(
                    type = LearningGoalType.CAREER,
                    title = "Career & Professional Fluency",
                    description = "Master workplace communication",
                    iconEmoji = "💼",
                    targetDays = 30,
                    targetWords = 500
                ),
                isSetupComplete = true
            )
            _userProfileState.value = restored

            // Synchronize MockDataProvider for immediate screen consistency
            MockDataProvider.currentUser = MockDataProvider.currentUser.copy(
                name = restored.name,
                targetLanguage = restored.targetLanguage.name,
                nativeLanguage = restored.motherTongue.name,
                dailyGoalMinutes = restored.preferences.dailyDurationMinutes
            )
        }
    }

    override fun getUserProfile(): Flow<UserProfile?> {
        restoreProfileFromPreferences()
        return _userProfileState.asStateFlow()
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        _userProfileState.value = profile

        // 1. Persist to local SharedPreferences immediately
        preferencesRepositoryProvider()?.let { prefs ->
            prefs.isOnboardingCompleted = true
            prefs.motherTongueCode = profile.motherTongue.code
            prefs.motherTongueName = profile.motherTongue.name
            prefs.targetLanguageCode = profile.targetLanguage.code
            prefs.targetLanguageName = profile.targetLanguage.name
            prefs.userLevel = profile.level.name
            prefs.dailyGoalMinutes = profile.preferences.dailyDurationMinutes
            prefs.reminderEnabled = profile.preferences.reminderNotificationEnabled
            prefs.userDisplayName = profile.name
        }

        // 2. Persist to Firestore if user is authenticated
        val currentUid = FirebaseManager.auth?.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val userDoc = UserDocument.fromUserProfile(profile)
            firestoreUserRepo.saveUser(currentUid, userDoc)
        }

        // 3. Synchronize with active MockDataProvider and MockUserProfileRepositoryImpl
        val updatedUser = MockDataProvider.currentUser.copy(
            name = profile.name.ifBlank { "Alex Vance" },
            targetLanguage = profile.targetLanguage.name,
            nativeLanguage = profile.motherTongue.name,
            dailyGoalMinutes = profile.preferences.dailyDurationMinutes
        )
        MockUserProfileRepositoryImpl.syncUser(updatedUser)

        // 4. Generate customized 30-day curriculum based on chosen setup
        MockDataProvider.generateCustomThirtyDayPlan(
            targetLanguage = profile.targetLanguage.name,
            level = profile.level,
            dailyMinutes = profile.preferences.dailyDurationMinutes
        )
    }

    override suspend fun getAvailableLanguages(): List<Language> {
        return MockDataProvider.languages
    }

    override suspend fun isSetupComplete(): Boolean {
        val prefs = preferencesRepositoryProvider()
        if (prefs?.isOnboardingCompleted == true) return true
        return _userProfileState.value?.isSetupComplete == true
    }

    override suspend fun generatePersonalizedPlan(profile: UserProfile): List<LearningPlan> {
        return MockDataProvider.generateCustomThirtyDayPlan(
            targetLanguage = profile.targetLanguage.name,
            level = profile.level,
            dailyMinutes = profile.preferences.dailyDurationMinutes
        )
    }
}
