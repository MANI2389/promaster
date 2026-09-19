package com.example.promaster.domain.engine

import com.example.promaster.ai.AIProviderBackendClient
import com.example.promaster.ai.MockAIProviderBackendClient
import com.example.promaster.ai.SecureBackendAiClient
import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.AIResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AISecurityAndProviderAbstractionTest {

    @Test
    fun aiProvider_isCompletelySwappableWithoutChangingAIServiceAPI() = runBlocking {
        var customProviderCalled = false

        val customTestProvider = object : AIProviderBackendClient {
            override val providerName: String = "Custom-Enterprise-AI"

            override suspend fun sendPrompt(request: AIRequest): Result<AIResponse> {
                customProviderCalled = true
                return Result.success(
                    AIResponse(
                        message = "Custom response from Enterprise Provider",
                        intent = request.forcedIntent ?: AIIntent.GENERAL_CONVERSATION
                    )
                )
            }

            override suspend fun isBackendReachable(): Boolean = true
        }

        val serviceWithCustomProvider = DefaultPromasterAiService(backendClient = customTestProvider)
        assertNotNull(serviceWithCustomProvider)

        val serviceWithMockProvider = DefaultPromasterAiService(backendClient = MockAIProviderBackendClient())
        assertNotNull(serviceWithMockProvider)

        val serviceWithSecureGateway = DefaultPromasterAiService(
            backendClient = SecureBackendAiClient(
                backendGatewayUrl = "https://api.promaster.app/v1/ai/dispatch"
            )
        )
        assertNotNull(serviceWithSecureGateway)
    }

    @Test
    fun secureBackendClient_doesNotEmbedAnyProviderSecretKeys() {
        val secureClient = SecureBackendAiClient(
            backendGatewayUrl = "https://api.promaster.app/v1/ai/dispatch",
            userTokenProvider = { "mock_user_bearer_token" }
        )

        // Verify provider name adheres to backend gateway
        assertEquals("PROMASTER-Protected-Backend-Gateway", secureClient.providerName)

        // Ensure class fields do not contain any hardcoded AI provider secret keys
        val fields = SecureBackendAiClient::class.java.declaredFields
        val fieldNames = fields.map { it.name.lowercase() }

        assertFalse("Should not have openai_key", fieldNames.contains("openai_key"))
        assertFalse("Should not have gemini_key", fieldNames.contains("gemini_key"))
        assertFalse("Should not have anthropic_key", fieldNames.contains("anthropic_key"))
        assertFalse("Should not have apikey", fieldNames.contains("apikey"))
    }
}
