package com.example.promaster.domain.engine

import com.example.promaster.domain.model.*

/**
 * Intelligent 30-Day Curriculum Generation Engine for PROMASTER.
 * Generates personalized daily syllabi and task flows based on:
 * - Mother tongue
 * - Target language
 * - Proficiency level (A1..C2)
 * - Daily commitment duration (10..60 mins)
 * - Learning goal (Career, Travel, Brain Training, etc.)
 */
object ThirtyDayLearningEngine {

    data class DayCurriculum(
        val dayNumber: Int,
        val phaseName: String,
        val title: String,
        val description: String,
        val estimatedMinutes: Int,
        val tasks: List<DailyTask>
    )

    fun generateCompleteCurriculum(
        motherTongue: String,
        targetLanguage: String,
        level: LanguageLevel,
        dailyDuration: Int,
        goal: LearningGoalType,
        currentDay: Int = 1
    ): List<DayCurriculum> {
        return (1..30).map { day ->
            val phase = getPhaseName(day)
            val title = getDayTitle(day, targetLanguage, level, goal)
            val description = getDayDescription(day, targetLanguage, level, goal)
            val tasks = generateTasksForDay(
                dayNumber = day,
                targetLanguage = targetLanguage,
                motherTongue = motherTongue,
                level = level,
                dailyDuration = dailyDuration,
                goal = goal,
                isCurrentOrPastDay = day <= currentDay
            )

            DayCurriculum(
                dayNumber = day,
                phaseName = phase,
                title = title,
                description = description,
                estimatedMinutes = dailyDuration,
                tasks = tasks
            )
        }
    }

    fun generateTasksForDay(
        dayNumber: Int,
        targetLanguage: String,
        motherTongue: String,
        level: LanguageLevel,
        dailyDuration: Int,
        goal: LearningGoalType,
        isCurrentOrPastDay: Boolean = true
    ): List<DailyTask> {
        val categories = getCategoriesForDuration(dailyDuration, dayNumber)
        val minutesPerTask = maxOf(3, dailyDuration / categories.size)

        return categories.mapIndexed { index, category ->
            val taskId = "d${dayNumber}_t${index + 1}_${category.name.lowercase()}"
            val taskTitle = getTaskTitle(category, dayNumber, targetLanguage, goal)
            val taskDesc = getTaskDescription(category, dayNumber, targetLanguage, motherTongue, goal)
            val minPassing = if (category in listOf(TaskCategory.SPEAKING, TaskCategory.CONVERSATION, TaskCategory.QUIZ)) 70 else 60
            val xp = calculateTaskXp(category, minutesPerTask)

            // Initial status:
            // If day is past (dayNumber < currentDay), tasks default to COMPLETED.
            // If current day: first task is AVAILABLE, rest are LOCKED according to sequential flow.
            // If future day: all tasks LOCKED.
            val status = when {
                !isCurrentOrPastDay -> TaskCompletionStatus.LOCKED
                index == 0 -> TaskCompletionStatus.AVAILABLE
                else -> TaskCompletionStatus.LOCKED
            }

            DailyTask(
                taskId = taskId,
                dayNumber = dayNumber,
                title = taskTitle,
                description = taskDesc,
                category = category,
                estimatedMinutes = minutesPerTask,
                completionStatus = status,
                score = 0,
                retryCount = 0,
                minPassingScore = minPassing,
                xpReward = xp,
                isRequired = true
            )
        }
    }

    private fun getCategoriesForDuration(dailyDuration: Int, dayNumber: Int): List<TaskCategory> {
        val isReviewDay = dayNumber % 7 == 0
        val isMidwayOrFinal = dayNumber == 15 || dayNumber == 30

        return when {
            isMidwayOrFinal -> listOf(
                TaskCategory.REVIEW,
                TaskCategory.SPEAKING,
                TaskCategory.QUIZ
            )
            dailyDuration <= 10 -> {
                if (dayNumber % 2 == 1) listOf(TaskCategory.VOCABULARY, TaskCategory.SPEAKING)
                else listOf(TaskCategory.GRAMMAR, TaskCategory.LISTENING)
            }
            dailyDuration <= 20 -> {
                listOf(
                    TaskCategory.VOCABULARY,
                    TaskCategory.GRAMMAR,
                    TaskCategory.SPEAKING
                )
            }
            dailyDuration <= 30 -> {
                listOf(
                    TaskCategory.VOCABULARY,
                    TaskCategory.GRAMMAR,
                    TaskCategory.LISTENING,
                    TaskCategory.SPEAKING,
                    if (isReviewDay) TaskCategory.REVIEW else TaskCategory.CONVERSATION
                )
            }
            dailyDuration <= 45 -> {
                listOf(
                    TaskCategory.VOCABULARY,
                    TaskCategory.GRAMMAR,
                    TaskCategory.LISTENING,
                    TaskCategory.SPEAKING,
                    TaskCategory.CONVERSATION,
                    TaskCategory.REVIEW
                )
            }
            else -> { // 60 minutes
                listOf(
                    TaskCategory.VOCABULARY,
                    TaskCategory.GRAMMAR,
                    TaskCategory.LISTENING,
                    TaskCategory.SPEAKING,
                    TaskCategory.CONVERSATION,
                    TaskCategory.REVIEW,
                    TaskCategory.QUIZ
                )
            }
        }
    }

