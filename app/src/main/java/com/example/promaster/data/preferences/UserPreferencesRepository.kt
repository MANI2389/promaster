package com.example.promaster.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Robust SharedPreferences-backed repository for persistent device state.
 * Survives process death, force-stop, and activity recreation.
 * Never resets during normal app startup.
 */
class UserPreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "promaster_user_preferences"

        // Keys
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_MOTHER_TONGUE_CODE = "key_mother_tongue_code"
        private const val KEY_MOTHER_TONGUE_NAME = "key_mother_tongue_name"
        private const val KEY_TARGET_LANGUAGE_CODE = "key_target_language_code"
        private const val KEY_TARGET_LANGUAGE_NAME = "key_target_language_name"
        private const val KEY_USER_LEVEL = "key_user_level"
        private const val KEY_DAILY_GOAL_MINUTES = "key_daily_goal_minutes"
        private const val KEY_REMINDER_ENABLED = "key_reminder_enabled"
        private const val KEY_REMINDER_TIME = "key_reminder_time"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_CACHED_STREAK = "key_cached_streak"
        private const val KEY_CACHED_TOTAL_XP = "key_cached_total_xp"
        private const val KEY_CACHED_CURRENT_DAY = "key_cached_current_day"
        private const val KEY_USER_DISPLAY_NAME = "key_user_display_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_UID = "key_user_uid"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_VOICE_GENDER = "key_voice_gender"
        private const val KEY_VOICE_SPEED = "key_voice_speed"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_CONVERSATION_LANGUAGE = "key_conversation_language"
        private const val KEY_CONFIRM_SENSITIVE_ACTIONS = "key_confirm_sensitive_actions"

        @Volatile
        private var _instance: UserPreferencesRepository? = null

        fun initialize(context: Context): UserPreferencesRepository {
            return _instance ?: synchronized(this) {
                _instance ?: UserPreferencesRepository(context).also {
                    _instance = it
                    it.syncToMockDataProvider()
                }
            }
        }

        val instance: UserPreferencesRepository
            get() = _instance ?: throw IllegalStateException("UserPreferencesRepository has not been initialized.")

        fun getInstanceOrNull(): UserPreferencesRepository? = _instance
    }

    // Reactive StateFlows for UI binding
    private val _isOnboardingCompleted = MutableStateFlow(isOnboardingCompleted)
    val isOnboardingCompletedFlow: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _targetLanguageName = MutableStateFlow(targetLanguageName)
    val targetLanguageNameFlow: StateFlow<String> = _targetLanguageName.asStateFlow()

    private val _motherTongueName = MutableStateFlow(motherTongueName)
    val motherTongueNameFlow: StateFlow<String> = _motherTongueName.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(dailyGoalMinutes)
    val dailyGoalMinutesFlow: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(wakeWordEnabled)
    val wakeWordEnabledFlow: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(reminderEnabled)
    val reminderEnabledFlow: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    // Synchronous accessors
    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
            _isOnboardingCompleted.value = value
        }

    var motherTongueCode: String
        get() = prefs.getString(KEY_MOTHER_TONGUE_CODE, "ta") ?: "ta"
        set(value) {
            prefs.edit().putString(KEY_MOTHER_TONGUE_CODE, value).apply()
        }

    var motherTongueName: String
        get() = prefs.getString(KEY_MOTHER_TONGUE_NAME, "Tamil") ?: "Tamil"
        set(value) {
            prefs.edit().putString(KEY_MOTHER_TONGUE_NAME, value).apply()
            _motherTongueName.value = value
        }

    var targetLanguageCode: String
        get() = prefs.getString(KEY_TARGET_LANGUAGE_CODE, "en") ?: "en"
        set(value) {
            prefs.edit().putString(KEY_TARGET_LANGUAGE_CODE, value).apply()
        }

    var targetLanguageName: String
        get() = prefs.getString(KEY_TARGET_LANGUAGE_NAME, "English") ?: "English"
        set(value) {
            prefs.edit().putString(KEY_TARGET_LANGUAGE_NAME, value).apply()
            _targetLanguageName.value = value
        }

    var userLevel: String
        get() = prefs.getString(KEY_USER_LEVEL, "BEGINNER") ?: "BEGINNER"
        set(value) {
            prefs.edit().putString(KEY_USER_LEVEL, value).apply()
        }

    var dailyGoalMinutes: Int
        get() = prefs.getInt(KEY_DAILY_GOAL_MINUTES, 15)
        set(value) {
            prefs.edit().putInt(KEY_DAILY_GOAL_MINUTES, value).apply()
            _dailyGoalMinutes.value = value
        }

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()
            _reminderEnabled.value = value
        }

    var reminderTime: String
        get() = prefs.getString(KEY_REMINDER_TIME, "08:00") ?: "08:00"
        set(value) {
            prefs.edit().putString(KEY_REMINDER_TIME, value).apply()
        }

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()
            _wakeWordEnabled.value = value
        }

    val targetLanguage: String get() = targetLanguageName
    val motherTongue: String get() = motherTongueName

    var cachedStreak: Int
        get() = prefs.getInt(KEY_CACHED_STREAK, 7)
        set(value) {
            prefs.edit().putInt(KEY_CACHED_STREAK, value).apply()
        }

    var cachedTotalXp: Int
        get() = prefs.getInt(KEY_CACHED_TOTAL_XP, 420)
        set(value) {
            prefs.edit().putInt(KEY_CACHED_TOTAL_XP, value).apply()
        }

    var cachedCurrentDay: Int
        get() = prefs.getInt(KEY_CACHED_CURRENT_DAY, 7)
        set(value) {
            prefs.edit().putInt(KEY_CACHED_CURRENT_DAY, value).apply()
        }

    var userDisplayName: String
        get() = prefs.getString(KEY_USER_DISPLAY_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_DISPLAY_NAME, value).apply()
        }

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_EMAIL, value).apply()
        }

    var userUid: String
        get() = prefs.getString(KEY_USER_UID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_UID, value).apply()
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()
        }

    var voiceGender: String
        get() = prefs.getString(KEY_VOICE_GENDER, "FEMALE") ?: "FEMALE"
        set(value) {
            prefs.edit().putString(KEY_VOICE_GENDER, value).apply()
        }

    var voiceSpeed: Float
        get() = prefs.getFloat(KEY_VOICE_SPEED, 1.0f)
        set(value) {
            prefs.edit().putFloat(KEY_VOICE_SPEED, value).apply()
        }

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 1.0f)
        set(value) {
            prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()
        }

    var conversationLanguage: String
        get() = prefs.getString(KEY_CONVERSATION_LANGUAGE, "auto") ?: "auto"
        set(value) {
            prefs.edit().putString(KEY_CONVERSATION_LANGUAGE, value).apply()
        }

    var confirmSensitiveActions: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_SENSITIVE_ACTIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CONFIRM_SENSITIVE_ACTIONS, value).apply()
        }

    fun onUserAuthenticated(uid: String, email: String, displayName: String) {
        prefs.edit()
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_DISPLAY_NAME, displayName)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * Clear user-specific session data on logout while preserving app configurations.
     */
    fun clearUserSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_USER_UID, "")
            .putString(KEY_USER_DISPLAY_NAME, "")
            .putString(KEY_USER_EMAIL, "")
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .apply()
        _isOnboardingCompleted.value = false
    }

    /**
     * Reset everything for testing or clean reinstall simulation.
     */
    fun resetAll() {
        prefs.edit().clear().apply()
        _isOnboardingCompleted.value = false
        _targetLanguageName.value = "English"
        _motherTongueName.value = "Tamil"
        _dailyGoalMinutes.value = 15
        _wakeWordEnabled.value = false
        _reminderEnabled.value = true
        syncToMockDataProvider()
    }

    /**
     * Synchronizes persistent preferences into the in-memory fallback layer
     * so all UI components reflect the user's authentic persisted profile,
     * target language, streak, and XP even across offline transitions or restarts.
     */
    fun syncToMockDataProvider() {
        try {
            val user = com.example.promaster.data.mock.MockDataProvider.currentUser
            com.example.promaster.data.mock.MockDataProvider.currentUser = user.copy(
                id = if (userUid.isNotBlank()) userUid else user.id,
                name = if (userDisplayName.isNotBlank()) userDisplayName else user.name,
                email = if (userEmail.isNotBlank()) userEmail else user.email,
                targetLanguage = if (targetLanguageName.isNotBlank()) targetLanguageName else user.targetLanguage,
                nativeLanguage = if (motherTongueName.isNotBlank()) motherTongueName else user.nativeLanguage,
                streakDays = if (cachedStreak > 0) cachedStreak else user.streakDays,
                totalXp = if (cachedTotalXp > 0) cachedTotalXp else user.totalXp,
                currentDay = if (cachedCurrentDay > 0) cachedCurrentDay else user.currentDay,
                dailyGoalMinutes = dailyGoalMinutes
            )
            val progress = com.example.promaster.data.mock.MockDataProvider.progressState
            com.example.promaster.data.mock.MockDataProvider.progressState = progress.copy(
                currentStreak = if (cachedStreak > 0) cachedStreak else progress.currentStreak,
                totalXp = if (cachedTotalXp > 0) cachedTotalXp else progress.totalXp
            )
        } catch (_: Throwable) {}
    }
}

