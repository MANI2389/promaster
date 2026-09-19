package com.example.promaster.presentation.conversation

import androidx.compose.runtime.Composable

/**
 * AI Conversation & Language Coach practice screen.
 * Delegates to the unified ChatScreen.
 */
@Composable
fun AiConversationScreen(
    onNavigateBack: () -> Unit
) {
    ChatScreen(onNavigateBack = onNavigateBack)
}
