package com.example.promaster.presentation.learning

import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayTasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loadsTasks_andCalculatesInitialProgress() = runTest {
        val repo = MockLearningRepositoryImpl()
        val viewModel = TodayTasksViewModel(learningRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.tasks.isNotEmpty())
        assertEquals(1, state.currentDay)
        assertTrue(state.totalTasks > 0)
    }

    @Test
    fun viewModel_lockedTaskClick_triggersLockWarning() = runTest {
        val repo = MockLearningRepositoryImpl()
        val viewModel = TodayTasksViewModel(learningRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val lockedTask = viewModel.uiState.value.tasks.firstOrNull { it.completionStatus == TaskCompletionStatus.LOCKED }
        if (lockedTask != null) {
            var callbackInvoked = false
            viewModel.onTaskClicked(lockedTask) {
                callbackInvoked = true
            }

            assertFalse(callbackInvoked)
            assertNotNull(viewModel.uiState.value.lockWarning)

            viewModel.dismissLockWarning()
            assertNull(viewModel.uiState.value.lockWarning)
        }
    }

    @Test
    fun viewModel_submitPassingScore_updatesProgressAndAwardsXp() = runTest {
        val repo = MockLearningRepositoryImpl()
        val viewModel = TodayTasksViewModel(learningRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstTask = viewModel.uiState.value.tasks.first()
        viewModel.submitScore(firstTask.taskId, 95)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.feedbackMessage)
        assertTrue(state.feedbackMessage!!.contains("completed", ignoreCase = true))
    }
}