    private fun getPhaseName(day: Int): String {
        return when (day) {
            in 1..7 -> "Phase 1: Foundations & Phonetics"
            in 8..14 -> "Phase 2: Structural Core & Sentences"
            in 15..22 -> "Phase 3: Situational & Goal Fluency"
            else -> "Phase 4: Nuance, Voice AI & Mastery"
        }
    }

    private fun getDayTitle(
        day: Int,
        targetLanguage: String,
        level: LanguageLevel,
        goal: LearningGoalType
    ): String {
        return when (day) {
            1 -> "Foundations: Essential Greetings & Phonetics in $targetLanguage"
            2 -> "Identity & Introductions: Names, Origins & Background"
            3 -> "Numbers 1-100, Quantities & Time Expressions"
            4 -> "Descriptive Language: Colors, Qualities & Basic Adjectives"
            5 -> "Social Connections: Family, Friends & Relationships"
            6 -> "Dining & Culture: Ordering Food, Drinks & Table Etiquette"
            7 -> "Week 1 Checkpoint: Core Present Tense & Action Verbs"
            8 -> "Question Mastery: Asking Who, What, Where, When & Why"
            9 -> "Calendar & Routines: Days, Months, Schedules & Habits"
            10 -> "Directions & Mobility: City Navigation & Landmarks"
            11 -> "Commerce & Everyday Trade: Shopping, Prices & Transactions"
            12 -> "Environment: Weather, Seasons & Small Talk"
            13 -> "Verb Mastery: Modal Verbs, Wants, Needs & Requests"
            14 -> "Sentence Expansion: Conjunctions, Connectors & Flow"
            15 -> "Midway Fluency Assessment: Comprehensive Milestone Review"
            in 16..22 -> getGoalSpecificDayTitle(day, targetLanguage, goal)
            23 -> "Narrative Skills: Speaking in Past Tenses & Storytelling"
            24 -> "Future Plans & Aspirations: Expressing Intentions & Goals"
            25 -> "Hypotheticals & Nuance: Conditionals & Polite Requests"
            26 -> "Modern Expressions: Idioms, Colloquialisms & Slang in $targetLanguage"
            27 -> "Expressing Opinions & Constructive Debate"
            28 -> "Cultural Nuances, Humor & Politeness Protocols"
            29 -> "Advanced AI Voice & Real-time Scenario Simulation"
            else -> "30-Day Graduation Milestone: Fluency Assessment & Certificate"
        }
    }

    private fun getGoalSpecificDayTitle(day: Int, targetLanguage: String, goal: LearningGoalType): String {
        return when (goal) {
            LearningGoalType.TRAVEL -> when (day) {
                16 -> "Airport Navigation: Boarding, Baggage & Customs in $targetLanguage"
                17 -> "Hotels & Accommodation: Reservations & Special Requests"
                18 -> "Public Transit: Metro, Trains & Taxi Communication"
                19 -> "Authentic Dining: Local Specialties & Dietary Requirements"
                20 -> "Emergency & Health: Pharmacy, Medical Care & Urgent Needs"
                21 -> "Excursions & Culture: Museums, Tours & Ticket Booking"
                else -> "Local Bargaining, Markets & Courteous Interactions"
            }
            LearningGoalType.CAREER -> when (day) {
                16 -> "Professional Etiquette: Formal Email & Greeting Protocols"
                17 -> "Self-Pitch & Resume Walkthrough in $targetLanguage"
                18 -> "Active Meeting Participation: Agreeing & Disagreeing"
                19 -> "Negotiations & Contracts: Terms, Deadlines & Agreements"
                20 -> "Delivering Presentations: Slide Commentary & Q&A"
                21 -> "Corporate Socializing & Networking at Industry Mixers"
                else -> "Cross-Cultural Business Problem Solving"
            }
            LearningGoalType.BRAIN_TRAINING -> when (day) {
                16 -> "Cognitive Speed Drills: Rapid Translation & Sentence Inversion"
                17 -> "Memory Matrix: 50 High-Yield Polysemous Words"
                18 -> "Syntactic Puzzles: Complex Word Order & Relative Clauses"
                19 -> "Audio Deciphering: Dialectal Accents & Ambient Noise"
                20 -> "Synonym Discrimination: Shades of Meaning & Tone"
                21 -> "Rapid-Fire Mental Recall: Conjugations Under Timer"
                else -> "Bilingual Conceptual Switching & Cognitive Agility"
            }
            LearningGoalType.SOCIAL -> when (day) {
                16 -> "Casual Hangouts: Inviting Friends & Making Weekend Plans"
                17 -> "Hobbies, Music & Pop Culture Conversations"
                18 -> "Humor, Wit & Banter: Understanding Cultural Jokes"
                19 -> "Storytelling: Sharing Memorable Personal Experiences"
                20 -> "Deep Discussions: Values, Passions & Life Perspectives"
                21 -> "Texting & Social Media: Modern Chat Abbreviations"
                else -> "Hosting Guests & Festive Hospitality in $targetLanguage"
            }
            LearningGoalType.ACADEMIC -> when (day) {
                16 -> "Academic Vocabulary: Citing Evidence & Formulating Hypotheses"
                17 -> "Reading Comprehension: Scientific & Cultural Essays"
                18 -> "Seminar Participation: Asking Probing Questions"
                19 -> "Argumentative Structures: Thesis, Counterpoint & Synthesis"
                20 -> "Formal Oral Presentations: Structuring Complex Ideas"
                21 -> "Summarizing Research Findings with Precision"
                else -> "Scholarly Debate & Defending Perspectives"
            }
            LearningGoalType.PERSONAL_GROWTH -> when (day) {
                16 -> "Reflective Journaling: Articulating Hopes & Feelings"
                17 -> "Literature & Poetry: Exploring Famous $targetLanguage Verses"
                18 -> "Art & Aesthetic Appreciation: Describing Visual Media"
                19 -> "Mindfulness & Well-being: Mindful Speech & Self-Talk"
                20 -> "Philosophy & Traditions of $targetLanguage Speakers"
                21 -> "Personal Values: Explaining Life Philosophy with Elegance"
                else -> "The Art of Eloquence: Speaking with Poise & Grace"
            }
        }
    }

