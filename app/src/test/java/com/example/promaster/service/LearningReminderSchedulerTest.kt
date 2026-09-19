package com.example.promaster.service

import com.example.promaster.data.service.AndroidLearningReminderScheduler
import com.example.promaster.data.service.MockLearningReminderScheduler
import com.example.promaster.domain.model.LearningReminderSettings
import com.example.promaster.domain.model.ReminderType
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class LearningReminderSchedulerTest {

    @Test
    fun parseTimeString_handlesStandard12HourAnd24HourFormats() {
        val (h1, m1) = AndroidLearningReminderScheduler.parseTimeString("08:00 AM")
        assertEquals(8, h1)
        assertEquals(0, m1)

        val (h2, m2) = AndroidLearningReminderScheduler.parseTimeString("08:30 PM")
        assertEquals(20, h2)
        assertEquals(30, m2)

        val (h3, m3) = AndroidLearningReminderScheduler.parseTimeString("19:45")
        assertEquals(19, h3)
        assertEquals(45, m3)

        val (h4, m4) = AndroidLearningReminderScheduler.parseTimeString("12:00 PM")
        assertEquals(12, h4)
        assertEquals(0, m4)

        val (h5, m5) = AndroidLearningReminderScheduler.parseTimeString("12:00 AM")
        assertEquals(0, h5)
        assertEquals(0, m5)
    }

    @Test
    fun calculateNextTriggerMillis_schedulesFutureTodayOrTomorrow() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        // Future today at 11:00 AM
        val triggerFuture = AndroidLearningReminderScheduler.calculateNextTriggerMillis(11, 0, now)
        assertTrue(triggerFuture > now)
        assertTrue(triggerFuture - now < 2 * 3600 * 1000L) // within 2 hours

        // Past today at 9:00 AM -> Should roll over to tomorrow
        val triggerPast = AndroidLearningReminderScheduler.calculateNextTriggerMillis(9, 0, now)
        assertTrue(triggerPast > now)
        assertTrue(triggerPast - now > 20 * 3600 * 1000L) // tomorrow (> 20 hours later)
    }

    @Test
    fun mockLearningReminderScheduler_managesAllThreeReminderTypes() {
        val scheduler = MockLearningReminderScheduler(
            LearningReminderSettings(
                preferredTime = "07:30 AM",
                dailyLessonReminderEnabled = true,
                missedTaskReminderEnabled = true,
                streakReminderEnabled = true
            )
        )

        assertTrue(scheduler.isReminderScheduled(ReminderType.DAILY_LESSON))
        assertTrue(scheduler.isReminderScheduled(ReminderType.MISSED_TASK))
        assertTrue(scheduler.isReminderScheduled(ReminderType.STREAK_PRESERVATION))
        assertEquals("07:30 AM", scheduler.lastScheduledTimeStr)

        // Cancel daily lesson
        scheduler.cancelReminder(ReminderType.DAILY_LESSON)
        assertFalse(scheduler.isReminderScheduled(ReminderType.DAILY_LESSON))
        assertTrue(scheduler.isReminderScheduled(ReminderType.MISSED_TASK))

        // Update settings with daily lesson disabled and streak disabled
        scheduler.updateSettings(
            LearningReminderSettings(
                preferredTime = "09:00 AM",
                dailyLessonReminderEnabled = false,
                missedTaskReminderEnabled = true,
                streakReminderEnabled = false
            )
        )

        assertFalse(scheduler.isReminderScheduled(ReminderType.DAILY_LESSON))
        assertTrue(scheduler.isReminderScheduled(ReminderType.MISSED_TASK))
        assertFalse(scheduler.isReminderScheduled(ReminderType.STREAK_PRESERVATION))

        // Cancel all
        scheduler.cancelAllReminders()
        assertFalse(scheduler.isReminderScheduled(ReminderType.MISSED_TASK))
    }
}
