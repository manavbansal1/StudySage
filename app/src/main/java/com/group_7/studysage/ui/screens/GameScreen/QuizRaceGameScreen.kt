package com.group_7.studysage.ui.screens.GameScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group_7.studysage.data.models.GameUiState
import com.group_7.studysage.data.models.LeaderboardEntry
import com.group_7.studysage.data.models.QuizQuestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuizRaceGameScreen(
    gameUiState: GameUiState,
    onAnswerSelected: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var timeRemaining by remember { mutableIntStateOf(gameUiState.currentQuestion?.timeLimit ?: 30) }
    var questionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showAnswerFeedback by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Timer effect
    LaunchedEffect(gameUiState.currentQuestion?.question?.id) {
        // Reset timer when question changes
        timeRemaining = gameUiState.currentQuestion?.timeLimit ?: 30
        questionStartTime = System.currentTimeMillis()
        showAnswerFeedback = false

        // Countdown timer
        while (timeRemaining > 0 && !gameUiState.isAnswered) {
            delay(1000)
            timeRemaining--
        }
    }

    // Show feedback effect
    LaunchedEffect(gameUiState.lastResult) {
        if (gameUiState.lastResult != null) {
            showAnswerFeedback = true
            delay(2000) // Show feedback for 2 seconds
            showAnswerFeedback = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Progress and Timer Section
            QuestionProgressBar(
                currentQuestion = gameUiState.currentQuestion?.questionNumber ?: 0,
                totalQuestions = gameUiState.currentQuestion?.totalQuestions ?: 0,
                timeRemaining = timeRemaining,
                timeLimit = gameUiState.currentQuestion?.timeLimit ?: 30
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Question Section
            gameUiState.currentQuestion?.let { questionData ->
                QuestionCard(
                    question = questionData.question,
                    questionNumber = questionData.questionNumber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Answer Options Section
            gameUiState.currentQuestion?.question?.let { question ->
                AnswerOptionsGrid(
                    options = question.options,
                    selectedIndex = gameUiState.selectedAnswerIndex,
                    correctIndex = if (showAnswerFeedback) question.correctAnswer else null,
                    isAnswered = gameUiState.isAnswered,
                    onOptionSelected = { index ->
                        if (!gameUiState.isAnswered) {
                            val timeElapsed = System.currentTimeMillis() - questionStartTime
                            onAnswerSelected(index, timeElapsed)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Leaderboard
            val hostPlayerId = gameUiState.currentSession?.players?.values?.firstOrNull { player ->
                player.isHost
            }?.id

            MiniLeaderboard(
                players = gameUiState.leaderboard.take(3),
                currentUserId = hostPlayerId
            )
        }

        // Answer Feedback Overlay
        AnimatedVisibility(
            visible = showAnswerFeedback,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AnswerFeedbackCard(
                isCorrect = gameUiState.lastResult?.isCorrect == true,
                points = gameUiState.lastResult?.points ?: 0,
                explanation = gameUiState.currentQuestion?.question?.explanation
            )
        }
    }
}

@Composable
private fun QuestionProgressBar(
    currentQuestion: Int,
    totalQuestions: Int,
    timeRemaining: Int,
    timeLimit: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question $currentQuestion of $totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TimerChip(
                timeRemaining = timeRemaining,
                timeLimit = timeLimit
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = currentQuestion.toFloat() / totalQuestions.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun TimerChip(
    timeRemaining: Int,
    timeLimit: Int
) {
    val progress = timeRemaining.toFloat() / timeLimit.toFloat()
    val timerColor = when {
        progress > 0.6f -> MaterialTheme.colorScheme.primary
        progress > 0.3f -> Color(0xFFFFA726) // Orange
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = timerColor.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, timerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = timerColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$timeRemaining",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuizQuestion,
    questionNumber: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Question number badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "#$questionNumber",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (question.difficulty.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                DifficultyChip(difficulty = question.difficulty)
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val (color, icon) = when (difficulty.lowercase()) {
        "easy" -> Color(0xFF4CAF50) to "⭐"
        "medium" -> Color(0xFFFFA726) to "⭐⭐"
        "hard" -> Color(0xFFF44336) to "⭐⭐⭐"
        else -> MaterialTheme.colorScheme.secondary to "⭐"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 12.sp
            )
            Text(
                text = difficulty.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AnswerOptionsGrid(
    options: List<String>,
    selectedIndex: Int?,
    correctIndex: Int?,
    isAnswered: Boolean,
    onOptionSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            AnswerOptionCard(
                option = option,
                optionLetter = ('A' + index).toString(),
                isSelected = index == selectedIndex,
                isCorrect = index == correctIndex,
                showResult = isAnswered && correctIndex != null,
                onClick = { onOptionSelected(index) },
                enabled = !isAnswered
            )
        }
    }
}

@Composable
private fun AnswerOptionCard(
    option: String,
    optionLetter: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    showResult: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val backgroundColor = when {
        showResult && isCorrect == true -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        showResult && isSelected && isCorrect == false -> Color(0xFFF44336).copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect == true -> Color(0xFF4CAF50)
        showResult && isSelected && isCorrect == false -> Color(0xFFF44336)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor
        ),
        enabled = enabled,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Option letter badge
            Surface(
                shape = CircleShape,
                color = when {
                    showResult && isCorrect == true -> Color(0xFF4CAF50)
                    showResult && isSelected && isCorrect == false -> Color(0xFFF44336)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showResult && isCorrect == true) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                    } else if (showResult && isSelected && isCorrect == false) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text = optionLetter,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnswerFeedbackCard(
    isCorrect: Boolean,
    points: Int,
    explanation: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                Color(0xFF4CAF50).copy(alpha = 0.95f)
            else
                Color(0xFFF44336).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isCorrect) "Correct!" else "Incorrect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (points > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+$points points",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            if (!explanation.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MiniLeaderboard(
    players: List<LeaderboardEntry>,
    currentUserId: String?
) {
    if (players.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFFD700) // Gold color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            players.take(3).forEachIndexed { index, player ->
                LeaderboardRow(
                    rank = index + 1,
                    playerName = player.playerName,
                    score = player.score,
                    isCurrentUser = player.playerId == currentUserId
                )
                if (index < players.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    playerName: String,
    score: Int,
    isCurrentUser: Boolean
) {
    val rankIcon = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentUser)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rankIcon,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = "$score pts",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}