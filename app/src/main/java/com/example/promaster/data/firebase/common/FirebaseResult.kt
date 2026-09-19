package com.example.promaster.data.firebase.common

sealed class FirebaseResult<out T> {
    data class Success<out T>(val data: T) : FirebaseResult<T>()
    data class Offline<out T>(val data: T, val cachedAt: Long = System.currentTimeMillis()) : FirebaseResult<T>()
    data class PermissionDenied(val message: String = "Access denied: You only have permission to view your own data.") : FirebaseResult<Nothing>()
    data class NetworkError(val message: String = "Unable to connect to the cloud. Working in offline mode.") : FirebaseResult<Nothing>()
    data class AuthError(val message: String, val code: String? = null) : FirebaseResult<Nothing>()
    data class MissingData(val message: String = "The requested record could not be found.") : FirebaseResult<Nothing>()
    data class Error(val exception: Throwable, val message: String = exception.localizedMessage ?: "An unexpected error occurred.") : FirebaseResult<Nothing>()

    val isSuccess: Boolean get() = this is Success || this is Offline

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Offline -> data
        else -> null
    }
}

fun <T> FirebaseResult<T>.getOrDefault(defaultValue: T): T = getOrNull() ?: defaultValue
