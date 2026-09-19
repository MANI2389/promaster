package com.example.promaster.domain.automation

/**
 * Parses natural language voice or text inputs into strongly-typed AutomationCommands.
 */
interface AutomationIntentRouter {
    suspend fun route(input: String, isConfirmed: Boolean = false): AutomationCommand
}

class DefaultAutomationIntentRouter : AutomationIntentRouter {

    override suspend fun route(input: String, isConfirmed: Boolean): AutomationCommand {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        if (trimmed.isEmpty()) {
            return AutomationCommand(
                type = AutomationCommandType.UNSUPPORTED,
                rawInput = trimmed,
                parameters = mapOf("reason" to "I couldn't understand that command.", "errorCode" to AutomationError.EMPTY_COMMAND.name),
                isConfirmed = isConfirmed
            )
        }

        // 1. Accessibility hacks / dangerous injection operations -> UNSUPPORTED
        if (lower.contains("accessibility") || lower.contains("click the button") ||
            lower.contains("tap screen") || lower.contains("screen scrape") ||
            lower.contains("keylog") || lower.contains("inject touch")
        ) {
            return AutomationCommand(
                type = AutomationCommandType.UNSUPPORTED,
                rawInput = trimmed,
                parameters = mapOf("reason" to "Accessibility service automation is not permitted"),
                isConfirmed = isConfirmed
            )
        }

        // 2. MAKE_CALL: e.g. "Call 9876543210", "Make call to 1234567890", "Make a call to 9876543210", "Dial 555-1234"
        if (lower.startsWith("call ") || lower.startsWith("make a call to ") ||
            lower.startsWith("make call to ") || lower.startsWith("dial ") ||
            lower.contains("phone call to ")
        ) {
            val phoneRegex = Regex("""(?:call|make a call to|make call to|dial|phone call to)\s+([\+0-9\-\s\(\)]+)""", RegexOption.IGNORE_CASE)
            val match = phoneRegex.find(trimmed)
            val phoneNumber = match?.groupValues?.getOrNull(1)?.trim() ?: extractDigitsOrPhone(trimmed)

            return AutomationCommand(
                type = AutomationCommandType.MAKE_CALL,
                rawInput = trimmed,
                parameters = mapOf("phoneNumber" to phoneNumber),
                isConfirmed = isConfirmed
            )
        }

        // 3. OPEN_WEBSITE: e.g. "Open website https://promaster.app", "Open website google.com", "Go to website https://...", "Open url https://..."
        if (lower.contains("website") || lower.contains("url ") || lower.startsWith("browse to ") ||
            lower.startsWith("open http://") || lower.startsWith("open https://")
        ) {
            val url = extractUrl(trimmed)
            return AutomationCommand(
                type = AutomationCommandType.OPEN_WEBSITE,
                rawInput = trimmed,
                parameters = mapOf("url" to url),
                isConfirmed = isConfirmed
            )
        }

        // 4. CREATE_ALARM: e.g. "Create alarm for 7:00 AM", "Set an alarm for 8:30 PM", "Create alarm", "Set alarm"
        if (lower.contains("alarm") || lower.contains("wake me up")) {
            val time = extractTime(lower)
            val params = mutableMapOf<String, String>()
            if (time != null) {
                params["time"] = time
            }
            return AutomationCommand(
                type = AutomationCommandType.CREATE_ALARM,
                rawInput = trimmed,
                parameters = params,
                isConfirmed = isConfirmed
            )
        }

        // 5. START_LANGUAGE_LESSON: e.g. "Start my English lesson", "Begin Spanish lesson", "Start language lesson"
        if ((lower.contains("start") || lower.contains("begin")) && (lower.contains("lesson") || lower.contains("class"))) {
            val language = when {
                lower.contains("english") -> "English"
                lower.contains("spanish") -> "Spanish"
                lower.contains("tamil") -> "Tamil"
                lower.contains("french") -> "French"
                lower.contains("german") -> "German"
                else -> "Current"
            }
            return AutomationCommand(
                type = AutomationCommandType.START_LANGUAGE_LESSON,
                rawInput = trimmed,
                parameters = mapOf("language" to language),
                isConfirmed = isConfirmed
            )
        }

        // 6. OPEN_SETTINGS: e.g. "Open settings", "Open wifi settings", "Open bluetooth settings", "Open sound settings"
        if (lower.contains("settings") || lower.equals("settings", ignoreCase = true)) {
            val settingType = when {
                lower.contains("wifi") || lower.contains("wi-fi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("sound") || lower.contains("volume") -> "sound"
                lower.contains("display") || lower.contains("brightness") -> "display"
                lower.contains("app") -> "apps"
                else -> "general"
            }
            return AutomationCommand(
                type = AutomationCommandType.OPEN_SETTINGS,
                rawInput = trimmed,
                parameters = mapOf("settingType" to settingType),
                isConfirmed = isConfirmed
            )
        }

        // 7. OPEN_APP: e.g. "Open YouTube", "Launch Chrome", "Open WhatsApp", "Launch Camera", "Open Spotify"
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start app ")) {
            val appName = extractAppName(trimmed)
            return AutomationCommand(
                type = AutomationCommandType.OPEN_APP,
                rawInput = trimmed,
                parameters = mapOf("appName" to appName),
                isConfirmed = isConfirmed
            )
        }

        // 8. Unsupported queries
        return AutomationCommand(
            type = AutomationCommandType.UNSUPPORTED,
            rawInput = trimmed,
            parameters = mapOf("reason" to "Unrecognized command"),
            isConfirmed = isConfirmed
        )
    }

    private fun extractUrl(input: String): String {
        val regex = Regex("""(?:website|url|browse to|open)\s+(https?://[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(input)
        val rawUrl = match?.groupValues?.getOrNull(1) ?: input.substringAfter("website").substringAfter("url").trim()
        val cleaned = rawUrl.trim(' ', '"', '\'')
        return if (!cleaned.startsWith("http://", ignoreCase = true) && !cleaned.startsWith("https://", ignoreCase = true)) {
            "https://$cleaned"
        } else {
            cleaned
        }
    }

    private fun extractTime(text: String): String? {
        val regex = Regex("""\b(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\b""")
        val match = regex.find(text)
        return match?.value
    }

    private fun extractAppName(input: String): String {
        return input
            .replaceFirst(Regex("""^(?:open|launch|start app|open app)\s+""", RegexOption.IGNORE_CASE), "")
            .trim(' ', '.', '!')
    }

    private fun extractDigitsOrPhone(input: String): String {
        val regex = Regex("""[\+0-9\-\s\(\)]{4,}""")
        val match = regex.find(input)
        return match?.value?.trim() ?: ""
    }
}
