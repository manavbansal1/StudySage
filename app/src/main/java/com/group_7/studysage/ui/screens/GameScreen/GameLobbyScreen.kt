package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.group_7.studysage.data.models.GameSessionData
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.viewmodels.GameLobbyViewModel
import com.group_7.studysage.viewmodels.GameLobbyViewModelFactory
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    navController: NavController,
    gameType: GameType,
    authViewModel: AuthViewModel,
    groupId: String
) {
    val gameLobbyViewModel: GameLobbyViewModel = viewModel(
        factory = GameLobbyViewModelFactory(GameApiService(), authViewModel)
    )

    val lobbyUiState by gameLobbyViewModel.lobbyUiState.collectAsState()

    LaunchedEffect(Unit) {
        gameLobbyViewModel.loadActiveSessions(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${gameType.name} Lobby") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Show create game dialog
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create Game")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (lobbyUiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            lobbyUiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lobbyUiState.activeSessions) { session ->
                    GameSessionCard(session = session) {
                        gameLobbyViewModel.joinGame(groupId, session.id) {
                            navController.navigate("game_play/${session.id}")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSessionCard(session: GameSessionData, onJoin: () -> Unit) {
    Card(
        onClick = onJoin,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = session.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Host: ${session.hostId}", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${session.players.size}/${session.maxPlayers}")
        }
    }
}