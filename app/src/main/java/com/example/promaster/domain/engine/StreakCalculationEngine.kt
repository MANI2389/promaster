package com.example.promaster.domain.engine

import com.example.promaster.domain.model.StreakResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Deterministic streak calculation engine for PROMASTER.
 *
 * SPECIFICATION:
 * - Calculates streaks from actual completed learning activities.
 * - Tracks currentStreak, longestStreak, and lastActivityDate.
 * - Strictly prevents duplicate activity on the same calendar day from incorrectly increasing the streak.
 * - Increments streak when active on consecutive calendar days.
 * - Resets currentStreak to 1 when a day is missed, while preserving longestStreak.
 */
object StreakCalculationEngine {

    /**
     * Calculates updated streak metrics following completion of a learning activity.
     */
    fun calculateStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastActivityTimestamp: Long,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakResult {
        // Case 1: First activity ever recorded
        if (lastActivityTimestamp <= 0L) {
            val initialStreak = 1
            return StreakResult(
                currentStreak = initialStreak,
                longestStreak = maxOf(longestStreak, initialStreak),
                lastActivityDate = nowTimestamp,
                streakIncremented = true,
                streakBroken = false
            )
        }

        val lastDate = Instant.ofEpochMilli(lastActivityTimestamp).atZone(zoneId).toLocalDate()
        val todayDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(lastDate, todayDate)

        return when {
            // Case 2: Same calendar day -> PREVENT DUPLICATE INCREMENT
            daysDiff == 0L -> {
                StreakResult(
                    currentStreak = maxOf(1, currentStreak),
                    longestStreak = maxOf(longestStreak, currentStreak, 1),
                    lastActivityDate = nowTimestamp,
                    streakIncremented = false,
                    streakBroken = false
                )
            }

            // Case 3: Consecutive calendar day (Yesterday was active) -> Increment by 1
            daysDiff == 1L -> {
                val newCurrent = currentStreak + 1
                StreakResult(
                    currentStreak = newCurrent,
                    longestStreak = maxOf(longestStreak, newCurrent),
                    lastActivityDate = nowTimestamp,
                    streakIncremented = true,
                    streakBroken = false
                )
            }

            // Case 4: Missed day(s) -> Reset currentStreak to 1, preserve longestStreak
            daysDiff > 1L -> {
                val resetStreak = 1
                StreakResult(
                    currentStreak = resetStreak,
                    longestStreak = maxOf(longestStreak, resetStreak),
                    lastActivityDate = nowTimestamp,
                    streakIncremented = false,
                    streakBroken = true
                )
            }

            // Case 5: System clock skewed backwards -> Retain streak safely
            else -> {
                StreakResult(
                    currentStreak = currentStreak,
                    longestStreak = maxOf(longestStreak, currentStreak),
                    lastActivityDate = nowTimestamp,
                    streakIncremented = false,
                    streakBroken = false
                )
            }
        }
    }

    /**
     * Checks if the user's streak is at risk of breaking before the current day ends.
     * True if the user was active yesterday, but has not yet completed an activity today.
     */
    fun isStreakAtRisk(
        lastActivityTimestamp: Long,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (lastActivityTimestamp <= 0L) return false
        val lastDate = Instant.ofEpochMilli(lastActivityTimestamp).atZone(zoneId).toLocalDate()
        val todayDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(lastDate, todayDate)
        return daysDiff == 1L
    }

    /**
     * Checks if the user has already secured their streak for today.
     */
    fun isStreakActiveToday(
        lastActivityTimestamp: Long,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (lastActivityTimestamp <= 0L) return false
        val lastDate = Instant.ofEpochMilli(lastActivityTimestamp).atZone(zoneId).toLocalDate()
        val todayDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        return lastDate == todayDate
    }
}
