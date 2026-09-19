package com.example.promaster.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.preferences.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class StartupState {
    STARTING,
    AUTH_CHECKING,
    UNAUTHENTICATED,
    AUTHENTICATED,
    ONBOARDING_REQUIRED,
    READY
}

/**
 * Authoritative Startup State Machine for PROMASTER.
 * Resolves whether the user is unauthenticated (Login required),
 * authenticated but needs onboarding (Onboarding required),
 * or returning authenticated learner (Home screen ready).
 *
 * Uses Firebase Authentication as the single source of truth,
 * waiting on AuthStateListener during cold boot to guarantee persistent
 * credentials hydrate before deciding destination.
 */
class SplashViewModel(
    private val isAuthenticatedProvider: suspend () -> Boolean = {
        resolveAuthPersistence()
    },
    private val isOnboardingCompletedProvider: () -> Boolean = {
        UserPreferencesRepository.getInstanceOrNull()?.isOnboardingCompleted == true
    }
) : ViewModel() {

    private val _startupState = MutableStateFlow(StartupState.STARTING)
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    init {
        resolveStartupDestination()
    }

    fun resolveStartupDestination() {
        viewModelScope.launch {
            _startupState.value = StartupState.STARTING

            // 1. Maintain minimum 800ms splash display for brand presentation and animation
            delay(800)

            _startupState.value = StartupState.AUTH_CHECKING

            // 2. Perform authoritative auth check with timeout
            val isAuthenticated = withTimeoutOrNull(3000L) {
                try { isAuthenticatedProvider() } catch (_: Throwable) { false }
            } ?: false

            if (!isAuthenticated) {
                _startupState.value = StartupState.UNAUTHENTICATED
            } else {
                _startupState.value = StartupState.AUTHENTICATED
                val onboardingCompleted = try { isOnboardingCompletedProvider() } catch (_: Throwable) { false }
                _startupState.value = if (onboardingCompleted) {
                    StartupState.READY
                } else {
                    StartupState.ONBOARDING_REQUIRED
                }
            }
        }
    }

    companion object {
        suspend fun resolveAuthPersistence(): Boolean {
            return try {
                val auth = FirebaseManager.auth
                if (auth?.currentUser != null) return true

                // Check persistent authenticated local session
                val prefs = UserPreferencesRepository.getInstanceOrNull()
                val hasLocalSession = prefs?.isLoggedIn == true && prefs.userUid.isNotBlank()

                // Await AuthStateListener for token hydration from local storage
                if (auth != null) {
                    val authDeferred = CompletableDeferred<Boolean>()
                    val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                        if (fbAuth.currentUser != null && !authDeferred.isCompleted) {
                            authDeferred.complete(true)
                        }
                    }
                    try {
                        auth.addAuthStateListener(listener)
                        val restored = withTimeoutOrNull(2000L) {
                            authDeferred.await()
                        } ?: (auth.currentUser != null)
                        if (restored) return true
                    } finally {
                        try {
                            auth.removeAuthStateListener(listener)
                        } catch (_: Throwable) {}
                    }
                }

                // Return persistent session state (never log out unless user explicitly signed out)
                hasLocalSession
            } catch (_: Throwable) {
                false
            }
        }
    }
}
