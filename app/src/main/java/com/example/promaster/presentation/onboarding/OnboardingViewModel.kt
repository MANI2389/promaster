package com.example.promaster.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.repository.LocalOnboardingRepositoryImpl
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.OnboardingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME,
    MOTHER_TONGUE,
    TARGET_LANGUAGE,
    LEVEL,
    DURATION,
    LEARNING_TIME,
    GOAL,
    CREATE_PLAN
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val nameError: String? = null,
    val availableLanguages: List<Language> = emptyList(),
    val motherTongueSearch: String = "",
    val selectedMotherTongue: Language? = null,
    val targetLanguageSearch: String = "",
    val selectedTargetLanguage: Language? = null,
    val selectedLevel: LanguageLevel = LanguageLevel.BEGINNER,
    val selectedDurationMinutes: Int = 15,
    val selectedLearningTime: PreferredLearningTime = PreferredLearningTime.MORNING,
    val reminderEnabled: Boolean = true,
    val selectedGoalType: LearningGoalType = LearningGoalType.CAREER,
    val isPlanGenerating: Boolean = false,
    val planGenerationProgress: Float = 0f,
    val planGenerationMessage: String = "",
    val isPlanReady: Boolean = false,
    val isCompleted: Boolean = false
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> name.trim().length >= 2
            OnboardingStep.MOTHER_TONGUE -> selectedMotherTongue != null
            OnboardingStep.TARGET_LANGUAGE -> selectedTargetLanguage != null &&
                    selectedTargetLanguage.code != selectedMotherTongue?.code
            OnboardingStep.LEVEL -> true
            OnboardingStep.DURATION -> selectedDurationMinutes in LearningPreferences.DURATION_OPTIONS
            OnboardingStep.LEARNING_TIME -> true
            OnboardingStep.GOAL -> true
            OnboardingStep.CREATE_PLAN -> isPlanReady
        }

    val filteredMotherTongues: List<Language>
        get() = if (motherTongueSearch.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter {
                it.name.contains(motherTongueSearch, ignoreCase = true) ||
                        it.nativeName.contains(motherTongueSearch, ignoreCase = true) ||
                        it.code.contains(motherTongueSearch, ignoreCase = true)
            }
        }

    val filteredTargetLanguages: List<Language>
        get() {
            val list = if (targetLanguageSearch.isBlank()) {
                availableLanguages
            } else {
                availableLanguages.filter {
                    it.name.contains(targetLanguageSearch, ignoreCase = true) ||
                            it.nativeName.contains(targetLanguageSearch, ignoreCase = true) ||
                            it.code.contains(targetLanguageSearch, ignoreCase = true)
                }
            }
            // Show all, but keep mother tongue distinguishable or filtered
            return list
        }
}

