package com.example.promaster.domain.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculationEngineTest {

    private val testZoneId = ZoneId.of("UTC")

    private fun localDateToEpochMillis(date: LocalDate, hour: Int = 10): Long {
        return date.atTime(hour, 0).atZone(testZoneId).toInstant().toEpochMilli()
    }

    @Test
    fun calculateStreak_firstActivityEver_initializesStreakToOne() {
        val today = LocalDate.of(2026, 9, 19)
        val nowMillis = localDateToEpochMillis(today)

        val result = StreakCalculationEngine.calculateStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastActivityTimestamp = 0L,
            nowTimestamp = nowMillis,
            zoneId = testZoneId
        )

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
        assertEquals(nowMillis, result.lastActivityDate)
        assertTrue(result.streakIncremented)
        assertFalse(result.streakBroken)
    }

    @Test
    fun calculateStreak_sameCalendarDay_preventsDuplicateIncrement() {
        val today = LocalDate.of(2026, 9, 19)
        val firstActivityMillis = localDateToEpochMillis(today, hour = 9)
        val secondActivityMillis = localDateToEpochMillis(today, hour = 14)
        val thirdActivityMillis = localDateToEpochMillis(today, hour = 21)

        // Initial state after first activity on Day 1
        val initialStreak = 3
        val longestStreak = 5

        // Second activity on same calendar day
        val secondResult = StreakCalculationEngine.calculateStreak(
            currentStreak = initialStreak,
            longestStreak = longestStreak,
            lastActivityTimestamp = firstActivityMillis,
            nowTimestamp = secondActivityMillis,
            zoneId = testZoneId
        )

        assertEquals("Same day activity must NOT increment streak", 3, secondResult.currentStreak)
        assertEquals(5, secondResult.longestStreak)
        assertFalse(secondResult.streakIncremented)
        assertFalse(secondResult.streakBroken)

        // Third activity on same day
        val thirdResult = StreakCalculationEngine.calculateStreak(
            currentStreak = secondResult.currentStreak,
            longestStreak = secondResult.longestStreak,
            lastActivityTimestamp = secondActivityMillis,
            nowTimestamp = thirdActivityMillis,
            zoneId = testZoneId
        )

        assertEquals("Multiple same-day activities must NOT increment streak", 3, thirdResult.currentStreak)
        assertFalse(thirdResult.streakIncremented)
    }

    @Test
    fun calculateStreak_consecutiveCalendarDay_incrementsStreakAndUpdatesLongest() {
        val day1 = LocalDate.of(2026, 9, 18)
        val day2 = LocalDate.of(2026, 9, 19)
        val day1Millis = localDateToEpochMillis(day1, hour = 18)
        val day2Millis = localDateToEpochMillis(day2, hour = 8)

        val result = StreakCalculationEngine.calculateStreak(
            currentStreak = 4,
            longestStreak = 4,
            lastActivityTimestamp = day1Millis,
            nowTimestamp = day2Millis,
            zoneId = testZoneId
        )

        assertEquals(5, result.currentStreak)
        assertEquals(5, result.longestStreak)
        assertTrue(result.streakIncremented)
        assertFalse(result.streakBroken)
    }

    @Test
    fun calculateStreak_consecutiveDay_doesNotDecreaseExistingLongestStreak() {
        val day1 = LocalDate.of(2026, 9, 18)
        val day2 = LocalDate.of(2026, 9, 19)
        val day1Millis = localDateToEpochMillis(day1)
        val day2Millis = localDateToEpochMillis(day2)

        val result = StreakCalculationEngine.calculateStreak(
            currentStreak = 2,
            longestStreak = 10,
            lastActivityTimestamp = day1Millis,
            nowTimestamp = day2Millis,
            zoneId = testZoneId
        )

        assertEquals(3, result.currentStreak)
        assertEquals(10, result.longestStreak)
        assertTrue(result.streakIncremented)
    }

    @Test
    fun calculateStreak_missedCalendarDay_resetsCurrentStreakWhilePreservingLongest() {
        val day1 = LocalDate.of(2026, 9, 15) // 4 days ago
        val today = LocalDate.of(2026, 9, 19)
        val day1Millis = localDateToEpochMillis(day1)
        val todayMillis = localDateToEpochMillis(today)

        val result = StreakCalculationEngine.calculateStreak(
            currentStreak = 7,
            longestStreak = 15,
            lastActivityTimestamp = day1Millis,
            nowTimestamp = todayMillis,
            zoneId = testZoneId
        )

        assertEquals("Missed days must reset current streak to 1", 1, result.currentStreak)
        assertEquals("Longest streak must be preserved on reset", 15, result.longestStreak)
        assertFalse(result.streakIncremented)
        assertTrue(result.streakBroken)
    }

    @Test
    fun streakHelperMethods_detectStreakRiskAndSecuredStatusCorrectly() {
        val yesterday = LocalDate.of(2026, 9, 18)
        val today = LocalDate.of(2026, 9, 19)
        val twoDaysAgo = LocalDate.of(2026, 9, 17)

        val yesterdayMillis = localDateToEpochMillis(yesterday)
        val todayMillis = localDateToEpochMillis(today)
        val twoDaysAgoMillis = localDateToEpochMillis(twoDaysAgo)

        // When active today
        assertTrue(StreakCalculationEngine.isStreakActiveToday(todayMillis, todayMillis, testZoneId))
        assertFalse(StreakCalculationEngine.isStreakAtRisk(todayMillis, todayMillis, testZoneId))

        // When active yesterday but not today -> At risk
        assertFalse(StreakCalculationEngine.isStreakActiveToday(yesterdayMillis, todayMillis, testZoneId))
        assertTrue(StreakCalculationEngine.isStreakAtRisk(yesterdayMillis, todayMillis, testZoneId))

        // When inactive for 2+ days -> Streak already broken, not at risk
        assertFalse(StreakCalculationEngine.isStreakActiveToday(twoDaysAgoMillis, todayMillis, testZoneId))
        assertFalse(StreakCalculationEngine.isStreakAtRisk(twoDaysAgoMillis, todayMillis, testZoneId))
    }
}
