package com.example.promaster.data.firebase

import android.content.Context
import com.example.promaster.data.firebase.common.FirebaseResult
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                configureFirestorePersistence()
                isInitialized = true
            } catch (e: Exception) {
                // If in test environment or missing services, allow graceful local fallback
                isInitialized = false
            }
        }
    }

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (_: Throwable) {}
            db
        } catch (_: Throwable) {
            null
        }

    val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (_: Throwable) {
            null
        }

    private fun configureFirestorePersistence() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore?.firestoreSettings = settings
        } catch (_: Exception) {}
    }

    val currentUserId: String
        get() = try {
            auth?.currentUser?.uid ?: "guest_user_local"
        } catch (_: Exception) {
            "guest_user_local"
        }

    fun <T> handleException(e: Throwable): FirebaseResult<T> {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        FirebaseResult.PermissionDenied("Access denied: You only have permission to access your private data.")
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        FirebaseResult.NetworkError("Cloud service temporarily unavailable. Working in offline mode.")
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        FirebaseResult.MissingData("Document not found.")
                    else -> FirebaseResult.Error(e, e.localizedMessage ?: "Firestore error: ${e.code}")
                }
            }
            is FirebaseNetworkException ->
                FirebaseResult.NetworkError("Network connection lost. Please check your Wi-Fi or mobile data.")
            is FirebaseAuthException ->
                FirebaseResult.AuthError(e.localizedMessage ?: "Authentication failed.", e.errorCode)
            else -> FirebaseResult.Error(e, e.localizedMessage ?: "An unexpected error occurred.")
        }
    }
}
