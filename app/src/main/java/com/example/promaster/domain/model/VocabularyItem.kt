package com.example.promaster.domain.model

enum class DifficultyLevel(val label: String) {
    BEGINNER("Beginner 🌱"),
    INTERMEDIATE("Intermediate 🌿"),
    ADVANCED("Advanced ⭐")
}

enum class ReviewStatus(val label: String) {
    NEW("New 🆕"),
    LEARNING("Learning ⏳"),
    NEEDS_REVIEW("Needs Review 🔄"),
    MASTERED("Mastered 🏆");

    val title: String get() = label
}

data class VocabularyItem(
    val id: String,
    val word: String,
    val meaning: String,
    val exampleSentence: String,
    val translation: String,
    val exampleTranslation: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "Noun",
    val difficulty: DifficultyLevel = DifficultyLevel.BEGINNER,
    val reviewStatus: ReviewStatus = ReviewStatus.NEW,
    val masteryLevel: Int = 0, // 0-100%
    val lastReviewedAt: Long? = null
) {
    constructor(
        id: String,
        word: String,
        phonetic: String,
        translation: String,
        partOfSpeech: String,
        exampleSentence: String,
        exampleTranslation: String,
        masteryLevel: Int
    ) : this(
        id = id,
        word = word,
        meaning = translation,
        exampleSentence = exampleSentence,
        translation = translation,
        exampleTranslation = exampleTranslation,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        difficulty = if (masteryLevel > 70) DifficultyLevel.ADVANCED else if (masteryLevel > 40) DifficultyLevel.INTERMEDIATE else DifficultyLevel.BEGINNER,
        reviewStatus = if (masteryLevel >= 90) ReviewStatus.MASTERED else if (masteryLevel >= 40) ReviewStatus.LEARNING else ReviewStatus.NEEDS_REVIEW,
        masteryLevel = masteryLevel,
        lastReviewedAt = null
    )
}
