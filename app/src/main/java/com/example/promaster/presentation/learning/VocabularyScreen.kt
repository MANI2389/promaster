package com.example.promaster.presentation.learning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.promaster.domain.model.DifficultyLevel
import com.example.promaster.domain.model.ReviewStatus
import com.example.promaster.domain.model.VocabularyItem
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.presentation.components.QuizComponent
import com.example.promaster.theme.*

@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    viewModel: VocabularyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val words = uiState.filteredWords.ifEmpty { uiState.words }
    val activeWord = words.getOrNull(uiState.currentCardIndex) ?: words.firstOrNull()

    var audioPlaying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Vocabulary Hub",
                subtitle = "${uiState.selectedMode.title} Mode • ${words.size} Words",
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Selector Tabs (Learn, Practice, Quiz, Review)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VocabularyMode.values()) { mode ->
                    val isSelected = uiState.selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setMode(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            val icon = when (mode) {
                                VocabularyMode.LEARN -> Icons.Default.School
                                VocabularyMode.PRACTICE -> Icons.Default.FitnessCenter
                                VocabularyMode.QUIZ -> Icons.Default.Quiz
                                VocabularyMode.REVIEW -> Icons.Default.HistoryEdu
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PromasterPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Feedback Banner
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

            // Screen Content by Mode
            when (uiState.selectedMode) {
                VocabularyMode.QUIZ -> {
                    QuizComponent(
                        quizId = "vocab_quiz_session",
                        title = "VOCABULARY_QUIZ",
                        questions = uiState.quizQuestions,
                        onQuizFinished = { result ->
                            viewModel.submitQuiz(result)
                        },
                        activityType = "VOCABULARY_QUIZ"
                    )
                }

                VocabularyMode.LEARN, VocabularyMode.PRACTICE, VocabularyMode.REVIEW -> {
                    if (activeWord == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "No vocabulary items found for this filter.",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // Flashcard Section
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clickable { viewModel.toggleCardFlip() },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(2.dp, PromasterPrimary.copy(alpha = 0.4f)),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Card Top: Tags & Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Part of Speech & Difficulty
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PromasterPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = activeWord.partOfSpeech.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PromasterPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        val diffColor = when (activeWord.difficulty) {
                                            DifficultyLevel.BEGINNER -> PromasterAccent
                                            DifficultyLevel.INTERMEDIATE -> PromasterSecondary
                                            DifficultyLevel.ADVANCED -> PromasterTertiary
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = diffColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = activeWord.difficulty.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = diffColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Review Status Tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (activeWord.reviewStatus) {
                                            ReviewStatus.MASTERED -> PromasterAccent.copy(alpha = 0.15f)
                                            ReviewStatus.NEEDS_REVIEW -> PromasterError.copy(alpha = 0.15f)
                                            ReviewStatus.LEARNING -> PromasterSecondary.copy(alpha = 0.15f)
                                            ReviewStatus.NEW -> MaterialTheme.colorScheme.surface
                                        }
                                    ) {
                                        Text(
                                            text = activeWord.reviewStatus.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (activeWord.reviewStatus) {
                                                ReviewStatus.MASTERED -> PromasterAccent
                                                ReviewStatus.NEEDS_REVIEW -> PromasterError
                                                ReviewStatus.LEARNING -> PromasterSecondary
                                                ReviewStatus.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Card Center: Word or Translation
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (!uiState.isCardFlipped) {
                                        Text(
                                            text = activeWord.word,
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (activeWord.phonetic.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = activeWord.phonetic,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (activeWord.meaning.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Meaning: ${activeWord.meaning}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = activeWord.translation,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PromasterPrimary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "\"${activeWord.exampleSentence}\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (activeWord.exampleTranslation.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = activeWord.exampleTranslation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Card Bottom hint
                                Text(
                                    text = if (!uiState.isCardFlipped) "💡 Tap card to reveal translation" else "💡 Tap to see word",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Practice & Navigation Controls
                        if (uiState.selectedMode == VocabularyMode.PRACTICE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.markNeedsPractice(activeWord.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, PromasterError)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PromasterError, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Need Practice", color = PromasterError)
                                }

                                Button(
                                    onClick = { viewModel.markKnowThis(activeWord.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PromasterAccent)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("I Know This", color = Color.White)
                                }
                            }
                        }

                        // Navigation and Audio bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { audioPlaying = !audioPlaying }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Pronounce",
                                    tint = if (audioPlaying) PromasterAccent else PromasterPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.previousCard() },
                                    enabled = uiState.currentCardIndex > 0,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Previous")
                                }

                                Button(
                                    onClick = { viewModel.nextCard() },
                                    enabled = uiState.currentCardIndex < words.lastIndex,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                                ) {
                                    Text("Next Word")
                                }
                            }
                        }

                        // Words List Header
                        Text(
                            text = "Words (${uiState.currentCardIndex + 1} of ${words.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        // Word List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(words) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.id == activeWord.id) PromasterPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.id == activeWord.id) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.word,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "• ${item.partOfSpeech}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${item.meaning} (${item.translation})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (item.reviewStatus) {
                                                ReviewStatus.MASTERED -> PromasterAccent.copy(alpha = 0.15f)
                                                ReviewStatus.NEEDS_REVIEW -> PromasterError.copy(alpha = 0.15f)
                                                else -> PromasterSecondary.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = "${item.masteryLevel}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = when (item.reviewStatus) {
                                                    ReviewStatus.MASTERED -> PromasterAccent
                                                    ReviewStatus.NEEDS_REVIEW -> PromasterError
                                                    else -> PromasterSecondary
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
