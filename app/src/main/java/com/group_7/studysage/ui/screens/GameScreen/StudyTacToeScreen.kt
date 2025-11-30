package com.group_7.studysage.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group_7.studysage.data.models.GameUiState
import com.group_7.studysage.data.models.QuizQuestion

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
    // ALWAYS sync from backend - don't maintain local state separately
    // Use remember with derivedStateOf to properly track board state changes
    val boardState = remember(session?.boardState) {
        session?.boardState?.toTypedArray() ?: Array(9) { "" }
    }

    // Convert to list for better change detection
    val boardStateList = remember(session?.boardState) {
        session?.boardState ?: List(9) { "" }
    }

    // Debug: Log session and board state on every recomposition
    LaunchedEffect(Unit) {
        android.util.Log.d("StudyTacToe", "Screen recomposed - session ID: ${session?.id}")
        android.util.Log.d("StudyTacToe", "Session status: ${session?.status}")
        android.util.Log.d("StudyTacToe", "Current turn: ${session?.currentTurn}")
    }

    // Debug logging for board state changes
    LaunchedEffect(session?.boardState) {
        android.util.Log.d("StudyTacToe", "LaunchedEffect: Board state updated")
        android.util.Log.d("StudyTacToe", "boardState value: ${session?.boardState}")
        android.util.Log.d("StudyTacToe", "boardState size: ${session?.boardState?.size ?: 0}")
        android.util.Log.d("StudyTacToe", "boardState content: ${session?.boardState?.joinToString(",")}")
    }

    // Get current turn from backend (player ID) - define early so it can be used as a key
    val currentTurnPlayerId = session?.currentTurn
    val isCurrentPlayerTurn = currentTurnPlayerId == currentUserId

    var selectedSquare by remember { mutableStateOf<Int?>(null) }
    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var waitingForAnswer by remember { mutableStateOf(false) }

    // Track attempted squares per player - reset when turn changes
    var attemptedSquares by remember(currentTurnPlayerId) { mutableStateOf(setOf<Int>()) }

    // Compute winner and draw status from current board state
    // Use boardStateList for better change detection
    val winResult = remember(boardStateList) { checkWinner(boardState) }
    val isDraw = remember(boardStateList) { winResult == null && boardState.all { it.isNotEmpty() } }

    // Expose winner and draw as val instead of var for reactive updates
    val winner = winResult

    // State to control showing the result screen
    var showResultScreen by remember { mutableStateOf(false) }

    // Detect when game is finished and trigger result screen
    LaunchedEffect(winner, isDraw) {
        if (winner != null || isDraw) {
            android.util.Log.d("StudyTacToe", "Game finished! Winner: $winner, Draw: $isDraw")
            // Delay to show the final move briefly before transitioning
            kotlinx.coroutines.delay(1500)
            showResultScreen = true
        }
    }

    // Show result screen if game is finished
    if (showResultScreen) {
        StudyTacToeResultScreen(
            gameUiState = gameUiState,
            onBackClick = {
                // Reset the flag and navigate back
                showResultScreen = false
                // This will be handled by the parent's back navigation
            }
        )
        return
    }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
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
        // Use key to force recomposition when board state changes
        key(boardStateList) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                        !attemptedSquares.contains(index) &&  // Prevent re-attempting same square
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
                    } else if (attemptedSquares.contains(index)) {
                        android.util.Log.d("StudyTacToe", "Square $index already attempted, cannot retry")
                    }
                },
                enabled = winner == null && !isDraw && isCurrentPlayerTurn && !waitingForAnswer
            )
        }
        }

        // Turn indicator - always show during active game
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
                        // Correct answer - build new board with this player's move
                        val newBoard = boardState.copyOf()
                        newBoard[square] = currentPlayer
                        
                        // Track the attempt locally
                        attemptedSquares = attemptedSquares + square

                        // Submit answer and new board state to backend
                        // Backend will validate, update Firebase, switch turn, and broadcast ROOM_UPDATE
                        onAnswerSubmit(square, answerIndex, newBoard.toList())

                        // DON'T update local state here - wait for backend ROOM_UPDATE
                        // This ensures both players see the same board state
                    } else {
                        // Incorrect answer - still submit but board won't change
                        attemptedSquares = attemptedSquares + square
                        // Submit answer to backend with current board (unchanged)
                        onAnswerSubmit(square, answerIndex, boardState.toList())
                    }

                    // Backend ROOM_UPDATE will handle board state sync and turn switching
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
    // Debug logging for grid
    LaunchedEffect(boardState.contentToString()) {
        android.util.Log.d("TicTacToeGrid", "Grid rendering with board: ${boardState.contentToString()}")
        boardState.forEachIndexed { index, value ->
            android.util.Log.d("TicTacToeGrid", "Square $index: '$value' (empty=${value.isEmpty()})")
        }
    }

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
                    // Use key to ensure each square recomposes when its value changes
                    key(index, boardState[index]) {
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
}

