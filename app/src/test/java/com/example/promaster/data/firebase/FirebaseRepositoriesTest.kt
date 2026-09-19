package com.example.promaster.data.firebase

import com.example.promaster.data.firebase.model.*
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.presentation.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.repository.*
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseRepositoriesTest {

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
    fun authViewModel_loginValidation_failsOnEmptyFields() {
        val viewModel = AuthViewModel()
        viewModel.login("", "") {}
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun authViewModel_registerValidation_requiresValidPasswordLength() {
        val viewModel = AuthViewModel()
        viewModel.register("Alex", "alex@promaster.ai", "123") {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("6 characters"))
    }

    @Test
    fun authViewModel_guestLogin_succeedsWithGuestUser() = runTest {
        val viewModel = AuthViewModel()
        var successTriggered = false
        viewModel.loginAsGuest {
            successTriggered = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successTriggered)
        assertNotNull(viewModel.uiState.value.user)
    }

    @Test
    fun firestoreDataModels_supportAllRequiredCollections() {
        // users/{userId}
        val userDoc = UserDocument(
            name = "Test User",
            email = "test@promaster.ai",
            motherTongue = "English",
            targetLanguage = "Spanish",
            level = "Beginner",
            dailyDuration = 20,
            preferredTime = "Morning"
        )
        assertNotNull(userDoc.name)

        // learningPlans/{userId}
        val planDoc = LearningPlanDocument(
            planId = "plan_1",
            userId = "uid_1",
            targetLanguage = "Spanish",
            duration = 20
        )
        assertEquals("Spanish", planDoc.targetLanguage)

        // dailyTasks/{userId}/tasks/{taskId}
        val taskDoc = DailyTaskDocument(
            taskId = "t_1",
            dayNumber = 1,
            title = "Task 1",
            category = "VOCABULARY",
            status = "PENDING"
        )
        assertEquals("PENDING", taskDoc.status)

        // progress/{userId}
        val progDoc = ProgressDocument(
            xp = 100,
            streak = 2,
            currentDay = 1
        )
        assertEquals(100, progDoc.xp)

        // conversations/{userId}/messages/{messageId}
        val convDoc = ConversationMessageDocument(
            messageId = "m_1",
            role = "AI_FRIEND",
            message = "Hello!",
            intent = "CHAT"
        )
        assertEquals("CHAT", convDoc.intent)

        // settings/{userId}
        val setDoc = SettingsDocument(
            notificationsEnabled = true,
            theme = "DARK"
        )
        assertTrue(setDoc.notificationsEnabled)
    }

    @Test
    fun firebaseAuthRepository_guestAndMockAuth_succeed() = runTest {
        val authRepo = FirebaseAuthRepository(auth = null)
        val guestResult = authRepo.loginAsGuest()
        assertTrue(guestResult.isSuccess)

        val user = guestResult.getOrNull()
        assertNotNull(user)
        assertEquals("Guest Explorer", user!!.name)

        authRepo.logout()
        assertNotNull(authRepo)
    }

    @Test
    fun firestoreUserRepository_handlesReadAndWrite() = runTest {
        val userRepo = FirestoreUserRepository(firestore = null)
        val testDoc = UserDocument(
            name = "Elena Vance",
            email = "elena@example.com",
            targetLanguage = "Japanese"
        )
        val saveResult = userRepo.saveUser("u_123", testDoc)
        assertTrue(saveResult.isSuccess)

        val fetchResult = userRepo.getUser("u_123")
        assertTrue(fetchResult.isSuccess)
        assertEquals("Elena Vance", fetchResult.getOrNull()?.name)
    }

    @Test
    fun firestoreLearningPlanRepository_providesThirtyDayPlan() = runTest {
        val planRepo = FirestoreLearningPlanRepository(firestore = null)
        val planResult = planRepo.getPlan("u_123")
        assertTrue(planResult.isSuccess)

        val plan = planResult.getOrNull()
        assertNotNull(plan)
        assertTrue(plan!!.days.isNotEmpty())
        assertEquals(30, plan.days.size)
    }

    @Test
    fun firestoreDailyTaskRepository_providesTasksForDay() = runTest {
        val taskRepo = FirestoreDailyTaskRepository(firestore = null)
        val tasksResult = taskRepo.getTasksForDay("u_123", 1)
        assertTrue(tasksResult.isSuccess)

        val tasks = tasksResult.getOrNull()
        assertNotNull(tasks)
        assertTrue(tasks!!.isNotEmpty())
    }

    @Test
    fun firestoreProgressRepository_tracksUserProgress() = runTest {
        val progressRepo = FirestoreProgressRepository(firestore = null)
        val getResult = progressRepo.getProgress("u_123")
        assertTrue(getResult.isSuccess)

        val prog = getResult.getOrNull()
        assertNotNull(prog)

        val recordResult = progressRepo.recordActivity("u_123", 25, "VOCABULARY", 90)
        assertTrue(recordResult.isSuccess)
    }

    @Test
    fun firestoreConversationRepository_tracksConversation() = runTest {
        val convRepo = FirestoreConversationRepository(firestore = null)
        val testMsg = ConversationMessageDocument(
            messageId = "msg_1",
            role = "USER",
            message = "Hello!",
            intent = "GREETING"
        )
        val sendResult = convRepo.sendMessage("u_123", testMsg)
        assertTrue(sendResult.isSuccess)
    }

    @Test
    fun firestoreSettingsRepository_managesPreferences() = runTest {
        val settingsRepo = FirestoreSettingsRepository(firestore = null)
        val getResult = settingsRepo.getSettings("u_123")
        assertTrue(getResult.isSuccess)

        val updateResult = settingsRepo.updateNotificationPreference("u_123", false)
        assertTrue(updateResult.isSuccess)
    }
}
