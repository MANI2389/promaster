package com.example.promaster.presentation.learning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.DailyTask
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.domain.model.TaskCompletionStatus
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterProgressBar
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.*

@Composable
fun TodayTasksScreen(
    viewModel: TodayTasksViewModel = viewModel(),
    onTaskClick: (TaskCategory) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPracticeDialogForTask by remember { mutableStateOf<DailyTask?>(null) }
    var simulatedScore by remember { mutableFloatStateOf(85f) }

    val categories = listOf(
        TaskCategory.VOCABULARY to "📖 Vocabulary",
        TaskCategory.GRAMMAR to "🧠 Grammar",
        TaskCategory.LISTENING to "🎧 Listening",
        TaskCategory.SPEAKING to "🎙️ Speaking",
        TaskCategory.CONVERSATION to "💬 AI Chat",
        TaskCategory.REVIEW to "🔄 Review",
        TaskCategory.QUIZ to "🏆 Quiz"
    )

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Day ${uiState.currentDay} Practice",
                subtitle = "30-Day Engine • Progression Hub",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = uiState.streak
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Offline Mode Banner
            if (uiState.isOffline) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Working in offline mode. Tasks and scores are saved locally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Lock warning alert card
            uiState.lockWarning?.let { warning ->
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exercise Locked",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = warning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                            IconButton(onClick = { viewModel.dismissLockWarning() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Success feedback card
            uiState.feedbackMessage?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PromasterAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PromasterAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = PromasterAccent
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissFeedback() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Daily & Overall Progression Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Day ${uiState.currentDay} Daily Goal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.completedTasks} of ${uiState.totalTasks} exercises passed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = XpPurple.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, XpPurple.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "⚡ +${uiState.totalXp} XP",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = XpPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PromasterProgressBar(
                        progress = uiState.dailyPercentage,
                        label = "Today's Completion (${(uiState.dailyPercentage * 100).toInt()}%)",
                        showPercentage = true,
                        color = PromasterPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PromasterProgressBar(
                        progress = uiState.overallPercentage,
                        label = "Overall 30-Day Plan (${(uiState.overallPercentage * 100).toInt()}%)",
                        showPercentage = true,
                        color = PromasterAccent
                    )
                }
            }

            // Quick Category Exercise Launcher Pills
            item {
                Text(
                    text = "Categories in Syllabus",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { (category, label) ->
                        Surface(
                            modifier = Modifier.clickable { onTaskClick(category) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Task List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sequential Learning Flow",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Step-by-step unlock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive Tasks with Lock System State
            items(uiState.tasks) { task ->
                val isLocked = task.completionStatus == TaskCompletionStatus.LOCKED
                val isCompleted = task.completionStatus == TaskCompletionStatus.COMPLETED
                val isInProgress = task.completionStatus == TaskCompletionStatus.IN_PROGRESS
                val isAvailable = task.completionStatus == TaskCompletionStatus.AVAILABLE

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isLocked) {
                                viewModel.onTaskClicked(task) {}
                            } else {
                                showPracticeDialogForTask = task
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCompleted -> PromasterAccent.copy(alpha = 0.6f)
                            isInProgress -> StreakGold.copy(alpha = 0.7f)
                            isAvailable -> PromasterPrimary.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        }
                    ),
                    tonalElevation = if (isAvailable) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Status Icon Indicator
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = when {
                                        isCompleted -> PromasterAccent.copy(alpha = 0.15f)
                                        isInProgress -> StreakGold.copy(alpha = 0.15f)
                                        isAvailable -> PromasterPrimary.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isCompleted -> Icons.Default.CheckCircle
                                    isInProgress -> Icons.Default.Refresh
                                    isAvailable -> Icons.Default.PlayArrow
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = task.completionStatus.name,
                                tint = when {
                                    isCompleted -> PromasterAccent
                                    isInProgress -> StreakGold
                                    isAvailable -> PromasterPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Task Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⏱️ ${task.estimatedMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (task.category in listOf(TaskCategory.SPEAKING, TaskCategory.CONVERSATION, TaskCategory.QUIZ)) {
                                    Text(
                                        text = "• Pass: ${task.minPassingScore}%+",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCompleted) PromasterAccent else StreakGold
                                    )
                                }

                                if (task.retryCount > 0 && !isCompleted) {
                                    Text(
                                        text = "• Attempts: ${task.retryCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StreakGold
                                    )
                                }

                                if (isAvailable) {
                                    Text(
                                        text = "• Tap to practice",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PromasterPrimary
                                    )
                                }
                            }
                        }

                        // Score or XP badge
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isCompleted -> PromasterAccent.copy(alpha = 0.15f)
                                    isLocked -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> XpPurple.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (isCompleted) "Score ${task.score}%" else "+${task.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCompleted) PromasterAccent else if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else XpPurple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Practice / Score Submission Modal
    showPracticeDialogForTask?.let { activeTask ->
        AlertDialog(
            onDismissRequest = { showPracticeDialogForTask = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = activeTask.category.icon)
                    Text(
                        text = activeTask.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = activeTask.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeTask.category in listOf(TaskCategory.SPEAKING, TaskCategory.CONVERSATION, TaskCategory.QUIZ)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StreakGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StreakGold.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "🔒 Passing Requirement: You must score at least ${activeTask.minPassingScore}% to complete this exercise and unlock subsequent tasks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Simulate Accuracy Score: ${simulatedScore.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Slider(
                        value = simulatedScore,
                        onValueChange = { simulatedScore = it },
                        valueRange = 40f..100f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = if (simulatedScore >= activeTask.minPassingScore) PromasterPrimary else StreakGold,
                            activeTrackColor = if (simulatedScore >= activeTask.minPassingScore) PromasterPrimary else StreakGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitScore(activeTask.taskId, simulatedScore.toInt())
                        showPracticeDialogForTask = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (simulatedScore >= activeTask.minPassingScore) PromasterPrimary else StreakGold
                    )
                ) {
                    Text(if (simulatedScore >= activeTask.minPassingScore) "Submit & Pass ✅" else "Submit (Under 70%) ⚠️")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPracticeDialogForTask = null
                        onTaskClick(activeTask.category)
                    }
                ) {
                    Text("Open Dedicated Screen 🚀")
                }
            }
        )
    }
}
