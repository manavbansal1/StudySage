package com.group_7.studysage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group_7.studysage.data.models.ContentSource
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.viewmodels.StandaloneGameViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * main screen that the user sees first
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(navController: NavController) {
    val viewModel: StandaloneGameViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showHostDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer Games", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = { navController.navigate("leaderboard") }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game Icon
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ready to Play?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Host a game or join with a code",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Host Game Button
            Button(
                onClick = { showHostDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Host a Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Join Game Button
            OutlinedButton(
                onClick = { showJoinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Join a Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Show loading/error states
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.gameCode?.let { code ->
                Spacer(modifier = Modifier.height(24.dp))
                GameCodeDisplay(
                    gameCode = code,
                    onNavigateToGame = {
                        navController.navigate("game_play/$code")
                    }
                )
            }
        }

        // Dialogs
        if (showHostDialog) {
            HostGameDialog(
                onDismiss = { showHostDialog = false },
                onHostGame = { gameType, contentSource, contentData, topicDescription ->
                    viewModel.hostGame(gameType, contentSource, contentData, topicDescription)
                    showHostDialog = false
                }
            )
        }

        if (showJoinDialog) {
            JoinGameDialog(
                onDismiss = { showJoinDialog = false },
                onJoinGame = { gameCode ->
                    viewModel.joinGame(gameCode) { success ->
                        if (success) {
                            showJoinDialog = false
                            navController.navigate("game_play/$gameCode")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GameCodeDisplay(gameCode: String, onNavigateToGame: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Game Created!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Share this code with your friends:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = gameCode,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToGame,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter Game Lobby")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostGameDialog(
    onDismiss: () -> Unit,
    onHostGame: (GameType, ContentSource, String?, String?) -> Unit
) {
    var selectedGameType by remember { mutableStateOf(GameType.QUIZ_RACE) }
    var selectedContentSource by remember { mutableStateOf(ContentSource.TEXT) }
    var topicText by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
    }

    val gameTypes = listOf(
        GameType.QUIZ_RACE to "Quiz Race",
        GameType.FLASHCARD_BATTLE to "Flashcard Battle",
        GameType.TEAM_TRIVIA to "Team Trivia",
        GameType.SPEED_MATCH to "Speed Match"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Host a Game", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Game Type Selection
                Text("Select Game Type", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                gameTypes.forEach { (type, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGameType == type,
                            onClick = { selectedGameType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Content Source Selection
                Text("Content Source", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContentSource == ContentSource.TEXT,
                        onClick = { selectedContentSource = ContentSource.TEXT }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Topics as Text")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContentSource == ContentSource.PDF,
                        onClick = { selectedContentSource = ContentSource.PDF }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload PDF")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Input
                when (selectedContentSource) {
                    ContentSource.TEXT -> {
                        OutlinedTextField(
                            value = topicText,
                            onValueChange = { topicText = it },
                            label = { Text("Describe topics for questions") },
                            placeholder = { Text("e.g., Photosynthesis, Cell Biology, etc.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                    ContentSource.PDF -> {
                        Button(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedPdfUri?.let { "PDF Selected" } ?: "Choose PDF")
                        }
                        selectedPdfUri?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it.lastPathSegment ?: "Selected PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val contentData = when (selectedContentSource) {
                        ContentSource.TEXT -> null
                        ContentSource.PDF -> selectedPdfUri?.toString()
                    }
                    val topic = if (selectedContentSource == ContentSource.TEXT) topicText else null

                    onHostGame(selectedGameType, selectedContentSource, contentData, topic)
                },
                enabled = when (selectedContentSource) {
                    ContentSource.TEXT -> topicText.isNotBlank()
                    ContentSource.PDF -> selectedPdfUri != null
                }
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameDialog(
    onDismiss: () -> Unit,
    onJoinGame: (String) -> Unit
) {
    var gameCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a Game", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the 6-character game code")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = gameCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            gameCode = it.uppercase()
                        }
                    },
                    label = { Text("Game Code") },
                    placeholder = { Text("ABC123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoinGame(gameCode) },
                enabled = gameCode.length == 6
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
