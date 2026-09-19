package com.example.promaster.presentation.settings

import com.example.promaster.data.firebase.model.ProgressDocument
import com.example.promaster.data.firebase.repository.FirestoreProgressRepository
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.MockUserProfileRepositoryImpl
import com.example.promaster.data.service.MockLearningReminderScheduler
import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.ReminderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningReminderSettingsAndProgressTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun settingsViewModel_requestsNotificationPermission_withRationaleWhenNotGranted() = runTest {
        val scheduler = MockLearningReminderScheduler()
        val viewModel = SettingsViewModel(
            userProfileRepo = MockUserProfileRepositoryImpl(),
            reminderScheduler = scheduler
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // User attempts to turn on daily reminder when permission is missing
        viewModel.onDailyLessonReminderToggled(enabled = true, hasNotificationPermission = false)

        val state = viewModel.uiState.value
        assertTrue("Educational rationale must be displayed before requesting permission", state.showPermissionRationale)
        assertEquals(ReminderType.DAILY_LESSON, state.pendingReminderToggle)

        // When permission is granted, reminder is activated and scheduled
        viewModel.onNotificationPermissionResult(isGranted = true)

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.showPermissionRationale)
        assertTrue(updatedState.reminderSettings.dailyLessonReminderEnabled)
        assertTrue(scheduler.isReminderScheduled(ReminderType.DAILY_LESSON))
    }

    @Test
    fun settingsViewModel_timeSelection_updatesSchedulerAndSettings() = runTest {
        val scheduler = MockLearningReminderScheduler()
        val viewModel = SettingsViewModel(
            userProfileRepo = MockUserProfileRepositoryImpl(),
            reminderScheduler = scheduler
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPreferredTimeSelected("09:00 PM")

        assertEquals("09:00 PM", viewModel.uiState.value.reminderSettings.preferredTime)
        assertEquals("09:00 PM", scheduler.lastScheduledTimeStr)
    }

    @Test
    fun firestoreProgressRepository_recordActivity_tracksXpAndEnforcesSameDayDuplicateStreakProtection() = runTest {
        val progressRepo = FirestoreProgressRepository(firestore = null)

        // Reset mock state
        MockDataProvider.progressState = ProgressState(
            currentStreak = 2,
            bestStreak = 4,
            longestStreak = 4,
            totalXp = 100,
            wordsLearned = 10,
            rulesMastered = 2,
            speakingMinutes = 5,
            listeningMinutes = 10,
            level = 1,
            weeklyXp = listOf(10, 20, 30),
            badges = emptyList(),
            lastActivityDate = System.currentTimeMillis() // active today
        )

        // Activity 1: Complete vocabulary exercise today -> XP awards, streak remains 2 (duplicate prevented)
        val res1 = progressRepo.recordActivity("user_test", xpEarned = 35, category = "VOCABULARY", score = 90)
        assertTrue(res1.isSuccess)
        assertEquals(135, MockDataProvider.progressState.totalXp)
        assertEquals("Same day activity must NOT increase streak count", 2, MockDataProvider.progressState.currentStreak)
        assertEquals(4, MockDataProvider.progressState.longestStreak)

        // Activity 2: Complete grammar quiz today -> XP accumulates, streak still remains 2
        val res2 = progressRepo.recordActivity("user_test", xpEarned = 50, category = "GRAMMAR", score = 100)
        assertTrue(res2.isSuccess)
        assertEquals(185, MockDataProvider.progressState.totalXp)
        assertEquals("Multiple same day activities must strictly maintain streak", 2, MockDataProvider.progressState.currentStreak)
        assertEquals(4, MockDataProvider.progressState.longestStreak)
    }
}
