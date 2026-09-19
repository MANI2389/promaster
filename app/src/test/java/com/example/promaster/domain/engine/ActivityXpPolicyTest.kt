package com.example.promaster.domain.engine

import org.junit.Assert.*
import org.junit.Test

class ActivityXpPolicyTest {

    @Test
    fun calculateXp_belowPassingScore_awardsZeroXpAndBlocksGate() {
        val result = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 65,
            attemptCount = 1,
            todayXpEarnedForActivity = 0
        )

        assertEquals(0, result.earnedXp)
        assertFalse(result.passedGate)
        assertTrue(result.message.contains("Score 70%+ to pass"))
    }

    @Test
    fun calculateXp_passingScore_awardsBaseXpWithMultiplier() {
        // Passing with 75% -> 0.85 multiplier of 30 base = 25 XP
        val pass75 = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 75,
            attemptCount = 1,
            todayXpEarnedForActivity = 0
        )
        assertTrue(pass75.passedGate)
        assertTrue(pass75.isFirstCompletion)
        assertEquals(25, pass75.earnedXp)

        // Passing with 85% -> 1.0 multiplier of 30 base = 30 XP
        val pass85 = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 85,
            attemptCount = 1,
            todayXpEarnedForActivity = 0
        )
        assertEquals(30, pass85.earnedXp)

        // Passing with 95% -> 1.15 perfection multiplier of 30 base = 34 XP
        val pass95 = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 95,
            attemptCount = 1,
            todayXpEarnedForActivity = 0
        )
        assertEquals(34, pass95.earnedXp)
    }

    @Test
    fun calculateXp_repeatAttempt_awardsDiminishingFlatBonus() {
        val repeatResult = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 100,
            attemptCount = 2,
            todayXpEarnedForActivity = 34
        )

        assertTrue(repeatResult.passedGate)
        assertFalse(repeatResult.isFirstCompletion)
        assertEquals(5, repeatResult.earnedXp)
        assertTrue(repeatResult.message.contains("Review completed"))
    }

    @Test
    fun calculateXp_dailyCapExceeded_awardsZeroXp() {
        val cappedResult = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 100,
            attemptCount = 5,
            todayXpEarnedForActivity = 50 // Cap reached
        )

        assertTrue(cappedResult.passedGate)
        assertEquals(0, cappedResult.earnedXp)
        assertTrue(cappedResult.message.contains("Daily XP cap"))
    }

    @Test
    fun calculateXp_partialAllowableXp_clampsToCap() {
        val partialResult = ActivityXpPolicy.calculateXp(
            activityType = "VOCABULARY_QUIZ",
            score = 100,
            attemptCount = 4,
            todayXpEarnedForActivity = 48 // 2 XP allowable before 50 cap
        )

        assertTrue(partialResult.passedGate)
        assertEquals(2, partialResult.earnedXp)
    }
}
