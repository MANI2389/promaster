package com.example.promaster.data.repository

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocalOnboardingRepositoryTest {

    private lateinit var repository: LocalOnboardingRepositoryImpl

    @Before
    fun setup() {
        repository = LocalOnboardingRepositoryImpl()
    }

    @Test
    fun getAvailableLanguages_containsMultipleLanguagesWithoutHardcodedRestrictions() = runTest {
        val languages = repository.getAvailableLanguages()
        assertTrue(languages.size >= 10)
        assertTrue(languages.any { it.code == "en" })
        assertTrue(languages.any { it.code == "ta" })
        assertTrue(languages.any { it.code == "hi" })
        assertTrue(languages.any { it.code == "es" })
        assertTrue(languages.any { it.code == "ja" })
        assertTrue(languages.any { it.code == "de" })
        assertTrue(languages.any { it.code == "fr" })
    }

    @Test
    fun saveUserProfile_persistsAndSyncsWithMockDataProvider() = runTest {
        val mother = Language("ta", "Tamil", "தமிழ்", "🇮🇳", "Classical", "1.4M")
        val target = Language("de", "German", "Deutsch", "🇩🇪", "Challenging", "1.1M")
        val profile = UserProfile(
            name = "Karthik",
            motherTongue = mother,
            targetLanguage = target,
            level = LanguageLevel.ELEMENTARY,
            preferences = LearningPreferences(
                dailyDurationMinutes = 45,
                preferredLearningTime = PreferredLearningTime.NIGHT,
                reminderNotificationEnabled = true
            ),
            goal = LearningGoal(type = LearningGoalType.CAREER)
        )

        repository.saveUserProfile(profile)

        val retrieved = repository.getUserProfile().first()
        assertNotNull(retrieved)
        assertEquals("Karthik", retrieved?.name)
        assertEquals("German", retrieved?.targetLanguage?.name)
        assertEquals("Tamil", retrieved?.motherTongue?.name)
        assertEquals(45, retrieved?.preferences?.dailyDurationMinutes)

        // Verify MockDataProvider.currentUser synchronization
        assertEquals("Karthik", MockDataProvider.currentUser.name)
        assertEquals("German", MockDataProvider.currentUser.targetLanguage)
        assertEquals("Tamil", MockDataProvider.currentUser.nativeLanguage)
        assertEquals(45, MockDataProvider.currentUser.dailyGoalMinutes)

        assertTrue(repository.isSetupComplete())
    }

    @Test
    fun generatePersonalizedPlan_createsCustomThirtyDayPlan() = runTest {
        val target = Language("ja", "Japanese", "日本語", "🇯🇵", "Advanced", "1.7M")
        val mother = Language("en", "English", "English", "🇬🇧", "Global", "15M")
        val profile = UserProfile(
            name = "Sarah",
            motherTongue = mother,
            targetLanguage = target,
            level = LanguageLevel.ADVANCED,
            preferences = LearningPreferences(dailyDurationMinutes = 30)
        )

        val plan = repository.generatePersonalizedPlan(profile)
        assertEquals(30, plan.size)
        assertEquals(1, plan.first().day)
        assertEquals(30, plan.last().day)
        assertTrue(plan.first().title.contains("Japanese"))
        assertEquals(30, plan.first().estimatedMinutes)
    }
}