class OnboardingViewModel(
    private val onboardingRepo: OnboardingRepository = LocalOnboardingRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadAvailableLanguages()
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            val languages = onboardingRepo.getAvailableLanguages()
            _uiState.update { state ->
                state.copy(
                    availableLanguages = languages,
                    selectedMotherTongue = languages.find { it.code == "en" } ?: languages.firstOrNull(),
                    selectedTargetLanguage = languages.find { it.code == "es" } ?: languages.getOrNull(1)
                )
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                name = newName,
                nameError = if (newName.isNotBlank() && newName.trim().length < 2) "Name must be at least 2 characters" else null
            )
        }
    }

    fun onMotherTongueSearchChanged(query: String) {
        _uiState.update { it.copy(motherTongueSearch = query) }
    }

    fun onMotherTongueSelected(language: Language) {
        _uiState.update {
            it.copy(
                selectedMotherTongue = language,
                selectedTargetLanguage = if (it.selectedTargetLanguage?.code == language.code) {
                    it.availableLanguages.firstOrNull { l -> l.code != language.code }
                } else {
                    it.selectedTargetLanguage
                }
            )
        }
    }

    fun onTargetLanguageSearchChanged(query: String) {
        _uiState.update { it.copy(targetLanguageSearch = query) }
    }

    fun onTargetLanguageSelected(language: Language) {
        _uiState.update { it.copy(selectedTargetLanguage = language) }
    }

    fun onLevelSelected(level: LanguageLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun onDurationSelected(minutes: Int) {
        if (minutes in LearningPreferences.DURATION_OPTIONS) {
            _uiState.update { it.copy(selectedDurationMinutes = minutes) }
        }
    }

    fun onLearningTimeSelected(time: PreferredLearningTime) {
        _uiState.update { it.copy(selectedLearningTime = time) }
    }

    fun onReminderToggled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onGoalTypeSelected(goalType: LearningGoalType) {
        _uiState.update { it.copy(selectedGoalType = goalType) }
    }

    fun onNextStep() {
        val current = _uiState.value.currentStep
        val next = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.MOTHER_TONGUE
            OnboardingStep.MOTHER_TONGUE -> OnboardingStep.TARGET_LANGUAGE
            OnboardingStep.TARGET_LANGUAGE -> OnboardingStep.LEVEL
            OnboardingStep.LEVEL -> OnboardingStep.DURATION
            OnboardingStep.DURATION -> OnboardingStep.LEARNING_TIME
            OnboardingStep.LEARNING_TIME -> OnboardingStep.GOAL
            OnboardingStep.GOAL -> {
                generatePersonalizedPlan()
                OnboardingStep.CREATE_PLAN
            }
            OnboardingStep.CREATE_PLAN -> OnboardingStep.CREATE_PLAN
        }
        _uiState.update { it.copy(currentStep = next) }
    }

    fun onPreviousStep() {
        val current = _uiState.value.currentStep
        val prev = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.MOTHER_TONGUE -> OnboardingStep.WELCOME
            OnboardingStep.TARGET_LANGUAGE -> OnboardingStep.MOTHER_TONGUE
            OnboardingStep.LEVEL -> OnboardingStep.TARGET_LANGUAGE
            OnboardingStep.DURATION -> OnboardingStep.LEVEL
            OnboardingStep.LEARNING_TIME -> OnboardingStep.DURATION
            OnboardingStep.GOAL -> OnboardingStep.LEARNING_TIME
            OnboardingStep.CREATE_PLAN -> OnboardingStep.GOAL
        }
        _uiState.update { it.copy(currentStep = prev) }
    }

    fun generatePersonalizedPlan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlanGenerating = true,
                    planGenerationProgress = 0.15f,
                    planGenerationMessage = "Analyzing phonetic nuances between native and target languages...",
                    isPlanReady = false
                )
            }
            delay(400)

            _uiState.update {
                it.copy(
                    planGenerationProgress = 0.50f,
                    planGenerationMessage = "Calibrating 30-day curriculum for ${_uiState.value.selectedLevel.title} level..."
                )
            }
            delay(450)

            _uiState.update {
                it.copy(
                    planGenerationProgress = 0.85f,
                    planGenerationMessage = "Configuring AI Friend, Voice Coach & Daily Habit Reminders..."
                )
            }
            delay(400)

            _uiState.update {
                it.copy(
                    planGenerationProgress = 1.0f,
                    planGenerationMessage = "Your Personalized 30-Day Master Plan is Ready! 🎉",
                    isPlanGenerating = false,
                    isPlanReady = true
                )
            }
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        val mother = state.selectedMotherTongue ?: state.availableLanguages.first()
        val target = state.selectedTargetLanguage ?: state.availableLanguages.getOrNull(1) ?: mother

        val userProfile = UserProfile(
            id = "user_${System.currentTimeMillis()}",
            name = state.name.trim().ifBlank { "Alex Vance" },
            motherTongue = mother,
            targetLanguage = target,
            level = state.selectedLevel,
            preferences = LearningPreferences(
                dailyDurationMinutes = state.selectedDurationMinutes,
                preferredLearningTime = state.selectedLearningTime,
                reminderNotificationEnabled = state.reminderEnabled
            ),
            goal = LearningGoal(
                type = state.selectedGoalType,
                title = state.selectedGoalType.title,
                description = state.selectedGoalType.subtitle,
                iconEmoji = state.selectedGoalType.iconEmoji,
                targetDays = 30,
                targetWords = state.selectedGoalType.defaultTargetWords
            ),
            isSetupComplete = true
        )

        viewModelScope.launch {
            onboardingRepo.saveUserProfile(userProfile)
            _uiState.update { it.copy(isCompleted = true) }
            onSuccess()
        }
    }
}
