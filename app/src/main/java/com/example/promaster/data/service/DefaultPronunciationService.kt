package com.example.promaster.data.service

import com.example.promaster.domain.model.PronunciationResult
import com.example.promaster.domain.service.PronunciationService
import kotlin.math.max
import kotlin.math.min

class DefaultPronunciationService(
    private var available: Boolean = true
) : PronunciationService {

    override fun isAvailable(): Boolean = available

    fun setAvailable(isAvail: Boolean) {
        this.available = isAvail
    }

    override suspend fun evaluatePronunciation(
        spokenText: String,
        expectedSentence: String
    ): Result<PronunciationResult> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Speaking analysis is currently unavailable."))
        }

        val spokenClean = cleanText(spokenText)
        val expectedClean = cleanText(expectedSentence)

        if (spokenClean.isBlank()) {
            return Result.success(
                PronunciationResult(
                    accuracyScore = 0,
                    isMatch = false,
                    feedback = "No speech detected. Please speak the phrase aloud.",
                    wordAccuracies = emptyMap()
                )
            )
        }

        val spokenWords = spokenClean.split("\\s+".toRegex())
        val expectedWords = expectedClean.split("\\s+".toRegex())

        val wordAccuracies = mutableMapOf<String, Int>()
        var totalWordScore = 0

        for (expWord in expectedWords) {
            // Find closest matching word in spokenWords
            var bestWordSimilarity = 0
            for (spkWord in spokenWords) {
                val similarity = calculateStringSimilarity(expWord, spkWord)
                if (similarity > bestWordSimilarity) {
                    bestWordSimilarity = similarity
                }
            }
            wordAccuracies[expWord] = bestWordSimilarity
            totalWordScore += bestWordSimilarity
        }

        val averageScore = if (expectedWords.isNotEmpty()) {
            (totalWordScore / expectedWords.size).coerceIn(0, 100)
        } else 0

        // Length penalty if user spoke too few words compared to target
        val lengthRatio = (spokenWords.size.toFloat() / expectedWords.size.toFloat()).coerceAtMost(1.0f)
        val finalScore = (averageScore * (0.4f + 0.6f * lengthRatio)).toInt().coerceIn(0, 100)

        val isPass = finalScore >= 70

        val feedback = when {
            finalScore >= 90 -> "Outstanding pronunciation! Your rhythm and articulation are exceptionally clear."
            finalScore >= 70 -> "Good pronunciation! You successfully verified the corrected sentence ($finalScore%)."
            else -> "Pronunciation score is $finalScore%. Please repeat the corrected sentence clearly to verify."
        }

        return Result.success(
            PronunciationResult(
                accuracyScore = finalScore,
                isMatch = isPass,
                feedback = feedback,
                wordAccuracies = wordAccuracies
            )
        )
    }

    private fun cleanText(text: String): String {
        return text.lowercase()
            .replace("[^a-zA-Z0-9áéíóúüñ\\s]".toRegex(), "")
            .trim()
    }

    private fun calculateStringSimilarity(s1: String, s2: String): Int {
        if (s1 == s2) return 100
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 100
        val distance = levenshteinDistance(s1, s2)
        val score = ((maxLen - distance).toFloat() / maxLen.toFloat()) * 100f
        return score.toInt().coerceIn(0, 100)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var cost = IntArray(lhs.length + 1) { it }
        var newCost = IntArray(lhs.length + 1) { 0 }

        for (i in 1..rhs.length) {
            newCost[0] = i
            for (j in 1..lhs.length) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhs.length]
    }
}
