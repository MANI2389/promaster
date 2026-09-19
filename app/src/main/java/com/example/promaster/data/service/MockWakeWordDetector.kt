package com.example.promaster.data.service

import com.example.promaster.domain.model.WakeWordStatus
import com.example.promaster.domain.service.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock wake-word detector for unit testing lifecycle, state transitions, and detection triggers.
 */
class MockWakeWordDetector(
    private val available: Boolean = true
) : WakeWordDetector {

    override val wakeWord: String = "Hey Bro"

    private val _status = MutableStateFlow(if (available) WakeWordStatus.AVAILABLE else WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL)
    override val status: StateFlow<WakeWordStatus> = _status.asStateFlow()

    private var onDetectedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    var startDetectingCallCount: Int = 0
        private set
    var stopDetectingCallCount: Int = 0
        private set

    override fun isAvailable(): Boolean = available

    override fun startDetecting(onDetected: () -> Unit, onError: (String) -> Unit) {
        startDetectingCallCount++
        if (!available) {
            _status.value = WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL
            onError("On-device wake-word engine is unavailable.")
            return
        }

        onDetectedCallback = onDetected
        onErrorCallback = onError
        _status.value = WakeWordStatus.LISTENING
    }

    override fun stopDetecting() {
        stopDetectingCallCount++
        onDetectedCallback = null
        onErrorCallback = null
        _status.value = if (available) WakeWordStatus.STOPPED else WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL
    }

    fun simulateWakeWordDetected() {
        if (_status.value == WakeWordStatus.LISTENING) {
            onDetectedCallback?.invoke()
        }
    }

    fun simulateDetection(phrase: String) {
        if (_status.value == WakeWordStatus.LISTENING && phrase.equals(wakeWord, ignoreCase = true)) {
            onDetectedCallback?.invoke()
        }
    }

    fun simulateError(error: String) {
        onErrorCallback?.invoke(error)
    }

    fun updateStatus(newStatus: WakeWordStatus) {
        _status.value = newStatus
    }
}
