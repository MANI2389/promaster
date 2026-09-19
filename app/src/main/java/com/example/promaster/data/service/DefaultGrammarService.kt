package com.example.promaster.data.service

import com.example.promaster.domain.model.GrammarAnalysisResult
import com.example.promaster.domain.service.GrammarService

class DefaultGrammarService(
    private var available: Boolean = true
) : GrammarService {

    override fun isAvailable(): Boolean = available

    fun setAvailable(isAvail: Boolean) {
        this.available = isAvail
    }

    override suspend fun analyzeSentence(
        sentence: String,
        targetLanguage: String,
        motherTongue: String
    ): Result<GrammarAnalysisResult> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }

        val cleaned = sentence.trim()
        val lower = cleaned.lowercase()

        // Rule 1: Past time indicator with present/am-go construction (e.g. "I am go to college yesterday")
        if ((lower.contains("am go") || lower.contains("is go") || lower.contains("are go")) &&
            (lower.contains("yesterday") || lower.contains("last night") || lower.contains("ago") || lower.contains("last week"))
        ) {
            val corrected = cleaned
                .replace(Regex("(?i)\\bam go to\\b"), "went to")
                .replace(Regex("(?i)\\bam go\\b"), "went")
                .replace(Regex("(?i)\\bis go to\\b"), "went to")
                .replace(Regex("(?i)\\bare go to\\b"), "went to")

            val tamilExp = "நேற்று நடந்த நிகழ்வைக் குறிக்க 'went' என்ற கடந்த கால வினைச்சொல்லைப் பயன்படுத்த வேண்டும் ('am go' தவறு)."
            val hindiExp = "कल घटी घटना के लिए भूतकाल 'went' का प्रयोग करें ('am go' गलत है)।"
            val spanishExp = "Para una acción completada en el pasado ('yesterday'), utiliza el pasado simple 'went' en lugar de 'am go'."
            val englishExp = "Use the past simple tense 'went' for an action completed in the past ('yesterday'). 'am go' is incorrect."

            val motherTongueExp = when (motherTongue.lowercase()) {
                "tamil" -> tamilExp
                "hindi" -> hindiExp
                "spanish" -> spanishExp
                else -> englishExp
            }

            return Result.success(
                GrammarAnalysisResult(
                    originalSentence = cleaned,
                    isCorrect = false,
                    correctedSentence = corrected,
                    explanation = englishExp,
                    motherTongueExplanation = motherTongueExp,
                    rulesViolated = listOf("Past Simple Tense", "Auxiliary Verb Redundancy")
                )
            )
        }

        // Rule 2: Third person singular subject-verb agreement (e.g. "He go to school")
        if (Regex("(?i)\\b(he|she|it)\\s+go\\b").containsMatchIn(cleaned)) {
            val corrected = cleaned.replace(Regex("(?i)\\bgo\\b"), "goes")
            val tamilExp = "படர்க்கை ஒருமை (He, She, It) எழுவாய்களுக்கு வினைச்சொல்லுடன் -es சேர்த்து 'goes' என பயன்படுத்த வேண்டும்."
            val hindiExp = "तृतीय पुरुष एकवचन (He, She, It) के साथ वर्तमान काल में क्रिया 'goes' होगी।"
            val englishExp = "Third-person singular subjects (he, she, it) require the verb ending -es ('goes') in present simple tense."

            val motherTongueExp = when (motherTongue.lowercase()) {
                "tamil" -> tamilExp
                "hindi" -> hindiExp
                else -> englishExp
            }

            return Result.success(
                GrammarAnalysisResult(
                    originalSentence = cleaned,
                    isCorrect = false,
                    correctedSentence = corrected,
                    explanation = englishExp,
                    motherTongueExplanation = motherTongueExp,
                    rulesViolated = listOf("Subject-Verb Agreement")
                )
            )
        }

        // Rule 3: Spanish infinitive with subject pronoun (e.g. "Yo querer un café")
        if (lower.contains("yo querer")) {
            val corrected = cleaned.replace(Regex("(?i)\\byo querer\\b"), "Yo quiero")
            val englishExp = "In Spanish, conjugate 'querer' to 'quiero' for the first-person singular 'Yo'."
            val tamilExp = "ஸ்பானிஷ் மொழியில் 'Yo' (நான்) எழுவாய்க்கு 'quiero' என்று வினைச்சொல்லை மாற்ற வேண்டும்."

            val motherTongueExp = when (motherTongue.lowercase()) {
                "tamil" -> tamilExp
                else -> englishExp
            }

            return Result.success(
                GrammarAnalysisResult(
                    originalSentence = cleaned,
                    isCorrect = false,
                    correctedSentence = corrected,
                    explanation = englishExp,
                    motherTongueExplanation = motherTongueExp,
                    rulesViolated = listOf("Present Tense Conjugation")
                )
            )
        }

        // Default: Sentence is grammatically sound
        val tamilGood = "வாழ்த்துக்கள்! உங்கள் வாக்கியம் இலக்கண ரீதியாக சரியாக உள்ளது."
        val hindiGood = "बहुत बढ़िया! आपका वाक्य व्याकरण की दृष्टि से सही है।"
        val englishGood = "Great job! Your sentence is grammatically correct."

        val motherTongueGood = when (motherTongue.lowercase()) {
            "tamil" -> tamilGood
            "hindi" -> hindiGood
            else -> englishGood
        }

        return Result.success(
            GrammarAnalysisResult(
                originalSentence = cleaned,
                isCorrect = true,
                correctedSentence = cleaned,
                explanation = englishGood,
                motherTongueExplanation = motherTongueGood,
                rulesViolated = emptyList()
            )
        )
    }
}
