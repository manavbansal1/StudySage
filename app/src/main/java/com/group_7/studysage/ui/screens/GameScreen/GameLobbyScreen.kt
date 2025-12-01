package com.group_7.studysage.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group_7.studysage.data.models.GameSessionData
import com.group_7.studysage.data.models.GameSettings
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.data.repository.Note
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
    val availableNotes by gameLobbyViewModel.availableNotes.collectAsState()
    val isLoadingNotes by gameLobbyViewModel.isLoadingNotes.collectAsState()
    val currentUserId = authViewModel.currentUser.value?.uid

    var showCreateGameDialog by remember { mutableStateOf(false) }

    // Refresh when coming back to this screen
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "game_lobby/{gameType}/{groupId}") {
                gameLobbyViewModel.loadActiveSessions(groupId)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        gameLobbyViewModel.loadActiveSessions(groupId)
        gameLobbyViewModel.loadAvailableNotes()
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
                showCreateGameDialog = true
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
                // Filter out sessions where current user is the host
                val sessionsToShow = lobbyUiState.activeSessions.filter { session ->
                    session.hostId != currentUserId
                }
                
                items(sessionsToShow) { session ->
                    GameSessionCard(session = session) {
                        gameLobbyViewModel.joinGame(groupId, session.id) {
                            navController.navigate("game_play/${session.id}")
                        }
                    }
                }
            }
        }

        // Create Game Dialog
        if (showCreateGameDialog) {
            CreateGameDialog(
                gameType = gameType,
                availableNotes = availableNotes,
                isLoadingNotes = isLoadingNotes,
                onDismiss = { showCreateGameDialog = false },
                onCreateGame = { selectedNote, settings ->
                    gameLobbyViewModel.createGame(
                        groupId = groupId,
                        gameType = gameType,
                        settings = settings,
                        documentId = selectedNote?.id,
                        documentName = selectedNote?.title,
                        onGameCreated = { sessionId ->
                            showCreateGameDialog = false
                            navController.navigate("game_play/$sessionId/$groupId")
                        }
                    )
                }
            )
        }

        // Show animated loading dialog while creating game
        if (lobbyUiState.isCreating) {
            Dialog(onDismissRequest = { }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Text(
                            "Creating Game",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                "Setting up the lobby...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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
    // Determine max players based on game type
    val maxPlayers = when (session.gameType) {
        GameType.STUDY_TAC_TOE -> 2
        GameType.QUIZ_RACE -> 8
        else -> session.maxPlayers
    }
    
    val isFull = session.players.size >= maxPlayers
    
    Card(
        onClick = onJoin,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFull) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        )
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
                if (isFull) {
                    Text(
                        text = "FULL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(text = "${session.players.size}/$maxPlayers")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameDialog(
    gameType: GameType,
    availableNotes: List<Note>,
    isLoadingNotes: Boolean,
    onDismiss: () -> Unit,
    onCreateGame: (Note?, GameSettings) -> Unit
) {
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var questionTimeLimit by remember { mutableStateOf("30") }
    var numberOfQuestions by remember { mutableStateOf("10") }

    // Determine player limits based on game type
    val playerLimitText = when (gameType) {
        GameType.STUDY_TAC_TOE -> "2 players only"
        GameType.QUIZ_RACE -> "1-8 players"
        else -> "Multiple players"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create ${gameType.name} Game") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player limit info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Player Limit: $playerLimitText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Document Selection Section
                Text(
                    text = "Select Document (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (isLoadingNotes) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (availableNotes.isEmpty()) {
                    Text(
                        text = "No documents available. You can still create a game without a document.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNote = null },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedNote == null)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No Document",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        items(availableNotes) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNote = note },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedNote?.id == note.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (note.fileType.isNotEmpty()) {
                                            Text(
                                                text = note.fileType.uppercase(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Game Settings Section
                Text(
                    text = "Game Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = numberOfQuestions,
                    onValueChange = { numberOfQuestions = it },
                    label = { Text("Number of Questions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = questionTimeLimit,
                    onValueChange = { questionTimeLimit = it },
                    label = { Text("Time per Question (seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val settings = GameSettings(
                    questionTimeLimit = questionTimeLimit.toIntOrNull() ?: 30,
                    numberOfQuestions = numberOfQuestions.toIntOrNull() ?: 10
                )
                onCreateGame(selectedNote, settings)
            }) {
                Text("Create Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}