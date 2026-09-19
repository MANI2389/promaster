package com.example.promaster.domain.repository

import com.example.promaster.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun register(name: String, email: String, pass: String): Result<User>
    suspend fun loginAsGuest(): Result<User>
    suspend fun logout()
    fun isAuthenticated(): Boolean
}
