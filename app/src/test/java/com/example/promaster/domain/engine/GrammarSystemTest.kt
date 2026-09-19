package com.example.promaster.domain.engine

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.GrammarExample
import com.example.promaster.domain.model.GrammarExercise
import com.example.promaster.domain.model.GrammarRule
import com.example.promaster.domain.model.QuizQuestion
import com.example.promaster.domain.model.QuizQuestionType
import org.junit.Assert.*
import org.junit.Test

class GrammarSystemTest {

    @Test
    fun grammarRule_supportsMotherTongueExplanationsAndExercises() {
        val rule = GrammarRule(
            id = "g_test_1",
            title = "Articles and Gender",
            topic = "Definite & Indefinite Articles",
            explanation = "Spanish nouns have gender (masculine or feminine).",
            motherTongueExplanations = mapOf(
                "English" to "Spanish nouns have gender (masculine or feminine).",
                "Tamil" to "ஸ்பானிஷ் பெயர்ச்சொற்கள் ஆண்பால் அல்லது பெண்பால் பாலினத்தைக் கொண்டுள்ளன.",
                "Hindi" to "स्पैनिश संज्ञाओं में लिंग (पुल्लिंग या स्त्रीलिंग) होता है।"
            ),
            formula = "el/la + noun",
            examples = listOf(
                GrammarExample("El libro es interesante.", "The book is interesting.", "El libro"),
                GrammarExample("La mesa es grande.", "The table is large.", "La mesa")
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "ex_1",
                    prompt = "Choose the correct article for 'casa' (house):",
                    options = listOf("El", "La", "Los", "Las"),
                    correctAnswer = "La",
                    explanation = "'Casa' is a feminine noun ending in -a."
                )
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = "quiz_g_1",
                    type = QuizQuestionType.MULTIPLE_CHOICE,
                    prompt = "Which article is used with masculine singular nouns?",
                    options = listOf("El", "La", "Los", "Las"),
                    correctAnswer = "El",
                    explanation = "'El' is the masculine singular definite article."
                )
            ),
            tip = "Most nouns ending in -o are masculine, and -a are feminine."
        )

        assertEquals("Definite & Indefinite Articles", rule.topic)
        assertEquals("ஸ்பானிஷ் பெயர்ச்சொற்கள் ஆண்பால் அல்லது பெண்பால் பாலினத்தைக் கொண்டுள்ளன.", rule.motherTongueExplanations["Tamil"])
        assertEquals("स्पैनिश संज्ञाओं में लिंग (पुल्लिंग या स्त्रीलिंग) होता है।", rule.motherTongueExplanations["Hindi"])
        assertEquals(2, rule.examples.size)
        assertEquals(1, rule.exercises.size)
        assertEquals("La", rule.exercises.first().correctAnswer)
        assertEquals(1, rule.quizQuestions.size)
    }

    @Test
    fun grammarRule_legacyConstructorCompatibility() {
        // Test secondary constructor with simple List<String> examples
        val rule = GrammarRule(
            id = "legacy_g",
            title = "Present Tense",
            explanation = "Used for habitual actions.",
            formula = "Root + Ending",
            examples = listOf("Yo hablo español.", "Tú comes pan."),
            tip = "Drop the infinitive ending before adding personal suffixes."
        )

        assertEquals("Present Tense", rule.topic) // defaults to title
        assertEquals(2, rule.examples.size)
        assertEquals("Yo hablo español.", rule.examples[0].sentence)
        assertTrue(rule.motherTongueExplanations.containsKey("English"))
        assertEquals("Used for habitual actions.", rule.motherTongueExplanations["English"])
    }

    @Test
    fun mockDataProvider_grammarRulesHaveValidMotherTongueAndExercises() {
        val rules = MockDataProvider.grammarRules
        assertTrue(rules.isNotEmpty())

        for (rule in rules) {
            assertTrue("Rule ${rule.id} should have non-blank title", rule.title.isNotBlank())
            assertTrue("Rule ${rule.id} should have non-blank topic", rule.topic.isNotBlank())
            assertTrue("Rule ${rule.id} should have English explanation", rule.motherTongueExplanations.containsKey("English"))
            assertTrue("Rule ${rule.id} should have Tamil explanation", rule.motherTongueExplanations.containsKey("Tamil"))
            assertTrue("Rule ${rule.id} should have Hindi explanation", rule.motherTongueExplanations.containsKey("Hindi"))
            assertTrue("Rule ${rule.id} should have at least 1 exercise", rule.exercises.isNotEmpty())
            assertTrue("Rule ${rule.id} should have at least 1 quiz question", rule.quizQuestions.isNotEmpty())
        }
    }
}
