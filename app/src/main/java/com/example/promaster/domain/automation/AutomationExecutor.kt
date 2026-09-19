package com.example.promaster.domain.automation

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.navigation.Screen

/**
 * Platform hooks interface enabling complete testability of Android automation
 * on both unit tests and real Android device environments.
 */
interface AndroidAutomationPlatform {
    fun isAppInstalled(packageName: String): Boolean = true
    fun launchApp(packageName: String): Boolean = true
    fun openWebsite(url: String): Boolean = true
    fun openSettings(settingType: String): Boolean = true
    fun dialPhoneNumber(phoneNumber: String): Boolean = true
    fun hasAlarmPermission(): Boolean = true
    fun setAlarm(hour: Int, minute: Int, message: String): Boolean = true
    fun isLessonAvailable(language: String): Boolean = true
    fun startLanguageLesson(language: String): Boolean = true
    fun isActionSupportedOnDevice(type: AutomationCommandType): Boolean = true
}

/**
 * Real Android implementation invoking standard system intents.
 * Strictly avoids Accessibility Service permissions.
 */
class NativeAndroidAutomationPlatform(
    private val context: Context
) : AndroidAutomationPlatform {

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    override fun openWebsite(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return true
        }
        return false
    }

    override fun openSettings(settingType: String): Boolean {
        val action = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    override fun dialPhoneNumber(phoneNumber: String): Boolean {
        return try {
            // Normal Android app intent: ACTION_DIAL opens dialer with pre-filled number.
            // Does NOT perform silent background calls and does NOT require dangerous CALL_PHONE permission.
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun hasAlarmPermission(): Boolean = true

    override fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return true
        }
        return false
    }

    override fun isActionSupportedOnDevice(type: AutomationCommandType): Boolean {
        return when (type) {
            AutomationCommandType.UNSUPPORTED -> false
            else -> true
        }
    }
}

/**
 * Executes validated automation commands using standard, permitted Android APIs.
 */
interface AutomationExecutor {
    suspend fun execute(
        command: AutomationCommand,
        validationResult: ValidationResult
    ): AutomationResult
}

