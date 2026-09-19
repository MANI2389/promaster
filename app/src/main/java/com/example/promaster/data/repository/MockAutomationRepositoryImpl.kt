package com.example.promaster.data.repository

import com.example.promaster.automation.AutomationManager
import com.example.promaster.automation.MockAutomationManager
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.AutomationAction
import com.example.promaster.domain.repository.AutomationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAutomationRepositoryImpl(
    private val automationManager: AutomationManager = MockAutomationManager()
) : AutomationRepository {

    private val _actions = MutableStateFlow(MockDataProvider.automationActions)

    override fun getAvailableActions(): Flow<List<AutomationAction>> = _actions.asStateFlow()

    override suspend fun toggleAction(actionId: String, isEnabled: Boolean): Result<Boolean> {
        val updated = _actions.value.map { action ->
            if (action.id == actionId) action.copy(isEnabled = isEnabled) else action
        }
        _actions.value = updated
        return Result.success(true)
    }

    override suspend fun triggerActionSafely(actionId: String): Result<String> {
        val action = _actions.value.find { it.id == actionId }
            ?: return Result.failure(IllegalArgumentException("Action not found"))
        return automationManager.executeSafeAction(action)
    }
}
