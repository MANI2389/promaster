package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.ProgressDocument
import com.example.promaster.data.mock.MockDataProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.example.promaster.domain.engine.StreakCalculationEngine

class FirestoreProgressRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) {
    private val progressCollection get() = firestore?.collection("progress")

    suspend fun saveProgress(userId: String, progressDoc: ProgressDocument): FirebaseResult<Unit> {
        val col = progressCollection ?: run {
            MockDataProvider.progressState = progressDoc.toDomain()
            return FirebaseResult.Success(Unit)
        }
        return try {
            col.document(userId).set(progressDoc, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            MockDataProvider.progressState = progressDoc.toDomain()
            FirebaseManager.handleException(e)
        }
    }

    suspend fun getProgress(userId: String): FirebaseResult<ProgressDocument> {
        val col = progressCollection ?: run {
            return FirebaseResult.Success(ProgressDocument.fromDomain(MockDataProvider.progressState))
        }
        return try {
            val snapshot = col.document(userId).get().await()
            if (snapshot.exists()) {
                val doc = snapshot.toObject(ProgressDocument::class.java) ?: ProgressDocument.fromDomain(MockDataProvider.progressState)
                FirebaseResult.Success(doc)
            } else {
                val initial = ProgressDocument.fromDomain(MockDataProvider.progressState)
                saveProgress(userId, initial)
                FirebaseResult.Success(initial)
            }
        } catch (e: Exception) {
            val fallback = ProgressDocument.fromDomain(MockDataProvider.progressState)
            FirebaseResult.Offline(fallback)
        }
    }

    fun getProgressFlow(userId: String): Flow<FirebaseResult<ProgressDocument>> = callbackFlow {
        val docRef = try { progressCollection?.document(userId) } catch (_: Exception) { null }
        if (docRef == null) {
            trySend(FirebaseResult.Offline(ProgressDocument.fromDomain(MockDataProvider.progressState)))
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(FirebaseResult.Offline(ProgressDocument.fromDomain(MockDataProvider.progressState)))
            } else {
                val doc = snapshot.toObject(ProgressDocument::class.java) ?: ProgressDocument.fromDomain(MockDataProvider.progressState)
                trySend(FirebaseResult.Success(doc))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun recordActivity(userId: String, xpEarned: Int, category: String, score: Int): FirebaseResult<Unit> {
        val col = progressCollection ?: run {
            val current = MockDataProvider.progressState
            val streakResult = StreakCalculationEngine.calculateStreak(
                currentStreak = current.currentStreak,
                longestStreak = current.longestStreak,
                lastActivityTimestamp = current.lastActivityDate
            )
            MockDataProvider.progressState = current.copy(
                totalXp = current.totalXp + xpEarned,
                currentStreak = streakResult.currentStreak,
                longestStreak = streakResult.longestStreak,
                bestStreak = streakResult.longestStreak,
                lastActivityDate = streakResult.lastActivityDate
            )
            return FirebaseResult.Success(Unit)
        }
        return try {
            val docRef = col.document(userId)
            val currentDoc: ProgressDocument = getProgress(userId).getOrNull() ?: ProgressDocument()

            val streakResult = StreakCalculationEngine.calculateStreak(
                currentStreak = currentDoc.streak,
                longestStreak = currentDoc.longestStreak,
                lastActivityTimestamp = currentDoc.lastActivityDate
            )

            val newXp = currentDoc.xp + xpEarned
            val newCompleted = currentDoc.completedTasks + 1
            val newGrammar = if (category == "GRAMMAR") maxOf(currentDoc.grammarScore, score) else currentDoc.grammarScore
            val newVocab = if (category == "VOCABULARY") maxOf(currentDoc.vocabularyScore, score) else currentDoc.vocabularyScore
            val newSpeaking = if (category == "SPEAKING") maxOf(currentDoc.speakingScore, score) else currentDoc.speakingScore
            val overall = ((newGrammar + newVocab + newSpeaking) / 300.0).coerceIn(0.0, 1.0)

            val updated = currentDoc.copy(
                xp = newXp,
                streak = streakResult.currentStreak,
                longestStreak = streakResult.longestStreak,
                completedTasks = newCompleted,
                grammarScore = newGrammar,
                vocabularyScore = newVocab,
                speakingScore = newSpeaking,
                overallProgress = overall,
                lastActivityDate = streakResult.lastActivityDate
            )

            docRef.set(updated, SetOptions.merge()).await()
            MockDataProvider.progressState = updated.toDomain()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }
}
