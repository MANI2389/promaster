package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.SettingsDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSettingsRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) {
    private val settingsCollection get() = firestore?.collection("settings")

    suspend fun saveSettings(userId: String, settingsDoc: SettingsDocument): FirebaseResult<Unit> {
        val col = settingsCollection ?: return FirebaseResult.Success(Unit)
        return try {
            col.document(userId).set(settingsDoc, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    suspend fun getSettings(userId: String): FirebaseResult<SettingsDocument> {
        val col = settingsCollection ?: return FirebaseResult.Success(SettingsDocument())
        return try {
            val snapshot = col.document(userId).get().await()
            if (snapshot.exists()) {
                val doc = snapshot.toObject(SettingsDocument::class.java) ?: SettingsDocument()
                FirebaseResult.Success(doc)
            } else {
                val initial = SettingsDocument()
                saveSettings(userId, initial)
                FirebaseResult.Success(initial)
            }
        } catch (e: Exception) {
            FirebaseResult.Offline(SettingsDocument())
        }
    }

    fun getSettingsFlow(userId: String): Flow<FirebaseResult<SettingsDocument>> = callbackFlow {
        val docRef = try { settingsCollection?.document(userId) } catch (_: Exception) { null }
        if (docRef == null) {
            trySend(FirebaseResult.Offline(SettingsDocument()))
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(FirebaseResult.Offline(SettingsDocument()))
            } else {
                val doc = snapshot.toObject(SettingsDocument::class.java) ?: SettingsDocument()
                trySend(FirebaseResult.Success(doc))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateNotificationPreference(userId: String, enabled: Boolean): FirebaseResult<Unit> {
        val col = settingsCollection ?: return FirebaseResult.Success(Unit)
        return try {
            col.document(userId).update("notificationsEnabled", enabled).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }
}
