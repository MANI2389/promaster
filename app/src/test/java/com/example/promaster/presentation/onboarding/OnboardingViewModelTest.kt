package com.example.promaster.presentation.onboarding

import com.example.promaster.data.repository.LocalOnboardingRepositoryImpl
import com.example.promaster.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalOnboardingRepositoryImpl
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = LocalOnboardingRepositoryImpl()
        viewModel = OnboardingViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultLanguagesAndStartsAtWelcome() {
        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.WELCOME, state.currentStep)
        assertTrue(state.availableLanguages.isNotEmpty())
        assertNotNull(state.selectedMotherTongue)
        assertNotNull(state.selectedTargetLanguage)
        assertEquals(LanguageLevel.BEGINNER, state.selectedLevel)
        assertEquals(15, state.selectedDurationMinutes)
        assertEquals(PreferredLearningTime.MORNING, state.selectedLearningTime)
        assertTrue(state.reminderEnabled)
    }

    @Test
    fun nameValidation_requiresAtLeastTwoCharacters() {
        assertFalse(viewModel.uiState.value.canProceed)

        viewModel.onNameChanged("A")
        assertFalse(viewModel.uiState.value.canProceed)
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameChanged("Alex")
        assertTrue(viewModel.uiState.value.canProceed)
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun motherTongueAndTargetLanguage_canBeSelectedAndFiltered() {
        val tamil = viewModel.uiState.value.availableLanguages.find { it.code == "ta" }
        assertNotNull("Tamil should be present in available languages", tamil)
        viewModel.onMotherTongueSelected(tamil!!)
        assertEquals("ta", viewModel.uiState.value.selectedMotherTongue?.code)

        val french = viewModel.uiState.value.availableLanguages.find { it.code == "fr" }
        assertNotNull("French should be present in available languages", french)
        viewModel.onTargetLanguageSelected(french!!)
        assertEquals("fr", viewModel.uiState.value.selectedTargetLanguage?.code)

        // Filter test
        viewModel.onMotherTongueSearchChanged("Hindi")
        val filtered = viewModel.uiState.value.filteredMotherTongues
        assertTrue(filtered.any { it.code == "hi" })
        assertFalse(filtered.any { it.code == "ja" })
    }

    @Test
    fun levels_canBeSelected() {
        LanguageLevel.values().forEach { level ->
            viewModel.onLevelSelected(level)
            assertEquals(level, viewModel.uiState.value.selectedLevel)
        }
    }

    @Test
    fun dailyDurations_supportAllRequiredOptions() {
        val validDurations = listOf(10, 15, 20, 30, 45, 60)
        validDurations.forEach { duration ->
            viewModel.onDurationSelected(duration)
            assertEquals(duration, viewModel.uiState.value.selectedDurationMinutes)
        }
    }

    @Test
    fun preferredLearningTime_andReminders_canBeToggled() {
        PreferredLearningTime.values().forEach { timeSlot ->
            viewModel.onLearningTimeSelected(timeSlot)
            assertEquals(timeSlot, viewModel.uiState.value.selectedLearningTime)
        }

        viewModel.onReminderToggled(false)
        assertFalse(viewModel.uiState.value.reminderEnabled)

        viewModel.onReminderToggled(true)
        assertTrue(viewModel.uiState.value.reminderEnabled)
    }

    @Test
    fun goalType_canBeSelected() {
        LearningGoalType.values().forEach { goal ->
            viewModel.onGoalTypeSelected(goal)
            assertEquals(goal, viewModel.uiState.value.selectedGoalType)
        }
    }

    @Test
    fun navigationSteps_advanceAndGoBack() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)
        viewModel.onNameChanged("Alex Vance")

        viewModel.onNextStep()
        assertEquals(OnboardingStep.MOTHER_TONGUE, viewModel.uiState.value.currentStep)

        viewModel.onNextStep()
        assertEquals(OnboardingStep.TARGET_LANGUAGE, viewModel.uiState.value.currentStep)

        viewModel.onNextStep()
        assertEquals(OnboardingStep.LEVEL, viewModel.uiState.value.currentStep)

        viewModel.onNextStep()
        assertEquals(OnboardingStep.DURATION, viewModel.uiState.value.currentStep)

        viewModel.onNextStep()
        assertEquals(OnboardingStep.LEARNING_TIME, viewModel.uiState.value.currentStep)

        viewModel.onNextStep()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.currentStep)

        viewModel.onPreviousStep()
        assertEquals(OnboardingStep.LEARNING_TIME, viewModel.uiState.value.currentStep)
    }

    @Test
    fun completeOnboarding_savesProfileAndExecutesCallback() = runTest {
        viewModel.onNameChanged("Maya Lin")
        val japanese = viewModel.uiState.value.availableLanguages.first { it.code == "ja" }
        val english = viewModel.uiState.value.availableLanguages.first { it.code == "en" }
        viewModel.onMotherTongueSelected(english)
        viewModel.onTargetLanguageSelected(japanese)
        viewModel.onLevelSelected(LanguageLevel.INTERMEDIATE)
        viewModel.onDurationSelected(30)
        viewModel.onLearningTimeSelected(PreferredLearningTime.EVENING)
        viewModel.onGoalTypeSelected(LearningGoalType.TRAVEL)

        var finishedCalled = false
        viewModel.completeOnboarding {
            finishedCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(finishedCalled)
        assertTrue(viewModel.uiState.value.isCompleted)
    }
}
