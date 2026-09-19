package com.example.promaster.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockUserProfileRepositoryImplTest {

    private lateinit var repository: MockUserProfileRepositoryImpl

    @Before
    fun setup() {
        repository = MockUserProfileRepositoryImpl()
    }

    @Test
    fun getUserProfile_returnsValidUser() = runTest {
        val user = repository.getUserProfile().first()
        assertNotNull(user.id)
        assertNotNull(user.name)
        assertTrue(user.streakDays > 0)
        assertTrue(user.totalXp > 0)
    }

    @Test
    fun updateTargetLanguage_modifiesUserProfile() = runTest {
        repository.updateTargetLanguage("fr")
        val updated = repository.getUserProfile().first()
        assertEquals("French", updated.targetLanguage)
    }

    @Test
    fun updateDailyGoal_modifiesMinutes() = runTest {
        repository.updateDailyGoal(30)
        val updated = repository.getUserProfile().first()
        assertEquals(30, updated.dailyGoalMinutes)
    }

    @Test
    fun addXp_incrementsUserAndProgressXp() = runTest {
        val initialXp = repository.getUserProfile().first().totalXp
        repository.addXp(50)
        val updatedXp = repository.getUserProfile().first().totalXp
        assertEquals(initialXp + 50, updatedXp)

        val progressXp = repository.getProgressState().first().totalXp
        assertEquals(updatedXp, progressXp)
    }
}
