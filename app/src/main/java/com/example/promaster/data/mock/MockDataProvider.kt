package com.example.promaster.data.mock

import com.example.promaster.domain.model.*

object MockDataProvider {

    var currentUser = User(
        id = "user_001",
        name = "Alex Vance",
        email = "alex@promaster.ai",
        targetLanguage = "Spanish",
        nativeLanguage = "English",
        streakDays = 7,
        totalXp = 850,
        level = 4,
        currentDay = 7,
        dailyGoalMinutes = 20,
        avatarEmoji = "⚡"
    )

    val languages = listOf(
        Language("en", "English", "English", "🇬🇧", "Global Standard", "15.0M learners"),
        Language("ta", "Tamil", "தமிழ்", "🇮🇳", "Classical & Rich", "1.4M learners"),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳", "Beginner Friendly", "4.2M learners"),
        Language("es", "Spanish", "Español", "🇪🇸", "Beginner Friendly", "2.8M learners"),
        Language("fr", "French", "Français", "🇫🇷", "Intermediate", "2.1M learners"),
        Language("de", "German", "Deutsch", "🇩🇪", "Challenging", "1.1M learners"),
        Language("ja", "Japanese", "日本語", "🇯🇵", "Advanced", "1.7M learners"),
        Language("zh", "Mandarin", "中文", "🇨🇳", "Advanced", "1.3M learners"),
        Language("ar", "Arabic", "العربية", "🇸🇦", "Challenging", "950K learners"),
        Language("pt", "Portuguese", "Português", "🇧🇷", "Beginner Friendly", "780K learners"),
        Language("it", "Italian", "Italiano", "🇮🇹", "Beginner Friendly", "850K learners"),
        Language("ko", "Korean", "한국어", "🇰🇷", "Intermediate", "1.4M learners"),
        Language("ru", "Russian", "Русский", "🇷🇺", "Challenging", "890K learners"),
        Language("te", "Telugu", "తెలుగు", "🇮🇳", "Rich Heritage", "620K learners"),
        Language("ml", "Malayalam", "മലയാളം", "🇮🇳", "Rich Heritage", "480K learners")
    )

    var thirtyDayPlan: List<LearningPlan> = (1..30).map { day ->
        val title = when (day) {
            1 -> "Foundations & Essential Greetings"
            2 -> "Introducing Yourself & Origin"
            3 -> "Numbers 1-100 & Everyday Objects"
            4 -> "Colors, Shapes & Descriptions"
            5 -> "Family Members & Relationships"
            6 -> "Food, Dining & Ordering in a Café"
            7 -> "Present Tense Regular Verbs (-ar)"
            8 -> "Present Tense Regular Verbs (-er, -ir)"
            9 -> "Days of Week, Months & Telling Time"
            10 -> "Directions & City Navigation"
            11 -> "Shopping & Inquiring About Prices"
            12 -> "Weather & Seasons Expressions"
            13 -> "Common Irregular Verbs: Ser vs Estar"
            14 -> "Common Irregular Verbs: Tener & Ir"
            15 -> "Midpoint Fluency Review & Milestone Quiz"
            16 -> "Expressing Likes & Dislikes (Gustar)"
            17 -> "Daily Routines & Reflexive Verbs"
            18 -> "Travel, Airport & Hotel Check-in"
            19 -> "Health, Symptoms & Doctor Visits"
            20 -> "Making Plans & Future Projections"
            21 -> "Past Tense: Pretérito Indefinido Intro"
            22 -> "Past Tense: Regular Past Narratives"
            23 -> "Past Tense: Common Irregular Past"
            24 -> "Imperfect Tense & Childhood Memories"
            25 -> "Connecting Sentences: Conjunctions"
            26 -> "Telephone & Digital Messaging Étiquette"
            27 -> "Expressing Opinions & Debating"
            28 -> "Idiomatic Expressions & Colloquialisms"
            29 -> "Simulated AI Friend Video/Voice Exam"
            else -> "30-Day Graduation & Fluency Certification"
        }
        LearningPlan(
            day = day,
            title = title,
            description = "Master key conversational skills and grammatical structures for Day $day.",
            estimatedMinutes = 15 + (day % 3) * 5,
            isCompleted = day < 7,
            isCurrent = day == 7,
            taskCount = 4
        )
    }

