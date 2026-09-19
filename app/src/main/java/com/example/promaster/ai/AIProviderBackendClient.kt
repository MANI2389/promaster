package com.example.promaster.ai

import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.promaster.PromasterApp
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.domain.model.AIIntent
import com.example.promaster.domain.model.AIRequest
import com.example.promaster.domain.model.AIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pluggable AI provider abstraction.
 *
 * SECURITY: Secret API keys for LLM providers (e.g., Gemini, OpenAI, Claude)
 * MUST NEVER be included inside the Android client APK.
 * All live AI communication routes through a secure backend proxy
 * authenticated with client user session tokens.
 */
interface AIProviderBackendClient {
    val providerName: String
    suspend fun sendPrompt(request: AIRequest): Result<AIResponse>
    suspend fun isBackendReachable(): Boolean
}

/**
 * Secure Server Proxy Client.
 * Routes requests to a protected backend gateway using user authentication tokens.
 * Provider secrets remain safe on the backend.
 */
class SecureBackendAiClient(
    val backendGatewayUrl: String = resolveGatewayUrl(),
    private val userTokenProvider: suspend () -> String? = {
        try {
            FirebaseManager.auth?.currentUser?.getIdToken(false)?.await()?.token
        } catch (_: Exception) {
            null
        }
    }
) : AIProviderBackendClient {

    companion object {
        private const val TAG = "PROMASTER_AI"
        const val DEFAULT_LOCAL_GATEWAY_URL = "http://127.0.0.1:8000"
        const val DEFAULT_LAN_GATEWAY_URL = "http://192.168.1.6:8000"
        const val DEFAULT_EMULATOR_GATEWAY_URL = "http://10.0.2.2:8000"
        const val PRODUCTION_GATEWAY_URL = "https://ai.promaster.app"

        @Volatile
        var customGatewayUrl: String? = null

        fun isDebugMode(): Boolean {
            return try {
                val ctx = PromasterApp.applicationContextSafe
                val flags = ctx?.applicationInfo?.flags ?: 0
                (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (_: Throwable) {
                false
            }
        }

        fun resolveGatewayUrl(): String {
            customGatewayUrl?.let { return it }
            return if (isDebugMode()) {
                DEFAULT_LOCAL_GATEWAY_URL
            } else {
                PRODUCTION_GATEWAY_URL
            }
        }
    }

    override val providerName: String = "PROMASTER-Protected-Backend-Gateway"

    override suspend fun sendPrompt(request: AIRequest): Result<AIResponse> = withContext(Dispatchers.IO) {
        val primaryUrl = backendGatewayUrl.trimEnd('/')
        // In debug, if primary local reverse fails, can retry with LAN IP
        val candidateUrls = if (isDebugMode()) {
            listOfNotNull(primaryUrl, if (primaryUrl != DEFAULT_LAN_GATEWAY_URL) DEFAULT_LAN_GATEWAY_URL else null)
        } else {
            listOf(primaryUrl)
        }

        var lastException: Exception? = null
        val token = userTokenProvider()
        val tokenPresent = !token.isNullOrBlank()

        for (baseUrl in candidateUrls) {
            val endpoint = "$baseUrl/v1/ai/chat"
            try {
                Log.d(TAG, "AI Request started -> url: $endpoint, tokenPresent: $tokenPresent")

                val url = URL(endpoint)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    if (tokenPresent && token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                val modeStr = when (request.forcedIntent) {
                    AIIntent.GRAMMAR_CORRECTION -> "Grammar Correction"
                    AIIntent.LANGUAGE_TEACHING -> "Language Coach"
                    AIIntent.SPEAKING_PRACTICE -> "Speaking Feedback"
                    AIIntent.VOCABULARY_EXPLANATION -> "Vocabulary Help"
                    AIIntent.TRANSLATION -> "Translation"
                    AIIntent.MOTIVATION -> "Language Coach"
                    AIIntent.AI_FRIEND -> "AI Friend"
                    AIIntent.AI_ASSISTANT, AIIntent.COMMAND_INTENT_DETECTION -> "AI Assistant"
                    else -> "AI Friend"
                }

                val jsonPayload = JSONObject().apply {
                    put("message", request.prompt)
                    put("conversationId", request.conversationId)
                    put("targetLanguage", request.targetLanguage)
                    put("motherTongue", request.motherTongue)
                    put("level", request.userLevel.displayName)
                    put("mode", modeStr)
                }

                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "AI HTTP response code: $responseCode from $baseUrl")

                if (responseCode in 200..299) {
                    val responseString = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val jsonResponse = JSONObject(responseString)
                    val success = jsonResponse.optBoolean("success", true)
                    val reply = jsonResponse.optString("reply", "")
                    val isFallback = jsonResponse.optBoolean("isFallback", false)

                    if (!success) {
                        val safeMessage = reply.ifBlank { "AI is temporarily unavailable. Please try again later." }
                        Log.w(TAG, "Backend returned success=false: $safeMessage")
                        return@withContext Result.failure(RuntimeException(safeMessage))
                    }

                    // Parse structured mobile action if present
                    val actionObj = jsonResponse.optJSONObject("action")
                    val structuredAction = if (actionObj != null && actionObj.has("intent")) {
                        val intentStr = actionObj.optString("intent", "")
                        val appName = actionObj.optString("appName", "").takeIf { it.isNotBlank() }
                        val params = mutableMapOf<String, Any?>()
                        val paramsObj = actionObj.optJSONObject("parameters")
                        if (paramsObj != null) {
                            val keys = paramsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                params[key] = paramsObj.get(key)
                            }
                        }
                        val requiresConfirm = actionObj.optBoolean("requiresConfirmation", false)
                        com.example.promaster.domain.model.StructuredAction(
                            intent = intentStr,
                            appName = appName,
                            parameters = params,
                            requiresConfirmation = requiresConfirm
                        )
                    } else null

                    var correctionText: String? = null
                    var ruleExplanation: String? = null
                    val correctionsArray = jsonResponse.optJSONArray("corrections")
                    if (correctionsArray != null && correctionsArray.length() > 0) {
                        val firstCorrection = correctionsArray.getJSONObject(0)
                        val original = firstCorrection.optString("original", "")
                        val corrected = firstCorrection.optString("corrected", "")
                        correctionText = when {
                            corrected.isBlank() -> null
                            original.isNotBlank() && request.prompt.contains(original) -> request.prompt.replace(original, corrected)
                            else -> corrected
                        }
                        val explanation = firstCorrection.optString("explanation", "")
                        ruleExplanation = if (explanation.isNotBlank()) explanation else null
                    }

                    val suggestionsList = mutableListOf<String>()
                    val suggestionsArray = jsonResponse.optJSONArray("suggestions")
                    if (suggestionsArray != null) {
                        for (i in 0 until suggestionsArray.length()) {
                            suggestionsList.add(suggestionsArray.getString(i))
                        }
                    }

                    Log.d(TAG, "AI Response parsed successfully (length=${reply.length}, isFallback=$isFallback, hasAction=${structuredAction != null})")
                    return@withContext Result.success(
                        AIResponse(
                            message = reply,
                            intent = request.forcedIntent ?: if (structuredAction != null) AIIntent.AI_ASSISTANT else AIIntent.AI_FRIEND,
                            action = structuredAction,
                            correction = correctionText,
                            explanation = ruleExplanation,
                            nextAction = suggestionsList.firstOrNull(),
                            confidence = if (isFallback) 0.95f else 1.0f
                        )
                    )
                } else {
                    val errorStream = connection.errorStream
                    errorStream?.close()
                    val errorMessage = when (responseCode) {
                        401 -> "Authentication failed: Session expired or invalid credentials. Please login again."
                        403 -> "Authentication failed: Access restricted. Please verify your PROMASTER account."
                        422 -> "AI request was invalid."
                        429 -> "Too many requests. Please wait a moment."
                        408, 504 -> "AI request timed out. Please check your connection."
                        in 500..599 -> "AI service is temporarily unavailable. Please try again later."
                        else -> "AI request failed ($responseCode)."
                    }
                    Log.w(TAG, "AI HTTP error: $responseCode -> $errorMessage")
                    return@withContext Result.failure(RuntimeException(errorMessage))
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Connection failed to $baseUrl: ${e.javaClass.simpleName} - ${e.message}")
            }
        }

        val userMessage = when (lastException) {
            is java.net.ConnectException ->
                "AI service is temporarily unavailable. Please check your internet connection."
            is java.net.SocketTimeoutException ->
                "AI request timed out. Please try again."
            is java.net.UnknownHostException ->
                "No internet connection. Please try again."
            else ->
                lastException?.localizedMessage ?: "AI service is temporarily unavailable. Please try again."
        }
        Log.e(TAG, "All candidate AI endpoints failed: $userMessage", lastException)
        Result.failure(RuntimeException(userMessage))
    }

    override suspend fun isBackendReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${backendGatewayUrl.trimEnd('/')}/health")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Mock & Local Testing Provider Client.
 */
class MockAIProviderBackendClient(
    override val providerName: String = "Mock-AI-Local-Engine"
) : AIProviderBackendClient {

    override suspend fun sendPrompt(request: AIRequest): Result<AIResponse> {
        return Result.success(
            AIResponse(
                message = "Response for prompt: '${request.prompt}'",
                intent = request.forcedIntent ?: AIIntent.GENERAL_CONVERSATION,
                nextAction = "Practice next exercise"
            )
        )
    }

    override suspend fun isBackendReachable(): Boolean = true
}
