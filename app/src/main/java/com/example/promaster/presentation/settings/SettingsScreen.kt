package com.example.promaster.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.data.service.AndroidLearningReminderScheduler
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.PromasterAccent
import com.example.promaster.theme.PromasterPrimary
import com.example.promaster.theme.PromasterSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToAutomation: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Connect Android-backed scheduler
    LaunchedEffect(Unit) {
        viewModel.setScheduler(AndroidLearningReminderScheduler(context))
    }

    val hasNotificationPermission = remember(context) {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onNotificationPermissionResult(isGranted)
    }

    // Pre-permission Educational Dialog
    if (uiState.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionRationale() },
            icon = {
                Text(text = "🔔", fontSize = 32.sp)
            },
            title = {
                Text(
                    text = "Stay on Track with Reminders",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To help you build a solid language learning routine and prevent your streak from breaking, PROMASTER sends timely reminders:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Daily Lesson: Prompts at ${uiState.reminderSettings.preferredTime}\n• Missed Tasks: Evening check at 7:00 PM\n• Streak Alert: Urgent heads-up at 10:00 PM if streak is at risk",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "We never send spam or marketing notifications.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PromasterPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onNotificationPermissionResult(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text("Allow Reminders", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionRationale() }) {
                    Text("Not Now")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val personas = listOf("Supportive Buddy", "Strict Professor", "Casual Native")

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Settings ⚙️",
                subtitle = "Preferences, reminders & streak",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = uiState.progress.currentStreak
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
            // Live Streak & Activity Status Card
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔥", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = "Learning Streak & XP",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.isStreakSecuredToday) "Streak secured for today!"
                                        else if (uiState.isStreakAtRisk) "Streak at risk! Practice today to keep it."
                                        else "Start today's lesson to build your streak.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.isStreakSecuredToday) PromasterAccent
                                        else if (uiState.isStreakAtRisk) Color(0xFFFFA726)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.isStreakSecuredToday) PromasterAccent.copy(alpha = 0.15f)
                                else if (uiState.isStreakAtRisk) Color(0xFFFFA726).copy(alpha = 0.15f)
                                else PromasterPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (uiState.isStreakSecuredToday) "Active Today ✅"
                                    else if (uiState.isStreakAtRisk) "At Risk 🔥"
                                    else "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.isStreakSecuredToday) PromasterAccent
                                    else if (uiState.isStreakAtRisk) Color(0xFFFFA726)
                                    else PromasterPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.progress.currentStreak}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = PromasterPrimary
                                )
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.progress.longestStreak}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFFFA726)
                                )
                                Text(
                                    text = "Longest Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.progress.totalXp}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = PromasterAccent
                                )
                                Text(
                                    text = "Total XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Study Reminders Section
            item {
                Text(
                    text = "Learning Reminders ⏰",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Learning Time Selector
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Preferred Study Time",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose when PROMASTER prompts your daily lesson",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = uiState.reminderSettings.preferredTime,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PromasterPrimary
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.availableTimes) { timeStr ->
                                val isSelected = uiState.reminderSettings.preferredTime == timeStr
                                Surface(
                                    modifier = Modifier.clickable {
                                        viewModel.onPreferredTimeSelected(timeStr)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reminder Toggles
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Daily Lesson Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Lesson Reminder",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Triggers daily at ${uiState.reminderSettings.preferredTime} to begin practice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.reminderSettings.dailyLessonReminderEnabled,
                                onCheckedChange = {
                                    viewModel.onDailyLessonReminderToggled(it, hasNotificationPermission.value)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PromasterPrimary,
                                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 2. Missed Task Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Missed Task Reminder",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Evening nudge at 07:00 PM if daily curriculum tasks are incomplete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.reminderSettings.missedTaskReminderEnabled,
                                onCheckedChange = {
                                    viewModel.onMissedTaskReminderToggled(it, hasNotificationPermission.value)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PromasterPrimary,
                                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. Streak Preservation Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Streak Preservation Alert",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "High-priority alert at 10:00 PM if streak is in danger of breaking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.reminderSettings.streakReminderEnabled,
                                onCheckedChange = {
                                    viewModel.onStreakReminderToggled(it, hasNotificationPermission.value)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PromasterPrimary,
                                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            // Audio & Voice Assistant Section
            item {
                Text(
                    text = "Audio & Voice Assistant",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 1. Voice Gender
                        Text(
                            text = "Assistant Voice Gender 🗣️",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("female" to "👩 Female Voice", "male" to "👨 Male Voice").forEach { (genderKey, label) ->
                                val isSelected = uiState.voiceGender.equals(genderKey, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setVoiceGender(genderKey) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 2. Language
                        Text(
                            text = "Voice Assistant Language 🌐",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val supportedLanguages = listOf("English", "Tamil", "Hindi", "Malayalam", "Telugu", "Kannada", "Bengali")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(supportedLanguages) { lang ->
                                val isSelected = uiState.conversationLanguage.equals(lang, ignoreCase = true)
                                Surface(
                                    modifier = Modifier.clickable { viewModel.setConversationLanguage(lang) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = lang,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 3. Speech Rate Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speech Rate (Speed)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%.1fx".format(uiState.voiceSpeed),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PromasterPrimary
                                )
                            }
                            Slider(
                                value = uiState.voiceSpeed,
                                onValueChange = { viewModel.setVoiceSpeed(it) },
                                valueRange = 0.5f..1.5f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = PromasterPrimary,
                                    activeTrackColor = PromasterPrimary
                                )
                            )
                        }

                        // 4. Pitch Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Pitch",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%.1fx".format(uiState.voicePitch),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PromasterPrimary
                                )
                            }
                            Slider(
                                value = uiState.voicePitch,
                                onValueChange = { viewModel.setVoicePitch(it) },
                                valueRange = 0.5f..1.5f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = PromasterPrimary,
                                    activeTrackColor = PromasterPrimary
                                )
                            )
                        }

                        // 5. Test Voice Button
                        Button(
                            onClick = { viewModel.testVoice(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PromasterPrimary.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, PromasterPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSpeakingTest) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PromasterPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Playing sample voice...", color = PromasterPrimary)
                            } else {
                                Text("🔊 Test Voice (${uiState.voiceGender.replaceFirstChar { it.uppercase() }})", color = PromasterPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 6. Wake Word Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Wake Word: 'Hey Bro'",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PromasterSecondary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "On-Device",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = PromasterSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Hands-free voice assistant trigger with privacy-first listening",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.wakeWordEnabled,
                                onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PromasterPrimary,
                                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 7. Auto-Play Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Play Native Audio",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically hear pronunciation on vocabulary cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoPlayAudio,
                                onCheckedChange = { viewModel.setAutoPlayAudio(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PromasterPrimary,
                                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            // AI Persona Section
            item {
                Text(
                    text = "AI Companion Personality",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select your preferred conversation partner demeanor:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            personas.forEach { persona ->
                                val isSelected = uiState.selectedPersona == persona
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setSelectedPersona(persona) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PromasterPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = persona,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) PromasterPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Safe Automation Shortcut
            item {
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAutomation() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "Safe Mobile Automation",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Review security constraints, alarms & routines",
                                    style = MaterialTheme.typography.bodySmall,
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
            }

            // About PROMASTER
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⚡", fontSize = 20.sp)
                            Text(
                                text = "PROMASTER",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v1.0.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "\"Your AI Friend. Your Language Coach. Your Voice Assistant.\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = PromasterPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Built with Native Android, Kotlin, Jetpack Compose, Material 3, Clean Architecture, AlarmManager reminders, and Firebase persistence.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
