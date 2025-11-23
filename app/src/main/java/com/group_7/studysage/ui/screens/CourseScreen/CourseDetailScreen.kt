package com.group_7.studysage.ui.screens.CourseScreen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.CourseWithNotes
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.viewmodels.NotesViewModel
import com.group_7.studysage.viewmodels.HomeViewModel
import com.group_7.studysage.viewmodels.CourseViewModel
import com.group_7.studysage.viewmodels.GameViewModel
import com.group_7.studysage.viewmodels.GameViewModelFactory
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.websocket.GameWebSocketManager
import com.group_7.studysage.ui.screens.nfc.ReceiveNFCScreen
import com.group_7.studysage.ui.screens.nfc.ShareNFCScreen
import com.group_7.studysage.ui.screens.GameScreen.PlayQuizScreen
import com.group_7.studysage.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseWithNotes: CourseWithNotes,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    notesViewModel: NotesViewModel = viewModel(),
    courseViewModel: CourseViewModel = viewModel(),
    authViewModel: AuthViewModel,
    gameViewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            gameApiService = GameApiService(),
            webSocketManager = GameWebSocketManager(),
            authViewModel = authViewModel
        )
    )
) {
    val context = LocalContext.current
    val course = courseWithNotes.course

    val courseNotes = courseWithNotes.notes
    val notesMap = homeViewModel.getNotesForCourseFromLibrary(course.id)
    val libraryNotes = notesMap.map { noteMap ->
        Note(
            id = noteMap["noteId"] as? String ?: "",
            title = noteMap["fileName"] as? String ?: "",
            fileUrl = noteMap["fileUrl"] as? String ?: "",
            courseId = noteMap["courseId"] as? String ?: "",
            originalFileName = noteMap["fileName"] as? String ?: ""
        )
    }
    val notes = if (courseNotes.isNotEmpty()) courseNotes else libraryNotes

    var noteToShare by remember { mutableStateOf<Note?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }
    var userPreferences by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showNoteOptions by remember { mutableStateOf(false) }
    var showSummaryScreen by remember { mutableStateOf(false) }
    var showGenerateSummaryDialog by remember { mutableStateOf(false) }
    var showFlashcardScreen by remember { mutableStateOf(false) }
    var showUploadOptionsSheet by remember { mutableStateOf(false) }
    var showShareNFCScreen by remember { mutableStateOf(false) }
    var showReceiveNFCScreen by remember { mutableStateOf(false) }
    var showPlayQuizScreen by remember { mutableStateOf(false) }
    var showQuizGeneratingDialog by remember { mutableStateOf(false) }

    // Upload states
    val isLoading by homeViewModel.isLoading
    val uploadStatus by homeViewModel.uploadStatus
    val errorMessage by homeViewModel.errorMessage
    val selectedNoteState by notesViewModel.selectedNote.collectAsState()
    val isSummaryLoading by notesViewModel.isNoteDetailsLoading.collectAsState()
    val quizGenerationState by gameViewModel.quizGenerationState.collectAsState()

    // Observe pending note id from CourseViewModel
    val pendingNoteId = courseViewModel.uiState.collectAsState().value.pendingOpenNoteId

    // When course notes are loaded and there's a pending note id, auto-open it
    LaunchedEffect(courseWithNotes, pendingNoteId) {
        android.util.Log.d("CourseDetailScreen", "LaunchedEffect: pendingNoteId=$pendingNoteId, courseNotes=${courseWithNotes.notes.size}, libraryNotes=${libraryNotes.size}")
        if (!pendingNoteId.isNullOrBlank()) {
            // Search in courseWithNotes.notes first, then in libraryNotes
            val foundInCourse = courseWithNotes.notes.find { it.id == pendingNoteId }
            val foundInLibrary = libraryNotes.find { it.id == pendingNoteId }
            android.util.Log.d("CourseDetailScreen", "Found in course: ${foundInCourse != null}, found in library: ${foundInLibrary != null}")
            val toOpen = foundInCourse ?: foundInLibrary
            toOpen?.let { note ->
                // Track that the note was opened
                homeViewModel.markNoteAsOpened(
                    noteId = note.id,
                    title = if (note.title.isNotBlank()) note.title else note.originalFileName,
                    fileName = note.originalFileName,
                    fileUrl = note.fileUrl,
                    courseId = course.id
                )

                // Ensure NotesViewModel has the selected note details
                try {
                    if (note.id.isNotBlank()) {
                        android.util.Log.d("CourseDetailScreen", "Auto-opening note id=${note.id}")
                        notesViewModel.selectNote(note)
                        // Kick off full load if content or summary isn't present
                        if (note.summary.isBlank() || note.content.isBlank()) {
                            notesViewModel.loadNoteById(note.id)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CourseDetailScreen", "Error selecting/loading note: ${e.message}", e)
                }

                selectedNote = note
                showNoteOptions = true
            }
            // Clear the pending request so it doesn't re-open
            courseViewModel.clearPendingOpenNote()
        }
    }

    // Notify navigation when fullscreen overlay screens are showing (quiz/NFC) so navbar can hide
    LaunchedEffect(showShareNFCScreen, showReceiveNFCScreen, showPlayQuizScreen) {
        val isShowingOverlay = showShareNFCScreen || showReceiveNFCScreen || showPlayQuizScreen
        courseViewModel.setFullscreenOverlay(isShowingOverlay)
    }

    if (showShareNFCScreen && noteToShare != null) {
        ShareNFCScreen(
            note = noteToShare!!,
            onBack = {
                showShareNFCScreen = false
                noteToShare = null
            }
        )
        return
    }

    if (showReceiveNFCScreen) {
        ReceiveNFCScreen(
            courseId = course.id,
            onBack = {
                showReceiveNFCScreen = false
            }
        )
        return
    }

    if (showPlayQuizScreen && quizGenerationState.generatedQuiz != null) {
        // Full-screen quiz overlay (hides bottom navigation)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PlayQuizScreen(
                quiz = quizGenerationState.generatedQuiz!!,
                onBack = {
                    showPlayQuizScreen = false
                    gameViewModel.resetQuizGeneration()
                }
            )
        }
        return
    }

    if (showSummaryScreen) {
        val summaryNote = selectedNoteState

        if (summaryNote != null) {
            LaunchedEffect(summaryNote.id, summaryNote.summary) {
                if (summaryNote.summary.isBlank() && summaryNote.id.isNotBlank()) {
                    notesViewModel.loadNoteById(summaryNote.id)
                }
            }

            NoteSummaryScreen(
                note = summaryNote,
                isLoading = isSummaryLoading,
                onBack = {
                    showSummaryScreen = false
                    notesViewModel.clearSelectedNote()
                },
                onDownload = { notesViewModel.downloadNote(context, summaryNote) },
                onToggleStar = { notesViewModel.toggleNoteStar(summaryNote.id) },
                onRegenerateSummary = { preferences ->
                    notesViewModel.updateNoteSummary(summaryNote.id, summaryNote.content, preferences)
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("AI Summary") },
                        navigationIcon = {
                            IconButton(onClick = {
                                showSummaryScreen = false
                                notesViewModel.clearSelectedNote()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        return
    }

    if (showFlashcardScreen && selectedNote != null) {
        com.group_7.studysage.ui.screens.Flashcards.FlashcardScreen(
            note = selectedNote!!,
            onBack = {
                showFlashcardScreen = false
                selectedNote = null
            }
        )
        return
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileInfo = FileUtils.getFileInfo(context, uri)
                if (fileInfo != null) {
                    val validationResult = FileUtils.validateFile(context, uri)
                    when (validationResult) {
                        is FileUtils.ValidationResult.Success -> {
                            pendingFileUri = uri
                            pendingFileName = fileInfo.name
                            showUploadDialog = true
                        }
                        is FileUtils.ValidationResult.Error -> {
                            Toast.makeText(context, validationResult.message, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show messages and refresh course on successful upload
    LaunchedEffect(uploadStatus) {
        uploadStatus?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            // Refresh course to show newly uploaded document
            if (message.contains("processed successfully", ignoreCase = true)) {
                courseViewModel.refreshCourse(course.id)
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = course.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showUploadOptionsSheet = true
                        },
                        enabled = !isLoading,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        // Simple swipe-to-refresh state using Accompanist (deprecated warning is acceptable for now)
        val isRefreshing = courseViewModel.uiState.collectAsState().value.isRefreshing
        val swipeState = rememberSwipeRefreshState(isRefreshing)

        SwipeRefresh(
            state = swipeState,
            onRefresh = { courseViewModel.refreshCourse(course.id) },
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Course Info Card
                item { CourseInfoCard(course) }

                // Notes Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Notes (${notes.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )

                        if (isLoading && uploadStatus != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Text(
                                    text = uploadStatus!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                            )
                            }
                        }
                    }
                }

                // Empty state or notes
                if (notes.isEmpty()) {
                    item { EmptyNotesCard() }
                } else {
                    val notesList = notes.toList()
                    itemsIndexed(notesList, key = { _, note: Note -> note.id }) { _, note ->
                        CourseNoteCard(note = note, onClick = {
                            // Track that the note was opened
                            homeViewModel.markNoteAsOpened(
                                noteId = note.id,
                                title = if (note.title.isNotBlank()) note.title else note.originalFileName,
                                fileName = note.originalFileName,
                                fileUrl = note.fileUrl,
                                courseId = note.courseId
                            )

                            selectedNote = note
                            showNoteOptions = true
                        })
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Upload Options Bottom Sheet
    if (showUploadOptionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showUploadOptionsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Add Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Upload from phone") },
                    supportingContent = {
                        Text(
                            text = "Select a file from your device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showUploadOptionsSheet = false
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            val mimeTypes = arrayOf(
                                "application/pdf",
                                "text/plain",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/markdown",
                                "application/rtf",
                                "image/*"
                            )
                            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        }
                        filePickerLauncher.launch(intent)
                    }
                )

                ListItem(
                    headlineContent = { Text("Receive via NFC") },
                    supportingContent = {
                        Text(
                            text = "Transfer notes from another device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showUploadOptionsSheet = false
                        showReceiveNFCScreen = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showNoteOptions && selectedNote != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val note = selectedNote!!
        ModalBottomSheet(
            onDismissRequest = {
                showNoteOptions = false
                selectedNote = null
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (note.title.isNotBlank()) note.title else note.originalFileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Download original note") },
                    supportingContent = {
                        Text(
                            text = note.originalFileName.ifBlank { "Download the uploaded file" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        notesViewModel.downloadNote(context, note)
                        showNoteOptions = false
                        selectedNote = null
                    }
                )

                ListItem(
                    headlineContent = { Text("View AI summary") },
                    supportingContent = {
                        Text(
                            text = if (note.summary.isNotBlank()) {
                                "Read the generated summary for this note"
                            } else {
                                "Summary will appear here when available"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showNoteOptions = false
                        selectedNote = null
                        if (note.id.isBlank()) {
                            Toast.makeText(
                                context,
                                "Note details not available yet for this document.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Select the note in the ViewModel so we can fetch details if needed
                            notesViewModel.selectNote(note)

                            if (note.summary.isNotBlank()) {
                                // Already has a summary -> show it
                                showSummaryScreen = true
                                // ensure latest details loaded
                                if (note.summary.isBlank()) {
                                    notesViewModel.loadNoteById(note.id)
                                }
                            } else {
                                // No summary yet -> ask user for preferences and generate on demand
                                showGenerateSummaryDialog = true
                                // Trigger load of full note content if it's not present
                                notesViewModel.loadNoteById(note.id)
                            }
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("View Flashcards") },
                    supportingContent = {
                        Text(
                            text = note.originalFileName.ifBlank { "Show flashcards" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showNoteOptions = false
                        selectedNote = note
                        showFlashcardScreen = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Generate Quiz") },
                    supportingContent = {
                        Text(
                            text = "Create an interactive quiz from this note",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showNoteOptions = false
                        selectedNote = note
                        showQuizGeneratingDialog = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Share via NFC") },
                    supportingContent = {
                        Text(
                            text = "Share the note with another device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        showNoteOptions = false
                        noteToShare = selectedNote
                        selectedNote = null
                        showShareNFCScreen = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Dialog to collect generation preferences and trigger summary generation
    if (showGenerateSummaryDialog) {
        val currentSelected = selectedNoteState

        // Ensure content is loaded
        LaunchedEffect(currentSelected?.id) {
            currentSelected?.let {
                if (it.content.isBlank()) {
                    notesViewModel.loadNoteById(it.id)
                }
            }
        }

        GenerateSummaryDialog(
            title = "Generate AI Summary",
            onDismiss = {
                showGenerateSummaryDialog = false
                notesViewModel.clearSelectedNote()
            },
            onGenerate = { preferences ->
                val contentToSummarize = selectedNoteState?.content ?: ""
                if (contentToSummarize.isNotBlank()) {
                    notesViewModel.updateNoteSummary(
                        selectedNoteState?.id ?: "",
                        contentToSummarize,
                        preferences
                    )
                    showGenerateSummaryDialog = false
                    showSummaryScreen = true
                } else {
                    selectedNoteState?.id?.let { notesViewModel.loadNoteById(it) }
                }
            }
        )
    }

    // Quiz generation dialog and logic
    if (showQuizGeneratingDialog && selectedNote != null) {
        var quizPreferences by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showQuizGeneratingDialog = false
                selectedNote = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Generate Quiz",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedNote!!.title.ifBlank { selectedNote!!.originalFileName },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Customize your quiz (optional):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = quizPreferences,
                        onValueChange = { quizPreferences = it },
                        label = { Text("Quiz Preferences") },
                        placeholder = { Text("e.g., 10 questions, focus on definitions, easy difficulty") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Leave empty for a standard quiz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameViewModel.setSelectedNote(selectedNote!!)
                        gameViewModel.setUserPreferences(quizPreferences.ifBlank { "Generate a quiz with multiple choice questions" })
                        gameViewModel.generateQuiz()
                        showQuizGeneratingDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Generate",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuizGeneratingDialog = false
                        selectedNote = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Show animated loading dialog while generating quiz
    if (quizGenerationState.isGenerating) {
        var dotCount by remember { mutableStateOf(0) }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                dotCount = (dotCount + 1) % 4
            }
        }
        
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            },
            title = { 
                Text(
                    "Generating Quiz",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = dotCount,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        },
                        label = "loading_text"
                    ) { dots ->
                        Text(
                            text = "Creating quiz questions${".".repeat(dots)}",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI is analyzing your note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = { },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Handle quiz generation completion
    LaunchedEffect(quizGenerationState.generatedQuiz) {
        if (quizGenerationState.generatedQuiz != null && !showPlayQuizScreen) {
            showPlayQuizScreen = true
        }
    }

    // Handle quiz generation errors
    LaunchedEffect(quizGenerationState.error) {
        quizGenerationState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    // Upload confirmation dialog
    if (showUploadDialog && pendingFileUri != null) {
        AlertDialog(
            onDismissRequest = {
                showUploadDialog = false
                pendingFileUri = null
                pendingFileName = ""
                userPreferences = ""
            },
            title = { Text("Add Note to Course") },
            text = {
                Column {
                    Text("File: $pendingFileName")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This note will be added to ${course.title} (${course.code})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // User Preferences TextField
                    OutlinedTextField(
                        value = userPreferences,
                        onValueChange = { userPreferences = it },
                        label = { Text("Summary Preferences (Optional)") },
                        placeholder = { Text("e.g., focus on key formulas, brief bullet points") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Provide preferences for AI summary generation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFileUri?.let { uri ->
                            homeViewModel.uploadAndProcessNote(
                                context,
                                uri,
                                pendingFileName,
                                course.id,
                                userPreferences
                            )
                        }
                        showUploadDialog = false
                        pendingFileUri = null
                        pendingFileName = ""
                        userPreferences = ""
                    }
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUploadDialog = false
                        pendingFileUri = null
                        pendingFileName = ""
                        userPreferences = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CourseInfoCard(course: Course) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Course color indicator
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(course.color.toColorInt()))
            ) {
                Text(
                    text = course.code.take(3),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = course.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                if (course.instructor.isNotBlank()) {
                    Text(
                        text = "Instructor: ${course.instructor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (course.credits > 0) {
                        Text(
                            text = "${course.credits} credits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${course.semester} ${course.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (course.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CourseNoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = note.fileType,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(note.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.keyPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Key Points:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                note.keyPoints.take(2).forEach { point ->
                    Text(
                        text = "• $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (note.keyPoints.size > 2) {
                    Text(
                        text = "• +${note.keyPoints.size - 2} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    note.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (note.tags.size > 3) {
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "+${note.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.NoteAdd,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Notes Yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Upload your first document for this course using the + button",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSummaryScreen(
    note: Note,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onToggleStar: () -> Unit = {},
    onRegenerateSummary: (String) -> Unit = {}
) {
    var showRegenerateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = note.title.ifBlank { "AI Summary" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Star button - only show when summary exists
                    if (note.summary.isNotBlank()) {
                        IconButton(onClick = onToggleStar) {
                            Icon(
                                imageVector = if (note.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (note.isStarred) "Unstar summary" else "Star summary",
                                tint = if (note.isStarred) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDownload,
                        enabled = note.fileUrl.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Download note"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column {
                    Text(
                        text = note.title.ifBlank { note.originalFileName.ifBlank { "Course note" } },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(note.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (note.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tags: ${note.tags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Regenerate button - only show when summary exists
                            if (note.summary.isNotBlank()) {
                                TextButton(
                                    onClick = { showRegenerateDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Regenerate")
                                }
                            }
                        }

                        when {
                            note.summary.isNotBlank() -> {
                                Text(
                                    text = note.summary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            isLoading -> {
                                Text(
                                    text = "Fetching the latest AI summary...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = "Summary not available yet. Please check back soon.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (note.keyPoints.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Key Points",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            note.keyPoints.forEach { point ->
                                Text(
                                    text = "• $point",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Original Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "File name: ${note.originalFileName.ifBlank { "Not available" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (note.fileType.isNotBlank()) {
                            Text(
                                text = "File type: ${note.fileType}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Updated: ${formatDate(note.updatedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (note.fileUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(onClick = onDownload) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download original")
                            }
                        }
                    }
                }
            }
        }

        // Regenerate Summary Dialog
        if (showRegenerateDialog) {
            GenerateSummaryDialog(
                title = "Regenerate AI Summary",
                onDismiss = { showRegenerateDialog = false },
                onGenerate = { preferences ->
                    onRegenerateSummary(preferences)
                    showRegenerateDialog = false
                }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun GenerateSummaryDialog(
    title: String = "Generate AI Summary",
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var preferences by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Provide preferences for the AI summary generation (optional):",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = preferences,
                    onValueChange = { preferences = it },
                    label = { Text("Summary Preferences") },
                    placeholder = { Text("e.g., Focus on key formulas, brief bullet points, detailed explanations...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "Leave empty for a standard summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(preferences) }
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
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