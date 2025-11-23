package com.group_7.studysage.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.group_7.studysage.data.models.GameUiState
import com.group_7.studysage.data.models.QuizQuestion
import kotlinx.coroutines.delay

/**
 * Study-Tac-Toe Game Screen
 * A twist on the classic 3x3 strategy game where players must answer questions correctly to claim squares
 */
@Composable
fun StudyTacToeScreen(
    gameUiState: GameUiState,
    onSquareClick: (Int) -> Unit,
    onAnswerSubmit: (Int, Int, List<String>) -> Unit // (squareIndex, answerIndex, boardState)
) {
    val session = gameUiState.currentSession
    val players = session?.players?.values?.toList() ?: emptyList()
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val questions = session?.questions ?: emptyList()

    // Limit to 2 players
    if (players.size > 2) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Study-Tac-Toe only supports 2 players",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Game board state (0-8 for 9 squares)
    // Empty string = empty, "X" = player 1, "O" = player 2
    // Sync from backend if available, otherwise use local state
    var boardState by remember { mutableStateOf(Array(9) { "" }) }
    
    // Update local board state when backend sends updates
    LaunchedEffect(session?.boardState) {
        session?.boardState?.let { backendBoard ->
            if (backendBoard.size == 9) {
                android.util.Log.d("StudyTacToe", "Backend board update received: $backendBoard")
                boardState = backendBoard.toTypedArray()
                android.util.Log.d("StudyTacToe", "Local board state updated")
            }
        }
    }
    var selectedSquare by remember { mutableStateOf<Int?>(null) }
    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<String?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var attemptedSquares by remember { mutableStateOf(setOf<Int>()) }
    var waitingForAnswer by remember { mutableStateOf(false) }

    // Map each square (0-8) to a question index
    // Use at least 9 questions for the 9 squares
    val squareToQuestionMap = remember(questions) {
        if (questions.size >= 9) {
            (0..8).associateWith { it }
        } else {
            // If less than 9 questions, repeat them
            (0..8).associateWith { it % questions.size.coerceAtLeast(1) }
        }
    }

    // Determine if current user is Player 1 (X) or Player 2 (O)
    val isPlayerX = players.getOrNull(0)?.id == currentUserId
    val isPlayerO = players.getOrNull(1)?.id == currentUserId

    // Get current turn from backend (player ID)
    val currentTurnPlayerId = session?.currentTurn
    val isCurrentPlayerTurn = currentTurnPlayerId == currentUserId

    // Map player ID to X/O symbol for display
    val currentPlayer = when (currentTurnPlayerId) {
        players.getOrNull(0)?.id -> "X"
        players.getOrNull(1)?.id -> "O"
        else -> "X" // Default to X
    }

    val player1Name = players.getOrNull(0)?.name ?: "Player 1"
    val player2Name = players.getOrNull(1)?.name ?: "Player 2"

    // Debug: Show questions count
    LaunchedEffect(questions.size) {
        android.util.Log.d("StudyTacToe", "Questions loaded: ${questions.size}")
    }

    // Debug: Log turn changes
    LaunchedEffect(currentTurnPlayerId) {
        android.util.Log.d("StudyTacToe", "Current turn changed to: $currentTurnPlayerId")
        android.util.Log.d("StudyTacToe", "Current user ID: $currentUserId")
        android.util.Log.d("StudyTacToe", "Is current player turn: $isCurrentPlayerTurn")
    }

    // Show loading state if no questions
    if (questions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading questions...",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Session has ${session?.questions?.size ?: 0} questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Header with game title and info icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study-Tac-Toe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            var showInfoDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Game Info",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (showInfoDialog) {
                GameInfoDialog(onDismiss = { showInfoDialog = false })
            }
        }

        // Player Status Cards
        PlayerStatusCards(
            player1Name = player1Name,
            player2Name = player2Name,
            currentPlayer = currentPlayer,
            winner = winner
        )

        // Game Board
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            TicTacToeGrid(
                boardState = boardState,
                onSquareClick = { index ->
                    android.util.Log.d("StudyTacToe", "Square clicked: $index")
                    android.util.Log.d("StudyTacToe", "Board empty: ${boardState[index].isEmpty()}")
                    android.util.Log.d("StudyTacToe", "Is turn: $isCurrentPlayerTurn")
                    android.util.Log.d("StudyTacToe", "Questions size: ${questions.size}")

                    if (boardState[index].isEmpty() &&
                        winner == null &&
                        !isDraw &&
                        isCurrentPlayerTurn &&
                        !waitingForAnswer &&
                        questions.isNotEmpty()) {

                        selectedSquare = index
                        val questionIndex = squareToQuestionMap[index] ?: 0
                        currentQuestion = questions.getOrNull(questionIndex)

                        android.util.Log.d("StudyTacToe", "Question index: $questionIndex")
                        android.util.Log.d("StudyTacToe", "Question: ${currentQuestion?.question}")

                        showQuestionDialog = true
                        waitingForAnswer = true
                    }
                },
                enabled = winner == null && !isDraw && isCurrentPlayerTurn && !waitingForAnswer
            )
        }

        // Turn indicator
        if (winner == null && !isDraw) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentPlayerTurn)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (isCurrentPlayerTurn)
                        "Your Turn! (${if (isPlayerX) "X" else "O"})"
                    else
                        "Opponent's Turn (${currentPlayer})",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Winner/Draw announcement
        AnimatedVisibility(
            visible = winner != null || isDraw,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (winner != null) Icons.Default.EmojiEvents else Icons.Default.Handshake,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (winner != null) Color(0xFFFFD700) else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            winner == "X" -> "$player1Name Wins!"
                            winner == "O" -> "$player2Name Wins!"
                            isDraw -> "It's a Draw!"
                            else -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Question Dialog
    if (showQuestionDialog && currentQuestion != null) {
        QuestionDialog(
            question = currentQuestion!!,
            onDismiss = {
                showQuestionDialog = false
                selectedSquare = null
                currentQuestion = null
                waitingForAnswer = false
            },
            onAnswerSelected = { answerIndex ->
                val isCorrect = answerIndex == currentQuestion!!.correctAnswer

                selectedSquare?.let { square ->
                    if (isCorrect) {
                        // Correct answer - claim the square
                        val newBoard = boardState.copyOf()
                        newBoard[square] = currentPlayer
                        
                        // Update local state
                        boardState = newBoard
                        attemptedSquares = attemptedSquares + square

                        // Check for winner
                        val winResult = checkWinner(newBoard)
                        winner = winResult
                        isDraw = winResult == null && newBoard.all { it.isNotEmpty() }

                        // Submit answer and NEW board state to backend
                        onAnswerSubmit(square, answerIndex, newBoard.toList())
                    } else {
                        // Incorrect answer - track attempted square
                        attemptedSquares = attemptedSquares + square
                        // Still submit answer to backend (wrong answer) with current board
                        onAnswerSubmit(square, answerIndex, boardState.toList())
                    }

                    // Backend will handle turn switching via TURN_UPDATE message
                }

                showQuestionDialog = false
                selectedSquare = null
                currentQuestion = null
                waitingForAnswer = false
            }
        )
    }
}