    fun generateCustomThirtyDayPlan(
        targetLanguage: String,
        level: LanguageLevel,
        dailyMinutes: Int
    ): List<LearningPlan> {
        val customPlan = (1..30).map { day ->
            val title = when (day) {
                1 -> "Foundations & Essential Greetings in $targetLanguage"
                2 -> "Self-Introduction, Name & Identity"
                3 -> "Numbers 1-100 & Everyday Essentials"
                4 -> "Descriptions, Colors & Basic Adjectives"
                5 -> "Family, Friends & Social Bonds"
                6 -> "Food, Dining & Ordering at Restaurants"
                7 -> "Essential Action Verbs & Present Tense"
                8 -> "High-frequency Questions & Responses"
                9 -> "Days, Months, Calendar & Scheduling"
                10 -> "Directions, Map Reading & Urban Travel"
                11 -> "Shopping, Prices & Market Etiquette"
                12 -> "Weather, Seasons & Small Talk"
                13 -> "Auxiliary & Irregular Verbs Mastery"
                14 -> "Expressing Wants, Needs & Preferences"
                15 -> "Midway Fluency Assessment & Milestone Review"
                16 -> "Expressing Emotions, Likes & Dislikes"
                17 -> "Daily Routines & Time Management"
                18 -> "Airports, Hotels & Global Commuting"
                19 -> "Health, Emergencies & Doctor Visits"
                20 -> "Future Plans, Aspirations & Wishes"
                21 -> "Past Tense: Narrating Yesterday's Events"
                22 -> "Past Tense: Storytelling & Key Irregulars"
                23 -> "Intermediate Conversation Connectors"
                24 -> "Memories, Past Habits & Descriptions"
                25 -> "Complex Sentence Formation & Conjunctions"
                26 -> "Phone Calls, Text Messaging & Slang"
                27 -> "Expressing Opinions, Nuance & Polite Debate"
                28 -> "Idioms, Cultural Metaphors & Nuance"
                29 -> "Full AI Voice & Video Conversation Simulation"
                else -> "30-Day Graduation & Official $targetLanguage Fluency Certificate"
            }
            LearningPlan(
                day = day,
                title = title,
                description = "Master ${level.title} level skills in $targetLanguage tailored for your $dailyMinutes-min daily schedule.",
                estimatedMinutes = dailyMinutes,
                isCompleted = day < 2,
                isCurrent = day == 2,
                taskCount = if (dailyMinutes >= 30) 5 else 4
            )
        }
        thirtyDayPlan = customPlan
        return customPlan
    }

    val dailyTasks = listOf(
        DailyTask(
            id = "task_01",
            title = "Morning Vocabulary Workout",
            description = "Memorize 8 high-frequency action verbs with audio playback",
            category = TaskCategory.VOCABULARY,
            xpReward = 30,
            isCompleted = true,
            durationMinutes = 5
        ),
        DailyTask(
            id = "task_02",
            title = "Grammar Mastery: -ar Verbs",
            description = "Learn the 6 conjugation endings for present tense regular verbs",
            category = TaskCategory.GRAMMAR,
            xpReward = 40,
            isCompleted = false,
            durationMinutes = 8
        ),
        DailyTask(
            id = "task_03",
            title = "Audio Comprehension: Ordering Tapas",
            description = "Listen to a Madrid café dialogue and answer 3 questions",
            category = TaskCategory.LISTENING,
            xpReward = 35,
            isCompleted = false,
            durationMinutes = 6
        ),
        DailyTask(
            id = "task_04",
            title = "Pronunciation Coach: Speak 5 Sentences",
            description = "Record your voice and receive AI phonetic feedback",
            category = TaskCategory.SPEAKING,
            xpReward = 50,
            isCompleted = false,
            durationMinutes = 7
        ),
        DailyTask(
            id = "task_05",
            title = "Chat with AI Friend: Today's Routine",
            description = "Exchange at least 3 messages in Spanish with your AI companion",
            category = TaskCategory.AI_PRACTICE,
            xpReward = 45,
            isCompleted = false,
            durationMinutes = 5
        )
    )

