package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    // Show game results
                }
                gameUiState.currentQuestion != null -> {
                    Text("Quiz Game Screen Placeholder")
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
        gameUiState.currentSession?.players?.forEach { player ->
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
