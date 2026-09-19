package com.example.promaster.data.firebase

import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.common.getOrDefault
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.*
import org.junit.Test

class FirebaseErrorHandlingTest {

    @Test
    fun firebaseResult_success_and_offline_states() {
        val success = FirebaseResult.Success("Ready")
        assertTrue(success.isSuccess)
        assertEquals("Ready", success.getOrNull())
        assertEquals("Ready", success.getOrDefault("Default"))

        val offline = FirebaseResult.Offline("Cached Plan")
        assertTrue(offline.isSuccess)
        assertEquals("Cached Plan", offline.getOrNull())
    }

    @Test
    fun firebaseResult_error_states_provideUsefulMessages() {
        val permError = FirebaseResult.PermissionDenied()
        val permissionDenied: FirebaseResult<String> = permError
        assertFalse(permissionDenied.isSuccess)
        assertNull(permissionDenied.getOrNull())
        assertEquals("Fallback", permissionDenied.getOrDefault("Fallback"))
        assertTrue(permError.message.contains("Access denied"))

        val networkError = FirebaseResult.NetworkError()
        assertFalse(networkError.isSuccess)
        assertTrue(networkError.message.contains("offline mode") || networkError.message.contains("cloud"))

        val authError = FirebaseResult.AuthError("The email address is already in use by another account.", "ERROR_EMAIL_ALREADY_IN_USE")
        assertFalse(authError.isSuccess)
        assertEquals("ERROR_EMAIL_ALREADY_IN_USE", authError.code)

        val missingData = FirebaseResult.MissingData()
        assertFalse(missingData.isSuccess)
    }

    @Test
    fun handleException_mapsNetworkExceptionProperly() {
        val netEx = FirebaseNetworkException("Connection timed out")
        val result: FirebaseResult<String> = FirebaseManager.handleException(netEx)
        assertTrue(result is FirebaseResult.NetworkError)
    }

    @Test
    fun handleException_mapsGenericExceptionToError() {
        val generic = IllegalStateException("Something broke")
        val result: FirebaseResult<String> = FirebaseManager.handleException(generic)
        assertTrue(result is FirebaseResult.Error)
        assertEquals("Something broke", (result as FirebaseResult.Error).message)
    }
}
