package com.group_7.studysage.ui.screens.CourseScreen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.CourseWithNotes
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.viewmodels.NotesViewModel
import com.group_7.studysage.viewmodels.HomeViewModel
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
    notesViewModel: NotesViewModel = viewModel()
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

    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }
    var userPreferences by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showNoteOptions by remember { mutableStateOf(false) }
    var showSummaryScreen by remember { mutableStateOf(false) }

    // Upload states
    val isLoading by homeViewModel.isLoading
    val uploadStatus by homeViewModel.uploadStatus
    val errorMessage by homeViewModel.errorMessage
    val selectedNoteState by notesViewModel.selectedNote.collectAsState()
    val isSummaryLoading by notesViewModel.isNoteDetailsLoading.collectAsState()

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
                onDownload = { notesViewModel.downloadNote(context, summaryNote) }
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
                                    imageVector = Icons.Default.ArrowBack,
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

    // Show messages
    LaunchedEffect(uploadStatus) {
        uploadStatus?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
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
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Note",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Course Info Card
            item {
                CourseInfoCard(course)
            }

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
                item {
                    EmptyNotesCard()
                }
            } else {
                val notesList = notes.toList()
                itemsIndexed(notesList, key = { _: Int, note: Note -> note.id }) { _: Int, note: Note ->
                    CourseNoteCard(note = note, onClick = {
                        selectedNote = note
                        showNoteOptions = true
                    })
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
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

                Divider()

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
                            notesViewModel.selectNote(note)
                            showSummaryScreen = true
                            if (note.summary.isBlank()) {
                                notesViewModel.loadNoteById(note.id)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
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
                Icons.Default.NoteAdd,
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
    onDownload: () -> Unit
) {
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
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
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}