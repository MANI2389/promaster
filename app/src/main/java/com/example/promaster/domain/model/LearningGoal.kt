package com.example.promaster.domain.model

enum class LanguageLevel(
    val title: String,
    val subtitle: String,
    val cefrCode: String,
    val iconEmoji: String
) {
    BEGINNER(
        title = "Beginner",
        subtitle = "Zero or minimal prior knowledge. Starting from foundational basics.",
        cefrCode = "A1",
        iconEmoji = "🌱"
    ),
    ELEMENTARY(
        title = "Elementary",
        subtitle = "Can understand simple everyday phrases and introduce yourself.",
        cefrCode = "A2",
        iconEmoji = "🌿"
    ),
    INTERMEDIATE(
        title = "Intermediate",
        subtitle = "Can hold conversations, navigate travel, and express clear opinions.",
        cefrCode = "B1 / B2",
        iconEmoji = "🌳"
    ),
    ADVANCED(
        title = "Advanced",
        subtitle = "Comfortable with complex topics, idiomatic speech, and professional fluency.",
        cefrCode = "C1 / C2",
        iconEmoji = "⭐"
    );

    val displayName: String get() = title
}

enum class LearningGoalType(
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val defaultTargetWords: Int
) {
    CAREER(
        title = "Career & Professional Growth",
        subtitle = "Ace international job interviews, emails, and business negotiations",
        iconEmoji = "💼",
        defaultTargetWords = 500
    ),
    TRAVEL(
        title = "Travel & Cultural Exploration",
        subtitle = "Order food, ask directions, and connect with locals effortlessly",
        iconEmoji = "✈️",
        defaultTargetWords = 350
    ),
    BRAIN_TRAINING(
        title = "Brain Health & Mental Fitness",
        subtitle = "Boost memory retention, neuroplasticity, and cognitive agility",
        iconEmoji = "🧠",
        defaultTargetWords = 300
    ),
    SOCIAL(
        title = "Family, Friends & Dating",
        subtitle = "Communicate authentically with loved ones and make global friends",
        iconEmoji = "💬",
        defaultTargetWords = 400
    ),
    ACADEMIC(
        title = "Studies & Language Exams",
        subtitle = "Prepare for standardized certifications, essays, and exams",
        iconEmoji = "🎓",
        defaultTargetWords = 600
    ),
    PERSONAL_GROWTH(
        title = "Personal Growth & Hobby",
        subtitle = "Enjoy foreign cinema, literature, music, and broaden horizons",
        iconEmoji = "🌱",
        defaultTargetWords = 350
    )
}

data class LearningGoal(
    val id: String = "goal_default",
    val type: LearningGoalType = LearningGoalType.CAREER,
    val title: String = type.title,
    val description: String = type.subtitle,
    val iconEmoji: String = type.iconEmoji,
    val targetDays: Int = 30,
    val targetWords: Int = type.defaultTargetWords
)
