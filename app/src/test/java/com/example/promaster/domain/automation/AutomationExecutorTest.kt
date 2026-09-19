package com.example.promaster.domain.automation

import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.navigation.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationExecutorTest {

    @Test
    fun execute_openApp_whenInstalled_returnsSuccess() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun launchApp(packageName: String): Boolean = true
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open YouTube",
            parameters = mapOf("appName" to "YouTube")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.message.contains("YouTube"))
    }

    @Test
    fun execute_openApp_whenNotInstalled_returnsFailed() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = false
            override fun openWebsite(url: String): Boolean = false
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open Spotify",
            parameters = mapOf("appName" to "Spotify")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals(AutomationError.APP_NOT_FOUND, result.error)
        assertTrue(result.message.contains("isn't installed", ignoreCase = true))
    }

    @Test
    fun execute_openWebsite_returnsSuccess() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun openWebsite(url: String): Boolean = true
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website https://promaster.app",
            parameters = mapOf("url" to "https://promaster.app")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.message.contains("https://promaster.app"))
    }

    @Test
    fun execute_openWebsite_whenRestricted_returnsUrlOpenFailed() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun openWebsite(url: String): Boolean = false
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website https://promaster.app",
            parameters = mapOf("url" to "https://promaster.app")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals(AutomationError.URL_OPEN_FAILED, result.error)
    }

    @Test
    fun execute_createAlarm_whenPermitted_returnsSuccess() = runBlocking {
        var scheduledHour = -1
        var scheduledMinute = -1
        val platform = object : AndroidAutomationPlatform {
            override fun hasAlarmPermission(): Boolean = true
            override fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
                scheduledHour = hour
                scheduledMinute = minute
                return true
            }
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Set alarm for 7:30 AM",
            parameters = mapOf("time" to "7:30 AM")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals(7, scheduledHour)
        assertEquals(30, scheduledMinute)
        assertTrue(result.message.contains("07:30"))
    }

    @Test
    fun execute_createAlarm_missingPermission_returnsNeedsPermission() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun hasAlarmPermission(): Boolean = false
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Set alarm for 7:00 AM",
            parameters = mapOf("time" to "7:00 AM")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.NEEDS_PERMISSION, result.state)
        assertEquals("com.android.alarm.permission.SET_ALARM", result.requiredPermission)
    }

    @Test
    fun execute_openSettings_returnsSuccessAndDestination() = runBlocking {
        val executor = DefaultAutomationExecutor(object : AndroidAutomationPlatform {})

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_SETTINGS,
            rawInput = "Open wifi settings",
            parameters = mapOf("settingType" to "wifi")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals(Screen.Settings.route, result.targetDestination)
        assertTrue(result.message.contains("wifi", ignoreCase = true))
    }

    @Test
    fun execute_makeCall_whenConfirmed_opensDialerSuccessfully() = runBlocking {
        var dialedNumber: String? = null
        val platform = object : AndroidAutomationPlatform {
            override fun dialPhoneNumber(phoneNumber: String): Boolean {
                dialedNumber = phoneNumber
                return true
            }
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call 9876543210",
            parameters = mapOf("phoneNumber" to "9876543210"),
            isConfirmed = true
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("9876543210", dialedNumber)
        assertTrue(result.message.contains("9876543210"))
    }

    @Test
    fun execute_makeCall_whenTelephonyUnsupported_returnsNotSupportedOnThisDevice() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun dialPhoneNumber(phoneNumber: String): Boolean = false
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call 9876543210",
            parameters = mapOf("phoneNumber" to "9876543210"),
            isConfirmed = true
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals(AutomationError.APP_LAUNCH_FAILED, result.error)
    }

    @Test
    fun execute_startLanguageLesson_returnsSuccessAndRoute() = runBlocking {
        val executor = DefaultAutomationExecutor(object : AndroidAutomationPlatform {})

        val command = AutomationCommand(
            type = AutomationCommandType.START_LANGUAGE_LESSON,
            rawInput = "Start English lesson",
            parameters = mapOf("language" to "English")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals(Screen.AiConversation.route, result.targetDestination)
    }

    @Test
    fun execute_createAlarm_invalidTime_returnsInvalidAlarmTime() = runBlocking {
        var alarmCalled = false
        val platform = object : AndroidAutomationPlatform {
            override fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
                alarmCalled = true
                return true
            }
        }
        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Set an alarm for 25:90",
            parameters = mapOf("time" to "25:90")
        )
        val validation = DefaultCommandValidator().validate(command)
        val result = DefaultAutomationExecutor(platform).execute(command, validation)

        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals(AutomationError.INVALID_ALARM_TIME, result.error)
        assertEquals(false, alarmCalled)
    }

    @Test
    fun execute_settings_whenUnsupported_returnsSettingsNotSupported() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun openSettings(settingType: String): Boolean = false
        }
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_SETTINGS,
            rawInput = "Open wifi settings",
            parameters = mapOf("settingType" to "wifi")
        )
        val result = DefaultAutomationExecutor(platform).execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.NOT_SUPPORTED, result.state)
        assertEquals(AutomationError.SETTINGS_NOT_SUPPORTED, result.error)
    }

    @Test
    fun execute_whenPlatformThrows_returnsExecutorException() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = throw IllegalStateException("unexpected")
        }
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open YouTube",
            parameters = mapOf("appName" to "YouTube")
        )
        val result = DefaultAutomationExecutor(platform).execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.FAILED, result.state)
        assertEquals(AutomationError.EXECUTOR_EXCEPTION, result.error)
    }

    @Test
    fun execute_confirmationRequired_returnsConfirmationRequiredState() = runBlocking {
        val executor = DefaultAutomationExecutor(object : AndroidAutomationPlatform {})

        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call 9876543210",
            parameters = mapOf("phoneNumber" to "9876543210"),
            isConfirmed = false
        )
        val validation = ValidationResult.ConfirmationRequired(
            command = command,
            prompt = "Are you sure you want to call 9876543210?",
            actionToken = "CONFIRM_CALL_9876543210"
        )
        val result = executor.execute(command, validation)

        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, result.state)
        assertEquals("Are you sure you want to call 9876543210?", result.message)
        assertEquals("CONFIRM_CALL_9876543210", result.pendingConfirmationAction)
    }

    @Test
    fun execute_deviceUnsupportedAction_returnsNotSupportedOnThisDevice() = runBlocking {
        val platform = object : AndroidAutomationPlatform {
            override fun isActionSupportedOnDevice(type: AutomationCommandType): Boolean = false
        }
        val executor = DefaultAutomationExecutor(platform)

        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open Camera",
            parameters = mapOf("appName" to "Camera")
        )
        val result = executor.execute(command, ValidationResult.Valid(command))

        assertEquals(CommandResultState.NOT_SUPPORTED, result.state)
        assertEquals("Not supported on this device/version.", result.message)
    }

    @Test
    fun execute_openApp_resolvesNewPackages_returnsSuccess() = runBlocking {
        val launchedPackages = mutableListOf<String>()
        val platform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun launchApp(packageName: String): Boolean {
                launchedPackages.add(packageName)
                return true
            }
        }
        val executor = DefaultAutomationExecutor(platform)

        val appsToTest = listOf(
            "WhatsApp" to "com.whatsapp",
            "Chrome" to "com.android.chrome",
            "Calculator" to "com.google.android.calculator",
            "Maps" to "com.google.android.apps.maps",
            "Gmail" to "com.google.android.gm",
            "Clock" to "com.google.android.deskclock"
        )

        for ((appName, expectedPkg) in appsToTest) {
            val command = AutomationCommand(
                type = AutomationCommandType.OPEN_APP,
                rawInput = "Open $appName",
                parameters = mapOf("appName" to appName)
            )
            val result = executor.execute(command, ValidationResult.Valid(command))
            assertEquals(CommandResultState.SUCCESS, result.state)
            assertTrue(launchedPackages.contains(expectedPkg))
        }
    }
}
