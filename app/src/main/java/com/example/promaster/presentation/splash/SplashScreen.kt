package com.example.promaster.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.promaster.theme.PromasterPrimary

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val startupState by viewModel.startupState.collectAsState()

    // Authoritative navigation trigger
    LaunchedEffect(startupState) {
        when (startupState) {
            StartupState.UNAUTHENTICATED -> onNavigateToLogin()
            StartupState.ONBOARDING_REQUIRED -> onNavigateToOnboarding()
            StartupState.READY -> onNavigateToHome()
            StartupState.STARTING,
            StartupState.AUTH_CHECKING,
            StartupState.AUTHENTICATED -> {
                // Waiting for resolution
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        PromasterPrimary.copy(alpha = 0.15f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // App Icon Orb
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale),
                shape = CircleShape,
                color = PromasterPrimary.copy(alpha = 0.18f),
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "⚡", fontSize = 48.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "PROMASTER",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your AI Friend. Your Language Coach. Your Voice Assistant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Subtle loading spinner during auth & state restoration
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = PromasterPrimary,
                strokeWidth = 2.5.dp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