    private fun getDayDescription(
        day: Int,
        targetLanguage: String,
        level: LanguageLevel,
        goal: LearningGoalType
    ): String {
        return "Comprehensive Day $day practice tailored for ${level.title} level in $targetLanguage, oriented around your ${goal.name.lowercase().replace('_', ' ')} goal."
    }

    private fun getTaskTitle(
        category: TaskCategory,
        dayNumber: Int,
        targetLanguage: String,
        goal: LearningGoalType
    ): String {
        return when (category) {
            TaskCategory.VOCABULARY -> "Day $dayNumber Vocabulary: Essential $targetLanguage Words"
            TaskCategory.GRAMMAR -> "Day $dayNumber Grammar: Structural Mechanics & Usage"
            TaskCategory.LISTENING -> "Day $dayNumber Listening: Native Audio Comprehension"
            TaskCategory.SPEAKING -> "Day $dayNumber Speaking: Pronunciation & Fluency Workout"
            TaskCategory.CONVERSATION -> "Day $dayNumber AI Dialogue: Interactive Scenario Simulation"
            TaskCategory.REVIEW -> "Day $dayNumber Flashcard Mastery & Error Correction"
            TaskCategory.QUIZ -> "Day $dayNumber Challenge: Fluency & Retention Quiz"
            TaskCategory.AI_PRACTICE -> "Day $dayNumber AI Companion Practice"
        }
    }

    private fun getTaskDescription(
        category: TaskCategory,
        dayNumber: Int,
        targetLanguage: String,
        motherTongue: String,
        goal: LearningGoalType
    ): String {
        return when (category) {
            TaskCategory.VOCABULARY -> "Learn and pronounce high-frequency words in $targetLanguage with $motherTongue translations."
            TaskCategory.GRAMMAR -> "Master essential grammar patterns and sentence structures with guided exercises."
            TaskCategory.LISTENING -> "Listen to authentic native dialogue and answer contextual questions."
            TaskCategory.SPEAKING -> "Record your voice, refine pronunciation, and achieve 70%+ passing accuracy."
            TaskCategory.CONVERSATION -> "Have a realistic conversation with the AI coach tailored to your ${goal.name.lowercase().replace('_', ' ')} goal."
            TaskCategory.REVIEW -> "Reinforce spaced-repetition vocabulary and correct past grammar mistakes."
            TaskCategory.QUIZ -> "Test your retention and earn bonus XP by demonstrating mastery."
            TaskCategory.AI_PRACTICE -> "Interactive practice with PROMASTER AI Language Coach."
        }
    }

    private fun calculateTaskXp(category: TaskCategory, minutes: Int): Int {
        val base = when (category) {
            TaskCategory.VOCABULARY -> 20
            TaskCategory.GRAMMAR -> 25
            TaskCategory.LISTENING -> 25
            TaskCategory.SPEAKING -> 35
            TaskCategory.CONVERSATION -> 40
            TaskCategory.REVIEW -> 20
            TaskCategory.QUIZ -> 50
            TaskCategory.AI_PRACTICE -> 30
        }
        return base + (minutes * 2)
    }
}
