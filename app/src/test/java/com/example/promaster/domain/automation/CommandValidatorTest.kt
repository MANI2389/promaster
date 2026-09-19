package com.example.promaster.domain.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandValidatorTest {

    private lateinit var validator: DefaultCommandValidator

    @Before
    fun setUp() {
        validator = DefaultCommandValidator()
    }

    @Test
    fun makeCall_unconfirmed_requiresConfirmation() {
        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call 9876543210",
            parameters = mapOf("phoneNumber" to "9876543210"),
            isConfirmed = false
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.ConfirmationRequired)
        val conf = result as ValidationResult.ConfirmationRequired
        assertTrue(conf.prompt.contains("9876543210"))
        assertTrue(conf.actionToken.contains("CALL_9876543210"))
    }

    @Test
    fun makeCall_confirmed_isValid() {
        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call 9876543210",
            parameters = mapOf("phoneNumber" to "9876543210"),
            isConfirmed = true
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun makeCall_invalidNumber_isNotPermitted() {
        val command = AutomationCommand(
            type = AutomationCommandType.MAKE_CALL,
            rawInput = "Call abc",
            parameters = mapOf("phoneNumber" to "abc"),
            isConfirmed = false
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.NotPermitted)
    }

    @Test
    fun createAlarm_noTimeUnconfirmed_requiresConfirmation() {
        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Create alarm",
            parameters = emptyMap(),
            isConfirmed = false
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.ConfirmationRequired)
        val conf = result as ValidationResult.ConfirmationRequired
        assertTrue(conf.prompt.contains("8:00 AM"))
    }

    @Test
    fun createAlarm_withTime_isValid() {
        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Set alarm for 7:00 AM",
            parameters = mapOf("time" to "7:00 AM"),
            isConfirmed = false
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun openWebsite_dangerousScheme_isNotPermitted() {
        val fileCommand = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website file:///etc/hosts",
            parameters = mapOf("url" to "file:///etc/hosts")
        )
        val result = validator.validate(fileCommand)
        assertTrue(result is ValidationResult.NotPermitted)
        assertEquals(AutomationError.UNSAFE_URL, (result as ValidationResult.NotPermitted).error)

        val jsCommand = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website javascript:alert(1)",
            parameters = mapOf("url" to "javascript:alert(1)")
        )
        val jsResult = validator.validate(jsCommand)
        assertTrue(jsResult is ValidationResult.NotPermitted)
        assertEquals(AutomationError.UNSAFE_URL, (jsResult as ValidationResult.NotPermitted).error)
    }

    @Test
    fun openWebsite_validHttps_isValid() {
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website https://promaster.app",
            parameters = mapOf("url" to "https://promaster.app")
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun openWebsite_malformedUrl_isInvalidUrl() {
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_WEBSITE,
            rawInput = "Open website https://",
            parameters = mapOf("url" to "https://")
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.NotPermitted)
        assertEquals(AutomationError.INVALID_URL, (result as ValidationResult.NotPermitted).error)
    }

    @Test
    fun createAlarm_ambiguousNaturalLanguageTime_isInvalidAlarmTime() {
        val command = AutomationCommand(
            type = AutomationCommandType.CREATE_ALARM,
            rawInput = "Set an alarm sometime tomorrow morning",
            parameters = emptyMap()
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.NotPermitted)
        assertEquals(AutomationError.INVALID_ALARM_TIME, (result as ValidationResult.NotPermitted).error)
    }

    @Test
    fun openApp_emptyAppName_isNotPermitted() {
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open",
            parameters = mapOf("appName" to "")
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.NotPermitted)
    }

    @Test
    fun openApp_validName_isValid() {
        val command = AutomationCommand(
            type = AutomationCommandType.OPEN_APP,
            rawInput = "Open YouTube",
            parameters = mapOf("appName" to "YouTube")
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun unsupported_returnsUnsupported() {
        val command = AutomationCommand(
            type = AutomationCommandType.UNSUPPORTED,
            rawInput = "Injected gesture",
            parameters = mapOf("reason" to "Accessibility service automation is not permitted")
        )

        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Unsupported)
        assertEquals("Accessibility service automation is not permitted", (result as ValidationResult.Unsupported).reason)
    }

    @Test
    fun emptyCommand_isMappedToEmptyCommand() = kotlinx.coroutines.runBlocking {
        val command = DefaultAutomationIntentRouter().route("   ")
        val result = validator.validate(command)

        assertTrue(result is ValidationResult.Unsupported)
        assertEquals(AutomationError.EMPTY_COMMAND, (result as ValidationResult.Unsupported).error)
    }
}
