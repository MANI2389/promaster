package com.example.promaster.domain.engine

import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.repository.MockLearningRepositoryImpl
import com.example.promaster.domain.model.DifficultyLevel
import com.example.promaster.domain.model.ReviewStatus
import com.example.promaster.domain.model.VocabularyItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VocabularySystemTest {

    @Test
    fun vocabularyItem_supportsCompleteFields() {
        val item = VocabularyItem(
            id = "v1",
            word = "Desarrollar",
            translation = "To develop",
            meaning = "To grow, expand or build",
            phonetic = "/de.sa.roˈʝaɾ/",
            partOfSpeech = "Verb",
            exampleSentence = "Queremos desarrollar una aplicación moderna.",
            exampleTranslation = "We want to develop a modern application.",
            masteryLevel = 75,
            difficulty = DifficultyLevel.INTERMEDIATE,
            reviewStatus = ReviewStatus.LEARNING
        )

        assertEquals("Desarrollar", item.word)
        assertEquals("To develop", item.translation)
        assertEquals("To grow, expand or build", item.meaning)
        assertEquals(DifficultyLevel.INTERMEDIATE, item.difficulty)
        assertEquals(ReviewStatus.LEARNING, item.reviewStatus)
        assertEquals(75, item.masteryLevel)
    }

    @Test
    fun vocabularyItem_legacyConstructorCompatibility() {
        // Test secondary constructor without meaning, difficulty, reviewStatus
        val item = VocabularyItem(
            id = "legacy_1",
            word = "Hola",
            translation = "Hello",
            phonetic = "/ˈo.la/",
            partOfSpeech = "Interjection",
            exampleSentence = "¡Hola! ¿Cómo estás?",
            exampleTranslation = "Hello! How are you?",
            masteryLevel = 100
        )

        assertEquals("Hola", item.word)
        assertEquals("Hello", item.meaning) // defaults to translation
        assertEquals(DifficultyLevel.ADVANCED, item.difficulty)
        assertEquals(ReviewStatus.MASTERED, item.reviewStatus) // 100% mastery defaults to MASTERED

        val beginnerItem = VocabularyItem(
            id = "legacy_2",
            word = "Casa",
            phonetic = "/ˈka.sa/",
            translation = "House",
            partOfSpeech = "Noun",
            exampleSentence = "Mi casa es tu casa.",
            exampleTranslation = "My house is your house.",
            masteryLevel = 20
        )
        assertEquals(DifficultyLevel.BEGINNER, beginnerItem.difficulty)
        assertEquals(ReviewStatus.NEEDS_REVIEW, beginnerItem.reviewStatus)
    }

    @Test
    fun mockLearningRepository_updatesVocabularyStatusAndMastery() = runBlocking {
        val repo = MockLearningRepositoryImpl()
        val initialWords = repo.getVocabulary().first()
        val firstWord = initialWords.first()

        val updateResult = repo.updateVocabularyStatus(
            wordId = firstWord.id,
            status = ReviewStatus.NEEDS_REVIEW,
            mastery = 30
        )

        assertTrue(updateResult.isSuccess)

        val updatedWords = repo.getVocabulary().first()
        val updatedWord = updatedWords.first { it.id == firstWord.id }

        assertEquals(ReviewStatus.NEEDS_REVIEW, updatedWord.reviewStatus)
        assertEquals(30, updatedWord.masteryLevel)
    }

    @Test
    fun mockDataProvider_hasRichVocabularyData() {
        val vocabList = MockDataProvider.vocabularyList
        assertTrue(vocabList.size >= 10)

        val beginnerCount = vocabList.count { it.difficulty == DifficultyLevel.BEGINNER }
        val intermediateCount = vocabList.count { it.difficulty == DifficultyLevel.INTERMEDIATE }
        val advancedCount = vocabList.count { it.difficulty == DifficultyLevel.ADVANCED }

        assertTrue(beginnerCount > 0)
        assertTrue(intermediateCount > 0)
        assertTrue(advancedCount > 0)
        assertTrue(vocabList.all { it.meaning.isNotBlank() })
        assertTrue(vocabList.all { it.exampleSentence.isNotBlank() })
        assertTrue(vocabList.all { it.translation.isNotBlank() })
    }
}
