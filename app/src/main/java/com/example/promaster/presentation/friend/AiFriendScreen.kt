package com.example.promaster.presentation.friend

import androidx.compose.runtime.Composable
import com.example.promaster.presentation.conversation.ChatScreen

/**
 * PROMASTER AI Friend Experience.
 * Connects directly to the unified ChatScreen, ChatViewModel, and Language Coach Engine.
 */
@Composable
fun AiFriendScreen(
    onNavigateBack: () -> Unit = {}
) {
    ChatScreen(onNavigateBack = onNavigateBack)
}
