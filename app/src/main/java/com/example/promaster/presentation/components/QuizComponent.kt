package com.example.promaster.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promaster.domain.engine.ActivityXpPolicy
import com.example.promaster.domain.model.QuizQuestion
import com.example.promaster.domain.model.QuizQuestionType
import com.example.promaster.domain.model.QuizResult
import com.example.promaster.theme.*

@Composable
fun QuizComponent(
    quizId: String,
    title: String,
    questions: List<QuizQuestion>,
    onQuizFinished: (QuizResult) -> Unit,
    modifier: Modifier = Modifier,
    activityType: String = "QUIZ"
) {
    if (questions.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "No questions available for this quiz.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf("") }
    var textInputAnswer by remember { mutableStateOf("") }
    var showExplanation by remember { mutableStateOf(false) }
    var isCurrentCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showMotherTongueHint by remember { mutableStateOf(false) }

    // Quiz Metrics
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var attemptCount by remember { mutableIntStateOf(1) }
    var isFinished by remember { mutableStateOf(false) }
    var finalResult by remember { mutableStateOf<QuizResult?>(null) }

    val currentQuestion = questions.getOrNull(currentIndex) ?: questions.first()

    if (isFinished && finalResult != null) {
        val result = finalResult!!
        val xpResult = ActivityXpPolicy.calculateXp(
            activityType = activityType,
            score = result.score,
            attemptCount = result.attempts
        )

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (result.completion) PromasterAccent else PromasterError)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (result.completion) PromasterAccent.copy(alpha = 0.15f) else PromasterError.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (result.completion) Icons.Default.EmojiEvents else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (result.completion) PromasterAccent else PromasterError,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = if (result.completion) "Quiz Passed!" else "Needs More Practice",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Score: ${result.score}% (${result.correctAnswers}/${result.totalQuestions} correct)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (result.completion) PromasterAccent else PromasterError
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Attempts:", style = MaterialTheme.typography.bodyMedium)
                            Text("${result.attempts}", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("XP Earned:", style = MaterialTheme.typography.bodyMedium)
                            Text("+${xpResult.earnedXp} XP", fontWeight = FontWeight.Bold, color = PromasterAccent)
                        }
                        Text(
                            text = xpResult.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentIndex = 0
                            selectedOption = ""
                            textInputAnswer = ""
                            showExplanation = false
                            isCurrentCorrect = null
                            showMotherTongueHint = false
                            correctCount = 0
                            wrongCount = 0
                            attemptCount++
                            isFinished = false
                            finalResult = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }

                    Button(
                        onClick = {
                            onQuizFinished(result.copy(earnedXp = xpResult.earnedXp))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                    ) {
                        Text("Finish")
                    }
                }
            }
        }
        return
    }

    // Active Quiz View
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Progress & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PromasterPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = currentQuestion.type.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PromasterPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = PromasterPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Question Prompt
            Text(
                text = currentQuestion.prompt,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Mother tongue hint
            if (currentQuestion.motherTongueHint.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showMotherTongueHint = !showMotherTongueHint }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = PromasterSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showMotherTongueHint) "Hint: ${currentQuestion.motherTongueHint}" else "Show Native Hint",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = PromasterSecondary
                    )
                }
            }

            // Options / Inputs by Question Type
            when (currentQuestion.type) {
                QuizQuestionType.MULTIPLE_CHOICE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentQuestion.options.forEach { option ->
                            val isSelected = selectedOption == option
                            val isAnswer = option.equals(currentQuestion.correctAnswer, ignoreCase = true)
                            val optionBorderColor = when {
                                showExplanation && isAnswer -> PromasterAccent
                                showExplanation && isSelected && !isAnswer -> PromasterError
                                isSelected -> PromasterPrimary
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            }
                            val optionBgColor = when {
                                showExplanation && isAnswer -> PromasterAccent.copy(alpha = 0.12f)
                                showExplanation && isSelected && !isAnswer -> PromasterError.copy(alpha = 0.12f)
                                isSelected -> PromasterPrimary.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !showExplanation) {
                                        selectedOption = option
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = optionBgColor,
                                border = BorderStroke(1.5.dp, optionBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (showExplanation && isAnswer) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PromasterAccent)
                                    } else if (showExplanation && isSelected && !isAnswer) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = PromasterError)
                                    }
                                }
                            }
                        }
                    }
                }

                QuizQuestionType.FILL_IN_THE_BLANK,
                QuizQuestionType.SENTENCE_CORRECTION,
                QuizQuestionType.TRANSLATION -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textInputAnswer,
                            onValueChange = { textInputAnswer = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !showExplanation,
                            placeholder = {
                                Text(
                                    when (currentQuestion.type) {
                                        QuizQuestionType.FILL_IN_THE_BLANK -> "Type missing word..."
                                        QuizQuestionType.SENTENCE_CORRECTION -> "Type corrected sentence..."
                                        QuizQuestionType.TRANSLATION -> "Type translation..."
                                        else -> "Type answer..."
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (currentQuestion.options.isNotEmpty()) {
                            Text(
                                text = "Or select:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentQuestion.options.forEach { opt ->
                                    FilterChip(
                                        selected = textInputAnswer == opt,
                                        onClick = { if (!showExplanation) textInputAnswer = opt },
                                        label = { Text(opt) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation Section
            AnimatedVisibility(visible = showExplanation) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrentCorrect == true) PromasterAccent.copy(alpha = 0.12f) else PromasterError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isCurrentCorrect == true) PromasterAccent.copy(alpha = 0.3f) else PromasterError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isCurrentCorrect == true) "Correct!" else "Incorrect. Expected: \"${currentQuestion.correctAnswer}\"",
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentCorrect == true) PromasterAccent else PromasterError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (currentQuestion.explanation.isNotBlank()) {
                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (!showExplanation) {
                Button(
                    onClick = {
                        val givenAnswer = if (currentQuestion.type == QuizQuestionType.MULTIPLE_CHOICE) {
                            selectedOption.trim()
                        } else {
                            textInputAnswer.trim()
                        }
                        val isCorrect = givenAnswer.equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
                        isCurrentCorrect = isCorrect
                        if (isCorrect) correctCount++ else wrongCount++
                        showExplanation = true
                    },
                    enabled = (currentQuestion.type == QuizQuestionType.MULTIPLE_CHOICE && selectedOption.isNotBlank()) ||
                            (currentQuestion.type != QuizQuestionType.MULTIPLE_CHOICE && textInputAnswer.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text("Check Answer")
                }
            } else {
                Button(
                    onClick = {
                        if (currentIndex < questions.lastIndex) {
                            currentIndex++
                            selectedOption = ""
                            textInputAnswer = ""
                            showExplanation = false
                            isCurrentCorrect = null
                            showMotherTongueHint = false
                        } else {
                            val total = questions.size
                            val score = if (total > 0) ((correctCount * 100) / total) else 0
                            val res = QuizResult(
                                quizId = quizId,
                                title = title,
                                score = score,
                                attempts = attemptCount,
                                correctAnswers = correctCount,
                                wrongAnswers = wrongCount,
                                totalQuestions = total,
                                completion = (score >= ActivityXpPolicy.MIN_PASSING_SCORE)
                            )
                            finalResult = res
                            isFinished = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PromasterPrimary)
                ) {
                    Text(if (currentIndex < questions.lastIndex) "Next Question" else "View Results")
                }
            }
        }
    }
}
