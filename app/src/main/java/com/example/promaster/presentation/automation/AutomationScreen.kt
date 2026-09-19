package com.example.promaster.presentation.automation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.automation.AutomationResult
import com.example.promaster.domain.automation.DefaultAutomationExecutor
import com.example.promaster.domain.automation.DefaultSafeAutomationFramework
import com.example.promaster.domain.automation.NativeAndroidAutomationPlatform
import com.example.promaster.domain.model.AutomationAction
import com.example.promaster.domain.model.CommandResultState
import com.example.promaster.domain.model.SafetyLevel
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.*
import kotlinx.coroutines.launch

@Composable
fun AutomationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var actions by remember { mutableStateOf(MockDataProvider.automationActions) }

    val automationFramework = remember {
        DefaultSafeAutomationFramework(
            executor = DefaultAutomationExecutor(NativeAndroidAutomationPlatform(context))
        )
    }
    var lastResult by remember { mutableStateOf<AutomationResult?>(null) }
    var pendingConfirmationInput by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "Safe Automation 🛡️",
                subtitle = "On-Device Learning Routines",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = 7
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Safety Assurance Banner
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = PromasterAccent.copy(alpha = 0.18f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🛡️", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Safe Android Automation Framework",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Normal Android application boundaries only. Zero Accessibility Service. Explicit confirmation required for sensitive actions like calling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Framework Test Execution Card
            item {
                AutomationFrameworkTestCard(
                    onExecute = { input, isConfirmed ->
                        coroutineScope.launch {
                            val result = automationFramework.process(input, isConfirmed)
                            lastResult = result
                            if (result.state == CommandResultState.CONFIRMATION_REQUIRED) {
                                pendingConfirmationInput = input
                            } else {
                                pendingConfirmationInput = null
                            }
                        }
                    },
                    lastResult = lastResult,
                    pendingInput = pendingConfirmationInput,
                    onConfirm = { input ->
                        coroutineScope.launch {
                            val result = automationFramework.process(input, isConfirmed = true)
                            lastResult = result
                            pendingConfirmationInput = null
                        }
                    },
                    onCancelConfirmation = {
                        pendingConfirmationInput = null
                        lastResult = null
                    }
                )
            }

            // Routine Actions List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Automation Routines",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${actions.count { it.isEnabled }} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = PromasterPrimary
                    )
                }
            }

            // Routine Cards
            items(actions) { action ->
                AutomationActionCard(
                    action = action,
                    onToggle = { isEnabled ->
                        actions = actions.map {
                            if (it.id == action.id) it.copy(isEnabled = isEnabled) else it
                        }
                    },
                    onTestRun = {
                        val message = when (action.safetyLevel) {
                            SafetyLevel.SAFE -> "Simulated execution: '${action.name}' completed safely."
                            SafetyLevel.REQUIRES_CONFIRMATION -> "Simulated prompt: User confirmed '${action.name}'."
                            SafetyLevel.RESTRICTED -> "Simulated block: Action requires elevated system permissions."
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AutomationActionCard(
    action: AutomationAction,
    onToggle: (Boolean) -> Unit,
    onTestRun: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = action.iconEmoji, fontSize = 22.sp)
                    }
                }
                Column {
                    Text(
                        text = action.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = action.triggers,
                        style = MaterialTheme.typography.labelSmall,
                        color = PromasterPrimary
                    )
                }
            }

            Switch(
                checked = action.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PromasterPrimary,
                    checkedTrackColor = PromasterPrimary.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = action.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Safety Level Pill
            val (badgeText, badgeColor) = when (action.safetyLevel) {
                SafetyLevel.SAFE -> "SAFE" to PromasterAccent
                SafetyLevel.REQUIRES_CONFIRMATION -> "CONFIRMATION" to Color(0xFFF59E0B)
                SafetyLevel.RESTRICTED -> "RESTRICTED" to MaterialTheme.colorScheme.error
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "● $badgeText",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            OutlinedButton(
                onClick = onTestRun,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Test Safely", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AutomationFrameworkTestCard(
    onExecute: (String, Boolean) -> Unit,
    lastResult: AutomationResult?,
    pendingInput: String?,
    onConfirm: (String) -> Unit,
    onCancelConfirmation: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = PromasterPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Framework Automation Console",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Test actions supported for normal Android applications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick command chips
            val sampleCommands = listOf(
                "Open YouTube",
                "Open website https://promaster.app",
                "Set an alarm for 7:00 AM",
                "Open wifi settings",
                "Call 9876543210",
                "Start English lesson",
                "Click screen button (accessibility)"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sampleCommands.forEach { cmd ->
                    SuggestionChip(
                        onClick = {
                            inputText = cmd
                            onExecute(cmd, false)
                        },
                        label = { Text(cmd, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Text input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("e.g. Call 9876543210 or Open YouTube", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onExecute(inputText, false)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text("Run")
                }
            }

            // Sensitive Action Confirmation Prompt
            if (pendingInput != null && lastResult?.state == CommandResultState.CONFIRMATION_REQUIRED) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PromasterPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PromasterPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = PromasterPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Confirmation Required Before Sensitive Action",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PromasterPrimary
                            )
                        }
                        Text(
                            text = lastResult.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = onCancelConfirmation) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onConfirm(pendingInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                            ) {
                                Text("Confirm & Execute")
                            }
                        }
                    }
                }
            }

            // Execution Result Display
            if (lastResult != null && lastResult.state != CommandResultState.CONFIRMATION_REQUIRED) {
                val (badgeColor, badgeText) = when (lastResult.state) {
                    CommandResultState.SUCCESS -> PromasterAccent to "SUCCESS ✓"
                    CommandResultState.FAILED -> MaterialTheme.colorScheme.error to "FAILED ✗"
                    CommandResultState.NEEDS_PERMISSION -> Color(0xFFFF9800) to "NEEDS PERMISSION ⚠️"
                    CommandResultState.NOT_SUPPORTED -> Color(0xFF9E9E9E) to "NOT SUPPORTED ℹ️"
                    CommandResultState.CONFIRMATION_REQUIRED -> PromasterPrimary to "CONFIRMATION REQUIRED ?"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = lastResult.message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        lastResult.requiredPermission?.let { perm ->
                            Text(
                                text = "Required Permission: $perm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
