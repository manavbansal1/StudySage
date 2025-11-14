package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group_7.studysage.viewmodels.GamePlayViewModel
import com.group_7.studysage.viewmodels.GamePlayViewModelFactory
import com.group_7.studysage.data.websocket.GameWebSocketManager
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.data.models.GameUiState
import com.group_7.studysage.data.models.Player
import com.group_7.studysage.ui.screens.GameScreen.QuizRaceGameScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    navController: NavController,
    sessionId: String,
    authViewModel: AuthViewModel,
    groupId: String
) {
    val gamePlayViewModel: GamePlayViewModel = viewModel(
        factory = GamePlayViewModelFactory(GameWebSocketManager(), authViewModel)
    )

    val gameUiState by gamePlayViewModel.gameUiState.collectAsState()

    LaunchedEffect(Unit) {
        gamePlayViewModel.connect(groupId, sessionId)
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
                        onBackToLobby = { navController.popBackStack() }
                    )
                }
                gameUiState.currentQuestion != null -> {
                    // Show Quiz Race game screen based on game type
                    when (gameUiState.currentSession?.gameType) {
                        GameType.QUIZ_RACE,
                        GameType.TEAM_TRIVIA,
                        GameType.SURVIVAL_MODE,
                        GameType.SPEED_QUIZ -> {
                            QuizRaceGameScreen(
                                gameUiState = gameUiState,
                                onAnswerSelected = { answerIndex, timeElapsed ->
                                    gamePlayViewModel.submitAnswer(answerIndex, timeElapsed)
                                }
                            )
                        }
                        GameType.FLASHCARD_BATTLE -> {
                            // TODO: Implement Flashcard Battle screen
                            Text("Flashcard Battle - Coming Soon", modifier = Modifier.align(Alignment.Center))
                        }
                        GameType.SPEED_MATCH -> {
                            // TODO: Implement Speed Match screen
                            Text("Speed Match - Coming Soon", modifier = Modifier.align(Alignment.Center))
                        }
                        else -> {
                            Text("Unknown game type", modifier = Modifier.align(Alignment.Center))
                        }
                    }
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
    gameUiState: GameUiState,
    onReadyClick: (Boolean) -> Unit,
    onStartClick: () -> Unit
) {
    val playersList = gameUiState.currentSession?.players?.values?.toList() ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Waiting for players...",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Players (${playersList.size}/${gameUiState.currentSession?.maxPlayers ?: 8})",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                playersList.forEach { player ->
                    PlayerRow(player = player)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onReadyClick(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ready")
        }

        if (gameUiState.isHost) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (player.isHost) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Host",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (player.isReady) {
            PlayerStatusChip(text = "Ready", color = MaterialTheme.colorScheme.primary)
        } else {
            PlayerStatusChip(text = "Not Ready", color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
fun GameResultsScreen(
    gameUiState: GameUiState,
    onBackToLobby: () -> Unit
) {
    val finalPlayers = gameUiState.finalResults?.players?.sortedByDescending { it.score } ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Game Finished!",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Final Leaderboard",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                finalPlayers.forEachIndexed { index, player ->
                    FinalScoreRow(
                        rank = index + 1,
                        playerName = player.name,
                        score = player.score
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToLobby,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Lobby")
        }
    }
}

@Composable
private fun FinalScoreRow(
    rank: Int,
    playerName: String,
    score: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val medal = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$rank."
            }
            Text(
                text = medal,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = "$score pts",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun PlayerStatusChip(text: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}