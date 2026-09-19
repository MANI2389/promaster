package com.example.promaster.data.firebase.repository

import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.data.firebase.common.FirebaseResult
import com.example.promaster.data.firebase.model.DayPlanItem
import com.example.promaster.data.firebase.model.LearningPlanDocument
import com.example.promaster.data.mock.MockDataProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreLearningPlanRepository(
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore
) {
    private val plansCollection get() = firestore?.collection("learningPlans")

    suspend fun savePlan(planDoc: LearningPlanDocument): FirebaseResult<Unit> {
        val col = plansCollection ?: return FirebaseResult.Success(Unit)
        return try {
            col.document(planDoc.userId).set(planDoc, SetOptions.merge()).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    suspend fun getPlan(userId: String): FirebaseResult<LearningPlanDocument> {
        val col = plansCollection ?: return FirebaseResult.Success(createDefaultPlan(userId))
        return try {
            val snapshot = col.document(userId).get().await()
            if (snapshot.exists()) {
                val doc = snapshot.toObject(LearningPlanDocument::class.java) ?: createDefaultPlan(userId)
                FirebaseResult.Success(doc)
            } else {
                FirebaseResult.Success(createDefaultPlan(userId))
            }
        } catch (e: Exception) {
            FirebaseResult.Offline(createDefaultPlan(userId))
        }
    }

    fun getPlanFlow(userId: String): Flow<FirebaseResult<LearningPlanDocument>> = callbackFlow {
        val docRef = try { plansCollection?.document(userId) } catch (_: Exception) { null }
        if (docRef == null) {
            trySend(FirebaseResult.Offline(createDefaultPlan(userId)))
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(FirebaseResult.Offline(createDefaultPlan(userId)))
            } else {
                val doc = snapshot.toObject(LearningPlanDocument::class.java) ?: createDefaultPlan(userId)
                trySend(FirebaseResult.Success(doc))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateCurrentDay(userId: String, day: Int): FirebaseResult<Unit> {
        val col = plansCollection ?: return FirebaseResult.Success(Unit)
        return try {
            col.document(userId).update("currentDay", day).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseManager.handleException(e)
        }
    }

    private fun createDefaultPlan(userId: String): LearningPlanDocument {
        val days = MockDataProvider.thirtyDayPlan.map { DayPlanItem.fromDomain(it) }
        return LearningPlanDocument(
            planId = "plan_30d_$userId",
            userId = userId,
            targetLanguage = MockDataProvider.currentUser.targetLanguage,
            level = "Beginner",
            duration = MockDataProvider.currentUser.dailyGoalMinutes,
            startDate = System.currentTimeMillis(),
            currentDay = 1,
            days = days
        )
    }
}
