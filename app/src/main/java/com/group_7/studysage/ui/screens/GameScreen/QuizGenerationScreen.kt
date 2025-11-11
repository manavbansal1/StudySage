package com.group_7.studysage.ui.screens.GameScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.viewmodels.GameViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGenerationScreen(
    onBack: () -> Unit,
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val quizState by gameViewModel.quizGenerationState.collectAsState()
    
    var expandedNoteDropdown by remember { mutableStateOf(false) }
    
    // Load notes when screen opens
    LaunchedEffect(Unit) {
        gameViewModel.loadAvailableNotes()
    }
    
    // Show error as toast
    LaunchedEffect(quizState.error) {
        quizState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            gameViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        
        if (quizState.generatedQuiz != null) {
            // Show generated quiz preview and JSON
            QuizResultScreen(
                quizState = quizState,
                onCopyJson = {
                    val json = gameViewModel.getQuizJson()
                    if (json != null) {
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Quiz JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                onSaveQuiz = {
                    gameViewModel.saveQuiz()
                },
                onGenerateNew = {
                    gameViewModel.resetQuizGeneration()
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Show note selection and preferences
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quiz Generation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a note and optionally add preferences to generate a customized quiz with 10 multiple-choice questions.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Note Selection
                Text(
                    text = "1. Select Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (quizState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedNoteDropdown,
                        onExpandedChange = { expandedNoteDropdown = !expandedNoteDropdown }
                    ) {
                        OutlinedTextField(
                            value = quizState.selectedNote?.title ?: "Select a note",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Choose Note") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedNoteDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedNoteDropdown,
                            onDismissRequest = { expandedNoteDropdown = false }
                        ) {
                            if (quizState.availableNotes.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No notes available") },
                                    onClick = { },
                                    enabled = false
                                )
                            } else {
                                quizState.availableNotes.forEach { note ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = note.title,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (note.originalFileName.isNotBlank()) {
                                                    Text(
                                                        text = note.originalFileName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            gameViewModel.setSelectedNote(note)
                                            expandedNoteDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Note info card
                if (quizState.selectedNote != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Selected: ${quizState.selectedNote!!.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Content length: ${quizState.selectedNote!!.content.length} characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider()

                // Preferences Input
                Text(
                    text = "2. Add Preferences (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = quizState.userPreferences,
                    onValueChange = { gameViewModel.setUserPreferences(it) },
                    label = { Text("Quiz Preferences") },
                    placeholder = { Text("e.g., Focus on key concepts, include diagrams, difficulty: medium") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    supportingText = {
                        Text("Describe what you want the quiz to focus on")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Generate Button
                Button(
                    onClick = { gameViewModel.generateQuiz() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = quizState.selectedNote != null && !quizState.isGenerating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (quizState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generating Quiz...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Quiz", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    quizState: com.group_7.studysage.viewmodels.QuizGenerationState,
    onCopyJson: () -> Unit,
    onSaveQuiz: () -> Unit,
    onGenerateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quiz = quizState.generatedQuiz ?: return
    
    // Debug: Log quiz details
    LaunchedEffect(quiz) {
        android.util.Log.d("QuizResultScreen", "Displaying quiz with ${quiz.questions.size} questions")
        quiz.questions.forEachIndexed { index, question ->
            android.util.Log.d("QuizResultScreen", "Q${index + 1}: ${question.question}")
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Success message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Quiz Generated Successfully!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "${quiz.totalQuestions} questions created from \"${quiz.noteTitle}\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scroll down to view all questions ↓",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quiz preview
        Text(
            text = "Quiz Preview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        quiz.questions.forEachIndexed { index, question ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Question ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    question.options.forEachIndexed { optIndex, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (option.isCorrect) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${'A' + optIndex}.",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(text = option.text)
                            if (option.isCorrect) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    if (question.explanation.isNotBlank()) {
                        Divider()
                        Text(
                            text = "Explanation: ${question.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Divider()

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCopyJson,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy JSON to Clipboard")
            }

            Button(
                onClick = onSaveQuiz,
                modifier = Modifier.fillMaxWidth(),
                enabled = !quizState.isSaving
            ) {
                if (quizState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else if (quizState.savedQuizId != null) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved to Database")
                } else {
                    Text("Save to Database")
                }
            }

            OutlinedButton(
                onClick = onGenerateNew,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Another Quiz")
            }
        }
    }
}
