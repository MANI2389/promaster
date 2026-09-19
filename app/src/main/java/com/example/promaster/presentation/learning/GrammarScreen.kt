package com.example.promaster.presentation.learning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.GrammarRule
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.presentation.components.QuizComponent
import com.example.promaster.theme.*

@Composable
fun GrammarScreen(
    onNavigateBack: () -> Unit,
    viewModel: GrammarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val rule = uiState.currentRule

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Grammar Coach",
                subtitle = rule?.topic ?: "Grammar Lessons",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = 7
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Topic Horizontal Selector
            if (uiState.rules.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.rules) { index, item ->
                        val isSelected = uiState.selectedRuleIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectRule(index) },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PromasterPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Mode Tabs (Lesson, Exercises, Quiz)
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = PromasterPrimary
            ) {
                GrammarTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Feedback Message Banner
            uiState.feedbackMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PromasterAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PromasterAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearFeedback() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (rule == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Loading grammar content...",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@Scaffold
            }

            // Tab Content
            when (uiState.activeTab) {
                GrammarTab.LESSON -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Mother-Tongue Language Selector
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = PromasterPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Explanation Language",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("English", "Tamil", "Hindi", "Spanish").forEach { lang ->
                                            val isSelected = uiState.selectedMotherTongue.equals(lang, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setMotherTongue(lang) },
                                                label = { Text(lang) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Explanation Card
                        item {
                            GlassCard {
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val motherTongueExp = rule.motherTongueExplanations[uiState.selectedMotherTongue]
                                    ?: rule.motherTongueExplanations["English"]
                                    ?: rule.explanation

                                Text(
                                    text = motherTongueExp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (rule.formula.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PromasterPrimary.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, PromasterPrimary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📐 Rule Formula: ${rule.formula}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PromasterPrimary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Examples
                        item {
                            Text(
                                text = "Examples",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(rule.examples) { ex ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = ex.sentence,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = ex.translation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (ex.highlight.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PromasterAccent.copy(alpha = 0.12f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Focus: ${ex.highlight}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PromasterAccent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pro Tip
                        if (rule.tip.isNotBlank()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PromasterTertiary.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = PromasterTertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "💡 Tip: " + rule.tip,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = PromasterTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GrammarTab.EXERCISES -> {
                    if (rule.exercises.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "No practice exercises available for this lesson.",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(rule.exercises) { ex ->
                                val currentAnswer = uiState.exerciseAnswers[ex.id] ?: ""
                                val isChecked = uiState.exerciseChecked.containsKey(ex.id)
                                val isCorrect = uiState.exerciseChecked[ex.id] == true
                                val feedback = uiState.exerciseFeedback[ex.id]

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChecked) (if (isCorrect) PromasterAccent else PromasterError) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = ex.question,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (ex.options.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ex.options.forEach { opt ->
                                                    FilterChip(
                                                        selected = currentAnswer == opt,
                                                        onClick = { viewModel.onAnswerChanged(ex.id, opt) },
                                                        label = { Text(opt) }
                                                    )
                                                }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = currentAnswer,
                                                onValueChange = { viewModel.onAnswerChanged(ex.id, it) },
                                                placeholder = { Text("Your answer...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )
                                        }

                                        // Feedback display
                                        if (isChecked && feedback != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isCorrect) PromasterAccent.copy(alpha = 0.12f) else PromasterError.copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = feedback,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = if (isCorrect) PromasterAccent else PromasterError,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.checkExercise(ex) },
                                            enabled = currentAnswer.isNotBlank(),
                                            modifier = Modifier.align(Alignment.End),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                                        ) {
                                            Text(if (isChecked) "Re-check" else "Check Answer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GrammarTab.QUIZ -> {
                    QuizComponent(
                        quizId = "grammar_quiz_${rule.id}",
                        title = "GRAMMAR_QUIZ",
                        questions = rule.quizQuestions,
                        onQuizFinished = { result ->
                            viewModel.submitQuiz(result)
                        },
                        activityType = "GRAMMAR_QUIZ"
                    )
                }
            }
        }
    }
}