    val vocabularyList = listOf(
        VocabularyItem(
            id = "voc_01",
            word = "Hablar",
            meaning = "To speak / talk with someone verbally",
            phonetic = "[a-ˈβlaɾ]",
            translation = "To speak / talk",
            partOfSpeech = "Verb",
            exampleSentence = "Yo hablo español todos los días con mi amigo.",
            exampleTranslation = "I speak Spanish every day with my friend.",
            difficulty = DifficultyLevel.BEGINNER,
            reviewStatus = ReviewStatus.MASTERED,
            masteryLevel = 90
        ),
        VocabularyItem(
            id = "voc_02",
            word = "Aprender",
            meaning = "To gain knowledge or skill by study or instruction",
            phonetic = "[a-pɾen-ˈdeɾ]",
            translation = "To learn",
            partOfSpeech = "Verb",
            exampleSentence = "Queremos aprender nuevas palabras hoy.",
            exampleTranslation = "We want to learn new words today.",
            difficulty = DifficultyLevel.BEGINNER,
            reviewStatus = ReviewStatus.LEARNING,
            masteryLevel = 75
        ),
        VocabularyItem(
            id = "voc_03",
            word = "El Desayuno",
            meaning = "The first meal of the day eaten in the morning",
            phonetic = "[el de-sa-ˈʝu-no]",
            translation = "Breakfast",
            partOfSpeech = "Noun",
            exampleSentence = "El desayuno es la comida más importante.",
            exampleTranslation = "Breakfast is the most important meal.",
            difficulty = DifficultyLevel.BEGINNER,
            reviewStatus = ReviewStatus.NEEDS_REVIEW,
            masteryLevel = 50
        ),
        VocabularyItem(
            id = "voc_04",
            word = "Hermoso / Hermosa",
            meaning = "Pleasing the senses or mind aesthetically",
            phonetic = "[eɾ-ˈmo-so]",
            translation = "Beautiful / lovely",
            partOfSpeech = "Adjective",
            exampleSentence = "Es una mañana hermosa para caminar.",
            exampleTranslation = "It is a beautiful morning for walking.",
            difficulty = DifficultyLevel.INTERMEDIATE,
            reviewStatus = ReviewStatus.LEARNING,
            masteryLevel = 80
        ),
        VocabularyItem(
            id = "voc_05",
            word = "Siempre",
            meaning = "At all times; on all occasions",
            phonetic = "[ˈsjem-pɾe]",
            translation = "Always",
            partOfSpeech = "Adverb",
            exampleSentence = "Siempre practico la pronunciación con PROMASTER.",
            exampleTranslation = "I always practice pronunciation with PROMASTER.",
            difficulty = DifficultyLevel.BEGINNER,
            reviewStatus = ReviewStatus.MASTERED,
            masteryLevel = 95
        ),
        VocabularyItem(
            id = "voc_06",
            word = "La Biblioteca",
            meaning = "A building or room containing collections of books",
            phonetic = "[la βi-βljo-ˈte-ka]",
            translation = "Library",
            partOfSpeech = "Noun",
            exampleSentence = "Estudio en la biblioteca por la tarde.",
            exampleTranslation = "I study at the library in the afternoon.",
            difficulty = DifficultyLevel.INTERMEDIATE,
            reviewStatus = ReviewStatus.NEEDS_REVIEW,
            masteryLevel = 45
        ),
        VocabularyItem(
            id = "voc_07",
            word = "Imprescindible",
            meaning = "Absolutely necessary, essential, indispensable",
            phonetic = "[im-pɾe-sin-ˈdi-βle]",
            translation = "Indispensable / Essential",
            partOfSpeech = "Adjective",
            exampleSentence = "La práctica constante es imprescindible para la fluidez.",
            exampleTranslation = "Consistent practice is essential for fluency.",
            difficulty = DifficultyLevel.ADVANCED,
            reviewStatus = ReviewStatus.NEW,
            masteryLevel = 10
        ),
        VocabularyItem(
            id = "voc_08",
            word = "Comprender",
            meaning = "To perceive the intended meaning of words or ideas",
            phonetic = "[kom-pɾen-ˈdeɾ]",
            translation = "To comprehend / understand",
            partOfSpeech = "Verb",
            exampleSentence = "Puedo comprender las lecciones de gramática fácilmente.",
            exampleTranslation = "I can understand the grammar lessons easily.",
            difficulty = DifficultyLevel.INTERMEDIATE,
            reviewStatus = ReviewStatus.LEARNING,
            masteryLevel = 70
        ),
        VocabularyItem(
            id = "voc_09",
            word = "El Restaurante",
            meaning = "A place where people pay to sit and eat meals cooked on premises",
            phonetic = "[el res-tow-ˈɾan-te]",
            translation = "The Restaurant",
            partOfSpeech = "Noun",
            exampleSentence = "Cenamos en un restaurante tradicional anoche.",
            exampleTranslation = "We had dinner at a traditional restaurant last night.",
            difficulty = DifficultyLevel.BEGINNER,
            reviewStatus = ReviewStatus.MASTERED,
            masteryLevel = 90
        ),
        VocabularyItem(
            id = "voc_10",
            word = "Extraordinario",
            meaning = "Very unusual or remarkable; beyond what is ordinary",
            phonetic = "[eks-tɾaoɾ-ði-ˈna-ɾjo]",
            translation = "Extraordinary",
            partOfSpeech = "Adjective",
            exampleSentence = "Tu progreso con PROMASTER es extraordinario.",
            exampleTranslation = "Your progress with PROMASTER is extraordinary.",
            difficulty = DifficultyLevel.ADVANCED,
            reviewStatus = ReviewStatus.LEARNING,
            masteryLevel = 85
        )
    )

