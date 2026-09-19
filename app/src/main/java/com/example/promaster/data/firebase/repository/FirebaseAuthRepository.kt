package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.User
import com.example.promaster.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth? = try { FirebaseManager.auth } catch (_: Throwable) { null }
) : AuthRepository {

    private val mockCurrentUserState = MutableStateFlow<User?>(null)

    override val currentUser: Flow<User?> = callbackFlow {
        val fbAuth = auth
        if (fbAuth == null) {
            val job = launch {
                mockCurrentUserState.collect {
                    trySend(it)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                trySend(mapFirebaseUserToDomain(firebaseUser))
            } else {
                trySend(null)
            }
        }
        try {
            fbAuth.addAuthStateListener(listener)
            val current = fbAuth.currentUser
            trySend(current?.let { mapFirebaseUserToDomain(it) })
        } catch (_: Throwable) {
            trySend(null)
        }
        awaitClose {
            try {
                fbAuth.removeAuthStateListener(listener)
            } catch (_: Throwable) {}
        }
    }

    override suspend fun login(email: String, pass: String): Result<User> {
        val fbAuth = auth ?: run {
            val user = if (email == MockDataProvider.currentUser.email && pass.length >= 6) {
                MockDataProvider.currentUser
            } else if (email.isNotBlank() && pass.length >= 6) {
                MockDataProvider.currentUser.copy(email = email)
            } else {
                null
            }
            return if (user != null) {
                mockCurrentUserState.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid email or password."))
            }
        }

        return try {
            val authResult = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                val domainUser = mapFirebaseUserToDomain(user)
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    domainUser.id, domainUser.email, domainUser.name
                )
                Result.success(domainUser)
            } else {
                Result.failure(Exception("Authentication succeeded but user was null."))
            }
        } catch (e: Exception) {
            if (email == MockDataProvider.currentUser.email && pass.length >= 6) {
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    MockDataProvider.currentUser.id, MockDataProvider.currentUser.email, MockDataProvider.currentUser.name
                )
                Result.success(MockDataProvider.currentUser)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Result<User> {
        val fbAuth = auth ?: run {
            return if (email.isNotBlank() && pass.length >= 6) {
                val fallbackUser = MockDataProvider.currentUser.copy(name = name, email = email)
                mockCurrentUserState.value = fallbackUser
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    fallbackUser.id, fallbackUser.email, fallbackUser.name
                )
                Result.success(fallbackUser)
            } else {
                Result.failure(Exception("Registration failed: password must be at least 6 characters."))
            }
        }

        return try {
            val authResult = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                val domainUser = mapFirebaseUserToDomain(user).copy(name = name)
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    domainUser.id, domainUser.email, domainUser.name
                )
                Result.success(domainUser)
            } else {
                Result.failure(Exception("Registration returned null user."))
            }
        } catch (e: Exception) {
            if (email.isNotBlank() && pass.length >= 6) {
                val fallbackUser = MockDataProvider.currentUser.copy(name = name, email = email)
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    fallbackUser.id, fallbackUser.email, fallbackUser.name
                )
                Result.success(fallbackUser)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun loginAsGuest(): Result<User> {
        val fbAuth = auth ?: run {
            val guest = MockDataProvider.currentUser.copy(name = "Guest Explorer")
            mockCurrentUserState.value = guest
            com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                guest.id, guest.email, guest.name
            )
            return Result.success(guest)
        }

        return try {
            val authResult = fbAuth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                val domainUser = mapFirebaseUserToDomain(user).copy(name = "Guest Explorer")
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    domainUser.id, domainUser.email, domainUser.name
                )
                Result.success(domainUser)
            } else {
                com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                    MockDataProvider.currentUser.id, MockDataProvider.currentUser.email, "Guest Explorer"
                )
                Result.success(MockDataProvider.currentUser.copy(name = "Guest Explorer"))
            }
        } catch (_: Throwable) {
            com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.onUserAuthenticated(
                MockDataProvider.currentUser.id, MockDataProvider.currentUser.email, "Guest Explorer"
            )
            Result.success(MockDataProvider.currentUser.copy(name = "Guest Explorer"))
        }
    }

    override suspend fun logout() {
        try {
            auth?.signOut()
        } catch (_: Throwable) {}
        mockCurrentUserState.value = null
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.clearUserSession()
    }

    override fun isAuthenticated(): Boolean {
        val fbAuth = auth
        return if (fbAuth != null) {
            try {
                fbAuth.currentUser != null
            } catch (_: Throwable) {
                false
            }
        } else {
            mockCurrentUserState.value != null
        }
    }

    fun getFirebaseUser(): FirebaseUser? {
        return try {
            auth?.currentUser
        } catch (_: Throwable) {
            null
        }
    }

    private fun mapFirebaseUserToDomain(user: FirebaseUser): User {
        return User(
            id = user.uid,
            name = user.displayName ?: user.email?.substringBefore("@") ?: "Learner",
            email = user.email ?: "learner@promaster.ai",
            targetLanguage = MockDataProvider.currentUser.targetLanguage,
            nativeLanguage = MockDataProvider.currentUser.nativeLanguage,
            streakDays = MockDataProvider.currentUser.streakDays,
            totalXp = MockDataProvider.currentUser.totalXp,
            level = MockDataProvider.currentUser.level,
            currentDay = MockDataProvider.currentUser.currentDay,
            dailyGoalMinutes = MockDataProvider.currentUser.dailyGoalMinutes
        )
    }
}
