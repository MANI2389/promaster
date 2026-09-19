package com.example.promaster.domain.engine

import com.example.promaster.domain.model.RoutedVoiceIntent
import com.example.promaster.domain.model.VoiceCommandType

/**
 * Routes natural language voice query strings into strongly-typed VoiceCommandType intents.
 */
interface IntentRouter {
    suspend fun routeIntent(voiceInput: String): RoutedVoiceIntent
}

class DefaultIntentRouter : IntentRouter {

    override suspend fun routeIntent(voiceInput: String): RoutedVoiceIntent {
        val trimmed = voiceInput.trim()
        val lower = trimmed.lowercase()

        return when {
            // 1. "Start my English lesson." or "Start my Spanish lesson."
            lower.contains("start") && (lower.contains("lesson") || lower.contains("class")) ||
            lower.contains("begin") && lower.contains("lesson") ||
            lower.startsWith("start lesson") || lower.startsWith("start my lesson") -> {
                val language = when {
                    lower.contains("english") -> "English"
                    lower.contains("spanish") -> "Spanish"
                    lower.contains("tamil") -> "Tamil"
                    lower.contains("french") -> "French"
                    lower.contains("german") -> "German"
                    else -> "Current"
                }
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.START_LESSON,
                    rawText = trimmed,
                    parameters = mapOf("language" to language)
                )
            }

            // 2. "What is today's task?"
            lower.contains("today's task") || lower.contains("today's tasks") ||
            lower.contains("today task") || lower.contains("today tasks") ||
            lower.contains("tasks for today") || lower.contains("what is today's task") ||
            lower.contains("what are my tasks") || lower.contains("show my tasks") -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.GET_TODAYS_TASK,
                    rawText = trimmed
                )
            }

            // 3. "Open YouTube."
            lower.contains("open youtube") || lower.contains("launch youtube") ||
            lower.contains("start youtube") || lower.contains("go to youtube") ||
            lower.equals("youtube", ignoreCase = true) -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.OPEN_YOUTUBE,
                    rawText = trimmed
                )
            }

            // 4. "Set an alarm."
            lower.contains("set an alarm") || lower.contains("set alarm") ||
            lower.contains("create alarm") || lower.contains("wake me up") ||
            lower.contains("study alarm") || lower.startsWith("alarm") -> {
                val timeParam = extractTime(lower)
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.SET_ALARM,
                    rawText = trimmed,
                    parameters = if (timeParam != null) mapOf("time" to timeParam) else emptyMap()
                )
            }

            // 5. "Open settings."
            lower.contains("open settings") || lower.contains("go to settings") ||
            lower.contains("show settings") || lower.contains("launch settings") ||
            lower.equals("settings", ignoreCase = true) -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.OPEN_SETTINGS,
                    rawText = trimmed
                )
            }

            // 5b. General app open commands: "Open WhatsApp", "Open Chrome", "Open Calculator", "Open Camera", "Open Maps"
            (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start app ")) &&
            !lower.contains("lesson") && !lower.contains("settings") && !lower.contains("youtube") && !lower.contains("speaking") && !lower.contains("website") -> {
                val appName = trimmed.replaceFirst(Regex("""^(?:open|launch|start app|open app)\s+""", RegexOption.IGNORE_CASE), "").trim(' ', '.', '!')
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.AUTOMATION_ACTION,
                    rawText = trimmed,
                    parameters = mapOf("appName" to appName)
                )
            }

            // 6. "Start speaking practice."
            lower.contains("speaking practice") || lower.contains("practice speaking") ||
            lower.contains("start speaking") || lower.contains("pronunciation practice") -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.START_SPEAKING_PRACTICE,
                    rawText = trimmed
                )
            }

            // 7. Make call: "Call 9876543210", "Make call to 1234567890"
            lower.startsWith("call ") || lower.startsWith("make call to ") || lower.startsWith("make a call to ") || lower.startsWith("dial ") -> {
                val phoneRegex = Regex("""(?:call|make a call to|make call to|dial)\s+([\+0-9\-\s\(\)]+)""", RegexOption.IGNORE_CASE)
                val match = phoneRegex.find(trimmed)
                val number = match?.groupValues?.getOrNull(1)?.trim() ?: ""
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.MAKE_CALL,
                    rawText = trimmed,
                    parameters = mapOf("phoneNumber" to number)
                )
            }

            // 8. Open website: "Open website https://promaster.app", "Open url..."
            lower.contains("website") || lower.contains("url ") || lower.startsWith("browse to ") -> {
                val regex = Regex("""(?:website|url|browse to)\s+(https?://[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?)""", RegexOption.IGNORE_CASE)
                val match = regex.find(trimmed)
                val rawUrl = match?.groupValues?.getOrNull(1) ?: trimmed.substringAfter("website").substringAfter("url").trim()
                val url = if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
                    "https://${rawUrl.trim(' ', '"', '\'')}"
                } else {
                    rawUrl.trim(' ', '"', '\'')
                }
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.OPEN_WEBSITE,
                    rawText = trimmed,
                    parameters = mapOf("url" to url)
                )
            }

            // 9. Automation actions
            lower.contains("automation") || lower.contains("run routine") -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.AUTOMATION_ACTION,
                    rawText = trimmed
                )
            }

            // 10. AI Conversation: Conversational phrases, greetings, questions to PROMASTER AI
            lower.startsWith("hello") || lower.startsWith("hi ") || lower == "hi" ||
            lower.startsWith("hey ") || lower.contains("chat") || lower.contains("tell me") ||
            lower.contains("who are you") || lower.contains("introduce yourself") ||
            lower.contains("how are you") || lower.contains("practice english") ||
            lower.contains("explain") || lower.contains("teach me") || lower.contains("what does") ||
            lower.contains("how do i say") || lower.contains("can you help") || lower.contains("conversation") ||
            lower.contains("translate") || lower.contains("grammar") || lower.contains("meaning of") -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.AI_CONVERSATION,
                    rawText = trimmed
                )
            }

            // 11. Unsupported
            else -> {
                RoutedVoiceIntent(
                    commandType = VoiceCommandType.UNSUPPORTED,
                    rawText = trimmed,
                    confidence = 0.0f
                )
            }
        }
    }

    private fun extractTime(text: String): String? {
        val regex = Regex("""\b(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\b""")
        val match = regex.find(text)
        return match?.value
    }
}