class DefaultAutomationExecutor(
    private val platform: AndroidAutomationPlatform = object : AndroidAutomationPlatform {}
) : AutomationExecutor {

    override suspend fun execute(
        command: AutomationCommand,
        validationResult: ValidationResult
    ): AutomationResult {
        // 1. Handle validation states directly
        when (validationResult) {
            is ValidationResult.ConfirmationRequired -> {
                return AutomationResult(
                    state = CommandResultState.CONFIRMATION_REQUIRED,
                    message = validationResult.prompt,
                    pendingConfirmationAction = validationResult.actionToken,
                    metadata = command.parameters
                )
            }
            is ValidationResult.PermissionRequired -> {
                return AutomationResult(
                    state = CommandResultState.NEEDS_PERMISSION,
                    message = validationResult.rationale,
                    requiredPermission = validationResult.permission,
                    metadata = command.parameters
                )
            }
            is ValidationResult.NotPermitted -> {
                return AutomationResult(
                    state = CommandResultState.FAILED,
                    message = validationResult.reason,
                    error = validationResult.error,
                    metadata = command.parameters
                )
            }
            is ValidationResult.Unsupported -> {
                return AutomationResult(
                    state = CommandResultState.NOT_SUPPORTED,
                    message = validationResult.reason,
                    error = validationResult.error,
                    metadata = command.parameters
                )
            }
            is ValidationResult.Valid -> {
                // Proceed to execution
            }
        }

        return try {
            if (!platform.isActionSupportedOnDevice(command.type)) {
                return AutomationResult(
                    state = CommandResultState.NOT_SUPPORTED,
                    message = AutomationResult.NOT_SUPPORTED_MESSAGE,
                    error = if (command.type == AutomationCommandType.OPEN_SETTINGS) {
                        AutomationError.SETTINGS_NOT_SUPPORTED
                    } else {
                        AutomationError.UNKNOWN_COMMAND
                    },
                    metadata = command.parameters
                )
            }

            when (command.type) {
                AutomationCommandType.OPEN_APP -> executeOpenApp(command)
                AutomationCommandType.OPEN_WEBSITE -> executeOpenWebsite(command)
                AutomationCommandType.CREATE_ALARM -> executeCreateAlarm(command)
                AutomationCommandType.OPEN_SETTINGS -> executeOpenSettings(command)
                AutomationCommandType.MAKE_CALL -> executeMakeCall(command)
                AutomationCommandType.START_LANGUAGE_LESSON -> executeStartLanguageLesson(command)
                AutomationCommandType.UNSUPPORTED -> AutomationResult(
                    state = CommandResultState.NOT_SUPPORTED,
                    message = AutomationResult.NOT_SUPPORTED_MESSAGE,
                    error = AutomationError.UNKNOWN_COMMAND
                )
            }
        } catch (_: ActivityNotFoundException) {
            AutomationResult(
                state = CommandResultState.FAILED,
                message = "The requested activity is unavailable.",
                error = AutomationError.ACTIVITY_NOT_FOUND,
                metadata = command.parameters
            )
        } catch (_: Exception) {
            AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't complete that action.",
                error = AutomationError.EXECUTOR_EXCEPTION,
                metadata = command.parameters
            )
        }
    }

    private fun executeOpenApp(command: AutomationCommand): AutomationResult {
        val appName = command.parameters["appName"]?.trim().orEmpty()
        val packageName = resolvePackageName(appName)

        val installed = platform.isAppInstalled(packageName)
        if (installed) {
            val launched = platform.launchApp(packageName)
            if (launched) {
                return AutomationResult(
                    state = CommandResultState.SUCCESS,
                    message = "Opening $appName.",
                    metadata = mapOf("target" to packageName)
                )
            }
        }

        if (!installed) {
            return AutomationResult(
                state = CommandResultState.FAILED,
                message = "Bro, that app isn't installed.",
                error = AutomationError.APP_NOT_FOUND,
                metadata = mapOf("appName" to appName)
            )
        }
        return AutomationResult(
            state = CommandResultState.FAILED,
            message = "I couldn't open that app.",
            error = AutomationError.APP_LAUNCH_FAILED,
            metadata = mapOf("appName" to appName)
        )
    }

    private fun executeOpenWebsite(command: AutomationCommand): AutomationResult {
        val url = command.parameters["url"]?.trim().orEmpty()
        val success = try {
            platform.openWebsite(url)
        } catch (_: ActivityNotFoundException) {
            return AutomationResult(CommandResultState.FAILED, "The requested activity is unavailable.", AutomationError.ACTIVITY_NOT_FOUND)
        } catch (_: Exception) {
            return AutomationResult(CommandResultState.FAILED, "I couldn't open that website.", AutomationError.URL_OPEN_FAILED)
        }

        return if (success) {
            AutomationResult(
                state = CommandResultState.SUCCESS,
                message = "Opening website: $url",
                metadata = mapOf("url" to url)
            )
        } else {
            AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't open that website.",
                error = AutomationError.URL_OPEN_FAILED,
                metadata = mapOf("url" to url)
            )
        }
    }

    private fun executeCreateAlarm(command: AutomationCommand): AutomationResult {
        val alarmPermission = "com.android.alarm.permission.SET_ALARM"
        if (!platform.hasAlarmPermission()) {
            return AutomationResult(
                state = CommandResultState.NEEDS_PERMISSION,
                message = "Alarm permission is required to schedule reminders.",
                requiredPermission = alarmPermission
            )
        }

        val timeStr = command.parameters["time"]
        val (hour, minute) = if (timeStr.isNullOrBlank()) {
            Pair(8, 0)
        } else {
            parseHourMinute(timeStr) ?: return AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't understand the alarm time.",
                error = AutomationError.INVALID_ALARM_TIME
            )
        }

        val success = try {
            platform.setAlarm(hour, minute, "PROMASTER Daily Language Practice")
        } catch (_: Exception) {
            false
        }
        return if (success) {
            val formattedTime = String.format("%02d:%02d", hour, minute)
            AutomationResult(
                state = CommandResultState.SUCCESS,
                message = "Study alarm successfully set for $formattedTime.",
                metadata = mapOf("scheduledTime" to formattedTime)
            )
        } else {
            AutomationResult(
                state = CommandResultState.FAILED,
                message = "Failed to set alarm on this device.",
                error = AutomationError.ALARM_CREATION_FAILED,
                metadata = mapOf("error" to "Alarm scheduler returned false")
            )
        }
    }

    private fun executeOpenSettings(command: AutomationCommand): AutomationResult {
        val settingType = command.parameters["settingType"] ?: "general"
        val success = try {
            platform.openSettings(settingType)
        } catch (_: ActivityNotFoundException) {
            return AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't open settings.",
                error = AutomationError.SETTINGS_OPEN_FAILED
            )
        } catch (_: Exception) {
            return AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't open settings.",
                error = AutomationError.SETTINGS_OPEN_FAILED
            )
        }

        return if (success) {
            AutomationResult(
                state = CommandResultState.SUCCESS,
                message = "Opening $settingType settings.",
                targetDestination = Screen.Settings.route,
                metadata = mapOf("settingType" to settingType)
            )
        } else {
            AutomationResult(
                state = CommandResultState.NOT_SUPPORTED,
                message = AutomationResult.NOT_SUPPORTED_MESSAGE,
                error = AutomationError.SETTINGS_NOT_SUPPORTED,
                metadata = mapOf("settingType" to settingType)
            )
        }
    }

    private fun executeMakeCall(command: AutomationCommand): AutomationResult {
        val phoneNumber = command.parameters["phoneNumber"]?.trim().orEmpty()

        // Normal Android application operation: Launch dialer
        val success = platform.dialPhoneNumber(phoneNumber)
        return if (success) {
            AutomationResult(
                state = CommandResultState.SUCCESS,
                message = "Opening phone dialer for $phoneNumber.",
                metadata = mapOf("phoneNumber" to phoneNumber)
            )
        } else {
            // Device does not support dialing (e.g. tablet without telephony or dialer activity)
            AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't complete that action.",
                error = AutomationError.APP_LAUNCH_FAILED,
                metadata = mapOf("phoneNumber" to phoneNumber)
            )
        }
    }

    private fun executeStartLanguageLesson(command: AutomationCommand): AutomationResult {
        val language = command.parameters["language"] ?: "English"
        if (!platform.isLessonAvailable(language)) {
            return AutomationResult(
                state = CommandResultState.FAILED,
                message = "That lesson isn't available.",
                error = AutomationError.LESSON_NOT_FOUND,
                metadata = mapOf("language" to language)
            )
        }
        if (!platform.startLanguageLesson(language)) {
            return AutomationResult(
                state = CommandResultState.FAILED,
                message = "I couldn't start that lesson.",
                error = AutomationError.LESSON_START_FAILED,
                metadata = mapOf("language" to language)
            )
        }
        val feedback = if (language.equals("Current", ignoreCase = true)) {
            "Starting your language lesson now. Let's make great progress!"
        } else {
            "Starting your $language lesson now. Let's make great progress!"
        }

        return AutomationResult(
            state = CommandResultState.SUCCESS,
            message = feedback,
            targetDestination = Screen.AiConversation.route,
            metadata = mapOf("language" to language)
        )
    }

    private fun resolvePackageName(appName: String): String {
        return when (appName.lowercase()) {
            "youtube" -> "com.google.android.youtube"
            "chrome", "browser", "google chrome" -> "com.android.chrome"
            "whatsapp" -> "com.whatsapp"
            "camera" -> "com.google.android.GoogleCamera"
            "spotify" -> "com.spotify.music"
            "settings" -> "com.android.settings"
            "calculator" -> {
                if (platform.isAppInstalled("com.google.android.calculator")) "com.google.android.calculator"
                else if (platform.isAppInstalled("com.android.calculator2")) "com.android.calculator2"
                else if (platform.isAppInstalled("com.vivo.calculator")) "com.vivo.calculator"
                else "com.google.android.calculator"
            }
            "maps", "google maps" -> "com.google.android.apps.maps"
            "gmail" -> "com.google.android.gm"
            "clock" -> "com.google.android.deskclock"
            else -> appName
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
