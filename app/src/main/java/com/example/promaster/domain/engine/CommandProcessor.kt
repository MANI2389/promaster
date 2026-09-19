package com.example.promaster.domain.engine

import android.Manifest
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.automation.DefaultSafeAutomationFramework
import com.example.promaster.domain.automation.AutomationError
import com.example.promaster.domain.automation.SafeAutomationFramework
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.LearningRepository
import com.example.promaster.domain.service.AIService
import com.example.promaster.navigation.Screen
import kotlinx.coroutines.flow.firstOrNull

/**
 * Platform hooks interface to allow testability without mocking Android framework classes directly.
 */
interface ExternalAppLauncher {
    fun isAppInstalled(packageName: String): Boolean = true
    fun launchApp(packageName: String): Boolean = true
    fun openWebUrl(url: String): Boolean = true
}

interface AlarmScheduler {
    fun hasAlarmPermission(): Boolean = true
    fun setAlarm(hour: Int, minute: Int, message: String): Boolean = true
}

interface PermissionChecker {
    fun hasPermission(permission: String): Boolean = true
}

interface SpeakingPracticeLauncher {
    fun isAvailable(): Boolean = true
    fun start(): Boolean = true
}

interface CommandProcessor {
    suspend fun processCommand(
        intent: RoutedVoiceIntent,
        confirmed: Boolean = false
    ): CommandExecutionResult
}

