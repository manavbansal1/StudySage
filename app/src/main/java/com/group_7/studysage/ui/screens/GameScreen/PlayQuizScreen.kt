package com.group_7.studysage.ui.screens.GameScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.model.QuizOption
import com.group_7.studysage.viewmodels.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQuizScreen(
    quiz: Quiz,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) } // questionIndex -> optionIndex
    var showFeedback by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }
    var questionVisible by remember { mutableStateOf(true) }
    var taskCompleted by remember { mutableStateOf(false) }

    val currentQuestion = quiz.questions.getOrNull(currentQuestionIndex)
    val selectedOptionIndex = selectedAnswers[currentQuestionIndex]

    // Calculate score
    val score = selectedAnswers.count { (questionIdx, optionIdx) ->
        quiz.questions.getOrNull(questionIdx)?.options?.getOrNull(optionIdx)?.isCorrect == true
    }

    // Automatically complete quiz task when quiz is finished
    LaunchedEffect(quizCompleted) {
        if (quizCompleted && !taskCompleted) {
            homeViewModel.checkAndCompleteTaskByType("quiz")
            taskCompleted = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.noteTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (quizCompleted) {
            // Final Score Screen
            QuizCompletedScreen(
                score = score,
                totalQuestions = quiz.questions.size,
                onRestart = {
                    currentQuestionIndex = 0
                    selectedAnswers = emptyMap()
                    showFeedback = false
                    quizCompleted = false
                },
                onExit = onBack,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Quiz Question Screen
            AnimatedContent(
                targetState = currentQuestionIndex,
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
            ) { questionIndex ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                                text = "Question ${questionIndex + 1} of ${quiz.questions.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            
                            // Animated score
                            AnimatedContent(
                                targetState = score,
                                transitionSpec = {
                                    scaleIn(tween(200)) + fadeIn() togetherWith
                                            scaleOut(tween(200)) + fadeOut()
                                },
                                label = "score_animation"
                            ) { animatedScore ->
                                Text(
                                    text = "Score: $animatedScore/${selectedAnswers.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Animated progress bar
                        val animatedProgress by animateFloatAsState(
                            targetValue = (questionIndex + 1).toFloat() / quiz.questions.size,
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
                    val question = quiz.questions.getOrNull(questionIndex)
                    question?.let {
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
                                            text = "${questionIndex + 1}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = it.question,
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
                        it.options.forEachIndexed { optionIndex, option ->
                            var isVisible by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(questionIndex) {
                                delay(optionIndex * 80L)
                                isVisible = true
                            }
                            
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = slideInHorizontally(
                                    initialOffsetX = { 100 },
                                    animationSpec = tween(300, easing = EaseOut)
                                ) + fadeIn(tween(300))
                            ) {
                                QuizOptionButton(
                                    option = option,
                                    isSelected = selectedOptionIndex == optionIndex,
                                    showFeedback = showFeedback,
                                    onClick = {
                                        if (!showFeedback) {
                                            selectedAnswers = selectedAnswers + (currentQuestionIndex to optionIndex)
                                            showFeedback = true
                                        }
                                    }
                                )
                            }
                        }

                        // Explanation (shown after selection)
                        AnimatedVisibility(
                            visible = showFeedback && it.explanation.isNotBlank(),
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
                                        text = it.explanation,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Navigation Button with animation
                        AnimatedVisibility(
                            visible = showFeedback,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(),
                            exit = fadeOut()
                        ) {
                            Button(
                                onClick = {
                                    if (currentQuestionIndex < quiz.questions.size - 1) {
                                        showFeedback = false
                                        currentQuestionIndex++
                                    } else {
                                        quizCompleted = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    text = if (currentQuestionIndex < quiz.questions.size - 1) "Next Question" else "View Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (currentQuestionIndex < quiz.questions.size - 1) 
                                        Icons.Default.ArrowForward else Icons.Default.CheckCircle,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizOptionButton(
    option: QuizOption,
    isSelected: Boolean,
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
        showFeedback && isSelected && option.isCorrect -> Color(0xFF4CAF50) // Green
        showFeedback && isSelected && !option.isCorrect -> Color(0xFFF44336) // Red
        showFeedback && !isSelected && option.isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Light green (show correct answer)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        showFeedback && isSelected -> Color.White
        showFeedback && !isSelected && option.isCorrect -> Color(0xFF1B5E20)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        showFeedback && option.isCorrect -> Color(0xFF4CAF50)
        showFeedback && isSelected && !option.isCorrect -> Color(0xFFF44336)
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
                text = option.text,
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
                        imageVector = if (isSelected && option.isCorrect || !isSelected && option.isCorrect) {
                            Icons.Default.CheckCircle
                        } else if (isSelected && !option.isCorrect) {
                            Icons.Default.Cancel
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (option.isCorrect) {
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
fun QuizCompletedScreen(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = (score.toFloat() / totalQuestions * 100).toInt()
    val message = when {
        percentage == 100 -> "Perfect Score! 🎉"
        percentage >= 80 -> "Great Job! 👏"
        percentage >= 60 -> "Good Work! 👍"
        percentage >= 40 -> "Keep Practicing! 💪"
        else -> "Keep Learning! 📚"
    }

    val emoji = when {
        percentage == 100 -> "🏆"
        percentage >= 80 -> "⭐"
        percentage >= 60 -> "✅"
        percentage >= 40 -> "📖"
        else -> "💡"
    }
    
    // Animations
    var visible by remember { mutableStateOf(false) }
    val emojiScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emoji_scale"
    )
    
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated emoji
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            modifier = Modifier.scale(emojiScale)
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
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(500, delayMillis = 500, easing = EaseOut)
            ) + fadeIn(tween(500, delayMillis = 500))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Score",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "$score / $totalQuestions",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
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
            Column {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Try Again",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Exit Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
