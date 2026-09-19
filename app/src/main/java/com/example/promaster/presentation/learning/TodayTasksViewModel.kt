package com.example.promaster.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.repository.FirebaseLearningRepositoryImpl
import com.example.promaster.domain.engine.LearningLockSystem
import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus
import com.example.promaster.domain.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TodayTasksUiState(
    val tasks: List<DailyTask> = emptyList(),
    val currentDay: Int = 1,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val dailyPercentage: Float = 0f,
    val overallPercentage: Float = 0f,
    val totalXp: Int = 0,
    val streak: Int = 1,
    val isOffline: Boolean = false,
    val lockWarning: String? = null,
    val feedbackMessage: String? = null,
    val selectedTaskForExercise: DailyTask? = null
)

class TodayTasksViewModel(
    private val learningRepo: LearningRepository = FirebaseLearningRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayTasksUiState())
    val uiState: StateFlow<TodayTasksUiState> = _uiState.asStateFlow()

    init {
        loadTasksForDay(1)
    }

    fun loadTasksForDay(dayNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentDay = dayNumber) }
            learningRepo.getTasksForDay(dayNumber).collect { tasks ->
                val completed = tasks.count { it.completionStatus == TaskCompletionStatus.COMPLETED }
                val dailyRatio = if (tasks.isNotEmpty()) completed.toFloat() / tasks.size.toFloat() else 0f
                val overallRatio = ((dayNumber - 1 + dailyRatio) / 30f).coerceIn(0f, 1f)

                _uiState.update { current ->
                    current.copy(
                        tasks = tasks,
                        completedTasks = completed,
                        totalTasks = tasks.size,
                        dailyPercentage = dailyRatio,
                        overallPercentage = overallRatio,
                        totalXp = tasks.filter { it.completionStatus == TaskCompletionStatus.COMPLETED }.sumOf { it.xpReward },
                        isOffline = FirebaseManager.firestore == null
                    )
                }
            }
        }
    }

    fun onTaskClicked(task: DailyTask, onStartExercise: (TaskCategory) -> Unit) {
        val (canStart, lockReason) = LearningLockSystem.canStartTask(_uiState.value.tasks, task.taskId)
        if (!canStart) {
            _uiState.update { it.copy(lockWarning = lockReason) }
        } else {
            _uiState.update { it.copy(selectedTaskForExercise = task, lockWarning = null) }
            onStartExercise(task.category)
        }
    }

    fun submitScore(taskId: String, score: Int) {
        viewModelScope.launch {
            val result = learningRepo.submitTaskScore(taskId, score)
            result.onSuccess { validation ->
                when (validation) {
                    is LearningLockSystem.TaskValidationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                feedbackMessage = validation.message,
                                lockWarning = null,
                                selectedTaskForExercise = null
                            )
                        }
                    }
                    is LearningLockSystem.TaskValidationResult.FailedPassingScore -> {
                        _uiState.update {
                            it.copy(
                                lockWarning = validation.message,
                                selectedTaskForExercise = null
                            )
                        }
                    }
                    is LearningLockSystem.TaskValidationResult.Locked -> {
                        _uiState.update {
                            it.copy(
                                lockWarning = validation.reason,
                                selectedTaskForExercise = null
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(lockWarning = error.message) }
            }
        }
    }

    fun dismissLockWarning() {
        _uiState.update { it.copy(lockWarning = null) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
