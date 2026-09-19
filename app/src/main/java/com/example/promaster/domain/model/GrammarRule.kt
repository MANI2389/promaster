package com.example.promaster.domain.model

data class GrammarExample(
    val sentence: String,
    val translation: String,
    val breakdown: String = "",
    val highlight: String = breakdown
)

data class GrammarExercise(
    val id: String,
    val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = ""
) {
    val question: String get() = prompt
}

data class GrammarRule(
    val id: String,
    val topic: String,
    val explanation: String,
    val motherTongueExplanations: Map<String, String> = emptyMap(),
    val formula: String = "",
    val examples: List<GrammarExample> = emptyList(),
    val exercises: List<GrammarExercise> = emptyList(),
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val commonMistake: String = "",
    val tip: String = "",
    val title: String = topic
) {
    constructor(
        id: String,
        title: String,
        explanation: String,
        formula: String = "",
        examples: List<String> = emptyList(),
        commonMistake: String = "",
        tip: String = ""
    ) : this(
        id = id,
        topic = title,
        explanation = explanation,
        motherTongueExplanations = mapOf("English" to explanation),
        formula = formula,
        examples = examples.map { GrammarExample(sentence = it, translation = "") },
        exercises = emptyList(),
        quizQuestions = emptyList(),
        commonMistake = commonMistake,
        tip = tip,
        title = title
    )
}
