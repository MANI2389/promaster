package com.example.promaster.firebase

interface FirebaseAuthService {
    suspend fun signInWithGoogle(): Result<String>
    suspend fun syncUserProfile(userId: String)
    fun isCloudSyncEnabled(): Boolean
}

interface FirestoreSyncService {
    suspend fun backupDailyProgress(userId: String, data: Map<String, Any>)
    suspend fun pullLearningMilestones(userId: String): Result<List<String>>
}

interface FirebaseAnalyticsService {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setTargetLanguageProperty(language: String)
}

class MockFirebaseAuthService : FirebaseAuthService {
    override suspend fun signInWithGoogle(): Result<String> = Result.success("mock_firebase_uid_123")
    override suspend fun syncUserProfile(userId: String) {}
    override fun isCloudSyncEnabled(): Boolean = false
}

class MockFirestoreSyncService : FirestoreSyncService {
    override suspend fun backupDailyProgress(userId: String, data: Map<String, Any>) {}
    override suspend fun pullLearningMilestones(userId: String): Result<List<String>> = Result.success(emptyList())
}

class MockFirebaseAnalyticsService : FirebaseAnalyticsService {
    override fun logEvent(name: String, params: Map<String, Any>) {}
    override fun setTargetLanguageProperty(language: String) {}
}
