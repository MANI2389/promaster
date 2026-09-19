package com.example.promaster.domain.automation

import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.navigation.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafeAutomationFrameworkTest {

    private lateinit var framework: DefaultSafeAutomationFramework
    private var dialedNumber: String? = null
    private var openedUrl: String? = null
    private var launchedApp: String? = null

    @Before
    fun setUp() {
        dialedNumber = null
        openedUrl = null
        launchedApp = null

        val testPlatform = object : AndroidAutomationPlatform {
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun launchApp(packageName: String): Boolean {
                launchedApp = packageName
                return true
            }
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

        framework = DefaultSafeAutomationFramework(
            intentRouter = DefaultAutomationIntentRouter(),
            validator = DefaultCommandValidator(),
            executor = DefaultAutomationExecutor(testPlatform)
        )
    }

    @Test
    fun pipeline_makeCall_unconfirmed_yieldsConfirmationRequired() = runBlocking {
        // Step 1 -> 2 -> 3: Voice input -> Router -> Validator -> Executor -> Confirmation
        val result = framework.process("Call 9876543210", isConfirmed = false)

        assertEquals(CommandResultState.CONFIRMATION_REQUIRED, result.state)
        assertTrue(result.message.contains("9876543210"))
        assertEquals(null, dialedNumber) // Strict: NEVER dial silently without confirmation
    }

    @Test
    fun pipeline_makeCall_confirmed_executesDialer() = runBlocking {
        val result = framework.process("Call 9876543210", isConfirmed = true)

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("9876543210", dialedNumber)
    }

    @Test
    fun pipeline_openWebsite_opensSystemBrowser() = runBlocking {
        val result = framework.process("Open website https://promaster.app")

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("https://promaster.app", openedUrl)
    }

    @Test
    fun pipeline_createAlarm_schedulesAlarm() = runBlocking {
        val result = framework.process("Set an alarm for 7:15 AM")

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertTrue(result.message.contains("07:15"))
    }

    @Test
    fun pipeline_openSettings_opensSettings() = runBlocking {
        val result = framework.process("Open bluetooth settings")

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals(Screen.Settings.route, result.targetDestination)
    }

    @Test
    fun pipeline_openApp_launchesInstalledApp() = runBlocking {
        val result = framework.process("Open YouTube")

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals("com.google.android.youtube", launchedApp)
    }

    @Test
    fun pipeline_startLanguageLesson_navigatesToConversation() = runBlocking {
        val result = framework.process("Start my English lesson")

        assertEquals(CommandResultState.SUCCESS, result.state)
        assertEquals(Screen.AiConversation.route, result.targetDestination)
    }

    @Test
    fun pipeline_accessibilityServiceRequest_returnsNotSupportedOnThisDevice() = runBlocking {
        val result = framework.process("Click the red button using accessibility")

        assertEquals(CommandResultState.NOT_SUPPORTED, result.state)
        assertEquals(AutomationError.UNKNOWN_COMMAND, result.error)
    }

    @Test
    fun pipeline_unsupportedDeviceFeature_returnsNotSupportedOnThisDevice() = runBlocking {
        val unsupportedPlatform = object : AndroidAutomationPlatform {
            override fun isActionSupportedOnDevice(type: AutomationCommandType): Boolean = false
        }
        val customFramework = DefaultSafeAutomationFramework(
            executor = DefaultAutomationExecutor(unsupportedPlatform)
        )

        val result = customFramework.process("Open YouTube")

        assertEquals(CommandResultState.NOT_SUPPORTED, result.state)
        assertEquals("Not supported on this device/version.", result.message)
    }
}
