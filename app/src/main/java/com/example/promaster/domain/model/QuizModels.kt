package com.example.promaster.domain.model

enum class QuizQuestionType(val title: String) {
    MULTIPLE_CHOICE("Multiple Choice"),
    FILL_IN_THE_BLANK("Fill in the Blank"),
    SENTENCE_CORRECTION("Sentence Correction"),
    TRANSLATION("Translation")
}

data class QuizQuestion(
    val id: String,
    val type: QuizQuestionType,
    val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = "",
    val motherTongueHint: String = ""
)

data class QuizResult(
    val quizId: String,
    val title: String,
    val score: Int, // 0..100
    val attempts: Int = 1,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val totalQuestions: Int,
    val completion: Boolean = (score >= 70),
    val earnedXp: Int = 0,
    val completedAt: Long = System.currentTimeMillis()
)
