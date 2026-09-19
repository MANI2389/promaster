package com.example.promaster.data.service

import com.example.promaster.domain.service.TranslationService

class DefaultTranslationService(
    private var available: Boolean = true
) : TranslationService {

    override fun isAvailable(): Boolean = available

    fun setAvailable(isAvail: Boolean) {
        this.available = isAvail
    }

    override suspend fun translate(
        text: String,
        fromLanguage: String,
        toLanguage: String
    ): Result<String> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }

        val lower = text.trim().lowercase()
        val translation = when {
            lower.contains("i went to college yesterday") -> {
                when (toLanguage.lowercase()) {
                    "tamil" -> "நான் நேற்று கல்லூரிக்குச் சென்றேன்."
                    "hindi" -> "मैं कल कॉलेज गया था।"
                    "spanish" -> "Ayer fui a la universidad."
                    else -> text
                }
            }
            lower.contains("buenos días") -> {
                when (toLanguage.lowercase()) {
                    "tamil" -> "காலை வணக்கம்"
                    "hindi" -> "शुभ प्रभात"
                    "english" -> "Good morning"
                    else -> text
                }
            }
            else -> text
        }

        return Result.success(translation)
    }

    override suspend fun getMotherTongueExplanation(
        errorContext: String,
        motherTongue: String
    ): Result<String> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }

        val explanation = when (motherTongue.lowercase()) {
            "tamil" -> "நேற்று நடந்த செயல்களைக் குறிப்பிட கடந்த கால வினைச்சொல்லைப் பயன்படுத்த வேண்டும்."
            "hindi" -> "भूतकाल में हुई घटनाओं को व्यक्त करने के लिए भूतकालिक क्रिया का उपयोग करें।"
            "spanish" -> "Para acciones concluidas en el pasado, utiliza el pretérito perfecto simple."
            else -> "Use the past simple tense for completed past actions."
        }

        return Result.success(explanation)
    }
}
