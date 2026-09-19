package com.example.promaster.domain.automation

import java.net.URI
import java.net.URISyntaxException

/**
 * Validates automation commands against security policies, normal application limits,
 * required confirmations, and parameter integrity.
 */
interface CommandValidator {
    fun validate(command: AutomationCommand): ValidationResult
}

class DefaultCommandValidator : CommandValidator {

    override fun validate(command: AutomationCommand): ValidationResult {
        return when (command.type) {
            AutomationCommandType.MAKE_CALL -> validateMakeCall(command)
            AutomationCommandType.CREATE_ALARM -> validateCreateAlarm(command)
            AutomationCommandType.OPEN_WEBSITE -> validateOpenWebsite(command)
            AutomationCommandType.OPEN_APP -> validateOpenApp(command)
            AutomationCommandType.OPEN_SETTINGS -> validateOpenSettings(command)
            AutomationCommandType.START_LANGUAGE_LESSON -> validateStartLanguageLesson(command)
            AutomationCommandType.UNSUPPORTED -> {
                val reason = command.parameters["reason"] ?: "This action is not supported on this device/version."
                val error = command.parameters["errorCode"]?.let { code ->
                    runCatching { AutomationError.valueOf(code) }.getOrNull()
                } ?: AutomationError.UNKNOWN_COMMAND
                ValidationResult.Unsupported(command, reason, error)
            }
        }
    }

    private fun validateMakeCall(command: AutomationCommand): ValidationResult {
        val rawPhone = command.parameters["phoneNumber"]?.trim().orEmpty()
        val digits = rawPhone.filter { it.isDigit() }

        // 1. Phone number structure validation
        if (digits.length < 3) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "Invalid phone number. A valid destination number is required."
            )
        }

        // 2. Sensitive action protection: NEVER perform calls silently.
        // Explicit user confirmation is strictly required before dialing.
        if (!command.isConfirmed) {
            return ValidationResult.ConfirmationRequired(
                command = command,
                prompt = "Are you sure you want to call $rawPhone?",
                actionToken = "CONFIRM_CALL_${digits}"
            )
        }

        return ValidationResult.Valid(command)
    }

    private fun validateCreateAlarm(command: AutomationCommand): ValidationResult {
        val timeStr = command.parameters["time"]?.trim()
        
        if (timeStr.isNullOrBlank() && command.rawInput.lowercase().let { it.contains("tomorrow") || it.contains("morning") || it.contains("sometime") }) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "I couldn't understand the alarm time.",
                error = AutomationError.INVALID_ALARM_TIME
            )
        }
        
        if (!timeStr.isNullOrBlank() && !Regex("^\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?$", RegexOption.IGNORE_CASE).matches(timeStr)) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "I couldn't understand the alarm time.",
                error = AutomationError.INVALID_ALARM_TIME
            )
        }
        
        if (!timeStr.isNullOrBlank()) {
            val parts = timeStr.lowercase().replace("am", "").replace("pm", "").trim().split(":")
            val hour = parts[0].toIntOrNull()
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val hasMeridiem = timeStr.contains("am", true) || timeStr.contains("pm", true)
            val validHour = hour != null && if (hasMeridiem) hour in 1..12 else hour in 0..23
            if (!validHour || minute !in 0..59) {
                return ValidationResult.NotPermitted(
                    command = command,
                    reason = "I couldn't understand the alarm time.",
                    error = AutomationError.INVALID_ALARM_TIME
                )
            }
        }

        // If time is unspecified and not confirmed, ask for confirmation of default time
        if (timeStr.isNullOrBlank() && !command.isConfirmed) {
            return ValidationResult.ConfirmationRequired(
                command = command,
                prompt = "Would you like to set your daily learning alarm for 8:00 AM?",
                actionToken = "CONFIRM_ALARM_8AM"
            )
        }

        return ValidationResult.Valid(command)
    }

    private fun validateOpenWebsite(command: AutomationCommand): ValidationResult {
        val url = command.parameters["url"]?.trim().orEmpty()

        if (url.isBlank()) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "I couldn't open that website.",
                error = AutomationError.INVALID_URL
            )
        }

        val lower = url.lowercase()

        // Block dangerous schemes
        val scheme = runCatching { URI(url).scheme?.lowercase() }.getOrNull()
        if (scheme != null && scheme != "http" && scheme != "https") {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "I can't open that type of link.",
                error = AutomationError.UNSAFE_URL
            )
        }

        // Must be http or https
        val parsedUrl = try {
            URI(url)
        } catch (_: URISyntaxException) {
            null
        }
        if (parsedUrl == null || scheme == null ||
            (scheme != "http" && scheme != "https") || parsedUrl.host.isNullOrBlank()
        ) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "I couldn't open that website.",
                error = AutomationError.INVALID_URL
            )
        }

        return ValidationResult.Valid(command)
    }

    private fun validateOpenApp(command: AutomationCommand): ValidationResult {
        val appName = command.parameters["appName"]?.trim().orEmpty()
        if (appName.isBlank()) {
            return ValidationResult.NotPermitted(
                command = command,
                reason = "App name is required to open an application."
            )
        }
        return ValidationResult.Valid(command)
    }

    private fun validateOpenSettings(command: AutomationCommand): ValidationResult {
        return ValidationResult.Valid(command)
    }

    private fun validateStartLanguageLesson(command: AutomationCommand): ValidationResult {
        return ValidationResult.Valid(command)
    }
}