class DefaultCommandProcessor(
    private val learningRepository: LearningRepository = MockLearningRepositoryImpl(),
    private val appLauncher: ExternalAppLauncher = object : ExternalAppLauncher {},
    private val alarmScheduler: AlarmScheduler = object : AlarmScheduler {},
    private val permissionChecker: PermissionChecker = object : PermissionChecker {},
    private val automationFramework: SafeAutomationFramework = DefaultSafeAutomationFramework(),
    private val aiService: AIService = DefaultPromasterAiService(backendClient = com.example.promaster.ai.SecureBackendAiClient()),
    private val speakingPracticeLauncher: SpeakingPracticeLauncher = object : SpeakingPracticeLauncher {}
) : CommandProcessor {

    override suspend fun processCommand(
        intent: RoutedVoiceIntent,
        confirmed: Boolean
    ): CommandExecutionResult {
        return when (intent.commandType) {
            VoiceCommandType.START_LESSON -> handleStartLesson(intent)
            VoiceCommandType.GET_TODAYS_TASK -> handleGetTodaysTask()
            VoiceCommandType.OPEN_YOUTUBE -> handleOpenYouTube()
            VoiceCommandType.SET_ALARM -> handleSetAlarm(intent, confirmed)
            VoiceCommandType.OPEN_SETTINGS -> handleOpenSettings()
            VoiceCommandType.START_SPEAKING_PRACTICE -> handleStartSpeakingPractice()
            VoiceCommandType.MAKE_CALL -> handleAutomationCall(intent, confirmed)
            VoiceCommandType.OPEN_WEBSITE -> handleAutomationWebsite(intent, confirmed)
            VoiceCommandType.AUTOMATION_ACTION -> handleAutomationAction(intent, confirmed)
            VoiceCommandType.AI_CONVERSATION -> handleAiConversation(intent)
            VoiceCommandType.UNSUPPORTED -> handleUnsupported(intent)
        }
    }

    private suspend fun handleAutomationCall(intent: RoutedVoiceIntent, confirmed: Boolean): CommandExecutionResult {
        val result = automationFramework.process(intent.rawText, confirmed)
        return CommandExecutionResult(
            state = result.state,
            spokenFeedback = result.message,
            errorCode = result.error?.name,
            requiredPermission = result.requiredPermission,
            targetDestination = result.targetDestination,
            pendingConfirmationAction = result.pendingConfirmationAction,
            metadata = result.metadata
        )
    }

    private suspend fun handleAutomationWebsite(intent: RoutedVoiceIntent, confirmed: Boolean): CommandExecutionResult {
        val result = automationFramework.process(intent.rawText, confirmed)
        return CommandExecutionResult(
            state = result.state,
            spokenFeedback = result.message,
            errorCode = result.error?.name,
            requiredPermission = result.requiredPermission,
            targetDestination = result.targetDestination,
            pendingConfirmationAction = result.pendingConfirmationAction,
            metadata = result.metadata
        )
    }

    private fun handleStartLesson(intent: RoutedVoiceIntent): CommandExecutionResult {
        val language = intent.parameters["language"] ?: "English"
        val feedback = if (language.equals("Current", ignoreCase = true)) {
            "Starting your language lesson now. Let's make great progress!"
        } else {
            "Starting your $language lesson now. Let's make great progress!"
        }

        return CommandExecutionResult(
            state = CommandResultState.SUCCESS,
            spokenFeedback = feedback,
            targetDestination = Screen.AiConversation.route,
            metadata = mapOf("language" to language)
        )
    }

    private suspend fun handleGetTodaysTask(): CommandExecutionResult {
        val tasks = try {
            learningRepository.getTodayTasks().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val total = tasks.size
        val completed = tasks.count { it.isCompleted }

        val feedback = when {
            total == 0 -> "You have no tasks remaining for today. Great job keeping your streak active!"
            completed == total -> "All $total of today's tasks are completed! Awesome dedication."
            else -> {
                val nextTask = tasks.firstOrNull { !it.isCompleted }?.title ?: "learning activity"
                "You have $total tasks today with $completed completed. Next up is $nextTask."
            }
        }

        return CommandExecutionResult(
            state = CommandResultState.SUCCESS,
            spokenFeedback = feedback,
            targetDestination = Screen.TodayTasks.route,
            metadata = mapOf("totalTasks" to total.toString(), "completedTasks" to completed.toString())
        )
    }

    private fun handleOpenYouTube(): CommandExecutionResult {
        val ytPackage = "com.google.android.youtube"
        val appAvailable = appLauncher.isAppInstalled(ytPackage)

        if (appAvailable) {
            val launched = appLauncher.launchApp(ytPackage)
            if (launched) {
                return CommandExecutionResult(
                    state = CommandResultState.SUCCESS,
                    spokenFeedback = "Opening YouTube.",
                    metadata = mapOf("target" to "youtube_app")
                )
            }
        }

        // Web fallback
        val webFallback = appLauncher.openWebUrl("https://www.youtube.com")
        if (webFallback) {
            return CommandExecutionResult(
                state = CommandResultState.SUCCESS,
                spokenFeedback = "Opening YouTube in browser.",
                metadata = mapOf("target" to "youtube_web")
            )
        }

        // Never pretend unsupported actions succeeded
        return CommandExecutionResult(
            state = CommandResultState.FAILED,
            spokenFeedback = "Unable to open YouTube on this device.",
            errorCode = AutomationError.APP_LAUNCH_FAILED.name
        )
    }

    private fun handleSetAlarm(intent: RoutedVoiceIntent, confirmed: Boolean): CommandExecutionResult {
        val alarmPermission = "com.android.alarm.permission.SET_ALARM"
        if (!alarmScheduler.hasAlarmPermission()) {
            return CommandExecutionResult(
                state = CommandResultState.NEEDS_PERMISSION,
                spokenFeedback = "Alarm permission is required to schedule your study reminder.",
                requiredPermission = alarmPermission
            )
        }

        val timeStr = intent.parameters["time"]
        if (timeStr.isNullOrBlank() && !confirmed) {
            return CommandExecutionResult(
                state = CommandResultState.CONFIRMATION_REQUIRED,
                spokenFeedback = "Would you like to set your daily learning alarm for 8:00 AM?",
                pendingConfirmationAction = "SET_ALARM_8AM",
                metadata = mapOf("defaultTime" to "08:00")
            )
        }

        val (hour, minute) = if (confirmed || timeStr.isNullOrBlank()) {
            Pair(8, 0)
        } else {
            parseHourMinute(timeStr) ?: return CommandExecutionResult(
                state = CommandResultState.FAILED,
                spokenFeedback = "I couldn't understand the alarm time.",
                errorCode = AutomationError.INVALID_ALARM_TIME.name
            )
        }

        val success = alarmScheduler.setAlarm(hour, minute, "PROMASTER Daily Language Practice")
        return if (success) {
            val formattedTime = String.format("%02d:%02d", hour, minute)
            CommandExecutionResult(
                state = CommandResultState.SUCCESS,
                spokenFeedback = "Study alarm successfully set for $formattedTime.",
                metadata = mapOf("scheduledTime" to formattedTime)
            )
        } else {
            CommandExecutionResult(
                state = CommandResultState.FAILED,
                spokenFeedback = "Failed to schedule the study alarm.",
                errorCode = AutomationError.ALARM_CREATION_FAILED.name
            )
        }
    }

    private fun handleOpenSettings(): CommandExecutionResult {
        return CommandExecutionResult(
            state = CommandResultState.SUCCESS,
            spokenFeedback = "Opening settings.",
            targetDestination = Screen.Settings.route
        )
    }

    private fun handleStartSpeakingPractice(): CommandExecutionResult {
        val micPermission = Manifest.permission.RECORD_AUDIO
        if (!permissionChecker.hasPermission(micPermission)) {
            return CommandExecutionResult(
                state = CommandResultState.NEEDS_PERMISSION,
                spokenFeedback = "Microphone permission is required for speaking practice.",
                requiredPermission = micPermission
            )
        }

        if (!speakingPracticeLauncher.isAvailable()) {
            return CommandExecutionResult(
                state = CommandResultState.FAILED,
                spokenFeedback = "I couldn't start speaking practice.",
                errorCode = AutomationError.SPEAKING_PRACTICE_START_FAILED.name
            )
        }
        if (!speakingPracticeLauncher.start()) {
            return CommandExecutionResult(
                state = CommandResultState.FAILED,
                spokenFeedback = "I couldn't start speaking practice.",
                errorCode = AutomationError.SPEAKING_PRACTICE_START_FAILED.name
            )
        }

        return CommandExecutionResult(
            state = CommandResultState.SUCCESS,
            spokenFeedback = "Starting speaking practice. Speak clearly into your microphone!",
            targetDestination = Screen.SpeakingPractice.route
        )
    }

    private suspend fun handleAutomationAction(intent: RoutedVoiceIntent, confirmed: Boolean): CommandExecutionResult {
        // If this is a specific app launch or routine, execute through safe automation framework
        val result = automationFramework.process(intent.rawText, confirmed)
        return CommandExecutionResult(
            state = result.state,
            spokenFeedback = result.message,
            errorCode = result.error?.name,
            requiredPermission = result.requiredPermission,
            targetDestination = result.targetDestination,
            pendingConfirmationAction = result.pendingConfirmationAction,
            metadata = result.metadata
        )
    }

    private fun handleUnsupported(intent: RoutedVoiceIntent): CommandExecutionResult {
        // Strict contract: Never pretend unsupported actions succeeded
        return CommandExecutionResult(
            state = CommandResultState.NOT_SUPPORTED,
            spokenFeedback = "I'm sorry, '${intent.rawText}' is not currently supported. You can ask me to start lessons, check today's tasks, open YouTube, set study alarms, open settings, or start speaking practice."
        )
    }

    private suspend fun handleAiConversation(intent: RoutedVoiceIntent): CommandExecutionResult {
        val prefs = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()
        val targetLang = prefs?.targetLanguage ?: "English"
        val motherTongue = prefs?.motherTongue ?: "Tamil"

        val request = AIRequest(
            prompt = intent.rawText,
            conversationId = "voice_session",
            targetLanguage = targetLang,
            motherTongue = motherTongue,
            userLevel = LanguageLevel.INTERMEDIATE,
            forcedIntent = AIIntent.AI_ASSISTANT
        )
        val result = aiService.processRequest(request)
        return if (result.isSuccess) {
            val aiResponse = result.getOrNull()
            if (aiResponse == null) {
                return CommandExecutionResult(
                    state = CommandResultState.FAILED,
                    spokenFeedback = "The AI service returned an invalid response.",
                    errorCode = AutomationError.BACKEND_INVALID_RESPONSE.name
                )
            }

            // If Gemini returned a structured action (e.g. open app or set alarm), execute it
            val structuredAction = aiResponse.action
            if (structuredAction != null && structuredAction.intent == "OPEN_APP" && !structuredAction.appName.isNullOrBlank()) {
                val autoResult = automationFramework.process("Open ${structuredAction.appName}", true)
                return CommandExecutionResult(
                    state = autoResult.state,
                    spokenFeedback = if (autoResult.state == CommandResultState.SUCCESS) aiResponse.message else autoResult.message,
                    errorCode = autoResult.error?.name,
                    targetDestination = autoResult.targetDestination,
                    metadata = autoResult.metadata
                )
            } else if (structuredAction != null && structuredAction.intent == "SET_ALARM") {
                val h = structuredAction.parameters["hour"] ?: 7
                val m = structuredAction.parameters["minute"] ?: 0
                val autoResult = automationFramework.process("Set alarm for $h:$m", true)
                return CommandExecutionResult(
                    state = autoResult.state,
                    spokenFeedback = if (autoResult.state == CommandResultState.SUCCESS) aiResponse.message else autoResult.message,
                    errorCode = autoResult.error?.name,
                    metadata = autoResult.metadata
                )
            }

            val replyText = aiResponse.message
            CommandExecutionResult(
                state = CommandResultState.SUCCESS,
                spokenFeedback = replyText,
                targetDestination = Screen.AiConversation.route,
                metadata = mapOf(
                    "source" to "ai_service",
                    "intent" to (aiResponse.intent.name),
                    "confidence" to (aiResponse.confidence).toString()
                )
            )
        } else {
            CommandExecutionResult(
                state = CommandResultState.FAILED,
                spokenFeedback = "The AI service is temporarily unavailable.",
                errorCode = classifyBackendError(result.exceptionOrNull()).name
            )
        }
    }
    private fun classifyBackendError(error: Throwable?): AutomationError {
        val text = error?.message.orEmpty().lowercase()
        return when {
            text.contains("401") || text.contains("unauthorized") -> AutomationError.BACKEND_UNAUTHORIZED
            text.contains("429") || text.contains("rate") -> AutomationError.BACKEND_RATE_LIMITED
            text.contains("timeout") -> AutomationError.BACKEND_TIMEOUT
            text.contains("network") || text.contains("offline") || text.contains("connect") -> AutomationError.NETWORK_UNAVAILABLE
            text.contains("500") || text.contains("server") -> AutomationError.BACKEND_SERVER_ERROR
            else -> AutomationError.AI_CONVERSATION_START_FAILED
        }
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int>? {
        val lower = timeStr.lowercase().trim()
        val isPm = lower.contains("pm")
        val digitsOnly = lower.replace("am", "").replace("pm", "").trim()

        val parts = digitsOnly.split(":")
        val rawHour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val hour = when {
            isPm && rawHour < 12 -> rawHour + 12
            !isPm && rawHour == 12 -> 0
            else -> rawHour
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }
}
