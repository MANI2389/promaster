package com.example.promaster.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.presentation.components.StatChip
import com.example.promaster.theme.*

@Composable
fun ProfileScreen(
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToProgress: () -> Unit = {},
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var user by remember { mutableStateOf(MockDataProvider.currentUser) }
    var dailyGoal by remember { mutableIntStateOf(user.dailyGoalMinutes) }

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "My Profile 👤",
                subtitle = "Manage goals and preferences",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = user.streakDays
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
            // User Header Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = PromasterPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, PromasterPrimary.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = user.avatarEmoji, fontSize = 32.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = XpPurple.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "LVL ${user.level}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = XpPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Learning ${user.targetLanguage} 🇪🇸",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PromasterPrimary
                            )
                        }
                    }
                }
            }

            // Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        icon = "🔥",
                        label = "Streak",
                        value = "${MockDataProvider.progressState.currentStreak} Days",
                        accentColor = StreakGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = "🏆",
                        label = "Longest",
                        value = "${MockDataProvider.progressState.longestStreak} Days",
                        accentColor = PromasterSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = "⚡",
                        label = "Total XP",
                        value = "${MockDataProvider.progressState.totalXp}",
                        accentColor = XpPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Habit Target Adjuster
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Goal Target",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Recommended: 15-30 minutes for best retention",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (dailyGoal > 5) {
                                        dailyGoal -= 5
                                        user = user.copy(dailyGoalMinutes = dailyGoal)
                                        MockDataProvider.currentUser = user
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$dailyGoal m",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PromasterPrimary
                            )
                            IconButton(
                                onClick = {
                                    if (dailyGoal < 60) {
                                        dailyGoal += 5
                                        user = user.copy(dailyGoalMinutes = dailyGoal)
                                        MockDataProvider.currentUser = user
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            // Navigation Links / Preferences List
            item {
                Text(
                    text = "Preferences & Utilities",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileMenuRow(
                            icon = Icons.Default.Language,
                            title = "Target Language",
                            subtitle = user.targetLanguage,
                            onClick = onNavigateToLanguageSelection
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Default.Security,
                            title = "Safe Automation Assistant",
                            subtitle = "Configure automated learning habits",
                            onClick = onNavigateToAutomation
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Default.DateRange,
                            title = "30-Day Plan Roadmap",
                            subtitle = "View full curriculum progress",
                            onClick = onNavigateToPlan
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Default.Leaderboard,
                            title = "Progress & Achievements",
                            subtitle = "XP milestones, badges, activity charts",
                            onClick = onNavigateToProgress
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Default.Settings,
                            title = "App Settings",
                            subtitle = "Audio, voice persona, reminders",
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }

            // Sign out button
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sign Out", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PromasterPrimary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
