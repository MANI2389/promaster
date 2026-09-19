package com.example.promaster.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.*
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.LanguageCard
import com.example.promaster.presentation.components.PromasterGradientButton
import com.example.promaster.theme.*

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onSelectLanguage: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val totalSteps = 8
    val currentStepNumber = when (state.currentStep) {
        OnboardingStep.WELCOME -> 1
        OnboardingStep.MOTHER_TONGUE -> 2
        OnboardingStep.TARGET_LANGUAGE -> 3
        OnboardingStep.LEVEL -> 4
        OnboardingStep.DURATION -> 5
        OnboardingStep.LEARNING_TIME -> 6
        OnboardingStep.GOAL -> 7
        OnboardingStep.CREATE_PLAN -> 8
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (state.currentStep != OnboardingStep.WELCOME && state.currentStep != OnboardingStep.CREATE_PLAN) {
                        IconButton(
                            onClick = { viewModel.onPreviousStep() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Text(
                        text = "Step $currentStepNumber of $totalSteps",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.currentStep != OnboardingStep.CREATE_PLAN) {
                        TextButton(
                            onClick = {
                                if (state.name.isBlank()) {
                                    viewModel.onNameChanged("Alex Vance")
                                }
                                viewModel.generatePersonalizedPlan()
                                viewModel.onNextStep()
                            }
                        ) {
                            Text(text = "Quick Setup", color = PromasterPrimary, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { currentStepNumber / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PromasterPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            if (state.currentStep != OnboardingStep.CREATE_PLAN) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.currentStep != OnboardingStep.WELCOME) {
                            OutlinedButton(
                                onClick = { viewModel.onPreviousStep() },
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                        }

                        PromasterGradientButton(
                            text = if (state.currentStep == OnboardingStep.GOAL) "Create My Plan ✨" else "Continue",
                            onClick = { viewModel.onNextStep() },
                            modifier = Modifier
                                .weight(if (state.currentStep == OnboardingStep.WELCOME) 1f else 0.65f),
                            enabled = state.canProceed
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            when (state.currentStep) {
                OnboardingStep.WELCOME -> WelcomeStepContent(state, viewModel)
                OnboardingStep.MOTHER_TONGUE -> MotherTongueStepContent(state, viewModel)
                OnboardingStep.TARGET_LANGUAGE -> TargetLanguageStepContent(state, viewModel)
                OnboardingStep.LEVEL -> LevelStepContent(state, viewModel)
                OnboardingStep.DURATION -> DurationStepContent(state, viewModel)
                OnboardingStep.LEARNING_TIME -> LearningTimeStepContent(state, viewModel)
                OnboardingStep.GOAL -> GoalStepContent(state, viewModel)
                OnboardingStep.CREATE_PLAN -> CreatePlanStepContent(state, viewModel, onFinished)
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale),
                shape = CircleShape,
                color = PromasterPrimary.copy(alpha = 0.15f),
                border = BorderStroke(2.dp, PromasterPrimary.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "⚡", fontSize = 44.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to PROMASTER",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your AI Friend, Language Coach & Voice Assistant in one unified dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text("What should we call you?") },
                placeholder = { Text("Enter your name (e.g., Alex)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PromasterPrimary)
                },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.canProceed) viewModel.onNextStep()
                    }
                )
            )
        }

        // Features list
        GlassCard(
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(emoji = "🎯", title = "30-Day Habit Method", subtitle = "Structured micro-lessons for daily progress")
                FeatureRow(emoji = "🤖", title = "Zero-Judgment AI Friend", subtitle = "Practice casual texting and voice roleplay")
                FeatureRow(emoji = "🎙️", title = "Acoustic Speech Analysis", subtitle = "Phonetic feedback on pitch and pronunciation")
            }
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = PromasterSecondary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, fontSize = 18.sp)
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MotherTongueStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Select Your Native Language",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "PROMASTER uses your mother tongue to deliver intuitive grammar explanations and contextual translations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.motherTongueSearch,
            onValueChange = { viewModel.onMotherTongueSearchChanged(it) },
            placeholder = { Text("Search language (e.g. English, தமிழ், हिन्दी)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = PromasterPrimary)
            },
            trailingIcon = {
                if (state.motherTongueSearch.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onMotherTongueSearchChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.filteredMotherTongues, key = { it.code }) { language ->
                LanguageCard(
                    language = language,
                    isSelected = state.selectedMotherTongue?.code == language.code,
                    onClick = { viewModel.onMotherTongueSelected(language) }
                )
            }
        }
    }
}

@Composable
private fun TargetLanguageStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Which Language to Master?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select your target language. Your 30-day curriculum and AI voice model will be configured for it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.targetLanguageSearch,
            onValueChange = { viewModel.onTargetLanguageSearchChanged(it) },
            placeholder = { Text("Search target language (e.g. Spanish, French, Japanese)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = PromasterPrimary)
            },
            trailingIcon = {
                if (state.targetLanguageSearch.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onTargetLanguageSearchChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.filteredTargetLanguages, key = { it.code }) { language ->
                val isMotherTongue = state.selectedMotherTongue?.code == language.code
                LanguageCard(
                    language = language,
                    isSelected = state.selectedTargetLanguage?.code == language.code,
                    onClick = {
                        if (!isMotherTongue) {
                            viewModel.onTargetLanguageSelected(language)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LevelStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    val targetLangName = state.selectedTargetLanguage?.name ?: "your target language"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Your Current Level in $targetLangName",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "We adapt lesson complexity, audio speed, and grammar depth to match your experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LanguageLevel.values()) { level ->
                val isSelected = state.selectedLevel == level
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onLevelSelected(level) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = if (isSelected) PromasterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = level.iconEmoji, fontSize = 24.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = level.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PromasterSecondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = level.cefrCode,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PromasterSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = level.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.onLevelSelected(level) },
                            colors = RadioButtonDefaults.colors(selectedColor = PromasterPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    val durationData = listOf(
        Triple(10, "Casual Pace ☕", "Light habit for ultra-busy days (~70 XP/day)"),
        Triple(15, "Steady Pace 🎯 (Recommended)", "Optimal retention, high long-term success (~120 XP/day)"),
        Triple(20, "Active Pace ⚡", "Solid daily progress with full dialogues (~160 XP/day)"),
        Triple(30, "Accelerated Pace 🚀", "Fast-track fluency with extra speaking drills (~240 XP/day)"),
        Triple(45, "Intensive Pace 🔥", "Deep immersive practice with AI roleplay (~350 XP/day)"),
        Triple(60, "Fluency Sprint 🏆", "Comprehensive daily mastery & exam readiness (~500 XP/day)")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Daily Learning Commitment",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "How many minutes can you dedicate every day? You can change this anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(durationData) { (minutes, title, description) ->
                val isSelected = state.selectedDurationMinutes == minutes

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onDurationSelected(minutes) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$minutes",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.onDurationSelected(minutes) },
                            colors = RadioButtonDefaults.colors(selectedColor = PromasterPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningTimeStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Preferred Learning Time",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "PROMASTER will synchronize your daily notification to align with your peak focus hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            PreferredLearningTime.values().forEach { timeSlot ->
                val isSelected = state.selectedLearningTime == timeSlot

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onLearningTimeSelected(timeSlot) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = if (isSelected) PromasterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = timeSlot.iconEmoji, fontSize = 22.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = timeSlot.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• ${timeSlot.timeSlot}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = PromasterPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeSlot.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.onLearningTimeSelected(timeSlot) },
                            colors = RadioButtonDefaults.colors(selectedColor = PromasterPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder toggle card
            GlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🔔", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Daily Practice Reminder",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keep streak safe with a smart reminder at ${state.selectedLearningTime.timeSlot}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { viewModel.onReminderToggled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PromasterPrimary)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Your Primary Goal",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "PROMASTER AI aligns daily vocabulary themes and conversation scenarios with this motivation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LearningGoalType.values()) { goalType ->
                val isSelected = state.selectedGoalType == goalType

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onGoalTypeSelected(goalType) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = if (isSelected) PromasterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = goalType.iconEmoji, fontSize = 22.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goalType.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = goalType.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.onGoalTypeSelected(goalType) },
                            colors = RadioButtonDefaults.colors(selectedColor = PromasterPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlanStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    val targetLang = state.selectedTargetLanguage
    val motherLang = state.selectedMotherTongue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = PromasterPrimary.copy(alpha = 0.15f),
                border = BorderStroke(2.dp, PromasterPrimary.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isPlanReady) "🎉" else "🧠",
                        fontSize = 42.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (state.isPlanReady) "Your 30-Day Plan is Ready!" else "Engineering Your Plan...",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.planGenerationMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { state.planGenerationProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PromasterAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Plan Breakdown Card
            GlassCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "📋 Personalized Curriculum Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PromasterPrimary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SummaryItem(
                        icon = targetLang?.flagEmoji ?: "🌍",
                        label = "Target Language",
                        value = "${targetLang?.name ?: "Spanish"} (${state.selectedLevel.title})"
                    )

                    SummaryItem(
                        icon = motherLang?.flagEmoji ?: "🗣️",
                        label = "Native Language",
                        value = motherLang?.name ?: "English"
                    )

                    SummaryItem(
                        icon = "⏱️",
                        label = "Daily Commitment",
                        value = "${state.selectedDurationMinutes} Minutes daily (${state.selectedLearningTime.label} at ${state.selectedLearningTime.timeSlot})"
                    )

                    SummaryItem(
                        icon = state.selectedGoalType.iconEmoji,
                        label = "Primary Goal",
                        value = "${state.selectedGoalType.title} (~${state.selectedGoalType.defaultTargetWords} words)"
                    )

                    SummaryItem(
                        icon = "🤖",
                        label = "AI Modules Ready",
                        value = "Coach, Voice Assistant, AI Friend, Streak Guard"
                    )
                }
            }
        }

        // CTA Launch button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PromasterGradientButton(
                text = "Enter PROMASTER Dashboard 🚀",
                onClick = {
                    viewModel.completeOnboarding(onSuccess = onFinished)
                },
                enabled = state.isPlanReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome ${state.name.ifBlank { "Learner" }}! 30 days to conversational fluency starts today.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryItem(
    icon: String,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = icon, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
