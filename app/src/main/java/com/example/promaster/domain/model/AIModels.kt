package com.example.promaster.domain.model

enum class AIIntent(val description: String) {
    GENERAL_CONVERSATION("Friendly multi-turn conversation and discussion"),
    AI_FRIEND("Warm, conversational buddy and personal companion"),
    AI_ASSISTANT("Voice and mobile assistant actions and tasks"),
    LANGUAGE_TEACHING("Interactive conceptual language teaching and coaching"),
    GRAMMAR_CORRECTION("Sentence analysis, mistake detection, and grammatical correction"),
    TRANSLATION("High-accuracy bidirectional phrase and text translation"),
    VOCABULARY_EXPLANATION("Word definitions, parts of speech, and usage examples"),
    SPEAKING_PRACTICE("Pronunciation modeling, phonetic tips, and speaking prompts"),
    PERSONALIZED_LESSONS("Adaptive custom lessons tailored to user progress and level"),
    MOTIVATION("Encouragement, streak milestones, and mindset coaching"),
    LEARNING_PLAN_ASSISTANCE("30-day curriculum guidance and schedule adjustments"),
    COMMAND_INTENT_DETECTION("Command and routing intent classification"),
    UNKNOWN("Unclassified general inquiry")
}

data class StructuredAction(
    val intent: String,
    val appName: String? = null,
    val parameters: Map<String, Any?> = emptyMap(),
    val requiresConfirmation: Boolean = false
)

data class AIRequest(
    val prompt: String,
    val conversationId: String = "",
    val contextMessages: List<AiMessage> = emptyList(),
    val targetLanguage: String = "English",
    val motherTongue: String = "Tamil",
    val userLevel: LanguageLevel = LanguageLevel.BEGINNER,
    val forcedIntent: AIIntent? = null,
    val learnerMemory: LearnerMemory? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class AIResponse(
    val message: String,
    val intent: AIIntent,
    val action: StructuredAction? = null,
    val correction: String? = null,
    val explanation: String? = null,
    val nextAction: String? = null,
    val confidence: Float = 1.0f,
    val motherTongueExplanation: String? = null,
    val encouragementType: String? = null
)

data class AIConversation(
    val conversationId: String,
    val userId: String = "",
    val messages: List<AiMessage> = emptyList(),
    val activeIntent: AIIntent = AIIntent.GENERAL_CONVERSATION,
    val updatedAt: Long = System.currentTimeMillis()
)

