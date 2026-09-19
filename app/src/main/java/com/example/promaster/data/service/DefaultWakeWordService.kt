package com.example.promaster.data.service

import android.Manifest
import com.example.promaster.domain.engine.PermissionChecker
import com.example.promaster.domain.model.AssistantSettings
import com.example.promaster.domain.model.WakeWordStatus
import com.example.promaster.domain.service.WakeWordDetector
import com.example.promaster.domain.service.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface ForegroundServiceController {
    fun startForegroundService()
    fun stopForegroundService()
}

/**
 * Orchestrator implementing wake-word lifecycle, permission gating, truthful engine checks,
 * foreground service coordination, and immediate stop on disable.
 */
class DefaultWakeWordService(
    private val detector: WakeWordDetector = OnDeviceWakeWordDetector(null),
    private val permissionChecker: PermissionChecker = object : PermissionChecker {},
    private val foregroundServiceController: ForegroundServiceController? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()),
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS
) : WakeWordService {

    companion object {
        const val DEFAULT_COOLDOWN_MS = 2000L
    }

    private var lastTriggerTimestamp = 0L

    fun getLastTriggerTimestamp(): Long = lastTriggerTimestamp

    fun canTrigger(now: Long = System.currentTimeMillis()): Boolean {
        return (now - lastTriggerTimestamp) >= cooldownMs
    }

    private val _settings = MutableStateFlow(
        AssistantSettings(
            isWakeWordEnabled = false,
            wakeWord = "Hey Bro",
            runInBackground = false,
            storeAudioRecordings = false,
            privacyConsentGiven = false,
            status = if (detector.isAvailable()) WakeWordStatus.DISABLED else WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL,
            statusMessage = if (detector.isAvailable()) "Wake word disabled" else "On-device model not integrated"
        )
    )
    override val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private val _status = MutableStateFlow(_settings.value.status)
    override val status: StateFlow<WakeWordStatus> = _status.asStateFlow()

    override val isListening: StateFlow<Boolean> = _status
        .map { it == WakeWordStatus.LISTENING }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private var onWakeWordTriggeredCallback: (() -> Unit)? = null

    init {
        // Synchronize status updates from detector
        coroutineScope.launch {
            detector.status.collect { detectorStatus ->
                if (_settings.value.isWakeWordEnabled) {
                    _status.value = detectorStatus
                }
            }
        }
    }

    override fun enableWakeWord(runInBackground: Boolean): Result<Unit> {
        // 1. Check microphone permission
        if (!permissionChecker.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            _status.value = WakeWordStatus.UNAVAILABLE_PERMISSION_MISSING
            _settings.update {
                it.copy(
                    isWakeWordEnabled = false,
                    status = WakeWordStatus.UNAVAILABLE_PERMISSION_MISSING,
                    statusMessage = "Microphone permission required for wake-word detection"
                )
            }
            return Result.failure(SecurityException("Microphone permission is required to listen for 'Hey Bro'"))
        }

        // 2. Truthful engine check: Never pretend unavailable wake-word engines work
        if (!detector.isAvailable()) {
            _status.value = WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL
            _settings.update {
                it.copy(
                    isWakeWordEnabled = false,
                    status = WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL,
                    statusMessage = "On-device wake-word engine is unavailable: model not installed"
                )
            }
            return Result.failure(IllegalStateException("Local on-device wake-word model for 'Hey Bro' is not integrated."))
        }

        // 3. Start detector
        detector.startDetecting(
            onDetected = {
                handleWakeWordDetected()
            },
            onError = { err ->
                handleDetectionError(err)
            }
        )

        // 4. Start foreground service if background execution requested
        if (runInBackground) {
            foregroundServiceController?.startForegroundService()
        }

        _status.value = WakeWordStatus.LISTENING
        _settings.update {
            it.copy(
                isWakeWordEnabled = true,
                runInBackground = runInBackground,
                status = WakeWordStatus.LISTENING,
                statusMessage = "Listening for 'Hey Bro'"
            )
        }

        return Result.success(Unit)
    }

    override fun disableWakeWord() {
        // Stop listening immediately when disabled
        detector.stopDetecting()
        foregroundServiceController?.stopForegroundService()

        _status.value = WakeWordStatus.DISABLED
        _settings.update {
            it.copy(
                isWakeWordEnabled = false,
                status = WakeWordStatus.DISABLED,
                statusMessage = "Wake word disabled"
            )
        }
    }

    override fun updatePrivacySettings(storeAudio: Boolean, consent: Boolean) {
        // Never store raw audio without explicit consent
        val allowedToStore = storeAudio && consent
        _settings.update {
            it.copy(
                storeAudioRecordings = allowedToStore,
                privacyConsentGiven = consent
            )
        }
    }

    override fun setWakeWordListener(onTriggered: () -> Unit) {
        onWakeWordTriggeredCallback = onTriggered
    }

    override fun shutdown() {
        disableWakeWord()
        onWakeWordTriggeredCallback = null
        lastTriggerTimestamp = 0L
    }

    /**
     * Resumes wake-word standby detection after a voice session completes.
     * Prevents competing microphone consumers while speech recognition was active.
     */
    fun resumeStandby() {
        if (_settings.value.isWakeWordEnabled && detector.isAvailable()) {
            detector.startDetecting(
                onDetected = { handleWakeWordDetected() },
                onError = { handleDetectionError(it) }
            )
            _status.value = WakeWordStatus.LISTENING
        }
    }

    private fun handleWakeWordDetected() {
        val now = System.currentTimeMillis()
        if (!canTrigger(now)) {
            // Cooldown active: Drop duplicate trigger to avoid repeated firing
            return
        }
        lastTriggerTimestamp = now
        // Temporarily suspend wake-word detector while SpeechRecognizer runs
        detector.stopDetecting()
        _status.value = WakeWordStatus.AVAILABLE
        onWakeWordTriggeredCallback?.invoke()
    }

    private fun handleDetectionError(errorMessage: String) {
        _status.value = WakeWordStatus.STOPPED
        _settings.update {
            it.copy(
                isWakeWordEnabled = false,
                status = WakeWordStatus.STOPPED,
                statusMessage = errorMessage
            )
        }
    }
}
