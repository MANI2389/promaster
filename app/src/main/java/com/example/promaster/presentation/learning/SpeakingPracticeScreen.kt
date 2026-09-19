package com.example.promaster.presentation.learning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.SpeakingSessionState
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterGradientButton
import com.example.promaster.presentation.components.PromasterProgressBar
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.presentation.components.VoiceControlOrb
import com.example.promaster.theme.*

@Composable
fun SpeakingPracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpeakingPracticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var manualTextInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    val isListening = uiState.sessionState == SpeakingSessionState.LISTENING_INITIAL ||
            uiState.sessionState == SpeakingSessionState.LISTENING_REPEAT

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Speaking Practice 🎙️",
                subtitle = "Target: ${uiState.targetLanguage} • Native: ${uiState.motherTongue}",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Unavailable or Error Banner
            if (uiState.sessionState == SpeakingSessionState.SERVICE_UNAVAILABLE || uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PromasterError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PromasterError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = PromasterError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage ?: "Speaking analysis is currently unavailable.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Prompt Card
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PromasterPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "SPEAKING PROMPT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PromasterPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PromasterSecondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Mother Tongue: ${uiState.motherTongue}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PromasterSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = uiState.currentPrompt.promptContext,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Native Meaning: ${uiState.currentPrompt.translation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.currentPrompt.motherTongueHint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 ${uiState.currentPrompt.motherTongueHint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PromasterTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Voice Control Orb & Status Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VoiceControlOrb(
                    isListening = isListening,
                    onClick = {
                        if (isListening) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    }
                )

                val statusText = when (uiState.sessionState) {
                    SpeakingSessionState.IDLE -> "Tap mic and speak your sentence"
                    SpeakingSessionState.LISTENING_INITIAL -> "Listening to your sentence..."
                    SpeakingSessionState.ANALYZING -> "Analyzing grammar & sentence structure..."
                    SpeakingSessionState.CORRECTION_REQUIRED -> "Tap mic to repeat the corrected sentence"
                    SpeakingSessionState.LISTENING_REPEAT -> "Listening to your repetition..."
                    SpeakingSessionState.VERIFYING -> "Verifying pronunciation and accuracy..."
                    SpeakingSessionState.VERIFIED_SUCCESS -> "Verification Successful! 🎉"
                    SpeakingSessionState.SERVICE_UNAVAILABLE -> "Service Offline"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isListening) PromasterAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Quick Text Input Toggle for Testing / Audio fallback
                TextButton(onClick = { showManualInput = !showManualInput }) {
                    Icon(
                        if (showManualInput) Icons.Default.Mic else Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showManualInput) "Hide text input" else "Type or paste speech directly")
                }

                if (showManualInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualTextInput,
                            onValueChange = { manualTextInput = it },
                            placeholder = {
                                Text(
                                    if (uiState.sessionState == SpeakingSessionState.CORRECTION_REQUIRED)
                                        "Type repetition..." else "Type spoken sentence..."
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (manualTextInput.isNotBlank()) {
                                    viewModel.processSpokenText(manualTextInput)
                                    manualTextInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }

            // Grammar Analysis & Correction Card
            uiState.grammarAnalysis?.let { analysis ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.5.dp,
                        if (analysis.isCorrect) PromasterAccent else PromasterPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Original Sentence
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ORIGINAL SPOKEN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${analysis.originalSentence}\"",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (!analysis.isCorrect) PromasterError else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Corrected Sentence
                        if (!analysis.isCorrect) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "CORRECTED SENTENCE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PromasterAccent
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PromasterAccent.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, PromasterAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "\"${analysis.correctedSentence}\"",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = PromasterAccent,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        // Linguistic Explanation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "💡 Explanation",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = analysis.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Mother-Tongue Explanation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PromasterSecondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PromasterSecondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = PromasterSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.motherTongue} Explanation",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PromasterSecondary
                                    )
                                }
                                Text(
                                    text = analysis.motherTongueExplanation,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Action / Retry Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.retry() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }

                            if (!uiState.isVerified) {
                                Button(
                                    onClick = {
                                        viewModel.startListening()
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Repeat Now")
                                }
                            }
                        }
                    }
                }
            }

            // Verification Result Card
            if (uiState.isVerified && uiState.pronunciationResult != null) {
                val pron = uiState.pronunciationResult!!
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, PromasterAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PromasterAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PromasterAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Task Verified & Completed!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Pronunciation Match: ${pron.accuracyScore}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PromasterAccent
                        )

                        PromasterProgressBar(
                            progress = (pron.accuracyScore / 100f).coerceIn(0f, 1f),
                            color = PromasterAccent,
                            showPercentage = false
                        )

                        Text(
                            text = pron.feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PromasterAccent.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("XP Earned:", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "+${uiState.earnedXp} XP",
                                    fontWeight = FontWeight.Black,
                                    color = PromasterAccent
                                )
                            }
                        }

                        PromasterGradientButton(
                            text = "Done / Return to Tasks",
                            onClick = { onNavigateBack() }
                        )
                    }
                }
            }
        }
    }
}
