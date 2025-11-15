package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group_7.studysage.viewmodels.GamePlayViewModel
import com.group_7.studysage.viewmodels.GamePlayViewModelFactory
import com.group_7.studysage.data.websocket.GameWebSocketManager
import com.group_7.studysage.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    navController: NavController,
    gameCode: String,
    authViewModel: AuthViewModel
) {
    val gamePlayViewModel: GamePlayViewModel = viewModel(
        factory = GamePlayViewModelFactory(GameWebSocketManager(), authViewModel)
    )

    val gameUiState by gamePlayViewModel.gameUiState.collectAsState()

    LaunchedEffect(Unit) {
        gamePlayViewModel.connectToStandaloneGame(gameCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gameUiState.currentSession?.name ?: "Game") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                gameUiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                gameUiState.error != null -> {
                    Text(
                        text = gameUiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                gameUiState.gameFinished -> {
                    GameResultsScreen(
                        gameUiState = gameUiState,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                gameUiState.currentQuestion != null -> {
                    QuizGameScreen(
                        gameUiState = gameUiState,
                        onAnswerClick = { answerIndex ->
                            gamePlayViewModel.submitAnswer(answerIndex, 0) // TODO: track time elapsed
                        }
                    )
                }
                else -> {
                    // Waiting for players screen
                    WaitingForPlayersScreen(
                        gameUiState = gameUiState,
                        onReadyClick = { gamePlayViewModel.setPlayerReady(it) },
                        onStartClick = { gamePlayViewModel.startGame() }
                    )
                }
            }
        }
    }
}

@Composable
fun WaitingForPlayersScreen(
    gameUiState: com.group_7.studysage.data.models.GameUiState,
    onReadyClick: (Boolean) -> Unit,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Waiting for players...", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        gameUiState.currentSession?.players?.values?.forEach { player ->
            Text(text = "${player.name} ${if (player.isReady) "(Ready)" else ""}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onReadyClick(true) }) {
            Text("Ready")
        }
        if (gameUiState.isHost) {
            Button(onClick = onStartClick) {
                Text("Start Game")
            }
        }
    }
}

@Composable
fun QuizGameScreen(
    gameUiState: com.group_7.studysage.data.models.GameUiState,
    onAnswerClick: (Int) -> Unit
) {
    val questionData = gameUiState.currentQuestion ?: return
    val question = questionData.question
    val isAnswered = gameUiState.isAnswered
    val selectedAnswer = gameUiState.selectedAnswerIndex
    val lastResult = gameUiState.lastResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Question header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Question ${questionData.questionNumber} of ${questionData.totalQuestions}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timer
            Text(
                text = "Time: ${gameUiState.timeRemaining}s",
                style = MaterialTheme.typography.bodyLarge,
                color = if (gameUiState.timeRemaining <= 5) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }

        // Question text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        // Answer options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            question.options.forEachIndexed { index, option ->
                val isSelected = selectedAnswer == index
                val isCorrect = isAnswered && index == question.correctAnswer
                val isWrong = isAnswered && isSelected && index != question.correctAnswer

                val buttonColors = when {
                    isCorrect -> ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green
                    )
                    isWrong -> ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336) // Red
                    )
                    isSelected -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                    else -> ButtonDefaults.buttonColors()
                }

                Button(
                    onClick = { if (!isAnswered) onAnswerClick(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = buttonColors,
                    enabled = !isAnswered,
                    border = if (isSelected && !isAnswered) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Result feedback
        if (isAnswered && lastResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (lastResult.isCorrect) 
                        Color(0xFF4CAF50).copy(alpha = 0.2f) 
                    else 
                        Color(0xFFF44336).copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lastResult.isCorrect) "Correct! ✓" else "Incorrect ✗",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (lastResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        text = "+${lastResult.points} points",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (question.explanation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun GameResultsScreen(
    gameUiState: com.group_7.studysage.data.models.GameUiState,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Game Complete!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Show leaderboard if available
        if (gameUiState.leaderboard.isNotEmpty()) {
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    gameUiState.leaderboard.take(5).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.playerName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "${entry.score} pts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (entry != gameUiState.leaderboard.take(5).last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Lobby")
        }
    }
}
