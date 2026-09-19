package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.DailyTaskDocument
import com.example.promaster.data.mock.MockDataProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreDailyTaskRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) {
    private fun getTasksSubcollection(userId: String) =
        firestore?.collection("dailyTasks")?.document(userId)?.collection("tasks")

    suspend fun saveTask(userId: String, taskDoc: DailyTaskDocument): FirebaseResult<Unit> {
        val sub = getTasksSubcollection(userId) ?: return FirebaseResult.Success(Unit)
        return try {
            sub.document(taskDoc.taskId).set(taskDoc, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    suspend fun getTasksForDay(userId: String, dayNumber: Int): FirebaseResult<List<DailyTaskDocument>> {
        val sub = getTasksSubcollection(userId) ?: run {
            val fallback = MockDataProvider.dailyTasks.map { DailyTaskDocument.fromDomain(it, dayNumber) }
            return FirebaseResult.Success(fallback)
        }
        return try {
            val querySnapshot = sub
                .whereEqualTo("dayNumber", dayNumber)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                seedInitialTasksIfEmpty(userId, dayNumber)
                val defaultTasks = MockDataProvider.dailyTasks.map { DailyTaskDocument.fromDomain(it, dayNumber) }
                FirebaseResult.Success(defaultTasks)
            } else {
                val tasks = querySnapshot.documents.mapNotNull { it.toObject(DailyTaskDocument::class.java) }
                FirebaseResult.Success(tasks)
            }
        } catch (e: Exception) {
            val fallback = MockDataProvider.dailyTasks.map { DailyTaskDocument.fromDomain(it, dayNumber) }
            FirebaseResult.Offline(fallback)
        }
    }

    fun getTasksFlow(userId: String, dayNumber: Int): Flow<List<DailyTaskDocument>> = callbackFlow {
        val collection = try { getTasksSubcollection(userId) } catch (_: Exception) { null }
        if (collection == null) {
            trySend(MockDataProvider.dailyTasks.map { DailyTaskDocument.fromDomain(it, dayNumber) })
            close()
            return@callbackFlow
        }

        val registration = collection
            .whereEqualTo("dayNumber", dayNumber)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(MockDataProvider.dailyTasks.map { DailyTaskDocument.fromDomain(it, dayNumber) })
                } else {
                    val tasks = snapshot.documents.mapNotNull { it.toObject(DailyTaskDocument::class.java) }
                    trySend(tasks)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun completeTask(userId: String, taskId: String, score: Int = 100): FirebaseResult<Unit> {
        val sub = getTasksSubcollection(userId) ?: return FirebaseResult.Success(Unit)
        return try {
            val updates = mapOf(
                "status" to "COMPLETED",
                "score" to score,
                "completedAt" to System.currentTimeMillis()
            )
            sub.document(taskId).set(updates, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    suspend fun incrementRetry(userId: String, taskId: String): FirebaseResult<Unit> {
        val sub = getTasksSubcollection(userId) ?: return FirebaseResult.Success(Unit)
        return try {
            val taskRef = sub.document(taskId)
            val doc = taskRef.get().await()
            val currentRetry = doc.getLong("retryCount")?.toInt() ?: 0
            taskRef.update("retryCount", currentRetry + 1).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    suspend fun seedInitialTasksIfEmpty(userId: String, dayNumber: Int = 1): FirebaseResult<Unit> {
        val db = firestore ?: return FirebaseResult.Success(Unit)
        val sub = getTasksSubcollection(userId) ?: return FirebaseResult.Success(Unit)
        return try {
            val batch = db.batch()
            MockDataProvider.dailyTasks.forEach { task ->
                val doc = DailyTaskDocument.fromDomain(task, dayNumber)
                val docRef = sub.document(doc.taskId)
                batch.set(docRef, doc, SetOptions.merge())
            }
            batch.commit().await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }
}