@Composable
fun GridSquare(
    value: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Debug logging
    LaunchedEffect(value) {
        android.util.Log.d("GridSquare", "Square value changed to: '$value' (isEmpty=${value.isEmpty()}, length=${value.length})")
    }

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
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QuestionMark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Knowledge Challenge",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Answer to claim your square",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select your answer:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                question.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "option_scale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .scale(scale)
                            .shadow(
                                elevation = if (isSelected) 8.dp else 2.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        onClick = { selectedAnswer = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ('A' + index).toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedAnswer?.let { onAnswerSelected(it) }
                },
                enabled = selectedAnswer != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Answer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
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

/**
 * Study-Tac-Toe Result Screen
 * Displays the winner or draw result with beautiful animations - matching QuizRace style
 */
@Composable
fun StudyTacToeResultScreen(
    gameUiState: GameUiState,
    onBackClick: () -> Unit
) {
    val session = gameUiState.currentSession
    val players = session?.players?.values?.toList() ?: emptyList()
    val boardState = session?.boardState?.toTypedArray() ?: Array(9) { "" }

    val winner = checkWinner(boardState)
    val isDraw = winner == null && boardState.all { it.isNotEmpty() }

    val player1Name = players.getOrNull(0)?.name ?: "Player 1"
    val player2Name = players.getOrNull(1)?.name ?: "Player 2"

    val winnerName = when (winner) {
        "X" -> player1Name
        "O" -> player2Name
        else -> null
    }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Trophy icon with bounce animation (matching QuizRace)
        val trophyScale by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "trophy_scale"
        )

        Text(
            text = if (winner != null) "🏆" else "🤝",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            modifier = Modifier.scale(trophyScale)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title with slide-in animation (matching QuizRace)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(400, delayMillis = 300, easing = EaseOut)
            ) + fadeIn(tween(400, delayMillis = 300))
        ) {
            Text(
                text = if (winner != null) "Game Complete!" else "It's a Draw!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Winner/Results card (matching QuizRace leaderboard style)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(500, delayMillis = 500, easing = EaseOut)
            ) + fadeIn(tween(500, delayMillis = 500))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (winnerName != null) {
                    // Winner display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Winner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Gold medal badge
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFD700)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "🏆",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontSize = 24.sp
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = winnerName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            text = if (winner == "X") "Player X" else "Player O",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (winner == "X") Color(0xFF2196F3) else Color(0xFFE91E63),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isDraw) {
                    // Draw display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Show both players with equal standing
                            listOf(
                                Triple(player1Name, "Player X", Color(0xFF2196F3)),
                                Triple(player2Name, "Player O", Color(0xFFE91E63))
                            ).forEachIndexed { index, (name, role, color) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = color.copy(alpha = 0.2f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (role == "Player X") "X" else "O",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = color,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                if (index == 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Well played by both!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // Final board state
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Final Board",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
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
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (boardState[index].isNotEmpty()) {
                                            Text(
                                                text = boardState[index],
                                                style = MaterialTheme.typography.displayMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (boardState[index] == "X")
                                                    Color(0xFF2196F3)
                                                else
                                                    Color(0xFFE91E63)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Back button (matching QuizRace)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(500, delayMillis = 700, easing = EaseOut)
            ) + fadeIn(tween(500, delayMillis = 700))
        ) {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Back to Lobby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

