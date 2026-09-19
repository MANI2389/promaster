package com.example.promaster.data.repository

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.User
import com.example.promaster.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepositoryImpl : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(MockDataProvider.currentUser)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, pass: String): Result<User> {
        val user = MockDataProvider.currentUser.copy(email = email)
        MockDataProvider.currentUser = user
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun register(name: String, email: String, pass: String): Result<User> {
        val user = MockDataProvider.currentUser.copy(name = name, email = email)
        MockDataProvider.currentUser = user
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun loginAsGuest(): Result<User> {
        val guest = MockDataProvider.currentUser.copy(name = "Guest Explorer", email = "guest@promaster.local")
        MockDataProvider.currentUser = guest
        _currentUser.value = guest
        return Result.success(guest)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isAuthenticated(): Boolean = _currentUser.value != null
}
