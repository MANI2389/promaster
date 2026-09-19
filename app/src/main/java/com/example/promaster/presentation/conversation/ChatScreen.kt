package com.example.promaster.presentation.conversation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.MessageSender
import com.example.promaster.presentation.components.ChatBubble
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new message or loading state change
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "PROMASTER AI Friend 🤖",
                subtitle = "${uiState.learnerMemory.targetLanguage} Coach • Day ${uiState.learnerMemory.currentDay}",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = uiState.learnerMemory.streakDays
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Learner Memory Telemetry Ribbon
            LearnerMemoryHeader(
                memory = uiState.learnerMemory,
                onWeakAreaClick = { weakArea ->
                    viewModel.selectCoachMode(AIIntent.GRAMMAR_CORRECTION)
                    inputText = "Help me understand and practice $weakArea"
                }
            )

            // 2. Language Coach Mode Selector (7 Modes)
            CoachModeSelector(
                selectedMode = uiState.selectedCoachMode,
                onModeSelected = { mode -> viewModel.selectCoachMode(mode) }
            )

            // 3. Conversation Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                ) {
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyConversationPlaceholder(
                                memory = uiState.learnerMemory,
                                selectedMode = uiState.selectedCoachMode,
                                onPromptSelected = { prompt ->
                                    viewModel.sendMessage(prompt)
                                }
                            )
                        }
                    }

                    items(uiState.messages) { msg ->
                        ChatBubble(
                            message = msg,
                            onHintClick = { hint ->
                                inputText = hint.removePrefix("Tip: ").removePrefix("Hint: '").removeSuffix("'")
                            }
                        )
                    }

                    // Loading State Bubble
                    if (uiState.isLoading) {
                        item {
                            AiTypingIndicator()
                        }
                    }

                    // Error State Card with Retry
                    uiState.errorMessage?.let { errorMsg ->
                        item {
                            ChatErrorCard(
                                errorMessage = errorMsg,
                                onRetry = { viewModel.retryLastMessage() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }
                }
            }

            // 4. Quick Suggestions Strip
            if (uiState.quickSuggestions.isNotEmpty()) {
                QuickSuggestionsStrip(
                    suggestions = uiState.quickSuggestions,
                    onSuggestionClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )
            }

            // 5. Chat Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = getPlaceholderForMode(uiState.selectedCoachMode, uiState.learnerMemory.targetLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PromasterPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && !uiState.isLoading) {
                                val textToSend = inputText
                                inputText = ""
                                viewModel.sendMessage(textToSend)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = if (inputText.isNotBlank() && !uiState.isLoading) PromasterPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (inputText.isNotBlank() && !uiState.isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnerMemoryHeader(
    memory: com.example.promaster.domain.model.LearnerMemory,
    onWeakAreaClick: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PromasterPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = memory.level.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PromasterPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${memory.progressPercentage}% 30-Day Mastery",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            memory.weakAreas.firstOrNull()?.let { weakArea ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PromasterAccent.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onWeakAreaClick(weakArea) }
                ) {
                    Text(
                        text = "Focus: $weakArea",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PromasterAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachModeSelector(
    selectedMode: AIIntent,
    onModeSelected: (AIIntent) -> Unit
) {
    val modes = listOf(
        Pair(AIIntent.GENERAL_CONVERSATION, "💬 Normal"),
        Pair(AIIntent.LANGUAGE_TEACHING, "🎯 Practice"),
        Pair(AIIntent.GRAMMAR_CORRECTION, "✍️ Grammar"),
        Pair(AIIntent.VOCABULARY_EXPLANATION, "📖 Vocab"),
        Pair(AIIntent.TRANSLATION, "🌐 Translate"),
        Pair(AIIntent.SPEAKING_PRACTICE, "🎙️ Speaking"),
        Pair(AIIntent.LEARNING_PLAN_ASSISTANCE, "📅 Daily Plan")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = selectedMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PromasterPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun QuickSuggestionsStrip(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.clickable { onSuggestionClick(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AiTypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = PromasterSecondary.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🤖", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = PromasterPrimary
                )
                Text(
                    text = "PROMASTER is thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Retry", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationPlaceholder(
    memory: com.example.promaster.domain.model.LearnerMemory,
    selectedMode: AIIntent,
    onPromptSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "👋", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Welcome to your AI Friend & Coach!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "I'm connected to your 30-day ${memory.targetLanguage} roadmap. Ask anything or pick a quick starter below:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val starters = listOf(
                    "Hello! Let's practice ${memory.targetLanguage}",
                    "Check my grammar in a sentence",
                    "How does Day ${memory.currentDay} help my learning goal?"
                )
                starters.forEach { starter ->
                    OutlinedButton(
                        onClick = { onPromptSelected(starter) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = starter, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun getPlaceholderForMode(mode: AIIntent, targetLanguage: String): String {
    return when (mode) {
        AIIntent.GENERAL_CONVERSATION -> "Talk about anything or share your thoughts..."
        AIIntent.LANGUAGE_TEACHING -> "Ask how to say something in $targetLanguage..."
        AIIntent.GRAMMAR_CORRECTION -> "Type a sentence to check grammar..."
        AIIntent.VOCABULARY_EXPLANATION -> "Type a word to learn meaning and usage..."
        AIIntent.TRANSLATION -> "Enter a phrase to translate..."
        AIIntent.SPEAKING_PRACTICE -> "Enter text for pronunciation coaching..."
        AIIntent.LEARNING_PLAN_ASSISTANCE -> "Ask about today's tasks or roadmap..."
        else -> "Type a message..."
    }
}
