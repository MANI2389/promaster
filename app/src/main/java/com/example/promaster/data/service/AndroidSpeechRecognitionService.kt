package com.example.promaster.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.promaster.PromasterApp
import com.example.promaster.domain.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Hardened Android SpeechRecognizer service.
 * Enforces MainLooper thread affinity, checks runtime permissions,
 * and maps all Android SpeechRecognizer errors to clear, actionable messages.
 */
class AndroidSpeechRecognitionService(
    private val context: Context? = PromasterApp.applicationContextSafe
) : SpeechRecognitionService {

    companion object {
        private const val TAG = "PROMASTER_SPEECH"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private fun resolveContext(): Context? {
        return context ?: PromasterApp.applicationContextSafe
    }

    override fun isAvailable(): Boolean {
        val ctx = resolveContext() ?: return false
        return try {
            SpeechRecognizer.isRecognitionAvailable(ctx)
        } catch (e: Exception) {
            Log.e(TAG, "isRecognitionAvailable check failed", e)
            false
        }
    }

    override fun startListening(
        language: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ctx = resolveContext()
        if (ctx == null) {
            _isListening.value = false
            onError("Speech service unavailable: application context is missing.")
            return
        }

        // 1. Permission validation gate
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _isListening.value = false
            onError("Microphone permission is required. Please grant permission in Settings.")
            return
        }

        // 2. Availability validation gate
        if (!isAvailable()) {
            _isListening.value = false
            onError("Speech recognition service is not available on this device.")
            return
        }

        // 3. Must execute on Main thread
        mainHandler.post {
            try {
                // Destroy previous instance cleanly
                speechRecognizer?.destroy()
                speechRecognizer = null

                val recognizer = SpeechRecognizer.createSpeechRecognizer(ctx.applicationContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO ->
                                "Audio recording error. Please check your microphone."
                            SpeechRecognizer.ERROR_CLIENT ->
                                "Speech recognition client error. Please try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission is required. Please grant permission in Settings."
                            SpeechRecognizer.ERROR_NETWORK ->
                                "Network connection error. Speech recognition requires an active network."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Network timed out. Please check your internet connection."
                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "No speech detected. Please speak clearly into the microphone."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Speech recognizer is busy. Please wait a moment and try again."
                            SpeechRecognizer.ERROR_SERVER ->
                                "Server error during speech recognition. Please try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "No speech heard. Please try speaking again."
                            else ->
                                "Speech recognition error ($error). Please try again."
                        }
                        Log.w(TAG, "SpeechRecognizer error code: $error -> $errorMessage")
                        onError(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: ""
                        Log.d(TAG, "onResults recognized: '$spokenText'")
                        if (spokenText.isNotBlank()) {
                            onResult(spokenText)
                        } else {
                            onError("No speech recognized. Please speak clearly.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = partialMatches?.firstOrNull()
                        if (!partialText.isNullOrBlank()) {
                            Log.d(TAG, "Partial speech: '$partialText'")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val resolvedLanguageTag = when (language.lowercase().trim()) {
                    "tamil", "ta" -> "ta-IN"
                    "hindi", "hi" -> "hi-IN"
                    "malayalam", "ml" -> "ml-IN"
                    "telugu", "te" -> "te-IN"
                    "kannada", "kn" -> "kn-IN"
                    "bengali", "bn" -> "bn-IN"
                    "spanish", "es" -> "es-ES"
                    "french", "fr" -> "fr-FR"
                    "german", "de" -> "de-DE"
                    "english", "en" -> "en-US"
                    else -> language.ifBlank { Locale.getDefault().toLanguageTag() }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLanguageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLanguageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                recognizer.startListening(intent)
                Log.d(TAG, "startListening invoked with language tag: $resolvedLanguageTag")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting speech recognizer", e)
                _isListening.value = false
                onError("Failed to initialize speech recognizer: ${e.localizedMessage}")
            }
        }
    }

    override fun stopListening() {
        _isListening.value = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Exception during stopListening", e)
            } finally {
                speechRecognizer = null
            }
        }
    }
}
