package com.group_7.studysage.ui.screens.Flashcards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.viewmodels.FlashcardViewModel
import com.group_7.studysage.viewmodels.HomeViewModel

data class Flashcard(
    val id: String,
    val question: String,
    val answer: String,
    val difficulty: String = "medium"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    note: Note,
    onBack: () -> Unit,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val flashcards by flashcardViewModel.flashcards.collectAsState()
    val isLoading by flashcardViewModel.isLoading.collectAsState()
    val errorMessage by flashcardViewModel.errorMessage.collectAsState()
    val generationProgress by flashcardViewModel.generationProgress.collectAsState()

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var taskCompleted by remember { mutableStateOf(false) }

    // Simple background color for clean Material look
    val solidBackgroundModifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.surfaceContainer)

    LaunchedEffect(note.id) {
        if (flashcards.isEmpty()) {
            flashcardViewModel.loadFlashcardsForNote(note.id)
        }
    }

    // Automatically complete flashcard task when user finishes all flashcards
    LaunchedEffect(showCompletionDialog) {
        if (showCompletionDialog && !taskCompleted) {
            homeViewModel.checkAndCompleteTaskByType("flashcards")
            taskCompleted = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flashcards", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGenerateDialog = true },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, "Regenerate")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = solidBackgroundModifier
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
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
                            // Title
                            Text(
                                "Generating Flashcards",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // Status message container
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Linear indeterminate progress bar
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                )

                                Spacer(Modifier.height(16.dp))

                                // Status message
                                Text(
                                    when {
                                        generationProgress < 20 -> "Connecting to AI..."
                                        generationProgress < 40 -> "Analyzing note content..."
                                        generationProgress < 75 -> "AI is creating questions and answers..."
                                        generationProgress < 90 -> "Processing and refining data..."
                                        generationProgress < 100 -> "Finalizing cards..."
                                        else -> "Complete!"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.Error, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Text("Error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(errorMessage ?: "Unknown error", textAlign = TextAlign.Center)
                            Button(onClick = { showGenerateDialog = true }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            flashcards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Generate Flashcards",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Create study flashcards from your note using AI.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showGenerateDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate")
                        }
                    }
                }
            }

            else -> {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress bar (of flashcards completed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${currentIndex + 1} / ${flashcards.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / flashcards.size },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )

                        Text(
                            "${flashcards.size} cards",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    FlashcardView(
                        flashcard = flashcards[currentIndex],
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 500.dp)
                    )

                    Spacer(Modifier.height(32.dp))

                    if (!isFlipped) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Tap card to reveal answer")
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    isFlipped = false
                                }
                            },
                            enabled = currentIndex > 0,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Previous")
                        }

                        Spacer(Modifier.width(16.dp))

                        Button(
                            onClick = {
                                if (currentIndex < flashcards.size - 1) {
                                    currentIndex++
                                    isFlipped = false
                                } else {
                                    showCompletionDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (currentIndex < flashcards.size - 1) "Next" else "Finish")
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (currentIndex < flashcards.size - 1)
                                    Icons.AutoMirrored.Filled.ArrowForward
                                else
                                    Icons.Default.Check,
                                null
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            flashcardViewModel.shuffleFlashcards()
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        GenerateFlashcardsDialog(
            isRegenerate = flashcards.isNotEmpty(),
            onDismiss = { showGenerateDialog = false },
            onGenerate = { num, difficulty ->
                flashcardViewModel.generateFlashcards(note.id, note.content, num, difficulty)
                showGenerateDialog = false
                currentIndex = 0
                isFlipped = false
            }
        )
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Great Job!", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Completed all ${flashcards.size} flashcards. Review again?",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    currentIndex = 0
                    isFlipped = false
                    showCompletionDialog = false
                }) {
                    Text("Review")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Exit")
                }
            }
        )
    }
}

@Composable
fun FlashcardView(
    flashcard: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "flip"
    )

    // Determine card colors based on flip state
    val frontContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val backContainerColor = MaterialTheme.colorScheme.tertiaryContainer

    val targetContainerColor = if (rotation < 90f) frontContainerColor else backContainerColor

    // Smooth color change animation
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cardColor"
    )

    Card(
        modifier = modifier
            .clickable(onClick = onFlip)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
            contentColor = if (rotation < 90f) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        val cardScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(cardScrollState)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.QuestionMark, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        flashcard.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        flashcard.answer,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GenerateFlashcardsDialog(
    isRegenerate: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (Int, String) -> Unit
) {
    var numberOfCards by remember { mutableIntStateOf(10) }
    var selectedDifficulty by remember { mutableStateOf("medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isRegenerate) "Regenerate Flashcards" else "Generate Flashcards")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AI will create flashcards from your note")

                // Number of cards slider
                Column {
                    Text(
                        "Number of Cards: $numberOfCards",
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = numberOfCards.toFloat(),
                        onValueChange = { numberOfCards = it.toInt() },
                        valueRange = 5f..15f,
                        steps = 9
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5", style = MaterialTheme.typography.bodySmall)
                        Text("15", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Difficulty level selector
                Column {
                    Text(
                        "Difficulty Level",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Easy button
                        FilterChip(
                            selected = selectedDifficulty == "easy",
                            onClick = { selectedDifficulty = "easy" },
                            label = { Text("Easy") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (selectedDifficulty == "easy") {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )

                        // Medium button
                        FilterChip(
                            selected = selectedDifficulty == "medium",
                            onClick = { selectedDifficulty = "medium" },
                            label = { Text("Med") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (selectedDifficulty == "medium") {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )

                        // Hard button
                        FilterChip(
                            selected = selectedDifficulty == "hard",
                            onClick = { selectedDifficulty = "hard" },
                            label = { Text("Hard") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (selectedDifficulty == "hard") {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onGenerate(numberOfCards, selectedDifficulty) }) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(4.dp))
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}