@Composable
fun TicTacToeGrid(
    boardState: Array<String>,
    onSquareClick: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    GridSquare(
                        value = boardState[index],
                        onClick = { onSquareClick(index) },
                        enabled = enabled && boardState[index].isEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GridSquare(
    value: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (value.isNotEmpty()) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (value == "X") Color(0xFF2196F3) else Color(0xFFE91E63),
                modifier = Modifier.scale(scale)
            )
        }
    }
}

@Composable
fun PlayerStatusCards(
    player1Name: String,
    player2Name: String,
    currentPlayer: String,
    winner: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Player 1 (X)
        PlayerCard(
            playerName = player1Name,
            symbol = "X",
            symbolColor = Color(0xFF2196F3),
            isActive = currentPlayer == "X" && winner == null,
            isWinner = winner == "X",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Player 2 (O)
        PlayerCard(
            playerName = player2Name,
            symbol = "O",
            symbolColor = Color(0xFFE91E63),
            isActive = currentPlayer == "O" && winner == null,
            isWinner = winner == "O",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PlayerCard(
    playerName: String,
    symbol: String,
    symbolColor: Color,
    isActive: Boolean,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isWinner -> MaterialTheme.colorScheme.tertiaryContainer
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = symbolColor
            )
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (isWinner) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Winner",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDialog(
    question: QuizQuestion,
    onDismiss: () -> Unit,
    onAnswerSelected: (Int) -> Unit
) {
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.QuestionMark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Answer to Claim Square",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                question.options.forEachIndexed { index, option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedAnswer = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAnswer == index)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (selectedAnswer == index) 2.dp else 1.dp,
                            color = if (selectedAnswer == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedAnswer?.let { onAnswerSelected(it) }
                },
                enabled = selectedAnswer != null
            ) {
                Text("Submit Answer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GameInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Study-Tac-Toe",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Strategy meets Study. Claim your spot with knowledge.",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "How to Play:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val rules = listOf(
                    "The Board" to "Play on a standard 3×3 grid. Player 1 is X (Host), Player 2 is O (Joiner).",
                    "How to Move" to "Click a square to trigger a question from your study material.",
                    "Knowledge Check" to "Answer correctly to place your symbol. Answer incorrectly and the square stays empty—your turn ends!",
                    "Winning" to "First player to align 3 symbols horizontally, vertically, or diagonally wins!",
                    "Draw" to "If all squares are filled with no winner, it's a draw."
                )

                rules.forEach { (title, description) ->
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = "• $title",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

/**
 * Check if there's a winner on the board
 * Returns "X", "O", or null
 */
private fun checkWinner(board: Array<String>): String? {
    // All winning combinations
    val winPatterns = listOf(
        // Rows
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        // Columns
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        // Diagonals
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    for (pattern in winPatterns) {
        val (a, b, c) = pattern
        if (board[a].isNotEmpty() &&
            board[a] == board[b] &&
            board[a] == board[c]) {
            return board[a]
        }
    }

    return null
}
