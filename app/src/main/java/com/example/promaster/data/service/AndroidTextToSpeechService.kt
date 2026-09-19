package com.example.promaster.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.promaster.domain.service.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Android TextToSpeech implementation with lifecycle safety and language resolution.
 */
class AndroidTextToSpeechService(
    private val context: Context?
) : TextToSpeechService, TextToSpeech.OnInitListener {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        if (context != null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                isInitialized = false
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        } else {
            isInitialized = false
        }
    }

    override fun isAvailable(): Boolean {
        return context != null && isInitialized && tts != null
    }

    override fun speak(text: String, language: String, onComplete: (() -> Unit)?) {
        if (!isAvailable()) {
            onComplete?.invoke()
            return
        }

        val locale = when (language.lowercase()) {
            "tamil", "ta" -> Locale.forLanguageTag("ta-IN")
            "hindi", "hi" -> Locale.forLanguageTag("hi-IN")
            "malayalam", "ml" -> Locale.forLanguageTag("ml-IN")
            "telugu", "te" -> Locale.forLanguageTag("te-IN")
            "kannada", "kn" -> Locale.forLanguageTag("kn-IN")
            "bengali", "bn" -> Locale.forLanguageTag("bn-IN")
            "spanish", "es" -> Locale.forLanguageTag("es-ES")
            "french", "fr" -> Locale.FRENCH
            "german", "de" -> Locale.GERMAN
            else -> Locale.ENGLISH
        }

        try {
            val prefs = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()
            val speed = prefs?.voiceSpeed ?: 1.0f
            val pitch = prefs?.voicePitch ?: 1.0f
            val targetGender = prefs?.voiceGender ?: "female"

            tts?.setSpeechRate(speed.coerceIn(0.5f, 1.5f))
            tts?.setPitch(pitch.coerceIn(0.5f, 1.5f))

            // Query and match real installed Android TTS voices
            val availableVoices = tts?.voices
            var matchedVoice: android.speech.tts.Voice? = null

            if (!availableVoices.isNullOrEmpty()) {
                val matchingLocaleVoices = availableVoices.filter { voice ->
                    voice.locale.language.equals(locale.language, ignoreCase = true)
                }

                if (matchingLocaleVoices.isNotEmpty()) {
                    matchedVoice = matchingLocaleVoices.firstOrNull { v ->
                        val nameLower = v.name.lowercase()
                        val features = v.features ?: emptySet()
                        val isFemaleMatch = targetGender == "female" && (nameLower.contains("female") || features.contains("gender=female"))
                        val isMaleMatch = targetGender == "male" && (nameLower.contains("male") || features.contains("gender=male"))
                        isFemaleMatch || isMaleMatch
                    } ?: matchingLocaleVoices.first() // Fallback to closest available voice for locale
                }
            }

            if (matchedVoice != null) {
                tts?.voice = matchedVoice
            } else {
                tts?.language = locale
            }

            val utteranceId = "promaster_${System.currentTimeMillis()}"
            _isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            _isSpeaking.value = false
            onComplete?.invoke()
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignored
        } finally {
            _isSpeaking.value = false
        }
    }

    override fun shutdown() {
        try {
            stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignored
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
