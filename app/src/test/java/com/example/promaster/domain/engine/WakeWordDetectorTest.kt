package com.example.promaster.domain.engine

import com.example.promaster.data.service.MockWakeWordDetector
import com.example.promaster.data.service.OnDeviceWakeWordDetector
import com.example.promaster.domain.model.WakeWordStatus
import org.junit.Assert.*
import org.junit.Test

class WakeWordDetectorTest {

    @Test
    fun onDeviceDetector_noModelInstalled_reportsUnavailableTruthfully() {
        val detector = OnDeviceWakeWordDetector(context = null)

        // Truthful check: without local model file, never fake availability
        assertFalse(detector.isAvailable())
        assertEquals(WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL, detector.status.value)
        assertEquals("Hey Bro", detector.wakeWord)
    }

    @Test
    fun onDeviceDetector_startDetectingWithoutModel_returnsFailureAndDoesNotFake() {
        val detector = OnDeviceWakeWordDetector(context = null)
        var errorReported: String? = null
        var detected = false

        detector.startDetecting(
            onDetected = { detected = true },
            onError = { err -> errorReported = err }
        )

        assertFalse(detected)
        assertNotNull(errorReported)
        assertTrue(errorReported!!.contains("model file not found", ignoreCase = true))
        assertEquals(WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL, detector.status.value)
    }

    @Test
    fun mockWakeWordDetector_startsAndTransitionsToListening() {
        val detector = MockWakeWordDetector()
        assertTrue(detector.isAvailable())
        assertEquals(WakeWordStatus.AVAILABLE, detector.status.value)

        var detected = false
        detector.startDetecting(
            onDetected = { detected = true },
            onError = { fail("Should not fail") }
        )

        assertEquals(WakeWordStatus.LISTENING, detector.status.value)
        assertFalse(detected)
    }

    @Test
    fun mockWakeWordDetector_detectsWakeWordAccurately() {
        val detector = MockWakeWordDetector()
        var detectionCount = 0

        detector.startDetecting(
            onDetected = { detectionCount++ },
            onError = { fail("Should not fail") }
        )

        // Different phrases should NOT trigger "Hey Bro"
        detector.simulateDetection("Hello Assistant")
        detector.simulateDetection("Hey Brother")
        assertEquals(0, detectionCount)

        // Exact wake word triggers detection
        detector.simulateDetection("Hey Bro")
        assertEquals(1, detectionCount)

        // Case-insensitive variation
        detector.simulateDetection("hey bro")
        assertEquals(2, detectionCount)
    }

    @Test
    fun mockWakeWordDetector_stopsDetectingImmediately() {
        val detector = MockWakeWordDetector()
        var detectionCount = 0

        detector.startDetecting(
            onDetected = { detectionCount++ },
            onError = { fail("Should not fail") }
        )

        assertEquals(WakeWordStatus.LISTENING, detector.status.value)

        // Stop listening immediately
        detector.stopDetecting()
        assertEquals(WakeWordStatus.STOPPED, detector.status.value)

        // After stopping, simulated utterances must NOT trigger callback
        detector.simulateDetection("Hey Bro")
        assertEquals(0, detectionCount)
    }
}
