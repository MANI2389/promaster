package com.example.promaster.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

object Constants {
    const val APP_NAME = "PROMASTER"
    const val APP_TAGLINE = "Your AI Friend. Your Language Coach. Your Voice Assistant."
    const val DEFAULT_TARGET_LANGUAGE = "es"
    const val DEFAULT_NATIVE_LANGUAGE = "en"
    const val XP_PER_TASK = 25
    const val TOTAL_PLAN_DAYS = 30
}
