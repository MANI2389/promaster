package com.example.promaster.presentation

import com.example.promaster.data.repository.MockConversationRepositoryImpl
import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.*
import com.example.promaster.domain.service.AIService
import com.example.promaster.presentation.conversation.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var conversationRepo: MockConversationRepositoryImpl
    private lateinit var aiService: AIService
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        conversationRepo = MockConversationRepositoryImpl(
            initialMessages = listOf(
                AiMessage(
                    id = "init_1",
                    sender = MessageSender.LANGUAGE_COACH,
                    text = "Welcome to PROMASTER! Let's practice.",
                    timestamp = "10:00 AM"
                )
            ),
            initialMemory = LearnerMemory(
                userId = "test_user_001",
                targetLanguage = "Spanish",
                motherTongue = "Tamil",
                streakDays = 5,
                currentDay = 3,
                weakAreas = listOf("Subjunctive"),
                completedLessons = listOf("Present Tense")
            )
        )
        aiService = DefaultPromasterAiService()
        viewModel = ChatViewModel(
            conversationRepository = conversationRepo,
            aiService = aiService,
            userId = "test_user_001"
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsMessagesAndLearnerMemory() {
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Welcome to PROMASTER! Let's practice.", state.messages.first().text)
        assertEquals("test_user_001", state.learnerMemory.userId)
        assertEquals(5, state.learnerMemory.streakDays)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.quickSuggestions.isNotEmpty())
    }

    @Test
    fun sendMessage_successFlow_savesUserMessageAndAiReply() {
        viewModel.sendMessage("Hello coach, how are you?")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.messages.size) // initial + user + AI reply

        val userMsg = state.messages[1]
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals("Hello coach, how are you?", userMsg.text)

        val aiReply = state.messages[2]
        assertEquals(MessageSender.LANGUAGE_COACH, aiReply.sender)
        assertTrue(aiReply.text.isNotBlank())
    }

    @Test
    fun sendMessage_failureFlow_setsErrorMessageAndAllowsRetry() {
        val failingAiService = object : AIService {
            override suspend fun processRequest(request: AIRequest): Result<AIResponse> {
                return Result.failure(RuntimeException("Network gateway timeout"))
            }
            override suspend fun detectIntent(input: String): AIIntent = AIIntent.GENERAL_CONVERSATION
            override suspend fun generateConversationReply(conversation: AIConversation, newMessage: String, motherTongue: String, targetLanguage: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun correctGrammar(text: String, targetLanguage: String, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun explainVocabulary(word: String, targetLanguage: String, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun translate(text: String, fromLang: String, toLang: String, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun evaluateSpeaking(spokenText: String, expectedPhrase: String, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun provideMotivation(streakDays: Int, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun assistLearningPlan(currentDay: Int, goal: String, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
            override suspend fun createPersonalizedLesson(topic: String, level: LanguageLevel, motherTongue: String): Result<AIResponse> = Result.failure(RuntimeException())
        }

        val failingViewModel = ChatViewModel(
            conversationRepository = conversationRepo,
            aiService = failingAiService,
            userId = "test_user_001"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        failingViewModel.sendMessage("Failing prompt")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Network gateway timeout"))
        assertEquals("Failing prompt", state.lastFailedPrompt)

        // Clear error test
        failingViewModel.clearError()
        assertNull(failingViewModel.uiState.value.errorMessage)
    }

    @Test
    fun selectCoachMode_updatesActiveIntentAndSuggestions() {
        viewModel.selectCoachMode(AIIntent.GRAMMAR_CORRECTION)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AIIntent.GRAMMAR_CORRECTION, state.selectedCoachMode)
        assertTrue(state.quickSuggestions.any { it.contains("Check:", ignoreCase = true) })

        viewModel.selectCoachMode(AIIntent.VOCABULARY_EXPLANATION)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AIIntent.VOCABULARY_EXPLANATION, viewModel.uiState.value.selectedCoachMode)
        assertTrue(viewModel.uiState.value.quickSuggestions.any { it.contains("meaning", ignoreCase = true) })
    }

    @Test
    fun clearHistory_clearsAllMessages() {
        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
    }
}
