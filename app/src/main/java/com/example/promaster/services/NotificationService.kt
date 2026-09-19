package com.example.promaster.services

interface NotificationService {
    fun scheduleDailyReminder(hour: Int, minute: Int)
    fun cancelDailyReminder()
    fun showStreakSaverNotification(streakCount: Int)
    fun showLessonCompletedNotification(lessonTitle: String, xpEarned: Int)
}

class MockNotificationService : NotificationService {
    override fun scheduleDailyReminder(hour: Int, minute: Int) {}
    override fun cancelDailyReminder() {}
    override fun showStreakSaverNotification(streakCount: Int) {}
    override fun showLessonCompletedNotification(lessonTitle: String, xpEarned: Int) {}
}
