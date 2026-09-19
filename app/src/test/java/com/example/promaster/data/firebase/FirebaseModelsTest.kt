package com.example.promaster.data.firebase

import com.example.promaster.data.firebase.model.*
import com.example.promaster.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class FirebaseModelsTest {

    @Test
    fun userDocument_toDomain_and_fromDomain_mapsAllFieldsCorrectly() {
        val user = User(
            id = "user_abc_123",
            name = "Ravi Kumar",
            email = "ravi@promaster.ai",
            targetLanguage = "Japanese",
            nativeLanguage = "Tamil",
            streakDays = 5,
            totalXp = 450,
            level = 2,
            currentDay = 5,
            dailyGoalMinutes = 30
        )

        val doc = UserDocument.fromDomain(
            user = user,
            motherTongue = "Tamil",
            level = "Intermediate",
            preferredTime = "Morning"
        )

        assertEquals("Ravi Kumar", doc.name)
        assertEquals("ravi@promaster.ai", doc.email)
        assertEquals("Tamil", doc.motherTongue)
        assertEquals("Japanese", doc.targetLanguage)
        assertEquals("Intermediate", doc.level)
        assertEquals(30, doc.dailyDuration)
        assertEquals("Morning", doc.preferredTime)

        val domainUser = doc.toDomain("user_abc_123")
        assertEquals("user_abc_123", domainUser.id)
        assertEquals("Ravi Kumar", domainUser.name)
        assertEquals("Japanese", domainUser.targetLanguage)
        assertEquals("Tamil", domainUser.nativeLanguage)
        assertEquals(30, domainUser.dailyGoalMinutes)
    }

    @Test
    fun progressDocument_mapsCorrectlyToDomain() {
        val doc = ProgressDocument(
            xp = 1200,
            streak = 14,
            currentDay = 10,
            completedTasks = 25,
            grammarScore = 85,
            vocabularyScore = 90,
            speakingScore = 75,
            overallProgress = 0.65
        )

        val domain = doc.toDomain()
        assertEquals(14, domain.currentStreak)
        assertEquals(1200, domain.totalXp)
        assertTrue(domain.wordsLearned >= 25)
        assertTrue(domain.rulesMastered >= 8)
    }

    @Test
    fun learningPlanDocument_andDayPlanItem_structureMatchesSpecification() {
        val day1 = DayPlanItem(day = 1, title = "Introductions", estimatedMinutes = 15, isCompleted = true)
        val day2 = DayPlanItem(day = 2, title = "Numbers & Food", estimatedMinutes = 20, isCompleted = false)

        val planDoc = LearningPlanDocument(
            planId = "plan_001",
            userId = "user_123",
            targetLanguage = "German",
            level = "Beginner",
            duration = 20,
            currentDay = 2,
            days = listOf(day1, day2)
        )

        assertEquals("German", planDoc.targetLanguage)
        assertEquals(2, planDoc.days.size)
        assertTrue(planDoc.days.first().isCompleted)
        assertFalse(planDoc.days.last().isCompleted)
        assertEquals(15, planDoc.days.first().toDomain().estimatedMinutes)
    }

    @Test
    fun dailyTaskDocument_handlesStatusesAndScore() {
        val task = DailyTask(
            id = "task_99",
            title = "Morning Listening Drill",
            description = "Listen to dialogue and answer",
            category = TaskCategory.LISTENING,
            xpReward = 40,
            isCompleted = true,
            durationMinutes = 10
        )

        val doc = DailyTaskDocument.fromDomain(task, dayNumber = 3)
        assertEquals("task_99", doc.taskId)
        assertEquals(3, doc.dayNumber)
        assertEquals("COMPLETED", doc.status)
        assertEquals(100, doc.score)
        assertNotNull(doc.completedAt)

        val domainTask = doc.toDomain()
        assertEquals(TaskCategory.LISTENING, domainTask.category)
        assertTrue(domainTask.isCompleted)
    }

    @Test
    fun conversationMessageDocument_handlesRolesAndIntents() {
        val doc = ConversationMessageDocument(
            messageId = "msg_123",
            role = "AI_FRIEND",
            message = "Bonjour! How was your day?",
            intent = "CHAT"
        )

        val domainMsg = doc.toDomain()
        assertEquals("msg_123", domainMsg.id)
        assertEquals(MessageSender.AI_FRIEND, domainMsg.sender)
        assertEquals("Bonjour! How was your day?", domainMsg.text)
    }

    @Test
    fun settingsDocument_hasSensibleDefaults() {
        val settings = SettingsDocument()
        assertTrue(settings.notificationsEnabled)
        assertTrue(settings.offlineSyncEnabled)
        assertTrue(settings.studyModeDnd)
        assertEquals("08:00 AM", settings.dailyReminderTime)
    }
}
