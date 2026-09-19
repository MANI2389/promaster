package com.example.promaster.domain.automation

import com.example.promaster.domain.model.CommandResultState

/**
 * Commands supported by the PROMASTER safe Android automation framework.
 * Restricted strictly to operations permitted for normal Android applications.
 */
enum class AutomationCommandType(val description: String) {
    OPEN_APP("Launch an installed application or fallback to safe alternative"),
    OPEN_WEBSITE("Open a validated web URL in the system browser"),
    CREATE_ALARM("Schedule a learning reminder or daily alarm"),
    OPEN_SETTINGS("Navigate to Android system settings panels"),
    MAKE_CALL("Open system dialer with pre-filled number after explicit confirmation"),
    START_LANGUAGE_LESSON("Initiate language curriculum and AI conversation"),
    UNSUPPORTED("Unsupported, restricted, or requires accessibility services")
}

enum class AutomationError {
    UNKNOWN_COMMAND,
    EMPTY_COMMAND,
    INVALID_ARGUMENT,
    APP_NOT_FOUND,
    APP_LAUNCH_FAILED,
    INVALID_URL,
    UNSAFE_URL,
    URL_OPEN_FAILED,
    INVALID_ALARM_TIME,
    ALARM_CREATION_FAILED,
    SETTINGS_NOT_SUPPORTED,
    SETTINGS_OPEN_FAILED,
    LESSON_NOT_FOUND,
    LESSON_START_FAILED,
    SPEAKING_PRACTICE_START_FAILED,
    AI_CONVERSATION_START_FAILED,
    PERMISSION_DENIED,
    PERMISSION_REVOKED,
    CONFIRMATION_DENIED,
    CONFIRMATION_EXPIRED,
    ACTIVITY_NOT_FOUND,
    NETWORK_UNAVAILABLE,
    BACKEND_TIMEOUT,
    BACKEND_UNAUTHORIZED,
    BACKEND_RATE_LIMITED,
    BACKEND_SERVER_ERROR,
    BACKEND_INVALID_RESPONSE,
    AI_INTENT_UNSAFE,
    AI_INTENT_INVALID,
    EXECUTOR_EXCEPTION,
    DUPLICATE_COMMAND,
    WAKE_WORD_UNAVAILABLE,
    UNKNOWN_ERROR
}

/**
 * Parsed automation command with raw input, parameters, and confirmation state.
 */
data class AutomationCommand(
    val type: AutomationCommandType,
    val rawInput: String,
    val parameters: Map<String, String> = emptyMap(),
    val isConfirmed: Boolean = false
)

/**
 * Security validation outcome produced by CommandValidator before execution.
 */
sealed interface ValidationResult {
    data class Valid(val command: AutomationCommand) : ValidationResult
    data class ConfirmationRequired(
        val command: AutomationCommand,
        val prompt: String,
        val actionToken: String
    ) : ValidationResult
    data class PermissionRequired(
        val command: AutomationCommand,
        val permission: String,
        val rationale: String
    ) : ValidationResult
    data class NotPermitted(
        val command: AutomationCommand,
        val reason: String,
        val error: AutomationError = AutomationError.INVALID_ARGUMENT
    ) : ValidationResult
    data class Unsupported(
        val command: AutomationCommand,
        val reason: String,
        val error: AutomationError = AutomationError.UNKNOWN_COMMAND
    ) : ValidationResult
}

/**
 * Standardized execution result returned by AutomationExecutor and SafeAutomationFramework.
 * Conforms strictly to the 5 mandatory result states.
 */
data class AutomationResult(
    val state: CommandResultState,
    val message: String,
    val error: AutomationError? = null,
    val requiredPermission: String? = null,
    val targetDestination: String? = null,
    val pendingConfirmationAction: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val NOT_SUPPORTED_MESSAGE = "Not supported on this device/version."
    }
}
