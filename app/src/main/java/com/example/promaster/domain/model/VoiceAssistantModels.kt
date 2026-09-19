package com.example.promaster.domain.model

/**
 * Strict command execution result states required by PROMASTER Voice Assistant.
 * Never pretend unsupported actions succeeded.
 */
enum class CommandResultState {
    SUCCESS,
    FAILED,
    NEEDS_PERMISSION,
    NOT_SUPPORTED,
    CONFIRMATION_REQUIRED
}

/**
 * Supported voice command types.
 */
enum class VoiceCommandType(val description: String) {
    START_LESSON("Start the user's current or specified language lesson"),
    GET_TODAYS_TASK("Retrieve and summarize today's learning tasks"),
    OPEN_YOUTUBE("Launch the YouTube app or educational language channels"),
    SET_ALARM("Schedule a study reminder or morning learning alarm"),
    OPEN_SETTINGS("Navigate to app settings and preferences"),
    START_SPEAKING_PRACTICE("Launch interactive microphone-based speaking drills"),
    AUTOMATION_ACTION("Trigger safe predefined on-device automation routines"),
    MAKE_CALL("Open dialer to call a verified phone number with explicit user confirmation"),
    OPEN_WEBSITE("Open a validated web URL in the system browser"),
    AI_CONVERSATION("Engage in interactive AI conversational learning with PROMASTER"),
    UNSUPPORTED("Unrecognized or unsupported voice command query")
}

/**
 * Classified voice intent emitted by the IntentRouter.
 */
data class RoutedVoiceIntent(
    val commandType: VoiceCommandType,
    val rawText: String,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f
)

/**
 * Standardized execution result returned by the CommandProcessor.
 */
data class CommandExecutionResult(
    val state: CommandResultState,
    val spokenFeedback: String,
    val errorCode: String? = null,
    val requiredPermission: String? = null,
    val targetDestination: String? = null,
    val pendingConfirmationAction: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Reactive state representing the Voice Assistant UI and audio pipeline.
 */
data class VoiceAssistantState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val recognizedText: String = "",
    val assistantReply: String = "PROMASTER Voice Assistant is ready. Tap the orb to begin speaking.",
    val lastResultState: CommandResultState? = null,
    val requiredPermission: String? = null,
    val pendingConfirmation: String? = null,
    val targetDestination: String? = null,
    val errorMessage: String? = null
)
