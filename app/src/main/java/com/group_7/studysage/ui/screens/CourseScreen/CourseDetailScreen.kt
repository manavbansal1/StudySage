package com.group_7.studysage.ui.screens.CourseScreen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
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
import com.group_7.studysage.ui.screens.podcast.PodcastScreen
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
    notesViewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application
                NotesViewModel(
                    application = app,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    ),
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

    // Handle device back button - intercept and call onBack callback
    BackHandler {
        onBack()
    }

    // Get notes from course - will be empty list if no notes uploaded yet
    val courseNotes = courseWithNotes.notes
    android.util.Log.d("CourseDetailScreen", "========================================")
    android.util.Log.d("CourseDetailScreen", "📚 Course: ${course.title} (${course.id})")
    android.util.Log.d("CourseDetailScreen", "   Course notes count: ${courseNotes.size}")

    val notesMap = homeViewModel.getNotesForCourseFromLibrary(course.id)
    android.util.Log.d("CourseDetailScreen", "   Library notes count: ${notesMap.size}")

    val libraryNotes = notesMap.map { noteMap ->
        Note(
            id = noteMap["noteId"] as? String ?: "",
            title = noteMap["fileName"] as? String ?: "",
            fileUrl = noteMap["fileUrl"] as? String ?: "",
            courseId = noteMap["courseId"] as? String ?: "",
            originalFileName = noteMap["fileName"] as? String ?: ""
        )
    }

    // Use course notes if available, otherwise fall back to library notes
    val notes = if (courseNotes.isNotEmpty()) {
        android.util.Log.d("CourseDetailScreen", "   Using course notes: ${courseNotes.size}")
        courseNotes
    } else if (libraryNotes.isNotEmpty()) {
        android.util.Log.d("CourseDetailScreen", "   Using library notes: ${libraryNotes.size}")
        libraryNotes
    } else {
        android.util.Log.d("CourseDetailScreen", "   ℹ️ No notes available - will show empty state")
        emptyList()
    }
    android.util.Log.d("CourseDetailScreen", "========================================")

    var noteToShare by remember { mutableStateOf<Note?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }

    // Use ViewModel state for selected note and showNoteOptions to survive rotation
    val selectedNote by notesViewModel.selectedNote.collectAsState()
    val showNoteOptions by notesViewModel.showNoteOptions.collectAsState()

    var showSummaryScreen by remember { mutableStateOf(false) }
    var showGenerateSummaryDialog by remember { mutableStateOf(false) }
    var showFlashcardScreen by remember { mutableStateOf(false) }
    var showUploadOptionsSheet by remember { mutableStateOf(false) }
    var showShareNFCScreen by remember { mutableStateOf(false) }
    var showReceiveNFCScreen by remember { mutableStateOf(false) }
    var showPlayQuizScreen by remember { mutableStateOf(false) }
    var showQuizGeneratingDialog by remember { mutableStateOf(false) }
    var showPodcastScreen by remember { mutableStateOf(false) }

    // Upload states
    val isLoading by homeViewModel.isLoading
    val uploadStatus by homeViewModel.uploadStatus
    val errorMessage by homeViewModel.errorMessage
    val selectedNoteState by notesViewModel.selectedNote.collectAsState()
    val isSummaryLoading by notesViewModel.isLoading.collectAsState()
    val quizGenerationState by gameViewModel.quizGenerationState.collectAsState()

    // Observe pending note id from CourseViewModel
    val pendingNoteId = courseViewModel.uiState.collectAsState().value.pendingOpenNoteId

    // Restore note state after configuration change (rotation)
    LaunchedEffect(Unit) {
        android.util.Log.d("CourseDetailScreen", "🔄 Restoring note state if needed...")
        notesViewModel.restoreSelectedNoteIfNeeded()
    }

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
                        notesViewModel.selectNote(note, showOptions = true)
                        // Kick off full load if content or summary isn't present
                        if (note.summary.isBlank() || note.content.isBlank()) {
                            notesViewModel.loadNoteById(note.id)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CourseDetailScreen", "Error selecting/loading note: ${e.message}", e)
                }
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

    if (showPodcastScreen && selectedNote != null) {
        PodcastScreen(
            note = selectedNote!!,
            notesViewModel = notesViewModel,
            onBack = {
                showPodcastScreen = false
                notesViewModel.clearSelectedNote()
            }
        )
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
                                "Loading Summary",
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
                                    "Fetching note details...",
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
        return
    }

    if (showFlashcardScreen && selectedNote != null) {
        com.group_7.studysage.ui.screens.Flashcards.FlashcardScreen(
            note = selectedNote!!,
            onBack = {
                showFlashcardScreen = false
                notesViewModel.clearSelectedNote()
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
                    // Enforce PDF validation
                    if (fileInfo.name.endsWith(".pdf", ignoreCase = true)) {
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
                        Toast.makeText(context, "Only PDF files are supported", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show messages and refresh course on successful upload
    // Show messages and refresh course on successful upload
    LaunchedEffect(isLoading, uploadStatus) {
        if (!isLoading && uploadStatus?.contains("processed successfully", ignoreCase = true) == true) {
            // Refresh course to show newly uploaded document immediately after loading finishes
            courseViewModel.refreshCourse(course.id)
        }
        
        uploadStatus?.let { message ->
            if (!isLoading) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${course.title} - ${course.code}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Note",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }

    ) { paddingValues ->
        // Simple swipe-to-refresh state using Accompanist (deprecated warning is acceptable for now)
        val isRefreshing = courseViewModel.uiState.collectAsState().value.isRefreshing
        @Suppress("DEPRECATION")
        val swipeState = rememberSwipeRefreshState(isRefreshing)

        @Suppress("DEPRECATION")
        SwipeRefresh(
            state = swipeState,
            onRefresh = { courseViewModel.refreshCourse(course.id) },
            indicator = { state, trigger ->
                @Suppress("DEPRECATION")
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

                            notesViewModel.selectNote(note, showOptions = true)
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
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Add Note",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Upload from phone
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showUploadOptionsSheet = false
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "application/pdf"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            filePickerLauncher.launch(intent)
                        },
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Upload from phone",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select a file from your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Receive via NFC
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showUploadOptionsSheet = false
                            showReceiveNFCScreen = true
                        },
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Nfc,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Receive via NFC",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Transfer notes from another device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNoteOptions && selectedNote != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val note = selectedNote!!
        ModalBottomSheet(
            onDismissRequest = {
                notesViewModel.clearSelectedNote()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = if (note.title.isNotBlank()) note.title else note.originalFileName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Helper to create consistent option items
                @Composable
                fun NoteOptionItem(
                    title: String,
                    subtitle: String,
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                    color: Color = MaterialTheme.colorScheme.primaryContainer,
                    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick: () -> Unit
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClick() },
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                NoteOptionItem(
                    title = "Download original note",
                    subtitle = note.originalFileName.ifBlank { "Download the uploaded file" },
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        notesViewModel.downloadNote(context, note)
                        notesViewModel.clearSelectedNote()
                    }
                )

                NoteOptionItem(
                    title = "View AI summary",
                    subtitle = if (note.summary.isNotBlank()) "Read the generated summary" else "Summary will appear here when available",
                    icon = Icons.Default.Description,
                    onClick = {
                        notesViewModel.setShowNoteOptions(false)
                        if (note.id.isBlank()) {
                            Toast.makeText(context, "Note details not available yet.", Toast.LENGTH_SHORT).show()
                        } else {
                            notesViewModel.selectNote(note, showOptions = false)
                            if (note.summary.isNotBlank()) {
                                showSummaryScreen = true
                                if (note.summary.isBlank()) notesViewModel.loadNoteById(note.id)
                            } else {
                                showGenerateSummaryDialog = true
                                notesViewModel.loadNoteById(note.id)
                            }
                        }
                    }
                )

                NoteOptionItem(
                    title = "View Flashcards",
                    subtitle = "Study with flashcards",
                    icon = Icons.Default.Style, // Changed to Style for cards
                    onClick = {
                        notesViewModel.setShowNoteOptions(false)
                        showFlashcardScreen = true
                    }
                )

                NoteOptionItem(
                    title = "Generate Quiz",
                    subtitle = "Create an interactive quiz",
                    icon = Icons.Default.Quiz,
                    onClick = {
                        notesViewModel.setShowNoteOptions(false)
                        showQuizGeneratingDialog = true
                    }
                )

                NoteOptionItem(
                    title = "Share via NFC",
                    subtitle = "Share with another device",
                    icon = Icons.Default.Nfc,
                    onClick = {
                        noteToShare = selectedNote
                        notesViewModel.clearSelectedNote()
                        showShareNFCScreen = true
                    }
                )

                NoteOptionItem(
                    title = "Listen to podcast",
                    subtitle = "Generate and listen to AI podcast",
                    icon = Icons.Default.Headphones,
                    onClick = {
                        notesViewModel.setShowNoteOptions(false)
                        showPodcastScreen = true
                    }
                )
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
                notesViewModel.clearSelectedNote()
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
                        notesViewModel.clearSelectedNote()
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
                        "Generating Quiz",
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
                            "AI is creating quiz questions...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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

    // Upload confirmation and rename dialog
    if (showUploadDialog && pendingFileUri != null) {
        // Initialize note name with filename when dialog opens
        LaunchedEffect(pendingFileName) {
            if (pendingFileName.isNotBlank() && pendingFileName != "null") {
                // Remove extension for the name, but keep it for the file processing
                val nameWithoutExt = pendingFileName.substringBeforeLast(".")
                pendingFileName = nameWithoutExt
            }
        }

        AlertDialog(
            onDismissRequest = {
                showUploadDialog = false
                pendingFileUri = null
                pendingFileName = ""
            },
            title = { Text("Add Note to Course") },
            text = {
                Column {
                    Text(
                        "Enter a name for your note:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pendingFileName,
                        onValueChange = { pendingFileName = it },
                        label = { Text("Note Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This note will be added to ${course.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingFileUri?.let { uri ->
                            // Ensure the name has .pdf extension for the system
                            val finalName = if (pendingFileName.endsWith(".pdf", ignoreCase = true)) {
                                pendingFileName
                            } else {
                                "$pendingFileName.pdf"
                            }

                            homeViewModel.uploadAndProcessNote(
                                context,
                                uri,
                                finalName, // Pass the name with .pdf extension
                                course.id,
                                ""
                            )
                        }
                        showUploadDialog = false
                        pendingFileUri = null
                        pendingFileName = ""
                    },
                    shape = RoundedCornerShape(12.dp)
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
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Upload Loading Animation Dialog
    if (isLoading) {
        Dialog(onDismissRequest = { /* Prevent dismissal while uploading */ }) {
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
                        "Uploading Note",
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
                            "Processing your document...",
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = note.title.ifBlank { "AI Summary" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "AI Summary",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Regenerate button - only show when summary exists
                            if (note.summary.isNotBlank()) {
                                IconButton(
                                    onClick = { showRegenerateDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        when {
                            note.summary.isNotBlank() -> {
                                Text(
                                    text = note.summary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 24.sp
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
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Key Points",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            
                            note.keyPoints.forEach { point ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }

        // Loading overlay when regenerating summary (shows after clicking Generate)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
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
                        Text(
                            "Generating AI Summary",
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
                                "AI is analyzing your note...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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