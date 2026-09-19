package com.example.promaster.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.promaster.presentation.components.*
import com.example.promaster.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onContinueLearning: () -> Unit,
    onPracticeSpeaking: () -> Unit,
    onTalkToAi: () -> Unit,
    onVoiceAssistant: () -> Unit,
    onLanguageClick: () -> Unit,
    onViewAllTasks: () -> Unit,
    onAutomationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. PROMASTER Branding & Target Language Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = PromasterPrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, PromasterPrimary.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "⚡", fontSize = 20.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "PROMASTER",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI Friend • Language Coach • Voice",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Current Learning Language
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onLanguageClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🇪🇸", fontSize = 16.sp)
                        Text(
                            text = user?.targetLanguage ?: "Spanish",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Language",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. PROMASTER AI Greeting Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    PromasterPrimary.copy(alpha = 0.88f),
                                    PromasterPrimaryVariant.copy(alpha = 0.95f),
                                    PromasterSecondary.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🤖", fontSize = 22.sp)
                            Text(
                                text = "PROMASTER AI COACH",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.aiGreeting,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily Mission: Master regular verbs and café conversation practice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 3. Status Highlights: Day X / 30, XP, Streak
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    icon = "📅",
                    label = "Curriculum",
                    value = "Day ${user?.currentDay ?: 7} / 30",
                    accentColor = PromasterSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    icon = "🔥",
                    label = "Streak",
                    value = "${state.progress?.currentStreak ?: user?.streakDays ?: 1} Days",
                    accentColor = StreakGold,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    icon = "⚡",
                    label = "Total XP",
                    value = "${state.progress?.totalXp ?: user?.totalXp ?: 0}",
                    accentColor = XpPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Daily Completion Percentage Progress Bar
        item {
            GlassCard {
                val progressFraction = if (state.totalTaskCount > 0)
                    state.completedTaskCount.toFloat() / state.totalTaskCount.toFloat() else 0.5f

                PromasterProgressBar(
                    progress = progressFraction,
                    label = "Daily Goal: ${state.completedTaskCount} of ${state.totalTaskCount} Tasks Completed",
                    showPercentage = true,
                    color = PromasterPrimary
                )
            }
        }

        // 5. Action Hub: Continue Learning, Practice Speaking, Talk to PROMASTER, Voice Assistant
        item {
            Text(
                text = "Core Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Continue Learning
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onContinueLearning() },
                    shape = RoundedCornerShape(18.dp),
                    color = PromasterPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PromasterPrimary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "🚀", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continue Learning",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Day ${user?.currentDay ?: 7} Curriculum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Practice Speaking
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPracticeSpeaking() },
                    shape = RoundedCornerShape(18.dp),
                    color = PromasterAccent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PromasterAccent.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "🎙️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Practice Speaking",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Phonetic Pronunciation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Talk to PROMASTER
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTalkToAi() },
                    shape = RoundedCornerShape(18.dp),
                    color = PromasterSecondary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PromasterSecondary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🤖", fontSize = 26.sp)
                        Column {
                            Text(
                                text = "Talk to PROMASTER",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI Coach & Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Voice Assistant
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVoiceAssistant() },
                    shape = RoundedCornerShape(18.dp),
                    color = PromasterTertiary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PromasterTertiary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "⚡", fontSize = 26.sp)
                        Column {
                            Text(
                                text = "Voice Assistant",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hands-free Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 6. Today's Learning Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Learning Cards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onViewAllTasks) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = PromasterPrimary
                    )
                }
            }
        }

        items(state.tasks.take(3)) { task ->
            TaskCardItem(
                task = task,
                onToggle = { taskId -> viewModel.toggleTask(taskId) }
            )
        }
    }
}
