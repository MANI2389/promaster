package com.example.promaster.presentation.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.presentation.components.StatChip
import com.example.promaster.theme.*

@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit = {}
) {
    val progress = MockDataProvider.progressState
    val user = MockDataProvider.currentUser
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxXp = progress.weeklyXp.maxOrNull() ?: 100

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Progress & Stats 📊",
                subtitle = "Level ${progress.level} • ${progress.totalXp} XP",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = progress.currentStreak
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Level & XP Progress Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = XpPurple.copy(alpha = 0.18f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "⚡", fontSize = 26.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "Level ${progress.level} Learner",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${progress.totalXp} / 1000 XP to Level 5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${((progress.totalXp % 1000) / 10)}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = XpPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (progress.totalXp % 1000) / 1000f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = XpPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Streak & Milestone Highlights
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(
                        icon = "🔥",
                        label = "Active Streak",
                        value = "${progress.currentStreak} Days",
                        accentColor = StreakGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = "🏆",
                        label = "Best Record",
                        value = "${progress.bestStreak} Days",
                        accentColor = PromasterSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Activity Chart
            item {
                GlassCard {
                    Text(
                        text = "Weekly Activity (XP Earned)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        progress.weeklyXp.forEachIndexed { index, xp ->
                            val heightFraction = if (maxXp > 0) xp.toFloat() / maxXp.toFloat() else 0.5f
                            val isToday = index == 6 // Sunday / Current

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = xp.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isToday) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(fraction = heightFraction.coerceIn(0.12f, 1f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isToday) PromasterPrimary
                                            else PromasterPrimary.copy(alpha = 0.35f)
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = daysOfWeek.getOrElse(index) { "" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isToday) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Cumulative Learning Stats Grid
            item {
                Text(
                    text = "Cumulative Skills Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        icon = "📖",
                        title = "Words Learned",
                        value = "${progress.wordsLearned}",
                        subtitle = "+12 this week",
                        accentColor = PromasterPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        icon = "🧠",
                        title = "Grammar Rules",
                        value = "${progress.rulesMastered}",
                        subtitle = "Mastered",
                        accentColor = PromasterSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        icon = "🎙️",
                        title = "Speaking Time",
                        value = "${progress.speakingMinutes} min",
                        subtitle = "Aloud practice",
                        accentColor = PromasterAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        icon = "🎧",
                        title = "Listening Time",
                        value = "${progress.listeningMinutes} min",
                        subtitle = "Audio dialogues",
                        accentColor = PromasterTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Achievement Badges
            item {
                Text(
                    text = "Unlocked Badges",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        progress.badges.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun StatMetricCard(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
