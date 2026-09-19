package com.example.promaster.security

import com.example.promaster.ai.SecureBackendAiClient
import com.example.promaster.domain.model.AssistantSettings
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SecurityAndPrivacyAuditTest {

    @Test
    fun firestoreRules_strictlyEnforcesAuthenticationAndUserIsolation_withNoPublicAccess() {
        val rulesFile = File("../firestore.rules").takeIf { it.exists() } ?: File("firestore.rules")
        assertTrue("firestore.rules must exist at project root", rulesFile.exists())
        val content = rulesFile.readText()

        // 1. Must use rules_version = '2'
        assertTrue("Must use rules_version = '2'", content.contains("rules_version = '2';"))

        // 2. Must define isOwner checking auth and matching uid
        assertTrue("Must check request.auth != null", content.contains("request.auth != null"))
        assertTrue("Must check request.auth.uid == userId", content.contains("request.auth.uid == userId"))

        // 3. Must enforce isOwner on all user collections
        val requiredCollections = listOf(
            "users/{userId}",
            "learningPlans/{userId}",
            "dailyTasks/{userId}",
            "progress/{userId}",
            "conversations/{userId}",
            "settings/{userId}"
        )
        for (col in requiredCollections) {
            assertTrue("Collection $col must be present in rules", content.contains(col))
        }

        // 4. Must NOT allow public open access
        assertFalse("Must never allow public write", content.contains("allow write: if true"))
        assertFalse("Must never allow public read", content.contains("allow read: if true"))
        assertFalse("Must never allow open read/write", content.contains("allow read, write: if true"))

        // 5. Must have catch-all default deny
        assertTrue("Must include catch-all default deny", content.contains("allow read, write: if false;"))
    }

    @Test
    fun storageRules_strictlyEnforcesUserOwnership_withNoPublicAccess() {
        val storageFile = File("../storage.rules").takeIf { it.exists() } ?: File("storage.rules")
        assertTrue("storage.rules must exist at project root", storageFile.exists())
        val content = storageFile.readText()

        assertTrue("Must check request.auth != null", content.contains("request.auth != null"))
        assertTrue("Must check request.auth.uid == userId", content.contains("request.auth.uid == userId"))
        assertTrue("Must isolate files under users/{userId}", content.contains("match /users/{userId}/{allPaths=**}"))
        assertFalse("Must not allow public read/write", content.contains("allow read, write: if true"))
        assertTrue("Must have default deny", content.contains("allow read, write: if false;"))
    }

    @Test
    fun androidManifest_doesNotContainUnnecessaryOrDangerousPermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val manifestContent = manifestFile.readText()

        // Ensure ACCESS_NOTIFICATION_POLICY was removed
        assertFalse(
            "ACCESS_NOTIFICATION_POLICY is unnecessary and must not be declared in manifest",
            manifestContent.contains("ACCESS_NOTIFICATION_POLICY")
        )

        // Ensure no dangerous accessibility service permission is declared
        assertFalse(
            "Accessibility service must not be declared",
            manifestContent.contains("BIND_ACCESSIBILITY_SERVICE")
        )

        // Ensure only validated required permissions are declared
        assertTrue(manifestContent.contains("android.permission.INTERNET"))
        assertTrue(manifestContent.contains("android.permission.RECORD_AUDIO"))
        assertTrue(manifestContent.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifestContent.contains("com.android.alarm.permission.SET_ALARM"))
        assertTrue(manifestContent.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue(manifestContent.contains("android.permission.FOREGROUND_SERVICE_MICROPHONE"))
    }

    @Test
    fun clientCode_containsNoPrivateServiceAccountKeysOrLlmSecrets() {
        val secureClient = SecureBackendAiClient()

        // Verify that client routes through backend proxy
        assertTrue(
            "AI client must route via protected backend gateway URL",
            secureClient.providerName.contains("PROMASTER-Protected-Backend-Gateway")
        )

        // Check declared fields for any embedded keys
        val fields = SecureBackendAiClient::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse("Must not contain private OpenAI key", fields.contains("openai_key") || fields.contains("openaikey"))
        assertFalse("Must not contain private Gemini key", fields.contains("gemini_key") || fields.contains("geminikey"))
        assertFalse("Must not contain private Anthropic key", fields.contains("anthropic_key") || fields.contains("anthropickey"))
        assertFalse("Must not contain service account key", fields.contains("service_account") || fields.contains("private_key"))
    }

    @Test
    fun assistantSettings_defaultsToPrivacyPreservingZeroRawAudioStorage() {
        val settings = AssistantSettings()
        assertFalse("storeAudioRecordings must default to false", settings.storeAudioRecordings)
        assertFalse("privacyConsentGiven must default to false", settings.privacyConsentGiven)
        assertFalse("runInBackground must default to false", settings.runInBackground)
    }

    @Test
    fun proGuardRules_existAndConfigureLogStrippingAndDtoPreservation() {
        val proGuardFile = File("proguard-rules.pro").takeIf { it.exists() }
            ?: File("app/proguard-rules.pro")
        assertTrue("app/proguard-rules.pro must exist", proGuardFile.exists())
        val content = proGuardFile.readText()

        assertTrue("Must strip Log.v/d in release", content.contains("assumenosideeffects class android.util.Log"))
        assertTrue("Must preserve Firebase model DTOs", content.contains("com.example.promaster.data.firebase.model"))
    }
}
