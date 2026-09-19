package com.example.promaster.domain.repository

import com.example.promaster.domain.model.AutomationAction
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun getAvailableActions(): Flow<List<AutomationAction>>
    suspend fun toggleAction(actionId: String, isEnabled: Boolean): Result<Boolean>
    suspend fun triggerActionSafely(actionId: String): Result<String>
}
