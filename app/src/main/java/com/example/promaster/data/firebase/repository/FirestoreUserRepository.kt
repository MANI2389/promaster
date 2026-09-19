package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.ProgressDocument
import com.example.promaster.data.firebase.model.UserDocument
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.User
import com.example.promaster.domain.repository.UserProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) : UserProfileRepository {

    private val usersCollection get() = firestore?.collection("users")

    suspend fun saveUser(userId: String, doc: UserDocument): FirebaseResult<Unit> {
        val col = usersCollection ?: run {
            MockDataProvider.currentUser = doc.toDomain(userId)
            return FirebaseResult.Success(Unit)
        }
        return try {
            col.document(userId).set(doc, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            // Local fallback
            MockDataProvider.currentUser = doc.toDomain(userId)
            FirebaseManager.handleException(e)
        }
    }

    suspend fun getUser(userId: String): FirebaseResult<UserDocument> {
        val col = usersCollection ?: run {
            return FirebaseResult.Success(UserDocument.fromDomain(MockDataProvider.currentUser))
        }
        return try {
            val snapshot = col.document(userId).get().await()
            if (snapshot.exists()) {
                val doc = snapshot.toObject(UserDocument::class.java) ?: UserDocument()
                FirebaseResult.Success(doc)
            } else {
                FirebaseResult.MissingData("User $userId does not exist.")
            }
        } catch (e: Exception) {
            val fallback = UserDocument.fromDomain(MockDataProvider.currentUser)
            FirebaseResult.Offline(fallback)
        }
    }

    fun getUserFlow(userId: String): Flow<FirebaseResult<UserDocument>> = callbackFlow {
        val docRef = try { usersCollection?.document(userId) } catch (_: Exception) { null }
        if (docRef == null) {
            trySend(FirebaseResult.Offline(UserDocument.fromDomain(MockDataProvider.currentUser)))
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(FirebaseResult.Offline(UserDocument.fromDomain(MockDataProvider.currentUser)))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val doc = snapshot.toObject(UserDocument::class.java) ?: UserDocument()
                trySend(FirebaseResult.Success(doc))
            } else {
                trySend(FirebaseResult.Offline(UserDocument.fromDomain(MockDataProvider.currentUser)))
            }
        }
        awaitClose { registration.remove() }
    }

    // --- Implementation of domain UserProfileRepository ---

    override fun getUserProfile(): Flow<User> = callbackFlow {
        val uid = FirebaseManager.currentUserId
        val docRef = try { usersCollection?.document(uid) } catch (_: Exception) { null }
        if (docRef == null) {
            trySend(MockDataProvider.currentUser)
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(MockDataProvider.currentUser)
            } else {
                val userDoc = snapshot.toObject(UserDocument::class.java) ?: UserDocument()
                trySend(userDoc.toDomain(uid))
            }
        }
        awaitClose { registration.remove() }
    }

    override fun getProgressState(): Flow<ProgressState> = callbackFlow {
        val uid = FirebaseManager.currentUserId
        val progRef = try { firestore?.collection("progress")?.document(uid) } catch (_: Exception) { null }
        if (progRef == null) {
            trySend(MockDataProvider.progressState)
            close()
            return@callbackFlow
        }

        val registration = progRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(MockDataProvider.progressState)
            } else {
                val progDoc = snapshot.toObject(ProgressDocument::class.java) ?: ProgressDocument()
                trySend(progDoc.toDomain())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateTargetLanguage(languageCode: String) {
        val uid = FirebaseManager.currentUserId
        val langName = MockDataProvider.languages.find { it.code == languageCode }?.name ?: languageCode
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.let { prefs ->
            prefs.targetLanguageCode = languageCode
            prefs.targetLanguageName = langName
        }
        try {
            usersCollection?.document(uid)?.update("targetLanguage", langName)?.await()
        } catch (_: Exception) {
            MockDataProvider.currentUser = MockDataProvider.currentUser.copy(targetLanguage = langName)
        }
    }

    override suspend fun updateDailyGoal(minutes: Int) {
        val uid = FirebaseManager.currentUserId
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.let { prefs ->
            prefs.dailyGoalMinutes = minutes
        }
        try {
            usersCollection?.document(uid)?.update("dailyDuration", minutes)?.await()
        } catch (_: Exception) {
            MockDataProvider.currentUser = MockDataProvider.currentUser.copy(dailyGoalMinutes = minutes)
        }
    }

    override suspend fun addXp(amount: Int) {
        val uid = FirebaseManager.currentUserId
        val currentXp = MockDataProvider.progressState.totalXp + amount
        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.let { prefs ->
            prefs.cachedTotalXp = currentXp
        }
        try {
            firestore?.collection("progress")?.document(uid)
                ?.set(mapOf("xp" to currentXp, "lastActivityDate" to System.currentTimeMillis()), SetOptions.merge())
                ?.await()
        } catch (_: Exception) {
            MockDataProvider.progressState = MockDataProvider.progressState.copy(totalXp = currentXp)
        }
    }
}
