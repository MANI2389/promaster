package com.example.promaster.domain.engine

/**
 * Enforces non-exploitable XP rewards across learning activities:
 * - Requires minimum 70% passing score
 * - Provides bonus for 90%+ perfection
 * - Diminishes repeat rewards on the same day
 * - Enforces daily caps per activity to prevent script/click exploitation
 */
object ActivityXpPolicy {

    const val MIN_PASSING_SCORE = 70
    const val DAILY_ACTIVITY_XP_CAP = 50

    data class XpAwardResult(
        val earnedXp: Int,
        val isFirstCompletion: Boolean,
        val passedGate: Boolean,
        val message: String
    )

    fun calculateXp(
        activityType: String,
        score: Int,
        attemptCount: Int,
        todayXpEarnedForActivity: Int = 0
    ): XpAwardResult {
        val normalizedScore = score.coerceIn(0, 100)

        // Rule 1: Passing gate
        if (normalizedScore < MIN_PASSING_SCORE) {
            return XpAwardResult(
                earnedXp = 0,
                isFirstCompletion = false,
                passedGate = false,
                message = "Score was $normalizedScore%. Score 70%+ to pass and unlock XP."
            )
        }

        // Rule 2: Daily cap check
        if (todayXpEarnedForActivity >= DAILY_ACTIVITY_XP_CAP) {
            return XpAwardResult(
                earnedXp = 0,
                isFirstCompletion = false,
                passedGate = true,
                message = "Daily XP cap ($DAILY_ACTIVITY_XP_CAP XP) reached for this exercise. Great practice!"
            )
        }

        val baseReward = when (activityType.uppercase()) {
            "VOCABULARY_LEARN" -> 15
            "VOCABULARY_PRACTICE" -> 20
            "VOCABULARY_QUIZ" -> 30
            "GRAMMAR_EXERCISE" -> 25
            "GRAMMAR_QUIZ" -> 35
            else -> 30
        }

        val scoreMultiplier = when {
            normalizedScore >= 90 -> 1.15f // 15% perfection bonus
            normalizedScore >= 80 -> 1.0f
            else -> 0.85f // 70-79% passing
        }

        val isFirst = attemptCount <= 1 && todayXpEarnedForActivity == 0

        val calculated = if (isFirst) {
            (baseReward * scoreMultiplier).toInt()
        } else {
            // Diminishing returns: repeat attempts award flat 5 XP review reinforcement
            5
        }

        // Enforce daily cap
        val allowable = (DAILY_ACTIVITY_XP_CAP - todayXpEarnedForActivity).coerceAtLeast(0)
        val finalXp = minOf(calculated, allowable)

        val message = if (isFirst) {
            "Passed with $normalizedScore%! +$finalXp XP awarded."
        } else {
            "Review completed with $normalizedScore%! +$finalXp XP review bonus."
        }

        return XpAwardResult(
            earnedXp = finalXp,
            isFirstCompletion = isFirst,
            passedGate = true,
            message = message
        )
    }
}
