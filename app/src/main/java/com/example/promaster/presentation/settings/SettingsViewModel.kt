package com.example.promaster.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.firebase.repository.FirestoreUserRepository
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.service.MockLearningReminderScheduler
import com.example.promaster.domain.engine.StreakCalculationEngine
import com.example.promaster.domain.model.LearningReminderSettings
import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.ReminderType
import com.example.promaster.domain.repository.UserProfileRepository
import com.example.promaster.domain.service.LearningReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val reminderSettings: LearningReminderSettings = LearningReminderSettings(),
    val progress: ProgressState = MockDataProvider.progressState,
    val showPermissionRationale: Boolean = false,
    val pendingReminderToggle: ReminderType? = null,
    val isStreakSecuredToday: Boolean = false,
    val isStreakAtRisk: Boolean = false,
    val availableTimes: List<String> = listOf(
        "06:00 AM", "07:00 AM", "08:00 AM", "09:00 AM", "10:00 AM",
        "12:00 PM", "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM", "10:00 PM"
    ),
    val wakeWordEnabled: Boolean = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.wakeWordEnabled == true,
    val autoPlayAudio: Boolean = true,
    val slowSpeechRate: Boolean = false,
    val selectedPersona: String = "Supportive Buddy",
    val voiceGender: String = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voiceGender ?: "female",
    val voiceSpeed: Float = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voiceSpeed ?: 1.0f,
    val voicePitch: Float = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voicePitch ?: 1.0f,
    val conversationLanguage: String = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.conversationLanguage ?: "English",
    val isSpeakingTest: Boolean = false
)

class SettingsViewModel(
    private val userProfileRepo: UserProfileRepository = FirestoreUserRepository(),
    var reminderScheduler: LearningReminderScheduler = MockLearningReminderScheduler()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepo.getProgressState().collect { progress ->
                val secured = StreakCalculationEngine.isStreakActiveToday(progress.lastActivityDate)
                val atRisk = StreakCalculationEngine.isStreakAtRisk(progress.lastActivityDate)
                _uiState.value = _uiState.value.copy(
                    progress = progress,
                    isStreakSecuredToday = secured,
                    isStreakAtRisk = atRisk
                )
            }
        }
    }

    fun setScheduler(scheduler: LearningReminderScheduler) {
        this.reminderScheduler = scheduler
        // Sync scheduler with current settings
        reminderScheduler.updateSettings(_uiState.value.reminderSettings)
    }

    fun onPreferredTimeSelected(time: String) {
        val updated = _uiState.value.reminderSettings.copy(preferredTime = time)
        _uiState.value = _uiState.value.copy(reminderSettings = updated)
        if (updated.dailyLessonReminderEnabled) {
            reminderScheduler.scheduleDailyLessonReminder(time)
        }
    }

    fun onDailyLessonReminderToggled(enabled: Boolean, hasNotificationPermission: Boolean) {
        if (enabled && !hasNotificationPermission) {
            _uiState.value = _uiState.value.copy(
                showPermissionRationale = true,
                pendingReminderToggle = ReminderType.DAILY_LESSON
            )
            return
        }
        val updated = _uiState.value.reminderSettings.copy(dailyLessonReminderEnabled = enabled)
        _uiState.value = _uiState.value.copy(reminderSettings = updated)
        if (enabled) {
            reminderScheduler.scheduleDailyLessonReminder(updated.preferredTime)
        } else {
            reminderScheduler.cancelReminder(ReminderType.DAILY_LESSON)
        }
    }

    fun onMissedTaskReminderToggled(enabled: Boolean, hasNotificationPermission: Boolean) {
        if (enabled && !hasNotificationPermission) {
            _uiState.value = _uiState.value.copy(
                showPermissionRationale = true,
                pendingReminderToggle = ReminderType.MISSED_TASK
            )
            return
        }
        val updated = _uiState.value.reminderSettings.copy(missedTaskReminderEnabled = enabled)
        _uiState.value = _uiState.value.copy(reminderSettings = updated)
        if (enabled) {
            reminderScheduler.scheduleMissedTaskReminder()
        } else {
            reminderScheduler.cancelReminder(ReminderType.MISSED_TASK)
        }
    }

    fun onStreakReminderToggled(enabled: Boolean, hasNotificationPermission: Boolean) {
        if (enabled && !hasNotificationPermission) {
            _uiState.value = _uiState.value.copy(
                showPermissionRationale = true,
                pendingReminderToggle = ReminderType.STREAK_PRESERVATION
            )
            return
        }
        val updated = _uiState.value.reminderSettings.copy(streakReminderEnabled = enabled)
        _uiState.value = _uiState.value.copy(reminderSettings = updated)
        if (enabled) {
            reminderScheduler.scheduleStreakReminder()
        } else {
            reminderScheduler.cancelReminder(ReminderType.STREAK_PRESERVATION)
        }
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        val pending = _uiState.value.pendingReminderToggle
        _uiState.value = _uiState.value.copy(
            showPermissionRationale = false,
            pendingReminderToggle = null
        )

        if (isGranted && pending != null) {
            when (pending) {
                ReminderType.DAILY_LESSON -> onDailyLessonReminderToggled(true, true)
                ReminderType.MISSED_TASK -> onMissedTaskReminderToggled(true, true)
                ReminderType.STREAK_PRESERVATION -> onStreakReminderToggled(true, true)
            }
        }
    }

    fun dismissPermissionRationale() {
        _uiState.value = _uiState.value.copy(
            showPermissionRationale = false,
            pendingReminderToggle = null
        )
    }

    fun setAutoPlayAudio(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayAudio = enabled)
    }

    fun setSlowSpeechRate(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(slowSpeechRate = enabled)
    }

    fun setVoiceGender(gender: String) {
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voiceGender = gender
        _uiState.value = _uiState.value.copy(voiceGender = gender)
    }

    fun setVoiceSpeed(speed: Float) {
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voiceSpeed = speed
        _uiState.value = _uiState.value.copy(voiceSpeed = speed)
    }

    fun setVoicePitch(pitch: Float) {
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.voicePitch = pitch
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
    }

    fun setConversationLanguage(lang: String) {
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.conversationLanguage = lang
        _uiState.value = _uiState.value.copy(conversationLanguage = lang)
    }

    fun testVoice(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpeakingTest = true)
            val tts = com.example.promaster.data.service.AndroidTextToSpeechService(context)
            val text = if (_uiState.value.conversationLanguage.equals("Tamil", ignoreCase = true)) {
                "வணக்கம் bro! நான் உங்க PROMASTER voice assistant. Super-ஆ பேசலாம்!"
            } else {
                "Hello bro! I am your PROMASTER voice assistant. Ready to practice and assist you anytime!"
            }
            tts.speak(text, _uiState.value.conversationLanguage) {
                _uiState.value = _uiState.value.copy(isSpeakingTest = false)
            }
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.wakeWordEnabled = enabled
        _uiState.value = _uiState.value.copy(wakeWordEnabled = enabled)
    }

    fun setSelectedPersona(persona: String) {
        _uiState.value = _uiState.value.copy(selectedPersona = persona)
    }
}
