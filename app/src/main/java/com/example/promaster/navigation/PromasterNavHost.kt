package com.example.promaster.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.promaster.domain.model.TaskCategory
import com.example.promaster.presentation.auth.LoginScreen
import com.example.promaster.presentation.auth.RegisterScreen
import com.example.promaster.presentation.automation.AutomationScreen
import com.example.promaster.presentation.components.PromasterBottomBar
import com.example.promaster.presentation.conversation.AiConversationScreen
import com.example.promaster.presentation.conversation.GrammarCorrectionScreen
import com.example.promaster.presentation.friend.AiFriendScreen
import com.example.promaster.presentation.home.HomeScreen
import com.example.promaster.presentation.home.HomeViewModel
import com.example.promaster.presentation.language.LanguageSelectionScreen
import com.example.promaster.presentation.learning.*
import com.example.promaster.presentation.onboarding.OnboardingScreen
import com.example.promaster.presentation.profile.ProfileScreen
import com.example.promaster.presentation.progress.ProgressScreen
import com.example.promaster.presentation.settings.SettingsScreen
import com.example.promaster.presentation.splash.SplashScreen
import com.example.promaster.presentation.voice.VoiceAssistantScreen

@Composable
fun PromasterNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Plan30Day.route,
        Screen.TodayTasks.route,
        Screen.AiFriend.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                PromasterBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onSelectLanguage = {
                        navController.navigate(Screen.LanguageSelection.route)
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        val onboardingComplete = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.isOnboardingCompleted == true
                        val targetRoute = if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onGuestLogin = {
                        val onboardingComplete = com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.isOnboardingCompleted == true
                        val targetRoute = if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        // Newly registered users must configure their profile and goals
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onContinueLearning = {
                        navController.navigate(Screen.TodayTasks.route)
                    },
                    onPracticeSpeaking = {
                        navController.navigate(Screen.SpeakingPractice.route)
                    },
                    onTalkToAi = {
                        navController.navigate(Screen.AiConversation.route)
                    },
                    onVoiceAssistant = {
                        navController.navigate(Screen.VoiceAssistant.route)
                    },
                    onLanguageClick = {
                        navController.navigate(Screen.LanguageSelection.route)
                    },
                    onViewAllTasks = {
                        navController.navigate(Screen.TodayTasks.route)
                    },
                    onAutomationClick = {
                        navController.navigate(Screen.Automation.route)
                    }
                )
            }

            composable(Screen.LanguageSelection.route) {
                LanguageSelectionScreen(
                    onLanguageSelected = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Plan30Day.route) {
                Plan30DayScreen(
                    onDayClick = {
                        navController.navigate(Screen.TodayTasks.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.TodayTasks.route) {
                TodayTasksScreen(
                    onTaskClick = { category ->
                        when (category) {
                            TaskCategory.VOCABULARY -> navController.navigate(Screen.Vocabulary.route)
                            TaskCategory.GRAMMAR -> navController.navigate(Screen.Grammar.route)
                            TaskCategory.LISTENING -> navController.navigate(Screen.Listening.route)
                            TaskCategory.SPEAKING -> navController.navigate(Screen.SpeakingPractice.route)
                            TaskCategory.CONVERSATION, TaskCategory.AI_PRACTICE -> navController.navigate(Screen.AiConversation.route)
                            TaskCategory.REVIEW -> navController.navigate(Screen.Vocabulary.route)
                            TaskCategory.QUIZ -> navController.navigate(Screen.GrammarCorrection.route)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Vocabulary.route) {
                VocabularyScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Grammar.route) {
                GrammarScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Listening.route) {
                ListeningScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SpeakingPractice.route) {
                SpeakingPracticeScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AiConversation.route) {
                AiConversationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.GrammarCorrection.route) {
                GrammarCorrectionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Progress.route) {
                ProgressScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AiFriend.route) {
                AiFriendScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.VoiceAssistant.route) {
                VoiceAssistantScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRoute = { route ->
                        navController.navigate(route)
                    }
                )
            }

            composable(Screen.Automation.route) {
                AutomationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAutomation = {
                        navController.navigate(Screen.Automation.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLanguageSelection = {
                        navController.navigate(Screen.LanguageSelection.route)
                    },
                    onNavigateToAutomation = {
                        navController.navigate(Screen.Automation.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToPlan = {
                        navController.navigate(Screen.Plan30Day.route)
                    },
                    onNavigateToProgress = {
                        navController.navigate(Screen.Progress.route)
                    },
                    onSignOut = {
                        try {
                            com.example.promaster.data.firebase.FirebaseManager.auth?.signOut()
                        } catch (_: Exception) {}
                        com.example.promaster.data.preferences.UserPreferencesRepository.getInstanceOrNull()?.clearUserSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
