package com.example.promaster

import android.app.Application
import android.util.Log
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.preferences.UserPreferencesRepository

/**
 * Main Application class for PROMASTER.
 * Provides application-wide singleton context and initializes core subsystems (Firebase, Preferences).
 */
class PromasterApp : Application() {

    companion object {
        private const val TAG = "PROMASTER_APP"

        @Volatile
        private var _instance: PromasterApp? = null

        val instance: PromasterApp
            get() = _instance
                ?: throw IllegalStateException("PromasterApp has not been initialized yet.")

        val applicationContextSafe: PromasterApp?
            get() = _instance
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        try {
            // 1. Initialize persistent local preferences
            UserPreferencesRepository.initialize(this)
            Log.d(TAG, "UserPreferencesRepository initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UserPreferencesRepository", e)
        }

        try {
            // 2. Initialize Firebase subsystems
            FirebaseManager.initialize(this)
            Log.d(TAG, "FirebaseManager initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseManager", e)
        }
    }
}
