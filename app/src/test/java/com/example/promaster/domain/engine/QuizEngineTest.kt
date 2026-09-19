package com.example.promaster.domain.engine

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.domain.model.QuizQuestion
import com.example.promaster.domain.model.QuizQuestionType
import com.example.promaster.domain.model.QuizResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QuizEngineTest {

    @Test
    fun quizQuestion_supportsAllFourTypes() {
        val mcq = QuizQuestion(
            id = "q1",
            type = QuizQuestionType.MULTIPLE_CHOICE,
            prompt = "What is 'water' in Spanish?",
            options = listOf("Agua", "Fuego", "Tierra", "Aire"),
            correctAnswer = "Agua",
            explanation = "Agua is water."
        )
        assertEquals(QuizQuestionType.MULTIPLE_CHOICE, mcq.type)

        val fitb = QuizQuestion(
            id = "q2",
            type = QuizQuestionType.FILL_IN_THE_BLANK,
            prompt = "Yo _____ (to be - permanent) estudiante.",
            options = listOf("soy", "estoy"),
            correctAnswer = "soy",
            explanation = "Soy is used for identity and occupation."
        )
        assertEquals(QuizQuestionType.FILL_IN_THE_BLANK, fitb.type)

        val correction = QuizQuestion(
            id = "q3",
            type = QuizQuestionType.SENTENCE_CORRECTION,
            prompt = "Correct the sentence: 'Yo estás feliz.'",
            correctAnswer = "Yo estoy feliz.",
            explanation = "With 'Yo', use 'estoy'."
        )
        assertEquals(QuizQuestionType.SENTENCE_CORRECTION, correction.type)

        val translation = QuizQuestion(
            id = "q4",
            type = QuizQuestionType.TRANSLATION,
            prompt = "Translate 'Thank you very much' into Spanish.",
            correctAnswer = "Muchas gracias",
            explanation = "Muchas gracias is thank you very much."
        )
        assertEquals(QuizQuestionType.TRANSLATION, translation.type)
    }

    @Test
    fun quizResult_tracksAllMetricsProperly() {
        val result = QuizResult(
            quizId = "quiz_test_101",
            title = "GRAMMAR_QUIZ",
            score = 80,
            attempts = 1,
            correctAnswers = 4,
            wrongAnswers = 1,
            totalQuestions = 5,
            completion = true,
            earnedXp = 30
        )

        assertEquals("quiz_test_101", result.quizId)
        assertEquals(80, result.score)
        assertEquals(1, result.attempts)
        assertEquals(4, result.correctAnswers)
        assertEquals(1, result.wrongAnswers)
        assertEquals(5, result.totalQuestions)
        assertTrue(result.completion)
        assertEquals(30, result.earnedXp)
    }

    @Test
    fun mockLearningRepository_submitQuizResult_awardsXpForPassingScore() = runBlocking {
        val repo = MockLearningRepositoryImpl()

        val passingQuiz = QuizResult(
            quizId = "q_pass",
            title = "VOCABULARY_QUIZ",
            score = 90,
            attempts = 1,
            correctAnswers = 9,
            wrongAnswers = 1,
            totalQuestions = 10
        )

        val submission = repo.submitQuizResult(passingQuiz)
        assertTrue(submission.isSuccess)

        val awarded = submission.getOrThrow()
        assertTrue(awarded.completion)
        assertTrue("Awarded XP should be greater than 0", awarded.earnedXp > 0)
    }

    @Test
    fun mockLearningRepository_submitQuizResult_blocksXpWhenFailingScore() = runBlocking {
        val repo = MockLearningRepositoryImpl()

        val failingQuiz = QuizResult(
            quizId = "q_fail",
            title = "VOCABULARY_QUIZ",
            score = 60, // below 70 passing threshold
            attempts = 1,
            correctAnswers = 6,
            wrongAnswers = 4,
            totalQuestions = 10
        )

        val submission = repo.submitQuizResult(failingQuiz)
        assertTrue(submission.isSuccess)

        val awarded = submission.getOrThrow()
        assertFalse(awarded.completion)
        assertEquals(0, awarded.earnedXp)
    }

    @Test
    fun mockDataProvider_hasSampleQuizQuestionsCoveringAllTypes() {
        val questions = MockDataProvider.sampleQuizQuestions
        assertTrue(questions.isNotEmpty())

        val types = questions.map { it.type }.toSet()
        assertTrue("Must include MULTIPLE_CHOICE", types.contains(QuizQuestionType.MULTIPLE_CHOICE))
        assertTrue("Must include FILL_IN_THE_BLANK", types.contains(QuizQuestionType.FILL_IN_THE_BLANK))
        assertTrue("Must include SENTENCE_CORRECTION", types.contains(QuizQuestionType.SENTENCE_CORRECTION))
        assertTrue("Must include TRANSLATION", types.contains(QuizQuestionType.TRANSLATION))
    }
}
