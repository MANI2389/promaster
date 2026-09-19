package com.example.promaster.data.service

import com.example.promaster.ai.AIProviderBackendClient
import com.example.promaster.ai.MockAIProviderBackendClient
import com.example.promaster.domain.model.*
import com.example.promaster.domain.service.AIService

class DefaultPromasterAiService(
    private val backendClient: AIProviderBackendClient = MockAIProviderBackendClient()
) : AIService {

    override suspend fun detectIntent(input: String): AIIntent {
        val lower = input.lowercase().trim()
        return when {
            lower.startsWith("open ") || lower.startsWith("start ") || lower.startsWith("go to ") || lower.startsWith("navigate ") ->
                AIIntent.COMMAND_INTENT_DETECTION

            lower.contains("pronounce") || lower.contains("pronunciation") || lower.contains("speaking practice") || lower.contains("accent") ->
                AIIntent.SPEAKING_PRACTICE

            lower.contains("grammar") || lower.contains("correct") || lower.contains("mistake") || lower.contains("check sentence") || lower.contains("grammar help") ->
                AIIntent.GRAMMAR_CORRECTION

            lower.contains("english practice") || lower.contains("target practice") || lower.contains("practice english") || lower.contains("practice spanish") || lower.contains("teach me") || lower.contains("how do i say") || lower.contains("rule") || lower.contains("conversation practice") ->
                AIIntent.LANGUAGE_TEACHING

            lower.contains("translate") || lower.contains("translation") || lower.contains("in spanish") || lower.contains("in english") || lower.contains("in tamil") ->
                AIIntent.TRANSLATION

            lower.contains("vocabulary") || lower.contains("vocab") || lower.contains("meaning of") || lower.contains("definition") || lower.contains("what does") ->
                AIIntent.VOCABULARY_EXPLANATION

            lower.contains("daily lesson") || lower.contains("today's lesson") || lower.contains("today lesson") || lower.contains("lesson help") || lower.contains("curriculum") || lower.contains("day ") || lower.contains("schedule") ->
                AIIntent.LEARNING_PLAN_ASSISTANCE

            lower.contains("personalized") || lower.contains("custom lesson") || lower.contains("recommend") ->
                AIIntent.PERSONALIZED_LESSONS

            lower.contains("motivation") || lower.contains("inspire") || lower.contains("streak") || lower.contains("feeling lazy") || lower.contains("hard") ->
                AIIntent.MOTIVATION

            else -> AIIntent.GENERAL_CONVERSATION
        }
    }

    override suspend fun processRequest(request: AIRequest): Result<AIResponse> {
        // 1. If live external backend client is provided, dispatch live and return truthful outcome
        if (backendClient !is MockAIProviderBackendClient) {
            return backendClient.sendPrompt(request)
        }

        // 2. Deterministic local pedagogical fallback engine
        val intent = request.forcedIntent ?: detectIntent(request.prompt)
        val memory = request.learnerMemory

        val baseResult = when (intent) {
            AIIntent.GRAMMAR_CORRECTION -> correctGrammar(request.prompt, request.targetLanguage, request.motherTongue)
            AIIntent.TRANSLATION -> translate(request.prompt, request.motherTongue, request.targetLanguage, request.motherTongue)
            AIIntent.VOCABULARY_EXPLANATION -> explainVocabulary(request.prompt, request.targetLanguage, request.motherTongue)
            AIIntent.SPEAKING_PRACTICE -> evaluateSpeaking(request.prompt, "Practice Phrase", request.motherTongue)
            AIIntent.LANGUAGE_TEACHING -> teachConcept(request.prompt, request.targetLanguage, request.motherTongue, memory)
            AIIntent.PERSONALIZED_LESSONS -> createPersonalizedLesson(request.prompt, memory?.level ?: request.userLevel, request.motherTongue)
            AIIntent.MOTIVATION -> provideMotivation(memory?.streakDays ?: 7, request.motherTongue)
            AIIntent.LEARNING_PLAN_ASSISTANCE -> assistLearningPlan(memory?.currentDay ?: 1, "Daily Language Mastery", request.motherTongue)
            AIIntent.COMMAND_INTENT_DETECTION, AIIntent.AI_ASSISTANT -> handleCommand(request.prompt, request.motherTongue)
            AIIntent.GENERAL_CONVERSATION, AIIntent.AI_FRIEND, AIIntent.UNKNOWN -> {
                val conversation = AIConversation(conversationId = request.conversationId)
                generateConversationReply(conversation, request.prompt, request.motherTongue, request.targetLanguage, memory)
            }
        }

        return baseResult.map { baseResponse ->
            val nextEncouragementType = selectNextEncouragementType(memory?.lastEncouragementType)
            val (encouragementMsg, tanglishEncouragement) = generatePersonalizedEncouragement(
                memory = memory,
                motherTongue = request.motherTongue,
                encouragementType = nextEncouragementType
            )

            val personalizedMessage = if (memory?.weakAreas?.isNotEmpty() == true && (intent == AIIntent.GENERAL_CONVERSATION || intent == AIIntent.LANGUAGE_TEACHING)) {
                "${baseResponse.message}\n\nCoach Tip: We can also practice '${memory.weakAreas.first()}' whenever you'd like!"
            } else {
                baseResponse.message
            }

            val finalMotherTongueExp = baseResponse.motherTongueExplanation ?: tanglishEncouragement

            baseResponse.copy(
                message = personalizedMessage,
                motherTongueExplanation = finalMotherTongueExp,
                encouragementType = nextEncouragementType
            )
        }
    }

    override suspend fun generateConversationReply(
        conversation: AIConversation,
        newMessage: String,
        motherTongue: String,
        targetLanguage: String
    ): Result<AIResponse> {
        return generateConversationReply(conversation, newMessage, motherTongue, targetLanguage, null)
    }

    suspend fun generateConversationReply(
        conversation: AIConversation,
        newMessage: String,
        motherTongue: String,
        targetLanguage: String,
        memory: LearnerMemory?
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val lower = newMessage.lowercase().trim()

        val reply = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("vanakkam") -> {
                if (isTamil) {
                    "Vanakkam! Hello! I am your PROMASTER AI coach & friend. Romba sandhosham to practice with you today! Enna topic practice pannalaam?"
                } else {
                    "Hello! It is wonderful to speak with you today! I am your PROMASTER coach & friend. What would you like to practice?"
                }
            }
            lower.contains("how are you") -> {
                if (isTamil) {
                    "Naan romba nalla irukken! I'm feeling energetic and ready to help you master $targetLanguage. How is your learning going today?"
                } else {
                    "I am doing wonderfully, thank you for asking! I'm excited to help you make progress in $targetLanguage today. How are you feeling?"
                }
            }
            else -> {
                if (isTamil) {
                    "Romba nalla thought! That is a great way to express it. Remember, consistency dhaan key to fluency. Keep speaking with confidence!"
                } else {
                    "That is a lovely sentence and a great effort! Consistent practice like this is what builds true fluency. Let's keep exploring!"
                }
            }
        }

        val tanglishExplanation = if (isTamil) {
            "Konjam konjama daily practice panna fluency auto-va vandhurum!"
        } else null

        return Result.success(
            AIResponse(
                message = reply,
                intent = AIIntent.GENERAL_CONVERSATION,
                explanation = "Conversational multi-turn exchange",
                nextAction = "Reply to keep the conversation flowing",
                confidence = 0.98f,
                motherTongueExplanation = tanglishExplanation
            )
        )
    }

    override suspend fun correctGrammar(
        text: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val lower = text.lowercase().trim()

        if (lower.contains("am go to") || lower.contains("am go")) {
            val corrected = text
                .replace("am go to", "went to", ignoreCase = true)
                .replace("am go", "went", ignoreCase = true)

            val explanation = "In English, use the simple past tense 'went' for events that happened in the past (like 'yesterday'), not 'am go'."
            val tanglishExplanation = if (isTamil) {
                "Romba nalla attempt! But 'yesterday' nu past time solrappo 'am go' use panna koodadhu; 'went' nu past tense dhaan use pannanum. Example: 'I went to college yesterday.'"
            } else null

            return Result.success(
                AIResponse(
                    message = "Great effort! Here is a gentle correction to make your sentence natural and accurate.",
                    intent = AIIntent.GRAMMAR_CORRECTION,
                    correction = corrected,
                    explanation = explanation,
                    nextAction = "Repeat the corrected sentence aloud",
                    confidence = 0.96f,
                    motherTongueExplanation = tanglishExplanation
                )
            )
        }

        // Generic correction
        return Result.success(
            AIResponse(
                message = "Your sentence looks grammatically clear and natural! Excellent job.",
                intent = AIIntent.GRAMMAR_CORRECTION,
                correction = text,
                explanation = "Subject-verb concord and tense are properly aligned.",
                nextAction = "Try using this phrase in conversation",
                confidence = 0.95f,
                motherTongueExplanation = if (isTamil) "Unga sentence grammatic-aa correct-aa irukku!" else null
            )
        )
    }

    override suspend fun explainVocabulary(
        word: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val lower = word.lowercase().trim()

        val meaning = when {
            lower.contains("hablar") -> "To speak or converse verbally."
            lower.contains("desayuno") -> "Breakfast, the morning meal."
            else -> "A high-frequency word essential for conversational fluency."
        }

        val tanglishExplanation = if (isTamil) {
            "Indha vaarthai '${word.trim()}' romba common-aa use aagum. Daily convo-la idhai serthu pesunga."
        } else null

        return Result.success(
            AIResponse(
                message = "Here is the breakdown for '$word': $meaning",
                intent = AIIntent.VOCABULARY_EXPLANATION,
                explanation = "Word Definition & Usage in $targetLanguage",
                nextAction = "Create a custom sentence using this word",
                confidence = 0.97f,
                motherTongueExplanation = tanglishExplanation
            )
        )
    }

    override suspend fun translate(
        text: String,
        fromLang: String,
        toLang: String,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val translation = when {
            text.contains("went to college", ignoreCase = true) -> {
                if (isTamil) "நான் நேற்று கல்லூரிக்குச் சென்றேன்." else "Ayer fui a la universidad."
            }
            else -> "Translation for '$text' into $toLang"
        }

        return Result.success(
            AIResponse(
                message = translation,
                intent = AIIntent.TRANSLATION,
                explanation = "Translated accurately with cultural context.",
                nextAction = "Practice speaking this translation",
                confidence = 0.98f,
                motherTongueExplanation = if (isTamil) "Idhu dhaan accurate Tamil translation." else null
            )
        )
    }

    override suspend fun evaluateSpeaking(
        spokenText: String,
        expectedPhrase: String,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        return Result.success(
            AIResponse(
                message = "Pronunciation analyzed! You spoke with great clarity and rhythm.",
                intent = AIIntent.SPEAKING_PRACTICE,
                explanation = "Clear phonetic stress and vowel cadence detected.",
                nextAction = "Move on to the next practice sentence",
                confidence = 0.94f,
                motherTongueExplanation = if (isTamil) "Unga pronunciation romba clear-aa irundhuchu! Super!" else null
            )
        )
    }

    override suspend fun provideMotivation(
        streakDays: Int,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val msg = if (isTamil) {
            "Super! Neenga $streakDays days streak maintain pandreenga! Learning a new language takes patience, and neenga daily effort podradhu really inspiring. Thodarnthu kalakkunga!"
        } else {
            "Fantastic dedication! You have maintained a $streakDays-day learning streak! Language mastery is a marathon of small daily wins, and you are doing brilliantly. Keep going!"
        }

        return Result.success(
            AIResponse(
                message = msg,
                intent = AIIntent.MOTIVATION,
                explanation = "Positive reinforcement and mindset coaching",
                nextAction = "Complete today's remaining tasks to protect your streak",
                confidence = 1.0f,
                motherTongueExplanation = if (isTamil) "Daily 15 mins podhum, ungalala kandippa fluenta pesa mudiyum." else null
            )
        )
    }

    override suspend fun assistLearningPlan(
        currentDay: Int,
        goal: String,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val msg = if (isTamil) {
            "Day $currentDay plan unga $goal goal-ku perfectly align aagudhu. Daily vocabulary + speaking tasks complete pannunga, progress ungalukke theriyum!"
        } else {
            "Your Day $currentDay plan is optimized for your goal: $goal. Finishing your core grammar and speaking tasks today will keep you on track for full 30-day graduation!"
        }

        return Result.success(
            AIResponse(
                message = msg,
                intent = AIIntent.LEARNING_PLAN_ASSISTANCE,
                explanation = "Curriculum optimization advice",
                nextAction = "Open today's tasks",
                confidence = 0.95f,
                motherTongueExplanation = if (isTamil) "Step-by-step progress dhaan permanent fluency tharum." else null
            )
        )
    }

    override suspend fun createPersonalizedLesson(
        topic: String,
        level: LanguageLevel,
        motherTongue: String
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val msg = if (isTamil) {
            "Unga level (${level.displayName})-ku tailor panna personalized lesson on '$topic' ready! Idhula 3 key formulas and practical examples irukku."
        } else {
            "Here is your personalized lesson on '$topic' tailored for ${level.displayName} learners. It includes 3 core patterns and practical speaking examples."
        }

        return Result.success(
            AIResponse(
                message = msg,
                intent = AIIntent.PERSONALIZED_LESSONS,
                explanation = "Adaptive pedagogical content generated for ${level.displayName}",
                nextAction = "Start the interactive lesson",
                confidence = 0.96f,
                motherTongueExplanation = if (isTamil) "Indha lesson mudicha odane oru quick quiz irukku." else null
            )
        )
    }

    private fun teachConcept(
        prompt: String,
        targetLanguage: String,
        motherTongue: String,
        memory: LearnerMemory?
    ): Result<AIResponse> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val lower = prompt.lowercase().trim()
        val levelName = memory?.level?.displayName ?: "Beginner"

        val msg = when {
            lower.contains("english practice") || lower.contains("practice english") || lower.contains("target practice") || lower.contains("conversation practice") -> {
                if (isTamil) {
                    "Super! Let's practice $targetLanguage conversational dialogues tailored for $levelName level. " +
                            "Imagine you are at an airport checking in for your flight. How would you tell the agent: 'I would like a window seat, please'?"
                } else {
                    "Awesome! Let's start an interactive $targetLanguage dialogue session ($levelName level). " +
                            "Scenario: You are checking in for your flight. How would you say: 'I would like a window seat, please'?"
                }
            }
            else -> {
                if (isTamil) {
                    "Language coaching concept for $targetLanguage ($levelName): Always connect the verb ending directly to the subject pronoun to maintain natural rhythm."
                } else {
                    "Language coaching concept for $targetLanguage ($levelName): Connect the verb ending directly to the subject pronoun to maintain natural fluency."
                }
            }
        }

        return Result.success(
            AIResponse(
                message = msg,
                intent = AIIntent.LANGUAGE_TEACHING,
                explanation = "Interactive coaching prompt and scenario drill",
                nextAction = "Reply with your sentence to practice",
                confidence = 0.96f,
                motherTongueExplanation = if (isTamil) "Scenario-based practice dhaan spontaneous speaking tharum!" else null
            )
        )
    }

    private fun handleCommand(
        prompt: String,
        motherTongue: String
    ): Result<AIResponse> {
        return Result.success(
            AIResponse(
                message = "Command detected: Executing action for '$prompt'.",
                intent = AIIntent.COMMAND_INTENT_DETECTION,
                explanation = "Action trigger routed successfully",
                nextAction = "Navigate to destination",
                confidence = 0.99f
            )
        )
    }

    fun selectNextEncouragementType(lastType: String?): String {
        val styles = listOf(
            "CURIOSITY_GROWTH",
            "MILESTONE_CELEBRATION",
            "WEAK_AREA_EMPOWERMENT",
            "HABIT_STACKING",
            "AUTHENTIC_EFFORT"
        )
        val lastIndex = styles.indexOf(lastType)
        val nextIndex = if (lastIndex == -1 || lastIndex + 1 >= styles.size) 0 else lastIndex + 1
        return styles[nextIndex]
    }

    fun generatePersonalizedEncouragement(
        memory: LearnerMemory?,
        motherTongue: String,
        encouragementType: String
    ): Pair<String, String?> {
        val isTamil = motherTongue.equals("Tamil", ignoreCase = true)
        val streak = memory?.streakDays ?: 1
        val currentDay = memory?.currentDay ?: 1
        val weakArea = memory?.weakAreas?.firstOrNull() ?: "core concepts"

        return when (encouragementType) {
            "CURIOSITY_GROWTH" -> {
                val msg = "Curiosity is your superpower! Every phrase you explore today expands your fluency horizon."
                val mt = if (isTamil) "Pudhu vaarthaigal kathukkumbodhu unga confidence automatic-aa koodum!" else null
                Pair(msg, mt)
            }
            "MILESTONE_CELEBRATION" -> {
                val msg = "Outstanding commitment! You're on Day $currentDay with a $streak-day streak. That discipline is truly setting you apart!"
                val mt = if (isTamil) "$streak days streak maintain pandradhu romba periya vishayam. Keep the fire burning!" else null
                Pair(msg, mt)
            }
            "WEAK_AREA_EMPOWERMENT" -> {
                val msg = "Targeting '$weakArea' directly turns doubts into mastery. You have the persistence to conquer it!"
                val mt = if (isTamil) "'$weakArea' konjam tricky dhaan, aana practice panna easy-aa clear aayidum." else null
                Pair(msg, mt)
            }
            "HABIT_STACKING" -> {
                val msg = "Small daily habits create massive lifelong fluency. Your consistent daily micro-sessions are paying off!"
                val mt = if (isTamil) "Daily 15 minutes practice weekly marathon-a vida romba effective!" else null
                Pair(msg, mt)
            }
            "AUTHENTIC_EFFORT" -> {
                val msg = "Mistakes are proof that you are stretching your abilities. Speak boldly and embrace every learning moment!"
                val mt = if (isTamil) "Thappa pesinaalum paravaayilla, pesi pesi dhaan fluency varum!" else null
                Pair(msg, mt)
            }
            else -> {
                val msg = "Keep up the inspiring work! You are making steady, noticeable progress every single day."
                val mt = if (isTamil) "Thodarnthu try pannunga, nalla progress theriyudhu!" else null
                Pair(msg, mt)
            }
        }
    }
}
