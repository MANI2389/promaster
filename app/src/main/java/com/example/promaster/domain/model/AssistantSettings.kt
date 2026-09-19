package com.example.promaster.domain.model

/**
 * Status states for the wake-word detection engine.
 * Never pretend unavailable models or unintegrated engines are working.
 */
enum class WakeWordStatus(val description: String) {
    AVAILABLE("On-device wake-word detection engine is ready"),
    UNAVAILABLE_NO_ON_DEVICE_MODEL("Local on-device wake-word model is not integrated or missing"),
    UNAVAILABLE_PERMISSION_MISSING("Microphone permission is required for wake-word detection"),
    DISABLED("Wake-word detection is disabled by user"),
    LISTENING("Actively listening for 'Hey Bro'"),
    STOPPED("Wake-word listening is stopped or suspended")
}

/**
 * Privacy-preserving configuration settings for the PROMASTER Voice Assistant & Wake Word.
 *
 * PRIVACY SPECIFICATION:
 * - Local-first: Wake word processing prefers on-device detection without continuous cloud audio upload.
 * - Zero unauthorized storage: Raw audio recordings are never stored by default (storeAudioRecordings = false).
 * - Explicit consent: Audio retention requires affirmative user consent.
 */
data class AssistantSettings(
    val isWakeWordEnabled: Boolean = false,
    val wakeWord: String = "Hey Bro",
    val runInBackground: Boolean = false,
    val storeAudioRecordings: Boolean = false, // Strictly false by default
    val privacyConsentGiven: Boolean = false,
    val status: WakeWordStatus = WakeWordStatus.DISABLED,
    val statusMessage: String = "Wake word disabled"
)
