package com.example.promaster.data.repository

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.User
import com.example.promaster.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class MockUserProfileRepositoryImpl : UserProfileRepository {
    companion object {
        private val _user = MutableStateFlow(MockDataProvider.currentUser)
        private val _progress = MutableStateFlow(MockDataProvider.progressState)

        fun syncUser(user: User) {
            _user.value = user
            MockDataProvider.currentUser = user
        }
    }

    override fun getUserProfile(): Flow<User> = _user.asStateFlow()

    override fun getProgressState(): Flow<ProgressState> = _progress.asStateFlow()

    override suspend fun updateTargetLanguage(languageCode: String) {
        val langName = MockDataProvider.languages.find { it.code == languageCode }?.name ?: languageCode
        val updated = _user.value.copy(targetLanguage = langName)
        _user.value = updated
        MockDataProvider.currentUser = updated
    }

    override suspend fun updateDailyGoal(minutes: Int) {
        val updated = _user.value.copy(dailyGoalMinutes = minutes)
        _user.value = updated
        MockDataProvider.currentUser = updated
    }

    override suspend fun addXp(amount: Int) {
        val updatedXp = _user.value.totalXp + amount
        val updated = _user.value.copy(totalXp = updatedXp)
        _user.value = updated
        MockDataProvider.currentUser = updated

        val currentProg = _progress.value
        _progress.value = currentProg.copy(totalXp = updatedXp)
    }
}
