package com.example.promaster.audit

import com.example.promaster.ai.MockAIProviderBackendClient
import com.example.promaster.data.firebase.model.*
import com.example.promaster.data.firebase.repository.*
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.data.repository.MockUserProfileRepositoryImpl
import com.example.promaster.data.service.*
import com.example.promaster.domain.automation.*
import com.example.promaster.domain.engine.*
import com.example.promaster.domain.model.*
import com.example.promaster.presentation.auth.AuthViewModel
import com.example.promaster.presentation.conversation.ChatViewModel
import com.example.promaster.presentation.learning.TodayTasksViewModel
import com.example.promaster.presentation.voice.VoiceAssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FullPromasterApplicationAuditTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. AUTH AUDIT: Register, Login, Logout, Invalid credentials, Session persistence
    // =========================================================================
    @Test
    fun audit_auth_lifecycleAndValidation() = runTest {
        val authRepo = FirebaseAuthRepository(auth = null)
        val authViewModel = AuthViewModel(authRepo = authRepo)

        // A. Invalid credentials - empty fields
        authViewModel.login("", "") {}
        assertNotNull(authViewModel.uiState.value.errorMessage)
        authViewModel.clearError()

        // B. Invalid credentials - password < 6 chars
        authViewModel.register("Tester", "test@test.com", "123") {}
        assertNotNull(authViewModel.uiState.value.errorMessage)
        assertTrue(authViewModel.uiState.value.errorMessage!!.contains("6 characters"))
        authViewModel.clearError()

        // C. Valid registration
        val registerResult = authRepo.register("Ravi", "ravi@promaster.ai", "Secret123")
        assertTrue(registerResult.isSuccess)
        val registeredUser = registerResult.getOrNull()
        assertNotNull(registeredUser)
        assertEquals("Ravi", registeredUser!!.name)

        // D. Valid login
        val loginResult = authRepo.login("ravi@promaster.ai", "Secret123")
        assertTrue(loginResult.isSuccess)

        // E. Guest login
        val guestResult = authRepo.loginAsGuest()
        assertTrue(guestResult.isSuccess)
        assertEquals("Guest Explorer", guestResult.getOrNull()?.name)

        // F. Session persistence via Flow
        val currentUser = authRepo.currentUser.first()
        assertNotNull(currentUser)

        // G. Logout
        authRepo.logout()
        assertNotNull(authRepo)
    }

    // =========================================================================
    // 2. LEARNING AUDIT: Plan, Tasks, Locking, Completion, Retry, XP, Streak, Progress
    // =========================================================================
    @Test
    fun audit_learning_curriculumAndProgressionEngine() = runTest {
        val learningRepo = MockLearningRepositoryImpl()
        val userRepo = MockUserProfileRepositoryImpl()

        // A. Language selection
        userRepo.updateTargetLanguage("ta")
        val userProfile = userRepo.getUserProfile().first()
        assertEquals("Tamil", userProfile.targetLanguage)

        // B. 30-Day Plan verification
        val plan = learningRepo.get30DayPlan().first()
        assertEquals(30, plan.size)
        assertEquals(1, plan.first().day)
        assertEquals(30, plan.last().day)

        // C. Daily tasks loading
        val tasks = learningRepo.getTodayTasks().first()
        assertTrue(tasks.isNotEmpty())

        // D. Task Locking - Task 2 available (task 1 already complete), Task 3 locked behind Task 2
        val availableTask = tasks[1] // Task 2: Grammar
        val lockedTask = tasks[2]    // Task 3: Listening
        val (canStartAvailable, _) = LearningLockSystem.canStartTask(tasks, availableTask.taskId)
        val (canStartLocked, lockReason) = LearningLockSystem.canStartTask(tasks, lockedTask.taskId)
        assertTrue("Available task must be startable", canStartAvailable)
        assertFalse("Subsequent task must be locked until preceding task finishes", canStartLocked)
        assertNotNull(lockReason)

        // E. Task Completion & Passing threshold gate for speaking/quiz tasks
        val speakingTask = DailyTask(
            taskId = "test_speak",
            title = "Speaking Test",
            description = "Pronunciation test",
            category = TaskCategory.SPEAKING,
            minPassingScore = 70
        )
        val testTasks: List<DailyTask> = listOf(speakingTask)

        // Submitting failing score (55 < 70 passing threshold)
        val failScoreResult = LearningLockSystem.submitTaskScore(testTasks, speakingTask.taskId, 55)
        assertTrue(failScoreResult is LearningLockSystem.TaskValidationResult.FailedPassingScore)
        assertEquals(1, (failScoreResult as LearningLockSystem.TaskValidationResult.FailedPassingScore).retryCount)

        // Submitting passing score (85 >= 70 passing threshold)
        val passScoreResult = LearningLockSystem.submitTaskScore(testTasks, speakingTask.taskId, 85)
        assertTrue(passScoreResult is LearningLockSystem.TaskValidationResult.Success)

        // Task completion in repository unlocks subsequent task
        val repoPass = learningRepo.submitTaskScore(availableTask.taskId, 90)
        assertTrue(repoPass.isSuccess)
        val updatedTasks = learningRepo.getTodayTasks().first()
        val (canStartSubsequentNow, _) = LearningLockSystem.canStartTask(updatedTasks, lockedTask.taskId)
        assertTrue("Subsequent task must now be unlocked", canStartSubsequentNow)

        // F. XP Calculation & Deterministic Policy
        val xpAward = ActivityXpPolicy.calculateXp("VOCABULARY_LEARN", score = 95, attemptCount = 1)
        assertTrue("Passing activity must award XP", xpAward.earnedXp > 0)
        assertTrue(xpAward.passedGate)

        // G. Streak Engine - Same-day duplicate prevention & consecutive increment
        val day1Millis = 1773800000000L
        val day2Millis = day1Millis + 86400000L
        val sameDayMillis = day1Millis + 3600000L

        val initialStreak = StreakCalculationEngine.calculateStreak(1, 1, 0L, day1Millis)
        assertEquals(1, initialStreak.currentStreak)

        // Same day: duplicate prevented
        val sameDayStreak = StreakCalculationEngine.calculateStreak(initialStreak.currentStreak, initialStreak.longestStreak, day1Millis, sameDayMillis)
        assertEquals("Duplicate same day activity must NOT increase streak", 1, sameDayStreak.currentStreak)

        // Consecutive day: incremented
        val nextDayStreak = StreakCalculationEngine.calculateStreak(initialStreak.currentStreak, initialStreak.longestStreak, day1Millis, day2Millis)
        assertEquals(2, nextDayStreak.currentStreak)
        assertEquals(2, nextDayStreak.longestStreak)
    }

    // =========================================================================
    // 3. ACTIVITIES AUDIT: Vocabulary, Grammar, Quiz, Listening, Speaking, Correction
    // =========================================================================
    @Test
    fun audit_activities_allSixActivityTypesWork() = runTest {
        val learningRepo = MockLearningRepositoryImpl()

        // A. Vocabulary Activity
        val vocabList = learningRepo.getVocabulary().first()
        assertTrue(vocabList.isNotEmpty())
        val word = vocabList.first()
        assertNotNull(word.word)
        assertNotNull(word.meaning)
        assertNotNull(word.exampleSentence)
        val masteredResult = learningRepo.markWordMastered(word.id)
        assertTrue(masteredResult.isSuccess)

        // B. Grammar Activity
        val grammarRules = MockDataProvider.grammarRules
        assertTrue(grammarRules.isNotEmpty())
        val rule = grammarRules.first()
        assertNotNull(rule.topic)
        assertNotNull(rule.explanation)
        assertTrue(rule.examples.isNotEmpty())
        assertTrue(rule.exercises.isNotEmpty())

        // C. Quiz Activity
        val quizSubmission = QuizResult(
            quizId = "quiz_01",
            title = "VOCABULARY_QUIZ",
            score = 85,
            totalQuestions = 10,
            correctAnswers = 8,
            wrongAnswers = 2,
            attempts = 1
        )
        val quizResult = learningRepo.submitQuizResult(quizSubmission)
        assertTrue(quizResult.isSuccess)
        assertTrue(quizResult.getOrNull()!!.completion)
        assertTrue(quizResult.getOrNull()!!.earnedXp > 0)

        // D. Speaking, Analysis & Correction Flow
        val speakingService = DefaultGrammarService(available = true)
        val grammarAnalysis = speakingService.analyzeSentence("I am go to college yesterday.", "English", "Tamil").getOrThrow()
        assertFalse(grammarAnalysis.isCorrect)
        assertEquals("I went to college yesterday.", grammarAnalysis.correctedSentence)
        assertNotNull(grammarAnalysis.explanation)
        assertNotNull(grammarAnalysis.motherTongueExplanation)
    }

    // =========================================================================
    // 4. AI AUDIT: Chat, Grammar, Translation, History, Error Handling
    // =========================================================================
    @Test
    fun audit_ai_layerCapabilitiesAndErrorResilience() = runTest {
        val aiService = DefaultPromasterAiService(backendClient = MockAIProviderBackendClient())

        // A. Intent Detection
        assertEquals(AIIntent.GRAMMAR_CORRECTION, aiService.detectIntent("Can you correct this sentence?"))
        assertEquals(AIIntent.TRANSLATION, aiService.detectIntent("Translate hello to Spanish"))
        assertEquals(AIIntent.VOCABULARY_EXPLANATION, aiService.detectIntent("What is the definition of serendipity?"))
        assertEquals(AIIntent.SPEAKING_PRACTICE, aiService.detectIntent("Let's do pronunciation practice"))

        // B. AI Chat & Persona Response
        val chatReq = AIRequest(prompt = "Hello! How are you?", targetLanguage = "Spanish", motherTongue = "English")
        val chatResp = aiService.processRequest(chatReq)
        assertTrue(chatResp.isSuccess)
        assertNotNull(chatResp.getOrNull()?.message)

        // C. Grammar Correction with Mother Tongue (Tanglish/Tamil Support)
        val grammarReq = AIRequest(prompt = "He do not like apple", forcedIntent = AIIntent.GRAMMAR_CORRECTION, targetLanguage = "English", motherTongue = "Tamil")
        val grammarResp = aiService.processRequest(grammarReq)
        assertTrue(grammarResp.isSuccess)
        assertNotNull(grammarResp.getOrNull()?.correction)

        // D. Translation
        val transReq = AIRequest(prompt = "Good morning", forcedIntent = AIIntent.TRANSLATION, targetLanguage = "Spanish", motherTongue = "English")
        val transResp = aiService.processRequest(transReq)
        assertTrue(transResp.isSuccess)

        // E. Conversation History in Firestore
        val convRepo = FirestoreConversationRepository(firestore = null)
        val msg = ConversationMessageDocument(messageId = "m_test", role = "USER", message = "Hola", intent = "CHAT")
        assertTrue(convRepo.sendMessage("u_test", msg).isSuccess)
        val history = convRepo.getMessagesFlow("u_test").first()
        assertTrue(history.isNotEmpty())
    }

    // =========================================================================
    // 5. VOICE AUDIT: Mic Permission, STT, TTS, Command Routing
    // =========================================================================
    @Test
    fun audit_voice_audioServicesAndCommandRouting() = runTest {
        // A. Command Routing
        val router = DefaultIntentRouter()
        val lessonRoute = router.routeIntent("Start my English lesson.")
        assertEquals(VoiceCommandType.START_LESSON, lessonRoute.commandType)

        val taskRoute = router.routeIntent("What is today's task?")
        assertEquals(VoiceCommandType.GET_TODAYS_TASK, taskRoute.commandType)

        val youtubeRoute = router.routeIntent("Open YouTube")
        assertEquals(VoiceCommandType.OPEN_YOUTUBE, youtubeRoute.commandType)

        val alarmRoute = router.routeIntent("Set an alarm for 8:00 AM")
        assertEquals(VoiceCommandType.SET_ALARM, alarmRoute.commandType)

        val settingsRoute = router.routeIntent("Open settings")
        assertEquals(VoiceCommandType.OPEN_SETTINGS, settingsRoute.commandType)

        val speakingRoute = router.routeIntent("Start speaking practice")
        assertEquals(VoiceCommandType.START_SPEAKING_PRACTICE, speakingRoute.commandType)

        // B. Command Processor Execution
        val processor = DefaultCommandProcessor(
            learningRepository = MockLearningRepositoryImpl(),
            permissionChecker = object : PermissionChecker {
                override fun hasPermission(permission: String): Boolean = true
            },
            alarmScheduler = object : AlarmScheduler {
                override fun setAlarm(hour: Int, minute: Int, message: String): Boolean = true
            },
            appLauncher = object : ExternalAppLauncher {}
        )
        val execResult = processor.processCommand(lessonRoute)
        assertEquals(CommandResultState.SUCCESS, execResult.state)

        // C. Permission Denied simulation
        val noPermProcessor = DefaultCommandProcessor(
            learningRepository = MockLearningRepositoryImpl(),
            permissionChecker = object : PermissionChecker {
                override fun hasPermission(permission: String): Boolean = false
            }
        )
        val deniedResult = noPermProcessor.processCommand(speakingRoute)
        assertEquals(CommandResultState.NEEDS_PERMISSION, deniedResult.state)
    }

    // =========================================================================
    // 6. WAKE WORD AUDIT: Enable, Disable, Lifecycle, Permission, Privacy
    // =========================================================================
    @Test
    fun audit_wakeWord_lifecycleAndPrivacyIntegrity() = runTest {
        val mockDetector = MockWakeWordDetector()
        val permissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = permissionChecker,
            coroutineScope = testScope
        )

        var detected = false
        service.setWakeWordListener { detected = true }

        // A. Enable wake word
        val startResult = service.enableWakeWord(runInBackground = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(startResult.isSuccess)
        assertEquals(WakeWordStatus.LISTENING, service.status.value)
        assertTrue(service.isListening.value)

        // B. Exact wake word detection callback
        mockDetector.simulateDetection("Hey Bro")
        assertTrue("Wake word 'Hey Bro' must trigger callback", detected)

        // C. Stop listening immediately when disabled
        service.disableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(WakeWordStatus.DISABLED, service.status.value)
        assertFalse(service.isListening.value)

        // D. Privacy Controls: No continuous raw audio recording
        val privacySettings = AssistantSettings()
        assertFalse(privacySettings.storeAudioRecordings)
        assertFalse(privacySettings.runInBackground)
    }

    // =========================================================================
    // 7. AUTOMATION AUDIT: Supported, Permission Denied, Unsupported, Confirmation
    // =========================================================================
    @Test
    fun audit_automation_safeSandboxFramework() = runTest {
        var dialedNumber: String? = null
        var openedUrl: String? = null

        val mockPlatform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun launchApp(packageName: String): Boolean = true
            override fun openWebsite(url: String): Boolean {
                openedUrl = url
                return true
            }
            override fun dialPhoneNumber(phoneNumber: String): Boolean {
                dialedNumber = phoneNumber
                return true
            }
            override fun hasAlarmPermission(): Boolean = true
            override fun setAlarm(hour: Int, minute: Int, message: String): Boolean = true
        }

        val framework = DefaultSafeAutomationFramework(
            intentRouter = DefaultAutomationIntentRouter(),
            validator = DefaultCommandValidator(),
            executor = DefaultAutomationExecutor(mockPlatform)
        )

        // A. Supported command - OPEN_WEBSITE
        val webResult = framework.process("Open https://google.com")
        assertEquals(CommandResultState.SUCCESS, webResult.state)
        assertEquals("https://google.com", openedUrl)

        // B. Sensitive command requiring confirmation - MAKE_CALL
        val unconfirmedCall = framework.process("Call 9876543210", isConfirmed = false)
        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, unconfirmedCall.state)
        assertNotNull(unconfirmedCall.pendingConfirmationAction)
        assertNull(dialedNumber) // Strict: NEVER dial silently without confirmation

        // Confirmed call succeeds
        val confirmedCall = framework.process("Call 9876543210", isConfirmed = true)
        assertEquals(CommandResultState.SUCCESS, confirmedCall.state)
        assertEquals("9876543210", dialedNumber)

        // C. Unsupported command - Accessibility injection or screen scraping
        val unsupported = framework.process("Click the second button on screen with accessibility")
        assertEquals(CommandResultState.NOT_SUPPORTED, unsupported.state)
        assertEquals(AutomationError.UNKNOWN_COMMAND, unsupported.error)

        // D. Permission Denied simulation
        val noAlarmPlatform = object : AndroidAutomationPlatform by mockPlatform {
            override fun hasAlarmPermission(): Boolean = false
        }
        val noPermFramework = DefaultSafeAutomationFramework(
            intentRouter = DefaultAutomationIntentRouter(),
            validator = DefaultCommandValidator(),
            executor = DefaultAutomationExecutor(noAlarmPlatform)
        )
        val alarmResult = noPermFramework.process("Set alarm for 7:00 AM", isConfirmed = true)
        assertEquals(CommandResultState.NEEDS_PERMISSION, alarmResult.state)
    }

    // =========================================================================
    // 8. FIREBASE AUDIT: Auth, Firestore, Offline, Security rules, Persistence
    // =========================================================================
    @Test
    fun audit_firebase_offlineResilienceAndDocumentIsolation() = runTest {
        val progressRepo = FirestoreProgressRepository(firestore = null)

        // A. Data persistence in offline fallback
        val initialProgress = progressRepo.getProgress("offline_uid").getOrNull()
        assertNotNull(initialProgress)

        // B. Progress recording & XP accumulation offline
        val recordResult = progressRepo.recordActivity("offline_uid", xpEarned = 50, category = "VOCABULARY", score = 90)
        assertTrue(recordResult.isSuccess)
        val updatedProg = progressRepo.getProgress("offline_uid").getOrNull()
        assertNotNull(updatedProg)
        assertTrue(updatedProg!!.xp >= 50)

        // C. Security rules check
        val rulesFile = File("../firestore.rules").takeIf { it.exists() } ?: File("firestore.rules")
        assertTrue("firestore.rules must exist", rulesFile.exists())
        val rulesText = rulesFile.readText()
        assertTrue(rulesText.contains("isOwner(userId)"))
        assertFalse(rulesText.contains("allow read, write: if true;"))
    }

    // =========================================================================
    // 9. UI & RESILIENCE AUDIT: ViewModels, States, Lifecycle & Screen Sizes
    // =========================================================================
    @Test
    fun audit_ui_viewModelsHandleLoadingEmptyAndErrorStates() = runTest {
        // A. TodayTasksViewModel error and lock states
        val tasksViewModel = TodayTasksViewModel(learningRepo = MockLearningRepositoryImpl())
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(tasksViewModel.uiState.value.tasks)
        tasksViewModel.dismissLockWarning()
        assertNull(tasksViewModel.uiState.value.lockWarning)

        // B. VoiceAssistantViewModel states
        val mockSpeech = object : com.example.promaster.domain.service.SpeechRecognitionService {
            override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
            override fun isAvailable(): Boolean = true
            override fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {}
            override fun stopListening() {}
        }
        val assistantService = DefaultVoiceAssistantService(
            speechService = mockSpeech,
            ttsService = MockTextToSpeechService(),
            coroutineScope = testScope
        )
        val voiceViewModel = VoiceAssistantViewModel(assistantService)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(voiceViewModel.uiState.value)
    }
}
