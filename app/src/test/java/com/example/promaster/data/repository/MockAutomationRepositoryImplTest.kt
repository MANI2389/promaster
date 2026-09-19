package com.example.promaster.data.repository

import com.example.promaster.domain.model.SafetyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockAutomationRepositoryImplTest {

    private lateinit var repository: MockAutomationRepositoryImpl

    @Before
    fun setup() {
        repository = MockAutomationRepositoryImpl()
    }

    @Test
    fun getAvailableActions_returnsConfiguredRoutines() = runTest {
        val actions = repository.getAvailableActions().first()
        assertTrue(actions.isNotEmpty())
        assertTrue(actions.any { it.safetyLevel == SafetyLevel.SAFE })
    }

    @Test
    fun toggleAction_updatesEnabledState() = runTest {
        val actions = repository.getAvailableActions().first()
        val firstAction = actions.first()
        val newState = !firstAction.isEnabled

        val result = repository.toggleAction(firstAction.id, newState)
        assertTrue(result.isSuccess)

        val updatedActions = repository.getAvailableActions().first()
        val updated = updatedActions.first { it.id == firstAction.id }
        assertEquals(newState, updated.isEnabled)
    }

    @Test
    fun triggerActionSafely_executesSafelyForSafeAction() = runTest {
        val actions = repository.getAvailableActions().first()
        val safeAction = actions.first { it.safetyLevel == SafetyLevel.SAFE }

        val result = repository.triggerActionSafely(safeAction.id)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("completed safely") == true)
    }

    @Test
    fun triggerActionSafely_handlesConfirmationAction() = runTest {
        val actions = repository.getAvailableActions().first()
        val confirmationAction = actions.firstOrNull { it.safetyLevel == SafetyLevel.REQUIRES_CONFIRMATION }

        if (confirmationAction != null) {
            val result = repository.triggerActionSafely(confirmationAction.id)
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.contains("confirmation") == true)
        }
    }
}
