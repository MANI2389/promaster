package com.example.promaster.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object LanguageSelection : Screen("language_selection")
    data object Plan30Day : Screen("plan_30_day")
    data object TodayTasks : Screen("today_tasks")
    data object Vocabulary : Screen("vocabulary")
    data object Grammar : Screen("grammar")
    data object Listening : Screen("listening")
    data object SpeakingPractice : Screen("speaking_practice")
    data object AiConversation : Screen("ai_conversation")
    data object GrammarCorrection : Screen("grammar_correction")
    data object Progress : Screen("progress")
    data object AiFriend : Screen("ai_friend")
    data object VoiceAssistant : Screen("voice_assistant")
    data object Automation : Screen("automation")
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
}
