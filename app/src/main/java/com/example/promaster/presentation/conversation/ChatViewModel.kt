package com.example.promaster.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promaster.data.repository.MockConversationRepositoryImpl
import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.*
import com.example.promaster.domain.repository.ConversationRepository
import com.example.promaster.domain.service.AIService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null,
    val selectedCoachMode: AIIntent = AIIntent.GENERAL_CONVERSATION,
    val learnerMemory: LearnerMemory = LearnerMemory(),
    val quickSuggestions: List<String> = emptyList()
)

class ChatViewModel(
    private val conversationRepository: ConversationRepository = com.example.promaster.data.firebase.repository.FirestoreConversationRepository(),
    private val aiService: AIService = DefaultPromasterAiService(backendClient = com.example.promaster.ai.SecureBackendAiClient()),
    val userId: String = com.example.promaster.data.firebase.FirebaseManager.auth?.currentUser?.uid ?: "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversationAndMemory()
    }

    private fun loadConversationAndMemory() {
        viewModelScope.launch {
            combine(
                conversationRepository.getConversationMessages(userId),
                conversationRepository.getLearnerMemory(userId)
            ) { messages, memory ->
                val suggestions = buildSuggestions(memory, _uiState.value.selectedCoachMode)
                _uiState.value.copy(
                    messages = messages,
                    learnerMemory = memory,
                    quickSuggestions = suggestions
                )
            }.catch { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load chat history") }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectCoachMode(mode: AIIntent) {
        _uiState.update { state ->
            val newSuggestions = buildSuggestions(state.learnerMemory, mode)
            state.copy(selectedCoachMode = mode, quickSuggestions = newSuggestions)
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = AiMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = MessageSender.USER,
            text = trimmed,
            timestamp = "Just now"
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, lastFailedPrompt = null) }
            
            // 1. Save user message to history
            val saveResult = conversationRepository.saveMessage(userId, userMessage)
            if (saveResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not deliver message. Please check connection.",
                        lastFailedPrompt = trimmed
                    )
                }
                return@launch
            }

            // 2. Prepare AI Request with learner memory context
            val currentMemory = _uiState.value.learnerMemory
            val forcedMode = if (_uiState.value.selectedCoachMode != AIIntent.GENERAL_CONVERSATION) {
                _uiState.value.selectedCoachMode
            } else null

            val request = AIRequest(
                prompt = trimmed,
                conversationId = "conv_$userId",
                targetLanguage = currentMemory.targetLanguage,
                motherTongue = currentMemory.motherTongue,
                userLevel = currentMemory.level,
                forcedIntent = forcedMode,
                learnerMemory = currentMemory
            )

            // 3. Process AI request via backend service abstraction
            val aiResult = aiService.processRequest(request)
            aiResult.onSuccess { response ->
                val aiMessage = AiMessage(
                    id = "ai_${System.currentTimeMillis()}",
                    sender = MessageSender.LANGUAGE_COACH,
                    text = response.message,
                    timestamp = "Just now",
                    grammarTip = response.nextAction?.let { "Tip: $it" },
                    translationHint = response.motherTongueExplanation,
                    audioDurationSec = 3
                )
                conversationRepository.saveMessage(userId, aiMessage)

                // Update non-repetitive encouragement style in learner memory
                if (!response.encouragementType.isNullOrBlank()) {
                    val updatedMem = currentMemory.copy(
                        lastEncouragementType = response.encouragementType,
                        updatedAt = System.currentTimeMillis()
                    )
                    conversationRepository.updateLearnerMemory(userId, updatedMem)
                }

                // If grammar correction was triggered and found a mistake, record weak area
                if (response.correction != null && response.correction != trimmed && response.intent == AIIntent.GRAMMAR_CORRECTION) {
                    conversationRepository.recordWeakArea(userId, "Grammar: ${trimmed.take(24)}...")
                }

                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "AI Friend is temporarily unavailable: ${err.message}",
                        lastFailedPrompt = trimmed
                    )
                }
            }
        }
    }

    fun retryLastMessage() {
        val failedPrompt = _uiState.value.lastFailedPrompt ?: return
        clearError()
        sendMessage(failedPrompt)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepository.clearHistory(userId)
        }
    }

    private fun buildSuggestions(memory: LearnerMemory, mode: AIIntent): List<String> {
        return when (mode) {
            AIIntent.GENERAL_CONVERSATION -> {
                val weakAreaHint = memory.weakAreas.firstOrNull()?.let { "Can you help me practice $it?" }
                listOfNotNull(
                    "Hello! How is your day going?",
                    "What should I learn today?",
                    weakAreaHint,
                    "Tell me a fun language fact!"
                ).take(4)
            }
            AIIntent.LANGUAGE_TEACHING -> listOf(
                "Let's practice a restaurant dialogue",
                "Teach me past tense conversation",
                "How do I introduce myself in ${memory.targetLanguage}?",
                "Give me a 3-sentence speaking drill"
            )
            AIIntent.GRAMMAR_CORRECTION -> listOf(
                "Check: 'I am go to college yesterday'",
                "Check: 'She don't like coffee'",
                "Is my sentence order correct?",
                "Explain why this verb conjugated this way"
            )
            AIIntent.VOCABULARY_EXPLANATION -> listOf(
                "Explain the meaning of 'hablar'",
                "What is the word for 'breakfast'?",
                "Give me 3 synonyms for 'happy'",
                "How do I use 'desayuno' in a sentence?"
            )
            AIIntent.TRANSLATION -> listOf(
                "Translate: 'I went to college yesterday'",
                "Translate: 'Where is the nearest train station?'",
                "How do I say 'Nice to meet you' in ${memory.targetLanguage}?",
                "Translate this into Tamil"
            )
            AIIntent.SPEAKING_PRACTICE -> listOf(
                "I want to practice my pronunciation",
                "Give me a phrase to read aloud",
                "How do I sound more natural?",
                "Evaluate my sentence flow"
            )
            AIIntent.LEARNING_PLAN_ASSISTANCE -> listOf(
                "What are my Day ${memory.currentDay} tasks?",
                "How can I improve my ${memory.weakAreas.firstOrNull() ?: "grammar"} today?",
                "Review my 30-day curriculum progress",
                "How do I maintain my ${memory.streakDays}-day streak?"
            )
            else -> listOf(
                "Hello!",
                "Help me practice",
                "Check my sentence"
            )
        }
    }
}
