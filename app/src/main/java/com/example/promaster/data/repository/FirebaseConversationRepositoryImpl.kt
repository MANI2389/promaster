package com.example.promaster.data.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.ConversationMessageDocument
import com.example.promaster.data.firebase.repository.FirestoreConversationRepository
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.LanguageLevel
import com.example.promaster.domain.model.LearnerMemory
import com.example.promaster.domain.repository.ConversationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseConversationRepositoryImpl(
    private val firestoreConversationRepo: FirestoreConversationRepository = FirestoreConversationRepository(),
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) : ConversationRepository {

    override fun getConversationMessages(userId: String): Flow<List<AiMessage>> {
        return firestoreConversationRepo.getConversationMessages(userId)
    }

    override suspend fun saveMessage(userId: String, message: AiMessage): Result<Unit> {
        return firestoreConversationRepo.saveMessage(userId, message)
    }

    override suspend fun clearHistory(userId: String): Result<Unit> {
        return firestoreConversationRepo.clearHistory(userId)
    }

    override fun getLearnerMemory(userId: String): Flow<LearnerMemory> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(createDefaultMemory(userId))
            close()
            return@callbackFlow
        }

        val docRef = db.collection("learners").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(createDefaultMemory(userId))
            } else {
                try {
                    val memory = LearnerMemory(
                        userId = userId,
                        targetLanguage = snapshot.getString("targetLanguage") ?: "Spanish",
                        motherTongue = snapshot.getString("motherTongue") ?: "English",
                        level = LanguageLevel.values().firstOrNull { it.name == snapshot.getString("level") } ?: LanguageLevel.BEGINNER,
                        currentDay = snapshot.getLong("currentDay")?.toInt() ?: 1,
                        progressPercentage = snapshot.getLong("progressPercentage")?.toInt() ?: 0,
                        streakDays = snapshot.getLong("streakDays")?.toInt() ?: 1,
                        weakAreas = (snapshot.get("weakAreas") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        completedLessons = (snapshot.get("completedLessons") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        lastEncouragementType = snapshot.getString("lastEncouragementType") ?: "",
                        updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    trySend(memory)
                } catch (e: Exception) {
                    trySend(createDefaultMemory(userId))
                }
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateLearnerMemory(userId: String, memory: LearnerMemory): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val map = hashMapOf(
                "userId" to userId,
                "targetLanguage" to memory.targetLanguage,
                "motherTongue" to memory.motherTongue,
                "level" to memory.level.name,
                "currentDay" to memory.currentDay,
                "progressPercentage" to memory.progressPercentage,
                "streakDays" to memory.streakDays,
                "weakAreas" to memory.weakAreas,
                "completedLessons" to memory.completedLessons,
                "lastEncouragementType" to memory.lastEncouragementType,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("learners").document(userId).set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordWeakArea(userId: String, weakArea: String): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val docRef = db.collection("learners").document(userId)
            val snap = docRef.get().await()
            val currentWeak = (snap.get("weakAreas") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (!currentWeak.contains(weakArea)) {
                val updated = currentWeak + weakArea
                docRef.set(mapOf("weakAreas" to updated, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordLessonCompleted(userId: String, lessonTitle: String): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val docRef = db.collection("learners").document(userId)
            val snap = docRef.get().await()
            val currentCompleted = (snap.get("completedLessons") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (!currentCompleted.contains(lessonTitle)) {
                val updated = currentCompleted + lessonTitle
                docRef.set(mapOf("completedLessons" to updated, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createDefaultMemory(userId: String): LearnerMemory {
        return LearnerMemory(
            userId = userId,
            targetLanguage = MockDataProvider.currentUser.targetLanguage.ifBlank { "Spanish" },
            motherTongue = MockDataProvider.currentUser.nativeLanguage.ifBlank { "English" },
            level = LanguageLevel.BEGINNER,
            currentDay = 1,
            progressPercentage = 15,
            streakDays = 7,
            weakAreas = listOf("Past Tense Conjugation", "Ser vs Estar"),
            completedLessons = listOf("Present Tense Regular Verbs", "Day 1 Greetings")
        )
    }
}
