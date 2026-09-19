package com.example.promaster.ai

import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.LanguageLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SecureBackendAiClientTest {

    @Test
    fun secureBackendClient_usesSafeDefaults() {
        val client = SecureBackendAiClient()
        assertEquals("PROMASTER-Protected-Backend-Gateway", client.providerName)
        assertEquals(SecureBackendAiClient.PRODUCTION_GATEWAY_URL, client.backendGatewayUrl)
    }

    @Test
    fun secureBackendClient_allowsCustomGatewayUrl() {
        val customUrl = "https://ai.promaster.internal"
        val client = SecureBackendAiClient(backendGatewayUrl = customUrl)
        assertEquals(customUrl, client.backendGatewayUrl)
    }

    @Test
    fun secureBackendClient_containsZeroHardcodedSecrets() {
        val fields = SecureBackendAiClient::class.java.declaredFields
        val fieldNames = fields.map { it.name.lowercase() }

        assertFalse("Should not contain gemini_key", fieldNames.any { it.contains("gemini") })
        assertFalse("Should not contain openai_key", fieldNames.any { it.contains("openai") })
        assertFalse("Should not contain claude_key", fieldNames.any { it.contains("claude") })
        assertFalse("Should not contain apikey", fieldNames.any { it.contains("apikey") })
    }

    @Test
    fun isBackendReachable_returnsFalseWhenOffline() = runBlocking {
        // Connect to a non-existent port on localhost
        val client = SecureBackendAiClient(backendGatewayUrl = "http://127.0.0.1:59999")
        val reachable = client.isBackendReachable()
        assertFalse(reachable)
    }

    @Test
    fun sendPrompt_returnsFailureWhenBackendIsOffline() = runBlocking {
        val client = SecureBackendAiClient(backendGatewayUrl = "http://127.0.0.1:59999")
        val request = AIRequest(
            prompt = "Hello AI",
            conversationId = "conv_test",
            targetLanguage = "English",
            motherTongue = "Tamil",
            userLevel = LanguageLevel.BEGINNER,
            forcedIntent = AIIntent.GENERAL_CONVERSATION
        )
        val result = client.sendPrompt(request)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun secureBackendClient_supportsConfigurableGatewayForPhysicalDevices() {
        try {
            val lanUrl = "http://192.168.1.50:8000"
            SecureBackendAiClient.customGatewayUrl = lanUrl
            assertEquals(lanUrl, SecureBackendAiClient.resolveGatewayUrl())
            val client = SecureBackendAiClient()
            assertEquals(lanUrl, client.backendGatewayUrl)
        } finally {
            SecureBackendAiClient.customGatewayUrl = null
        }
    }

    @Test
    fun isBackendReachable_connectsToLiveBackendOnPort8000() = runBlocking {
        val client = SecureBackendAiClient(backendGatewayUrl = "http://127.0.0.1:8000")
        val reachable = client.isBackendReachable()
        assertTrue("Live local backend on port 8000 should be reachable", reachable)
    }

    @Test
    fun sendPrompt_liveChatWithRunningBackend() = runBlocking {
        val client = SecureBackendAiClient(
            backendGatewayUrl = "http://127.0.0.1:8000",
            userTokenProvider = { "test_android_e2e_token_123" }
        )
        val request = AIRequest(
            prompt = "Hello PROMASTER, introduce yourself in one short sentence.",
            conversationId = "android_e2e_test",
            targetLanguage = "English",
            motherTongue = "Tamil",
            userLevel = LanguageLevel.BEGINNER,
            forcedIntent = AIIntent.GENERAL_CONVERSATION
        )
        val result = client.sendPrompt(request)
        if (result.isSuccess) {
            val response = result.getOrNull()
            assertNotNull(response)
            assertTrue(response!!.message.isNotEmpty())
        } else {
            val error = result.exceptionOrNull()?.message ?: ""
            assertTrue(
                "Expected live success or clean server configuration message, got: $error",
                error.contains("Gemini API is not configured") || error.contains("temporarily unavailable")
            )
        }
    }

    @Test
    fun sendPrompt_liveGrammarCorrectionWithRunningBackend() = runBlocking {
        val client = SecureBackendAiClient(
            backendGatewayUrl = "http://127.0.0.1:8000",
            userTokenProvider = { "test_android_e2e_token_123" }
        )
        val request = AIRequest(
            prompt = "I am go to college yesterday.",
            conversationId = "android_e2e_grammar",
            targetLanguage = "English",
            motherTongue = "Tamil",
            userLevel = LanguageLevel.BEGINNER,
            forcedIntent = AIIntent.GRAMMAR_CORRECTION
        )
        val result = client.sendPrompt(request)
        if (result.isSuccess) {
            val response = result.getOrNull()
            assertNotNull(response)
            val correction = response!!.correction
            assertNotNull("Correction should not be null", correction)
            val matchesExpected = correction == "I went to college yesterday." || correction?.contains("went", ignoreCase = true) == true
            assertTrue("Expected correction with 'went', got: '$correction'", matchesExpected)
            assertNotNull(response.explanation)
        } else {
            val error = result.exceptionOrNull()?.message ?: ""
            assertTrue(
                "Expected live success or clean server configuration message, got: $error",
                error.contains("Gemini API is not configured") || error.contains("temporarily unavailable")
            )
        }
    }

    @Test
    fun sendPrompt_unauthenticatedReturnsAuthError() = runBlocking {
        val client = SecureBackendAiClient(
            backendGatewayUrl = "http://127.0.0.1:8000",
            userTokenProvider = { null }
        )
        val request = AIRequest(
            prompt = "Hello",
            conversationId = "android_e2e_unauth",
            targetLanguage = "English",
            motherTongue = "Tamil",
            userLevel = LanguageLevel.BEGINNER,
            forcedIntent = AIIntent.GENERAL_CONVERSATION
        )
        val result = client.sendPrompt(request)
        assertTrue("Missing token should result in failure", result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Should contain authentication error message", errorMsg.contains("Authentication failed"))
    }
}
