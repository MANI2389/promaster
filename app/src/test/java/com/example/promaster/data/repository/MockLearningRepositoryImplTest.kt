package com.example.promaster.data.repository

import com.example.promaster.domain.model.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockLearningRepositoryImplTest {

    private lateinit var repository: MockLearningRepositoryImpl

    @Before
    fun setup() {
        repository = MockLearningRepositoryImpl()
    }

    @Test
    fun get30DayPlan_returnsFullCurriculum() = runTest {
        val plan = repository.get30DayPlan().first()
        assertEquals(30, plan.size)
        assertEquals(1, plan.first().day)
        assertEquals(30, plan.last().day)
        assertTrue(plan.any { it.isCurrent })
    }

    @Test
    fun getTodayTasks_returnsTasksWithRewards() = runTest {
        val tasks = repository.getTodayTasks().first()
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.any { it.category == TaskCategory.VOCABULARY })
        assertTrue(tasks.any { it.category == TaskCategory.SPEAKING })
    }

    @Test
    fun completeTask_updatesTaskCompletionStatus() = runTest {
        val initialTasks = repository.getTodayTasks().first()
        val targetTask = initialTasks.first { !it.isCompleted }

        repository.completeTask(targetTask.id)

        val updatedTasks = repository.getTodayTasks().first()
        val updatedTask = updatedTasks.first { it.id == targetTask.id }
        assertTrue(updatedTask.isCompleted)
    }

    @Test
    fun getVocabularyList_returnsRichItems() = runTest {
        val vocab = repository.getVocabulary().first()
        assertTrue(vocab.isNotEmpty())
        val item = vocab.first()
        assertNotNull(item.word)
        assertNotNull(item.phonetic)
        assertNotNull(item.translation)
        assertNotNull(item.exampleSentence)
    }

    @Test
    fun getGrammarRules_returnsRulesWithFormulas() = runTest {
        val rules = repository.getGrammarRules().first()
        assertTrue(rules.isNotEmpty())
        val rule = rules.first()
        assertNotNull(rule.title)
        assertNotNull(rule.formula)
        assertTrue(rule.examples.isNotEmpty())
    }
}
