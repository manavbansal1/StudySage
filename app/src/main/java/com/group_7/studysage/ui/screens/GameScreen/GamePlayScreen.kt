package com.group_7.studysage.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
                gameUiState.currentFlashcard != null -> {
                    FlashcardBattleScreen(
                        gameUiState = gameUiState,
                        onFlashcardAnswer = { isCorrect ->
                            gamePlayViewModel.submitFlashcardAnswer(isCorrect, 0) // TODO: track time elapsed
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

    AnimatedContent(
        targetState = questionData.questionNumber,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = EaseOut)
            ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300, easing = EaseIn)
                    ) + fadeOut(animationSpec = tween(300))
        },
        label = "question_transition"
    ) { questionNumber ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Indicator with animation
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question $questionNumber of ${questionData.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    // Timer with color change
                    val timerColor = when {
                        gameUiState.timeRemaining <= 5 -> Color(0xFFF44336)
                        gameUiState.timeRemaining <= 10 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    
                    Row(
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
                            text = "${gameUiState.timeRemaining}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = timerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                // Animated progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = questionNumber.toFloat() / questionData.totalQuestions,
                    animationSpec = tween(500, easing = EaseInOut),
                    label = "progress_animation"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question Card with elevation animation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Question number badge
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$questionNumber",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Options with staggered animation
            question.options.forEachIndexed { index, option ->
                var isVisible by remember { mutableStateOf(false) }
                
                LaunchedEffect(questionNumber) {
                    delay(index * 80L)
                    isVisible = true
                }
                
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInHorizontally(
                        initialOffsetX = { 100 },
                        animationSpec = tween(300, easing = EaseOut)
                    ) + fadeIn(tween(300))
                ) {
                    GameQuizOptionButton(
                        option = option,
                        isSelected = selectedAnswer == index,
                        isCorrect = index == question.correctAnswer,
                        showFeedback = isAnswered,
                        onClick = { if (!isAnswered) onAnswerClick(index) }
                    )
                }
            }

            // Explanation (shown after selection)
            AnimatedVisibility(
                visible = isAnswered && question.explanation != null,
                enter = slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(400, easing = EaseOut)
                ) + fadeIn(tween(400)),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = question.explanation ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Result feedback card
            if (isAnswered && lastResult != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lastResult.isCorrect) 
                                Color(0xFF4CAF50).copy(alpha = 0.15f) 
                            else 
                                Color(0xFFF44336).copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            2.dp,
                            if (lastResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (lastResult.isCorrect) 
                                        Icons.Default.CheckCircle 
                                    else 
                                        Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (lastResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = if (lastResult.isCorrect) "Correct!" else "Incorrect",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (lastResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "+${lastResult.points} points",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameQuizOptionButton(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showFeedback: Boolean,
    onClick: () -> Unit
) {
    // Scale animation for selected option
    val scale by animateFloatAsState(
        targetValue = if (isSelected && showFeedback) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "option_scale"
    )
    
    val backgroundColor = when {
        showFeedback && isSelected && isCorrect -> Color(0xFF4CAF50) // Green
        showFeedback && isSelected && !isCorrect -> Color(0xFFF44336) // Red
        showFeedback && !isSelected && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Light green
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        showFeedback && isSelected -> Color.White
        showFeedback && !isSelected && isCorrect -> Color(0xFF1B5E20)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        showFeedback && isCorrect -> Color(0xFF4CAF50)
        showFeedback && isSelected && !isCorrect -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale),
        onClick = onClick,
        enabled = !showFeedback,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                lineHeight = 22.sp
            )

            // Animated checkmark or cross after selection
            AnimatedVisibility(
                visible = showFeedback,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = fadeOut()
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isSelected && isCorrect || !isSelected && isCorrect) {
                            Icons.Default.CheckCircle
                        } else if (isSelected && !isCorrect) {
                            Icons.Default.Cancel
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (isCorrect) {
                            if (isSelected) Color.White else Color(0xFF4CAF50)
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameResultsScreen(
    gameUiState: com.group_7.studysage.data.models.GameUiState,
    onBackClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
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
        // Trophy icon with bounce animation
        val trophyScale by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "trophy_scale"
        )
        
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            modifier = Modifier.scale(trophyScale)
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(400, delayMillis = 300, easing = EaseOut)
            ) + fadeIn(tween(400, delayMillis = 300))
        ) {
            Text(
                text = "Game Complete!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Show leaderboard if available
        if (gameUiState.leaderboard.isNotEmpty()) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(500, delayMillis = 500, easing = EaseOut)
                ) + fadeIn(tween(500, delayMillis = 500))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Leaderboard",
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
                            gameUiState.leaderboard.take(5).forEachIndexed { index, entry ->
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
                                        // Rank badge
                                        Surface(
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = when (entry.rank) {
                                                1 -> Color(0xFFFFD700) // Gold
                                                2 -> Color(0xFFC0C0C0) // Silver
                                                3 -> Color(0xFFCD7F32) // Bronze
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${entry.rank}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (entry.rank <= 3) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = entry.playerName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${entry.score} pts",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (index < gameUiState.leaderboard.take(5).size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                    imageVector = Icons.Default.ArrowBack,
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

@Composable
fun FlashcardBattleScreen(
    gameUiState: com.group_7.studysage.data.models.GameUiState,
    onFlashcardAnswer: (Boolean) -> Unit
) {
    val flashcardData = gameUiState.currentFlashcard ?: return
    val flashcard = flashcardData.flashcard
    val isAnswered = gameUiState.isAnswered
    val lastResult = gameUiState.lastResult

    var showAnswer by remember { mutableStateOf(false) }

    // Reset showAnswer when flashcard changes
    LaunchedEffect(flashcard.id) {
        showAnswer = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Flashcard header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Flashcard ${flashcardData.flashcardNumber} of ${flashcardData.totalFlashcards}",
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

        // Flashcard content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Question side
                Text(
                    text = "Question",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = flashcard.question,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (showAnswer) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )

                    // Answer side
                    Text(
                        text = "Answer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = flashcard.answer,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showAnswer && !isAnswered) {
                Button(
                    onClick = { showAnswer = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Show Answer",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (showAnswer && !isAnswered) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onFlashcardAnswer(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336) // Red
                        )
                    ) {
                        Text(
                            text = "Need Review",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = { onFlashcardAnswer(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green
                        )
                    ) {
                        Text(
                            text = "Got it!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
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
                        text = if (lastResult.isCorrect) "Great job! ✓" else "Keep practicing!",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (lastResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        text = "+${lastResult.points} points",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
