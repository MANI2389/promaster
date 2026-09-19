package com.example.promaster.domain.service

import com.example.promaster.domain.model.WakeWordStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * On-device wake-word detection engine interface for "Hey Bro".
 *
 * POLICY:
 * Do not fake wake-word detection. If an on-device engine/model is not integrated,
 * it must report isAvailable() = false and UNAVAILABLE_NO_ON_DEVICE_MODEL.
 */
interface WakeWordDetector {
    val status: StateFlow<WakeWordStatus>
    val wakeWord: String get() = "Hey Bro"

    fun isAvailable(): Boolean
    fun startDetecting(onDetected: () -> Unit, onError: (String) -> Unit)
    fun stopDetecting()
}
