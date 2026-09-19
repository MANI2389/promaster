package com.example.promaster.presentation.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promaster.data.mock.MockDataProvider
import com.example.promaster.data.service.DefaultPromasterAiService
import com.example.promaster.domain.model.AIResponse
import com.example.promaster.domain.service.AIService
import com.example.promaster.presentation.components.GlassCard
import com.example.promaster.presentation.components.PromasterGradientButton
import com.example.promaster.presentation.components.PromasterTopBar
import com.example.promaster.theme.PromasterAccent
import com.example.promaster.theme.PromasterPrimary
import com.example.promaster.theme.PromasterSecondary
import kotlinx.coroutines.launch

@Composable
fun GrammarCorrectionScreen(
    onNavigateBack: () -> Unit,
    aiService: AIService = remember { DefaultPromasterAiService() }
) {
    var inputText by remember { mutableStateOf("I am go to college yesterday.") }
    var aiResponse by remember { mutableStateOf<AIResponse?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val motherTongue = MockDataProvider.currentUser.nativeLanguage.ifBlank { "Tamil" }
    val targetLanguage = MockDataProvider.currentUser.targetLanguage.ifBlank { "English" }

    Scaffold(
        topBar = {
            PromasterTopBar(
                title = "AI Grammar Corrector",
                subtitle = "Instant Sentence Analysis & Polishing",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                streakDays = 7
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Input Sentence",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Write any sentence to verify and polish...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp)
            )

            PromasterGradientButton(
                text = if (isAnalyzing) "Analyzing Grammar with AI..." else "Check with PROMASTER AI",
                onClick = {
                    coroutineScope.launch {
                        isAnalyzing = true
                        val res = aiService.correctGrammar(
                            text = inputText,
                            targetLanguage = targetLanguage,
                            motherTongue = motherTongue
                        )
                        aiResponse = res.getOrNull()
                        isAnalyzing = false
                    }
                }
            )

            aiResponse?.let { item ->
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PromasterAccent)
                        Text(
                            text = "Correction & Linguistic Feedback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    item.correction?.let { corrected ->
                        Text(
                            text = "Corrected Sentence:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PromasterAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PromasterAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = corrected,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PromasterAccent,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item.explanation?.let { exp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Explanation:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exp,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item.motherTongueExplanation?.let { nativeExp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PromasterSecondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PromasterSecondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = PromasterSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$motherTongue Explanation:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PromasterSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nativeExp,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item.nextAction?.let { action ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PromasterPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Next Action: $action",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = PromasterPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
