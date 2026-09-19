package com.example.promaster.domain.engine

import android.Manifest
import com.example.promaster.data.service.DefaultWakeWordService
import com.example.promaster.data.service.ForegroundServiceController
import com.example.promaster.data.service.MockWakeWordDetector
import com.example.promaster.data.service.OnDeviceWakeWordDetector
import com.example.promaster.domain.model.WakeWordStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WakeWordServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun permissionMissing_enableWakeWord_failsAndReportsMissingPermission() {
        val mockDetector = MockWakeWordDetector()
        val deniedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = false
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = deniedPermissionChecker,
            coroutineScope = testScope
        )

        val result = service.enableWakeWord()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(WakeWordStatus.UNAVAILABLE_PERMISSION_MISSING, service.status.value)
        assertFalse(service.settings.value.isWakeWordEnabled)
        assertFalse(service.isListening.value)
    }

    @Test
    fun engineUnavailable_enableWakeWord_failsTruthfullyWithoutFaking() {
        val unavailableDetector = OnDeviceWakeWordDetector(context = null)
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = unavailableDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope
        )

        // Initial status reflects truthfully that engine is not installed
        assertEquals(WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL, service.status.value)

        val result = service.enableWakeWord()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL, service.status.value)
        assertFalse(service.settings.value.isWakeWordEnabled)
    }

    @Test
    fun enableWakeWord_success_startsListeningAndTriggersCallback() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope
        )

        var wakeWordDetected = false
        service.setWakeWordListener {
            wakeWordDetected = true
        }

        val result = service.enableWakeWord(runInBackground = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(WakeWordStatus.LISTENING, service.status.value)
        assertTrue(service.settings.value.isWakeWordEnabled)
        assertTrue(service.isListening.value)

        // Simulate hearing "Hey Bro"
        mockDetector.simulateDetection("Hey Bro")
        assertTrue(wakeWordDetected)
    }

    @Test
    fun disableWakeWord_stopsListeningImmediately() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope
        )

        service.enableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(WakeWordStatus.LISTENING, service.status.value)

        // Disable immediately
        service.disableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WakeWordStatus.DISABLED, service.status.value)
        assertFalse(service.settings.value.isWakeWordEnabled)
        assertFalse(service.isListening.value)
        assertEquals(WakeWordStatus.STOPPED, mockDetector.status.value)
    }

    @Test
    fun runInBackground_coordinatesForegroundServiceController() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        var foregroundStarted = false
        var foregroundStopped = false

        val mockController = object : ForegroundServiceController {
            override fun startForegroundService() {
                foregroundStarted = true
            }

            override fun stopForegroundService() {
                foregroundStopped = true
            }
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            foregroundServiceController = mockController,
            coroutineScope = testScope
        )

        service.enableWakeWord(runInBackground = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(foregroundStarted)
        assertFalse(foregroundStopped)
        assertTrue(service.settings.value.runInBackground)

        service.disableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(foregroundStopped)
        assertFalse(service.settings.value.isWakeWordEnabled)
    }

    @Test
    fun privacyControls_rawAudioNeverStoredByDefaultAndRequiresExplicitConsent() {
        val service = DefaultWakeWordService(
            detector = MockWakeWordDetector(),
            coroutineScope = testScope
        )

        val initialSettings = service.settings.value
        // Privacy requirement: zero raw storage by default
        assertFalse(initialSettings.storeAudioRecordings)
        assertFalse(initialSettings.privacyConsentGiven)

        // Attempting to store audio without explicit consent is denied
        service.updatePrivacySettings(storeAudio = true, consent = false)
        assertFalse(service.settings.value.storeAudioRecordings)
        assertFalse(service.settings.value.privacyConsentGiven)

        // Explicit consent enables storage
        service.updatePrivacySettings(storeAudio = true, consent = true)
        assertTrue(service.settings.value.storeAudioRecordings)
        assertTrue(service.settings.value.privacyConsentGiven)
    }

    @Test
    fun cooldownAndDuplicatePrevention_dropsRepeatedTriggersInCooldownWindow() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope,
            cooldownMs = 2000L
        )

        var triggerCount = 0
        service.setWakeWordListener {
            triggerCount++
        }

        service.enableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()

        // First trigger succeeds
        mockDetector.simulateDetection("Hey Bro")
        assertEquals(1, triggerCount)

        // Rapid duplicate trigger within cooldown is dropped
        mockDetector.simulateDetection("Hey Bro")
        assertEquals(1, triggerCount)
        assertFalse(service.canTrigger(System.currentTimeMillis()))
    }

    @Test
    fun resumeStandby_restartsListeningWhenWakeWordStillEnabled() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope
        )

        service.enableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger suspends detector
        mockDetector.simulateDetection("Hey Bro")
        assertEquals(WakeWordStatus.AVAILABLE, service.status.value)

        // Voice session completes -> resume standby
        service.resumeStandby()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(WakeWordStatus.LISTENING, service.status.value)
    }

    @Test
    fun lifecycleCleanup_shutdown_disablesDetectionAndClearsState() {
        val mockDetector = MockWakeWordDetector()
        val grantedPermissionChecker = object : PermissionChecker {
            override fun hasPermission(permission: String): Boolean = true
        }

        val service = DefaultWakeWordService(
            detector = mockDetector,
            permissionChecker = grantedPermissionChecker,
            coroutineScope = testScope
        )

        service.enableWakeWord()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(WakeWordStatus.LISTENING, service.status.value)

        service.shutdown()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WakeWordStatus.DISABLED, service.status.value)
        assertFalse(service.settings.value.isWakeWordEnabled)
        assertEquals(0L, service.getLastTriggerTimestamp())
    }
}
