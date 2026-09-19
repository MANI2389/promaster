package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.ConversationMessageDocument
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.preferences.UserPreferencesRepository
import com.example.promaster.domain.model.AiMessage
import com.example.promaster.domain.model.LanguageLevel
import com.example.promaster.domain.model.LearnerMemory
import com.example.promaster.domain.repository.ConversationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreConversationRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) : ConversationRepository {

    private fun getMessagesCollection(userId: String) =
        firestore?.collection("conversations")?.document(userId)?.collection("messages")

    private val localMemoryState = MutableStateFlow<LearnerMemory?>(null)

    fun getMessagesFlow(userId: String): Flow<List<ConversationMessageDocument>> = callbackFlow {
        val collection = try { getMessagesCollection(userId) } catch (_: Exception) { null }
        if (collection == null) {
            trySend(MockDataProvider.friendMessages.map { ConversationMessageDocument.fromDomain(it) })
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(MockDataProvider.friendMessages.map { ConversationMessageDocument.fromDomain(it) })
                } else {
                    val messages = snapshot.documents.mapNotNull {
                        it.toObject(ConversationMessageDocument::class.java)
                    }
                    if (messages.isNotEmpty()) {
                        trySend(messages)
                    } else {
                        trySend(MockDataProvider.friendMessages.map { ConversationMessageDocument.fromDomain(it) })
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(userId: String, message: ConversationMessageDocument): FirebaseResult<Unit> {
        val db = firestore
        val col = getMessagesCollection(userId)
        val docRef = if (message.messageId.isNotBlank()) col?.document(message.messageId) else col?.document()
        val finalDoc = message.copy(messageId = docRef?.id ?: message.messageId)

        if (db == null || col == null || docRef == null) {
            MockDataProvider.friendMessages.add(finalDoc.toDomain())
            return FirebaseResult.Success(Unit)
        }

        return try {
            docRef.set(finalDoc, SetOptions.merge()).await()
            MockDataProvider.friendMessages.add(finalDoc.toDomain())
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            MockDataProvider.friendMessages.add(finalDoc.toDomain())
            FirebaseResult.Success(Unit)
        }
    }

    override fun getConversationMessages(userId: String): Flow<List<AiMessage>> {
        return getMessagesFlow(userId).map { docs ->
            if (docs.isEmpty()) {
                MockDataProvider.friendMessages
            } else {
                docs.map { it.toDomain() }
            }
        }
    }

    override suspend fun saveMessage(userId: String, message: AiMessage): Result<Unit> {
        val doc = ConversationMessageDocument.fromDomain(message)
        sendMessage(userId, doc)
        return Result.success(Unit)
    }

    override suspend fun clearHistory(userId: String): Result<Unit> {
        val db = firestore
        val col = getMessagesCollection(userId)
        MockDataProvider.friendMessages.clear()

        if (db == null || col == null) return Result.success(Unit)

        return try {
            val snapshot = col.get().await()
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun getLearnerMemory(userId: String): Flow<LearnerMemory> {
        if (localMemoryState.value == null) {
            val prefs = UserPreferencesRepository.getInstanceOrNull()
            val level = try {
                LanguageLevel.valueOf(prefs?.userLevel ?: "BEGINNER")
            } catch (_: Exception) {
                LanguageLevel.BEGINNER
            }
            localMemoryState.value = LearnerMemory(
                userId = userId,
                targetLanguage = prefs?.targetLanguageName ?: MockDataProvider.currentUser.targetLanguage,
                motherTongue = prefs?.motherTongueName ?: MockDataProvider.currentUser.nativeLanguage,
                level = level,
                currentDay = prefs?.cachedCurrentDay ?: MockDataProvider.currentUser.currentDay,
                streakDays = prefs?.cachedStreak ?: MockDataProvider.currentUser.streakDays,
                weakAreas = listOf("Past Tense Conjugation", "Speaking Cadence")
            )
        }
        return localMemoryState.filterNotNull()
    }

    override suspend fun updateLearnerMemory(userId: String, memory: LearnerMemory): Result<Unit> {
        localMemoryState.value = memory
        return Result.success(Unit)
    }

    override suspend fun recordWeakArea(userId: String, weakArea: String): Result<Unit> {
        val current = localMemoryState.value ?: return Result.success(Unit)
        if (!current.weakAreas.contains(weakArea)) {
            localMemoryState.value = current.copy(weakAreas = current.weakAreas + weakArea)
        }
        return Result.success(Unit)
    }

    override suspend fun recordLessonCompleted(userId: String, lessonTitle: String): Result<Unit> {
        val current = localMemoryState.value ?: return Result.success(Unit)
        if (!current.completedLessons.contains(lessonTitle)) {
            localMemoryState.value = current.copy(completedLessons = current.completedLessons + lessonTitle)
        }
        return Result.success(Unit)
    }

    suspend fun seedInitialMessagesIfEmpty(userId: String): FirebaseResult<Unit> {
        val db = firestore ?: return FirebaseResult.Success(Unit)
        val col = getMessagesCollection(userId) ?: return FirebaseResult.Success(Unit)
        return try {
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                MockDataProvider.friendMessages.forEach { msg ->
                    val doc = ConversationMessageDocument.fromDomain(msg)
                    val docRef = col.document(doc.messageId)
                    batch.set(docRef, doc, SetOptions.merge())
                }
                batch.commit().await()
            }
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }
}
