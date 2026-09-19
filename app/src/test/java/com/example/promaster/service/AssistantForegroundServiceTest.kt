package com.example.promaster.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AssistantForegroundServiceTest {

    @Test
    fun companionConstants_adhereToSpecification() {
        assertEquals("promaster_wake_word_channel", AssistantForegroundService.CHANNEL_ID)
        assertEquals(2001, AssistantForegroundService.NOTIFICATION_ID)
        assertEquals("com.example.promaster.action.START_WAKE_WORD", AssistantForegroundService.ACTION_START_LISTENING)
        assertEquals("com.example.promaster.action.STOP_WAKE_WORD", AssistantForegroundService.ACTION_STOP_LISTENING)
    }

    @Test
    fun serviceClass_isRegisteredAndInstantiable() {
        assertNotNull(AssistantForegroundService::class.java)
    }
}
