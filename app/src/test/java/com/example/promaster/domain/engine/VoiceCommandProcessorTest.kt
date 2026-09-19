package com.example.promaster.domain.engine

import android.Manifest
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VoiceCommandProcessorTest {

    private val learningRepo = MockLearningRepositoryImpl()

    @Test
    fun processCommand_startLesson_returnsSuccess() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.START_LESSON,
            rawText = "Start my English lesson.",
            parameters = mapOf("language" to "English")
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.spokenFeedback.contains("English lesson", ignoreCase = true))
        assertNotNull(result.targetDestination)
    }

    @Test
    fun processCommand_getTodaysTask_returnsSuccessWithSummary() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.GET_TODAYS_TASK,
            rawText = "What is today's task?"
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.spokenFeedback.isNotBlank())
        assertEquals("today_tasks", result.targetDestination)
    }

    @Test
    fun processCommand_openSettings_returnsSuccess() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.OPEN_SETTINGS,
            rawText = "Open settings."
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("settings", result.targetDestination)
    }

    @Test
    fun processCommand_openYouTube_failsCleanlyWhenAppAndWebBothUnavailable() = runBlocking {
        val failingLauncher = object : ExternalAppLauncher {
            override fun isAppInstalled(packageName: String): Boolean = false
            override fun launchApp(packageName: String): Boolean = false
            override fun openWebUrl(url: String): Boolean = false
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            appLauncher = failingLauncher
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.OPEN_YOUTUBE,
            rawText = "Open YouTube."
        )

        val result = processor.processCommand(intent)
        // Strict contract: Never pretend unsupported or failing actions succeeded
        assertEquals(CommandResultState.FAILED, result.state)
        assertNotEquals(CommandResultState.SUCCESS, result.state)
    }

    @Test
    fun processCommand_openYouTube_succeedsWhenAvailable() = runBlocking {
        val workingLauncher = object : ExternalAppLauncher {
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun launchApp(packageName: String): Boolean = true
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            appLauncher = workingLauncher
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.OPEN_YOUTUBE,
            rawText = "Open YouTube."
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
    }

    @Test
    fun processCommand_setAlarm_returnsNeedsPermissionWhenPermissionDenied() = runBlocking {
        val noPermissionAlarm = object : AlarmScheduler {
            override fun hasAlarmPermission(): Boolean = false
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            alarmScheduler = noPermissionAlarm
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.SET_ALARM,
            rawText = "Set an alarm."
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.NEEDS_PERMISSION, result.state)
        assertEquals("com.android.alarm.permission.SET_ALARM", result.requiredPermission)
    }

    @Test
    fun processCommand_setAlarm_requiresConfirmationWhenTimeUnspecified() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.SET_ALARM,
            rawText = "Set an alarm."
        )

        val unconfirmedResult = processor.processCommand(intent, confirmed = false)
        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, unconfirmedResult.state)
        assertNotNull(unconfirmedResult.pendingConfirmationAction)

        val confirmedResult = processor.processCommand(intent, confirmed = true)
        assertEquals(CommandResultState.SUCCESS, confirmedResult.state)
    }

    @Test
    fun processCommand_startSpeakingPractice_returnsNeedsPermissionWhenMicDenied() = runBlocking {
        val noMicPermission = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = false
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            permissionChecker = noMicPermission
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.START_SPEAKING_PRACTICE,
            rawText = "Start speaking practice."
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.NEEDS_PERMISSION, result.state)
        assertEquals(Manifest.permission.RECORD_AUDIO, result.requiredPermission)
    }

    @Test
    fun processCommand_startSpeakingPractice_returnsSuccessWhenMicGranted() = runBlocking {
        val grantedPermission = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            permissionChecker = grantedPermission
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.START_SPEAKING_PRACTICE,
            rawText = "Start speaking practice."
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("speaking_practice", result.targetDestination)
    }

    @Test
    fun processCommand_unsupportedCommand_strictlyReturnsNotSupported() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.UNSUPPORTED,
            rawText = "Book me a hotel room in Paris"
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.NOT_SUPPORTED, result.state)
        assertNotEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.spokenFeedback.contains("not currently supported", ignoreCase = true))
    }

    @Test
    fun processCommand_aiConversation_routesToAiServiceAndReturnsSuccess() = runBlocking {
        val mockAiService = object : com.example.promaster.domain.service.AIService by com.example.promaster.data.service.DefaultPromasterAiService() {
            override suspend fun processRequest(request: AIRequest): Result<AIResponse> {
                return Result.success(
                    AIResponse(
                        message = "Hello bro! I am your PROMASTER AI partner.",
                        intent = AIIntent.GENERAL_CONVERSATION
                    )
                )
            }
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            aiService = mockAiService
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.AI_CONVERSATION,
            rawText = "Hello bro, introduce yourself"
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("ai_conversation", result.targetDestination)
        assertTrue(result.spokenFeedback.contains("PROMASTER AI partner"))
    }

    @Test
    fun processCommand_aiConversation_handlesBackendFailureGracefully() = runBlocking {
        val failingAiService = object : com.example.promaster.domain.service.AIService by com.example.promaster.data.service.DefaultPromasterAiService() {
            override suspend fun processRequest(request: AIRequest): Result<AIResponse> {
                return Result.failure(RuntimeException("Network offline"))
            }
        }

        val processor = DefaultCommandProcessor(
            learningRepository = learningRepo,
            aiService = failingAiService
        )
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.AI_CONVERSATION,
            rawText = "Hello bro, introduce yourself"
        )

        val result = processor.processCommand(intent)
        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals("NETWORK_UNAVAILABLE", result.errorCode)
        assertTrue(result.spokenFeedback.contains("temporarily unavailable", ignoreCase = true))
    }

    @Test
    fun processCommand_makeCall_requiresConfirmationWhenUnconfirmed() = runBlocking {
        val processor = DefaultCommandProcessor(learningRepository = learningRepo)
        val intent = RoutedVoiceIntent(
            commandType = VoiceCommandType.MAKE_CALL,
            rawText = "Call 9876543210"
        )

        val unconfirmedResult = processor.processCommand(intent, confirmed = false)
        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, unconfirmedResult.state)
        assertNotNull(unconfirmedResult.pendingConfirmationAction)

        val confirmedResult = processor.processCommand(intent, confirmed = true)
        assertEquals(CommandResultState.SUCCESS, confirmedResult.state)
    }
}
