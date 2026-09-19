package com.example.promaster.presentation.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.LearningPlan
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.PromasterAccent
import com.example.promaster.theme.PromasterPrimary
import com.example.promaster.theme.StreakGold

@Composable
fun Plan30DayScreen(
    onDayClick: (Int) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val repository = remember { com.example.promaster.data.repository.FirebaseLearningRepositoryImpl() }
    val plan by repository.get30DayPlan().collectAsState(initial = MockDataProvider.thirtyDayPlan)
    var lockedDayNotice by remember { mutableStateOf<String?>(null) }
    val currentDayItem = plan.find { it.isCurrent } ?: plan.firstOrNull()

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "30-Day Fluency Plan",
                subtitle = "Day ${currentDayItem?.day ?: 1} of 30 • Active Roadmap",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = 7
            )
        },
        snackbarHost = {
            lockedDayNotice?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { lockedDayNotice = null }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                com.example.promaster.presentation.components.GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🎯", fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Milestone: Foundations Completed",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Days 1-6 complete! You are now entering conversational grammar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val completedDays = plan.count { it.isCompleted }
                    com.example.promaster.presentation.components.PromasterProgressBar(
                        progress = completedDays.toFloat() / plan.size.toFloat(),
                        label = "Curriculum Roadmap ($completedDays of ${plan.size} Days)",
                        showPercentage = true,
                        color = PromasterPrimary
                    )
                }
            }

            items(plan) { dayItem ->
                DayPlanRow(
                    item = dayItem,
                    onClick = {
                        if (dayItem.isCompleted || dayItem.isCurrent) {
                            onDayClick(dayItem.day)
                        } else {
                            lockedDayNotice = "🔒 Day ${dayItem.day} is locked. Complete Day ${dayItem.day - 1} first to unlock this milestone."
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DayPlanRow(
    item: LearningPlan,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = when {
            item.isCurrent -> PromasterPrimary.copy(alpha = 0.12f)
            item.isCompleted -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        },
        border = BorderStroke(
            1.dp,
            when {
                item.isCurrent -> PromasterPrimary
                item.isCompleted -> PromasterAccent.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day Badge
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = when {
                    item.isCompleted -> PromasterAccent.copy(alpha = 0.15f)
                    item.isCurrent -> PromasterPrimary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = PromasterAccent)
                    } else if (item.isCurrent) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Current", tint = Color.White)
                    } else {
                        Text(
                            text = "${item.day}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Day ${item.day}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (item.isCurrent) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PromasterPrimary
                        ) {
                            Text(
                                text = "ACTIVE TODAY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.estimatedMinutes} mins • ${item.taskCount} tasks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.isCompleted && !item.isCurrent) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
