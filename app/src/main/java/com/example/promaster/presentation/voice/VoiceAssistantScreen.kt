package com.example.promaster.presentation.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.AssistantSettings
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.domain.model.WakeWordStatus
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.*

@Composable
fun VoiceAssistantScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: ((String) -> Unit)? = null,
    viewModel: VoiceAssistantViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val wakeWordSettings by viewModel.wakeWordSettings.collectAsState()
    val wakeWordStatus by viewModel.wakeWordStatus.collectAsState()
    val context = LocalContext.current

    // Permission launcher for microphone
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startListening()
        }
    }

    // Orb animation
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = when {
            uiState.isListening -> 1.25f
            uiState.isSpeaking -> 1.15f
            uiState.isProcessing -> 1.05f
            else -> 1.00f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (uiState.isListening) 600 else 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Voice Assistant 🎙️",
                subtitle = "Hands-free AI Language Engine",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Permission Explanation Card (Shown when microphone permission is required)
            if (uiState.requiredPermission != null || !hasMicPermission) {
                MicrophonePermissionCard(
                    onGrantClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onDismiss = {
                        viewModel.dismissPermissionExplanation()
                    }
                )
            }

            // 2. Result State Indicator Tag
            uiState.lastResultState?.let { state ->
                ResultStateBadge(state = state)
            }

            // 3. Central Glowing Assistant Orb
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow halo when active
                if (uiState.isListening || uiState.isSpeaking) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(orbScale)
                            .clip(CircleShape)
                            .background(
                                (if (uiState.isListening) PromasterSecondary else PromasterAccent)
                                    .copy(alpha = 0.25f)
                            )
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(orbScale),
                    shape = CircleShape,
                    color = when {
                        uiState.isListening -> PromasterSecondary
                        uiState.isSpeaking -> PromasterAccent
                        uiState.isProcessing -> PromasterPrimary.copy(alpha = 0.8f)
                        else -> PromasterPrimary
                    },
                    shadowElevation = 12.dp
                ) {
                    IconButton(
                        onClick = {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleListening()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    uiState.isListening -> Icons.Default.Mic
                                    uiState.isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                                    else -> Icons.Default.MicNone
                                },
                                contentDescription = "Toggle voice recognition",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Status label under Orb
            Text(
                text = when {
                    uiState.isListening -> "Listening... Speak your command"
                    uiState.isProcessing -> "Processing voice intent..."
                    uiState.isSpeaking -> "Speaking aloud..."
                    else -> "Tap the orb to speak"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = when {
                        uiState.isListening -> PromasterSecondary
                        uiState.isSpeaking -> PromasterAccent
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )

            // 4. Conversation Bubble Display
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.recognizedText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🗣️", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "You said:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "\"${uiState.recognizedText}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🤖", fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PROMASTER Assistant:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.assistantReply,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Navigation Action Prompt
                    uiState.targetDestination?.let { destination ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onNavigateToRoute?.invoke(destination) ?: onNavigateBack() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Go to ${destination.replace("_", " ").replaceFirstChar { it.uppercase() }}")
                        }
                    }
                }
            }

            // 5. Confirmation Action Dialog Card
            if (uiState.pendingConfirmation != null) {
                ConfirmationCard(
                    message = uiState.assistantReply,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onCancel = { viewModel.cancelPendingAction() }
                )
            }

            // 6. Wake Word ("Hey Bro") Settings Card
            WakeWordCard(
                settings = wakeWordSettings,
                status = wakeWordStatus,
                onToggleWakeWord = { enabled ->
                    if (enabled && !hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleWakeWord(enabled, runInBackground = wakeWordSettings.runInBackground)
                    }
                },
                onToggleBackground = { runInBackground ->
                    viewModel.toggleWakeWord(wakeWordSettings.isWakeWordEnabled, runInBackground = runInBackground)
                }
            )

            // 7. Privacy & On-Device Security Notice
            WakeWordPrivacyCard(
                settings = wakeWordSettings,
                onUpdatePrivacy = { storeAudio, consent ->
                    viewModel.updatePrivacySettings(storeAudio, consent)
                }
            )

            // 8. Quick Command Test Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Supported Commands",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val quickCommands = listOf(
                    "Start my English lesson.",
                    "What is today's task?",
                    "Open YouTube.",
                    "Set an alarm.",
                    "Open settings.",
                    "Start speaking practice."
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickCommands.forEach { command ->
                        SuggestionChip(
                            onClick = { viewModel.executeCommand(command) },
                            label = { Text(command, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStateBadge(state: CommandResultState) {
    val (bgColor, textColor, label) = when (state) {
        CommandResultState.SUCCESS -> Triple(PromasterAccent.copy(alpha = 0.15f), PromasterAccent, "SUCCESS ✓")
        CommandResultState.FAILED -> Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error, "FAILED ✗")
        CommandResultState.NEEDS_PERMISSION -> Triple(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFE65100), "NEEDS PERMISSION ⚠️")
        CommandResultState.NOT_SUPPORTED -> Triple(Color(0xFF9E9E9E).copy(alpha = 0.2f), Color(0xFF616161), "NOT SUPPORTED ℹ️")
        CommandResultState.CONFIRMATION_REQUIRED -> Triple(PromasterPrimary.copy(alpha = 0.15f), PromasterPrimary, "CONFIRMATION REQUIRED ?")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MicrophonePermissionCard(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Microphone Permission Required",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "Microphone access is required so PROMASTER can listen to your voice commands, evaluate pronunciation, and coach speaking fluency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PromasterPrimary.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, PromasterPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = PromasterPrimary)
                Text(
                    text = "Confirmation Required",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PromasterPrimary
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun WakeWordCard(
    settings: AssistantSettings,
    status: WakeWordStatus,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleBackground: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (status == WakeWordStatus.LISTENING) PromasterSecondary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = PromasterPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Wake Word: \"${settings.wakeWord}\"",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Say \"Hey Bro\" for hands-free assistance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status Badge
            WakeWordStatusBadge(status = status)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Wake Word Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Wake Word",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Detects \"Hey Bro\" on-device to begin voice commands immediately",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.isWakeWordEnabled,
                    onCheckedChange = onToggleWakeWord
                )
            }

            // Background Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Run in Background",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.isWakeWordEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Keeps listening while using other apps via a foreground service notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.isWakeWordEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.runInBackground,
                    onCheckedChange = onToggleBackground,
                    enabled = settings.isWakeWordEnabled
                )
            }

            // Zero-fake notice if model missing
            if (status == WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Zero-fake guarantee: No on-device wake model file found. Download or integrate the model package to activate real-time detection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WakeWordStatusBadge(status: WakeWordStatus) {
    val (bgColor, textColor, icon, label) = when (status) {
        WakeWordStatus.LISTENING -> Quadruple(
            PromasterSecondary.copy(alpha = 0.15f),
            PromasterSecondary,
            Icons.Default.Hearing,
            "LISTENING: Say \"Hey Bro\""
        )
        WakeWordStatus.UNAVAILABLE_NO_ON_DEVICE_MODEL -> Quadruple(
            Color(0xFFFF9800).copy(alpha = 0.15f),
            Color(0xFFE65100),
            Icons.Default.Warning,
            "ENGINE UNAVAILABLE: Model Not Loaded"
        )
        WakeWordStatus.UNAVAILABLE_PERMISSION_MISSING -> Quadruple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.error,
            Icons.Default.MicOff,
            "MICROPHONE PERMISSION NEEDED"
        )
        WakeWordStatus.DISABLED -> Quadruple(
            Color.Gray.copy(alpha = 0.15f),
            Color.Gray,
            Icons.Default.PowerSettingsNew,
            "WAKE WORD DISABLED"
        )
        WakeWordStatus.STOPPED -> Quadruple(
            Color.Gray.copy(alpha = 0.15f),
            Color.Gray,
            Icons.Default.Pause,
            "STOPPED"
        )
        WakeWordStatus.AVAILABLE -> Quadruple(
            PromasterPrimary.copy(alpha = 0.15f),
            PromasterPrimary,
            Icons.Default.CheckCircle,
            "READY TO LISTEN"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun WakeWordPrivacyCard(
    settings: AssistantSettings,
    onUpdatePrivacy: (Boolean, Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = PromasterPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Privacy & On-Device Security",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "• 🔒 On-Device Processing: Wake-word recognition runs 100% locally on your device.\n" +
                        "• 🚫 No Continuous Upload: Voice audio is never constantly streamed to the cloud.\n" +
                        "• 🗑️ Zero Raw Storage: Audio buffers are discarded immediately after analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Store Audio Recordings",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Disabled by default. Requires explicit consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.storeAudioRecordings && settings.privacyConsentGiven,
                    onCheckedChange = { optIn ->
                        onUpdatePrivacy(optIn, optIn)
                    }
                )
            }
        }
    }
}
