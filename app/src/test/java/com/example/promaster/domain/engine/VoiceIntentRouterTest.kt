package com.example.promaster.domain.engine

import com.example.promaster.domain.model.VoiceCommandType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VoiceIntentRouterTest {

    private val router = DefaultIntentRouter()

    @Test
    fun routeIntent_classifiesAllSixRequiredCommandsAccurately() = runBlocking {
        // 1. "Start my English lesson."
        val lesson = router.routeIntent("Start my English lesson.")
        assertEquals(VoiceCommandType.START_LESSON, lesson.commandType)
        assertEquals("English", lesson.parameters["language"])

        // Spanish variation
        val spanishLesson = router.routeIntent("Start my Spanish lesson")
        assertEquals(VoiceCommandType.START_LESSON, spanishLesson.commandType)
        assertEquals("Spanish", spanishLesson.parameters["language"])

        // 2. "What is today's task?"
        val task = router.routeIntent("What is today's task?")
        assertEquals(VoiceCommandType.GET_TODAYS_TASK, task.commandType)

        val tasksVariation = router.routeIntent("Show my tasks for today")
        assertEquals(VoiceCommandType.GET_TODAYS_TASK, tasksVariation.commandType)

        // 3. "Open YouTube."
        val youtube = router.routeIntent("Open YouTube.")
        assertEquals(VoiceCommandType.OPEN_YOUTUBE, youtube.commandType)

        val launchYoutube = router.routeIntent("Launch YouTube")
        assertEquals(VoiceCommandType.OPEN_YOUTUBE, launchYoutube.commandType)

        // 4. "Set an alarm."
        val alarm = router.routeIntent("Set an alarm.")
        assertEquals(VoiceCommandType.SET_ALARM, alarm.commandType)

        val alarmWithTime = router.routeIntent("Set an alarm for 7:30 am")
        assertEquals(VoiceCommandType.SET_ALARM, alarmWithTime.commandType)
        assertNotNull(alarmWithTime.parameters["time"])

        // 5. "Open settings."
        val settings = router.routeIntent("Open settings.")
        assertEquals(VoiceCommandType.OPEN_SETTINGS, settings.commandType)

        val settingsVariation = router.routeIntent("Go to settings")
        assertEquals(VoiceCommandType.OPEN_SETTINGS, settingsVariation.commandType)

        // 6. "Start speaking practice."
        val speaking = router.routeIntent("Start speaking practice.")
        assertEquals(VoiceCommandType.START_SPEAKING_PRACTICE, speaking.commandType)

        val speakingVariation = router.routeIntent("Practice speaking")
        assertEquals(VoiceCommandType.START_SPEAKING_PRACTICE, speakingVariation.commandType)
    }

    @Test
    fun routeIntent_routesUnsupportedCommandsToUnsupportedState() = runBlocking {
        val unsupported1 = router.routeIntent("Order a large pizza with extra cheese")
        assertEquals(VoiceCommandType.UNSUPPORTED, unsupported1.commandType)

        val unsupported2 = router.routeIntent("Book a flight to Mars")
        assertEquals(VoiceCommandType.UNSUPPORTED, unsupported2.commandType)

        val unsupported3 = router.routeIntent("What is the weather in Tokyo?")
        assertEquals(VoiceCommandType.UNSUPPORTED, unsupported3.commandType)
    }
}