    val grammarRules = listOf(
        GrammarRule(
            id = "gram_01",
            topic = "Present Tense: -AR Regular Verbs",
            explanation = "Remove the -ar ending from the infinitive and append subject endings according to the person performing the action.",
            motherTongueExplanations = mapOf(
                "English" to "In Spanish, the verb changes its ending based on who is speaking (I, you, he/she, we, they). For -ar verbs, drop -ar and attach: -o, -as, -a, -amos, -an.",
                "Tamil" to "ஸ்பானிஷ் மொழியில் எழுவாய் (I, You, We) மாறும் போது வினைச்சொல்லின் விகுதி மாறும். '-ar' வினைகளுக்கு விகுதியாக -o, -as, -a, -amos, -an சேருங்கள்.",
                "Hindi" to "स्पैनिश में कर्ता (I, You, We) के अनुसार क्रिया का अंतिम रूप बदलता है। '-ar' क्रियाओं के लिए -o, -as, -a, -amos, -an जोड़ें।",
                "Spanish" to "Para conjugar verbos regulares terminados en -ar en presente, eliminamos la terminación y agregamos las desinencias: -o, -as, -a, -amos, -an."
            ),
            formula = "Stem + [-o, -as, -a, -amos, -áis, -an]",
            examples = listOf(
                GrammarExample("Yo hablo español.", "I speak Spanish.", "Yo (I) + habl- + -o"),
                GrammarExample("Tú hablas con fluidez.", "You speak fluently.", "Tú (You) + habl- + -as"),
                GrammarExample("Ella habla con el profesor.", "She speaks with the teacher.", "Ella (She) + habl- + -a"),
                GrammarExample("Nosotros hablamos juntos.", "We speak together.", "Nosotros (We) + habl- + -amos")
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "ex_01",
                    prompt = "Select the correct form: 'Yo _____ (trabajar) en la oficina.'",
                    options = listOf("trabajo", "trabajas", "trabaja", "trabajamos"),
                    correctAnswer = "trabajo",
                    explanation = "'Yo' takes the -o ending: 'trabajo'."
                ),
                GrammarExercise(
                    id = "ex_02",
                    prompt = "Select the correct form: 'Ellos _____ (estudiar) mucho.'",
                    options = listOf("estudio", "estudia", "estudian", "estudiamos"),
                    correctAnswer = "estudian",
                    explanation = "'Ellos' takes the -an ending: 'estudian'."
                )
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = "q_g1",
                    type = QuizQuestionType.MULTIPLE_CHOICE,
                    prompt = "¿Cuál es la forma correcta para 'Nosotros (cantar)'?",
                    options = listOf("canto", "cantas", "cantamos", "cantan"),
                    correctAnswer = "cantamos",
                    explanation = "'Nosotros' always conjugates with '-amos' for -ar verbs."
                ),
                QuizQuestion(
                    id = "q_g2",
                    type = QuizQuestionType.FILL_IN_THE_BLANK,
                    prompt = "Complete: 'Tú _____ (escuchar) música.'",
                    options = listOf("escucho", "escuchas", "escucha", "escuchan"),
                    correctAnswer = "escuchas",
                    explanation = "'Tú' takes '-as'."
                )
            ),
            commonMistake = "Don't confuse informal 'tú hablas' with formal 'usted habla'.",
            tip = "Subject pronouns (yo, tú) are frequently omitted because the verb ending clarifies who is doing the action!"
        ),
        GrammarRule(
            id = "gram_02",
            topic = "The Distinction: Ser vs Estar",
            explanation = "Both verbs translate to 'to be', but 'Ser' is used for permanent traits, identity, and origin, while 'Estar' indicates temporary states, feelings, and physical locations.",
            motherTongueExplanations = mapOf(
                "English" to "Use 'Ser' for inherent characteristics, professions, origin, and time (DOCTOR). Use 'Estar' for temporary conditions, feelings, and locations (PLACE).",
                "Tamil" to "ஸ்பானிஷ் மொழியில் 'இருக்கிறேன்' (to be) என்பதை குறிக்க Ser மற்றும் Estar என இரண்டு வினைகள் உள்ளன. நிலையான குணங்களுக்கு 'Ser' மற்றும் தற்காலிக நிலை அல்லது இடத்திற்கு 'Estar' பயன்படுத்தவும்.",
                "Hindi" to "'हूँ / है' (to be) के लिए स्पैनिश में दो शब्द हैं: 'Ser' (स्थाई गुण, पहचान) और 'Estar' (अस्थाई स्थिति, भावना, स्थान)।",
                "Spanish" to "Utilizamos 'Ser' para características inherentes, identidad y tiempo. Utilizamos 'Estar' para ubicaciones, estados temporales y emociones."
            ),
            formula = "Ser = DOCTOR (Description, Occupation, Characteristic, Time, Origin) | Estar = PLACE (Position, Location, Action, Condition, Emotion)",
            examples = listOf(
                GrammarExample("Ella es médica.", "She is a doctor.", "Profession -> Ser (es)"),
                GrammarExample("Madrid es la capital.", "Madrid is the capital.", "Identity -> Ser (es)"),
                GrammarExample("Estoy cansado hoy.", "I am tired today.", "Temporary feeling -> Estar (estoy)"),
                GrammarExample("El café está en la mesa.", "The coffee is on the table.", "Physical location -> Estar (está)")
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "ex_03",
                    prompt = "Choose the correct verb: '¿Dónde _____ el hotel?'",
                    options = listOf("está", "es", "son", "están"),
                    correctAnswer = "está",
                    explanation = "Physical locations always require 'Estar'."
                ),
                GrammarExercise(
                    id = "ex_04",
                    prompt = "Choose the correct verb: 'Carlos _____ inteligente.'",
                    options = listOf("es", "está", "somos", "están"),
                    correctAnswer = "es",
                    explanation = "Permanent traits and personality characteristics require 'Ser'."
                )
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = "q_g3",
                    type = QuizQuestionType.SENTENCE_CORRECTION,
                    prompt = "Correct the mistake: 'Yo soy muy cansado después del trabajo.'",
                    options = listOf(
                        "Yo estoy muy cansado después del trabajo.",
                        "Yo ser muy cansado después del trabajo.",
                        "Yo cansado estoy después del trabajo.",
                        "Yo tengo muy cansado después del trabajo."
                    ),
                    correctAnswer = "Yo estoy muy cansado después del trabajo.",
                    explanation = "Being tired is a temporary physical state, so use 'estar'."
                ),
                QuizQuestion(
                    id = "q_g4",
                    type = QuizQuestionType.TRANSLATION,
                    prompt = "Translate to Spanish: 'Where is the restaurant?'",
                    options = listOf(
                        "¿Dónde está el restaurante?",
                        "¿Dónde es el restaurante?",
                        "¿Cómo está el restaurante?",
                        "¿Quién es el restaurante?"
                    ),
                    correctAnswer = "¿Dónde está el restaurante?",
                    explanation = "Location uses 'está'."
                )
            ),
            commonMistake = "Saying 'Soy enfermo' (I am an ill person fundamentally) instead of 'Estoy enfermo' (I am feeling sick today).",
            tip = "Remember the rhyme: 'How you feel and where you are, always use the verb Estar!'"
        ),
        GrammarRule(
            id = "gram_03",
            topic = "Gender and Number of Adjectives",
            explanation = "Adjectives in Spanish match the gender (masculine/feminine) and number (singular/plural) of the noun they modify.",
            motherTongueExplanations = mapOf(
                "English" to "Adjectives in Spanish match the gender (masculine/feminine) and number (singular/plural) of the noun they modify. Usually placed after the noun.",
                "Tamil" to "ஸ்பானிஷ் மொழியில் பெயரடைகள் (Adjectives) பெயர்ச்சொல்லின் பால் மற்றும் எண்ணிக்கைக்கு (ஒருமை/பன்மை) ஏற்ப மாறும். பொதுவாக பெயர்ச்சொல்லுக்குப் பின்னரே வரும்.",
                "Hindi" to "स्पैनिश में विशेषण (Adjective) संज्ञा के लिंग (पुल्लिंग/स्त्रीलिंग) और वचन (एकवचन/बहुवचन) के अनुसार बदलते हैं।",
                "Spanish" to "Los adjetivos concuerdan en género y número con el sustantivo al que modifican y generalmente se colocan después de este."
            ),
            formula = "Noun (m/f, s/p) + Adjective (matches m/f, s/p)",
            examples = listOf(
                GrammarExample("El libro rojo.", "The red book.", "Masculine singular -> rojo"),
                GrammarExample("Las casas blancas.", "The white houses.", "Feminine plural -> blancas"),
                GrammarExample("Un estudiante inteligente.", "An intelligent student.", "Gender neutral ending in -e")
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "ex_05",
                    prompt = "Select the correct plural form: 'Las manzanas _____ (rojo)'",
                    options = listOf("rojas", "rojo", "rojos", "roja"),
                    correctAnswer = "rojas",
                    explanation = "'Manzanas' is feminine and plural, so the adjective must be 'rojas'."
                )
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = "q_g5",
                    type = QuizQuestionType.MULTIPLE_CHOICE,
                    prompt = "Which form correctly completes: 'Los coches son _____ (rápido)'?",
                    options = listOf("rápidos", "rápido", "rápidas", "rápida"),
                    correctAnswer = "rápidos",
                    explanation = "'Los coches' is masculine plural, needing 'rápidos'."
                )
            ),
            commonMistake = "Placing the adjective before the noun like in English (e.g. 'rojo libro' is incorrect).",
            tip = "Descriptive adjectives almost always go AFTER the noun!"
        )
    )

    val sampleQuizQuestions = listOf(
        QuizQuestion(
            id = "quiz_01",
            type = QuizQuestionType.MULTIPLE_CHOICE,
            prompt = "What is the English meaning of 'El Desayuno'?",
            options = listOf("Lunch", "Breakfast", "Dinner", "Snack"),
            correctAnswer = "Breakfast",
            explanation = "'El desayuno' translates to breakfast, the morning meal."
        ),
        QuizQuestion(
            id = "quiz_02",
            type = QuizQuestionType.FILL_IN_THE_BLANK,
            prompt = "Complete the sentence: 'Nosotros _____ (aprender) palabras nuevas.'",
            options = listOf("aprendemos", "aprendo", "aprende", "aprenden"),
            correctAnswer = "aprendemos",
            explanation = "The regular -er present tense ending for 'nosotros' is '-emos'."
        ),
        QuizQuestion(
            id = "quiz_03",
            type = QuizQuestionType.SENTENCE_CORRECTION,
            prompt = "Identify the corrected sentence: 'La casa es muy hermoso.'",
            options = listOf(
                "La casa es muy hermosa.",
                "La casa son muy hermoso.",
                "El casa es muy hermoso.",
                "La casa está muy hermosos."
            ),
            correctAnswer = "La casa es muy hermosa.",
            explanation = "Adjectives must match gender: 'casa' is feminine, so 'hermosa'."
        ),
        QuizQuestion(
            id = "quiz_04",
            type = QuizQuestionType.TRANSLATION,
            prompt = "Translate to Spanish: 'I speak with my friend every day.'",
            options = listOf(
                "Hablo con mi amigo todos los días.",
                "Hablamos con mi amigo todos los días.",
                "Habla con mi amigo cada día.",
                "Hablan con mi amigo todos los días."
            ),
            correctAnswer = "Hablo con mi amigo todos los días.",
            explanation = "'Hablo' is first person singular (I speak)."
        )
    )

    val friendMessages = mutableListOf(
        AiMessage(
            id = "msg_01",
            sender = MessageSender.AI_FRIEND,
            text = "Hey Alex! 👋 Awesome job keeping your 7-day streak alive today!",
            timestamp = "09:15 AM"
        ),
        AiMessage(
            id = "msg_02",
            sender = MessageSender.USER,
            text = "Thanks! I'm feeling more confident speaking Spanish now.",
            timestamp = "09:16 AM"
        ),
        AiMessage(
            id = "msg_03",
            sender = MessageSender.AI_FRIEND,
            text = "¡Qué genial! That's wonderful to hear. What was your favorite word you learned this week?",
            timestamp = "09:17 AM"
        )
    )

    val coachMessages = mutableListOf(
        AiMessage(
            id = "coach_01",
            sender = MessageSender.LANGUAGE_COACH,
            text = "¡Bienvenido a tu sesión de práctica! Today we are practicing ordering drinks and snacks. Try asking me for a coffee with milk in Spanish.",
            timestamp = "10:00 AM",
            translationHint = "Hint: 'Me gustaría un café con leche, por favor.'"
        ),
        AiMessage(
            id = "coach_02",
            sender = MessageSender.USER,
            text = "Hola coach, me gustaría un café con leche y una tostada.",
            timestamp = "10:01 AM"
        ),
        AiMessage(
            id = "coach_03",
            sender = MessageSender.LANGUAGE_COACH,
            text = "¡Perfecto! Grammatically flawless. You used the polite conditional form 'me gustaría' and proper conjunction 'y'. Now try asking: '¿Cuánto cuesta?'",
            timestamp = "10:02 AM",
            grammarTip = "'¿Cuánto cuesta?' is used for singular items, '¿Cuánto cuestan?' for plural!"
        )
    )

    val automationActions = listOf(
        AutomationAction(
            id = "auto_01",
            name = "Study Mode: Silence Distractions",
            description = "Automatically enable Do Not Disturb on your device during active lesson sessions.",
            iconEmoji = "🔕",
            safetyLevel = SafetyLevel.SAFE,
            isEnabled = true,
            triggers = "Activates when a lesson starts"
        ),
        AutomationAction(
            id = "auto_02",
            name = "Morning Vocabulary Flashcard Prompt",
            description = "Shows a high-priority quick flashcard reminder at 8:00 AM to kickstart your day.",
            iconEmoji = "🌅",
            safetyLevel = SafetyLevel.SAFE,
            isEnabled = true,
            triggers = "Daily at 08:00 AM"
        ),
        AutomationAction(
            id = "auto_03",
            name = "Streak Protection Alarm",
            description = "Triggers an evening reminder if your daily practice goal is incomplete by 8:00 PM.",
            iconEmoji = "🔥",
            safetyLevel = SafetyLevel.SAFE,
            isEnabled = true,
            triggers = "Daily at 08:00 PM (if pending tasks exist)"
        ),
        AutomationAction(
            id = "auto_04",
            name = "Automated Audio Review on Headphone Connect",
            description = "Plays the latest 3-minute listening dialogue when Bluetooth headphones connect.",
            iconEmoji = "🎧",
            safetyLevel = SafetyLevel.REQUIRES_CONFIRMATION,
            isEnabled = false,
            triggers = "Bluetooth Audio Connect"
        )
    )

    var progressState = ProgressState(
        currentStreak = 7,
        bestStreak = 14,
        totalXp = 850,
        wordsLearned = 142,
        rulesMastered = 18,
        speakingMinutes = 45,
        listeningMinutes = 80,
        level = 4,
        weeklyXp = listOf(90, 110, 130, 85, 140, 125, 170),
        badges = listOf("🔥 7-Day Streak Master", "🎯 Vocabulary Pioneer", "🎙️ Golden Pronunciation", "🧠 Grammar Guru", "⚡ Rapid Learner")
    )
}
