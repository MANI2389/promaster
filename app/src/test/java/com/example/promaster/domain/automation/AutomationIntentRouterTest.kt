package com.example.promaster.domain.automation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutomationIntentRouterTest {

    private lateinit var router: DefaultAutomationIntentRouter

    @Before
    fun setUp() {
        router = DefaultAutomationIntentRouter()
    }

    @Test
    fun route_openApp_extractsAppNameCorrectly() = runBlocking {
        val cmd1 = router.route("Open YouTube")
        assertEquals(AutomationCommandType.OPEN_APP, cmd1.type)
        assertEquals("YouTube", cmd1.parameters["appName"])

        val cmd2 = router.route("Launch Chrome")
        assertEquals(AutomationCommandType.OPEN_APP, cmd2.type)
        assertEquals("Chrome", cmd2.parameters["appName"])

        val cmd3 = router.route("Open WhatsApp")
        assertEquals(AutomationCommandType.OPEN_APP, cmd3.type)
        assertEquals("WhatsApp", cmd3.parameters["appName"])
    }

    @Test
    fun route_openWebsite_extractsAndNormalizesUrl() = runBlocking {
        val cmd1 = router.route("Open website https://promaster.app")
        assertEquals(AutomationCommandType.OPEN_WEBSITE, cmd1.type)
        assertEquals("https://promaster.app", cmd1.parameters["url"])

        val cmd2 = router.route("Open website google.com")
        assertEquals(AutomationCommandType.OPEN_WEBSITE, cmd2.type)
        assertEquals("https://google.com", cmd2.parameters["url"])

        val cmd3 = router.route("Browse to https://kotlinlang.org/docs")
        assertEquals(AutomationCommandType.OPEN_WEBSITE, cmd3.type)
        assertEquals("https://kotlinlang.org/docs", cmd3.parameters["url"])
    }

    @Test
    fun route_createAlarm_extractsTimeParameter() = runBlocking {
        val cmd1 = router.route("Set an alarm for 7:00 AM")
        assertEquals(AutomationCommandType.CREATE_ALARM, cmd1.type)
        assertEquals("7:00 am", cmd1.parameters["time"]?.lowercase())

        val cmd2 = router.route("Create alarm for 8:30 PM")
        assertEquals(AutomationCommandType.CREATE_ALARM, cmd2.type)
        assertEquals("8:30 pm", cmd2.parameters["time"]?.lowercase())

        val cmd3 = router.route("Create alarm")
        assertEquals(AutomationCommandType.CREATE_ALARM, cmd3.type)
        assertTrue(cmd3.parameters["time"].isNullOrBlank())
    }

    @Test
    fun route_openSettings_extractsSettingType() = runBlocking {
        val cmd1 = router.route("Open settings")
        assertEquals(AutomationCommandType.OPEN_SETTINGS, cmd1.type)
        assertEquals("general", cmd1.parameters["settingType"])

        val cmd2 = router.route("Open wifi settings")
        assertEquals(AutomationCommandType.OPEN_SETTINGS, cmd2.type)
        assertEquals("wifi", cmd2.parameters["settingType"])

        val cmd3 = router.route("Open bluetooth settings")
        assertEquals(AutomationCommandType.OPEN_SETTINGS, cmd3.type)
        assertEquals("bluetooth", cmd3.parameters["settingType"])
    }

    @Test
    fun route_makeCall_extractsPhoneNumber() = runBlocking {
        val cmd1 = router.route("Call 9876543210")
        assertEquals(AutomationCommandType.MAKE_CALL, cmd1.type)
        assertEquals("9876543210", cmd1.parameters["phoneNumber"])

        val cmd2 = router.route("Make call to 1234567890")
        assertEquals(AutomationCommandType.MAKE_CALL, cmd2.type)
        assertEquals("1234567890", cmd2.parameters["phoneNumber"])

        val cmd3 = router.route("Dial +1-555-1234")
        assertEquals(AutomationCommandType.MAKE_CALL, cmd3.type)
        assertEquals("+1-555-1234", cmd3.parameters["phoneNumber"])
    }

    @Test
    fun route_startLanguageLesson_extractsLanguage() = runBlocking {
        val cmd1 = router.route("Start my English lesson")
        assertEquals(AutomationCommandType.START_LANGUAGE_LESSON, cmd1.type)
        assertEquals("English", cmd1.parameters["language"])

        val cmd2 = router.route("Begin Spanish lesson")
        assertEquals(AutomationCommandType.START_LANGUAGE_LESSON, cmd2.type)
        assertEquals("Spanish", cmd2.parameters["language"])

        val cmd3 = router.route("Start language lesson")
        assertEquals(AutomationCommandType.START_LANGUAGE_LESSON, cmd3.type)
        assertEquals("Current", cmd3.parameters["language"])
    }

    @Test
    fun route_accessibilityRequest_routesToUnsupported() = runBlocking {
        val cmd = router.route("Click the button on screen using accessibility service")
        assertEquals(AutomationCommandType.UNSUPPORTED, cmd.type)
    }

    @Test
    fun route_arbitraryText_routesToUnsupported() = runBlocking {
        val cmd = router.route("What is the weather in Paris tomorrow?")
        assertEquals(AutomationCommandType.UNSUPPORTED, cmd.type)
    }
}
