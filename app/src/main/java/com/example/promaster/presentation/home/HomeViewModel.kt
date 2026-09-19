package com.example.promaster.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.firebase.repository.FirestoreUserRepository
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.data.repository.MockUserProfileRepositoryImpl
import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.ProgressState
import com.example.promaster.domain.model.User
import com.example.promaster.domain.repository.LearningRepository
import com.example.promaster.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val progress: ProgressState? = null,
    val tasks: List<DailyTask> = emptyList(),
    val completedTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val aiGreeting: String = "¡Hola! Ready to master Day 7?"
)

class HomeViewModel(
    private val userProfileRepo: UserProfileRepository = FirestoreUserRepository(),
    private val learningRepo: LearningRepository = com.example.promaster.data.repository.FirebaseLearningRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userProfileRepo.getUserProfile(),
                userProfileRepo.getProgressState(),
                learningRepo.getTodayTasks()
            ) { user, progress, tasks ->
                val completed = tasks.count { it.isCompleted }
                val greeting = when {
                    user.streakDays > 5 -> "¡Hola ${user.name}! 🔥 ${user.streakDays}-day streak going strong!"
                    else -> "¡Hola ${user.name}! Ready to learn ${user.targetLanguage} today?"
                }
                HomeUiState(
                    user = user,
                    progress = progress,
                    tasks = tasks,
                    completedTaskCount = completed,
                    totalTaskCount = tasks.size,
                    aiGreeting = greeting
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            learningRepo.completeTask(taskId)
            userProfileRepo.addXp(30)
        }
    }
}
