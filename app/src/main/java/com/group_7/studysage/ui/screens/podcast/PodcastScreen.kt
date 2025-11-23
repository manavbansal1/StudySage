package com.group_7.studysage.ui.screens.podcast

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.viewmodels.NotesViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    note: Note,
    notesViewModel: NotesViewModel = viewModel(),
    onBack: () -> Unit
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var showScriptDialog by remember { mutableStateOf(false) }

    val isPodcastGenerating by notesViewModel.isPodcastGenerating.collectAsState()
    val podcastGenerationStatus by notesViewModel.podcastGenerationStatus.collectAsState()
    val podcastAudioPath by notesViewModel.podcastAudioPath.collectAsState()
    val errorMessage by notesViewModel.errorMessage.collectAsState()
    val podcastScript by notesViewModel.podcastScript.collectAsState()

    // Automatically generate podcast when screen opens
    LaunchedEffect(Unit) {
        notesViewModel.generatePodcast(
            noteId = note.id,
            content = note.content,
            noteTitle = note.title
        )
    }

    // Initialize media player when audio path is available
    LaunchedEffect(podcastAudioPath) {
        podcastAudioPath?.let { path ->
            try {
                mediaPlayer?.release()
                mediaPlayer = null

                mediaPlayer = MediaPlayer().apply {
                    reset()
                    setDataSource(path)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnPreparedListener {
                        duration = this.duration
                        android.util.Log.d("PodcastScreen", "Audio prepared, duration: ${this.duration}ms")
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosition = 0
                        android.util.Log.d("PodcastScreen", "Playback completed")
                    }
                    setOnErrorListener { mp, what, extra ->
                        android.util.Log.e("PodcastScreen", "MediaPlayer error: what=$what, extra=$extra")
                        notesViewModel.setErrorMessage("Playback error occurred")
                        isPlaying = false
                        mp.reset()
                        true // Error handled
                    }
                    setOnSeekCompleteListener {
                        // Resume playing after seek if it was playing
                        if (isPlaying && !it.isPlaying) {
                            try {
                                it.start()
                            } catch (e: Exception) {
                                android.util.Log.e("PodcastScreen", "Error resuming after seek: ${e.message}")
                            }
                        }
                    }
                    setOnInfoListener { mp, what, extra ->
                        android.util.Log.d("PodcastScreen", "MediaPlayer info: what=$what, extra=$extra")
                        false
                    }
                    prepareAsync() // Use async to avoid blocking
                }
            } catch (e: Exception) {
                android.util.Log.e("PodcastScreen", "Failed to initialize MediaPlayer: ${e.message}")
                notesViewModel.setErrorMessage("Failed to load audio: ${e.message}")
            }
        }
    }

    // Update playback position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100)
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        currentPosition = it.currentPosition
                    }
                } catch (e: Exception) {
                    // Handle state exceptions
                    android.util.Log.e("PodcastScreen", "Error getting position: ${e.message}")
                }
            }
        }
    }

    // Clean up media player
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            notesViewModel.clearPodcastData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podcast") },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        notesViewModel.clearPodcastData()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show script button when script is available
                    if (podcastScript != null) {
                        IconButton(onClick = { showScriptDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "View Script",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Note title
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Show loading state
                if (isPodcastGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 24.dp),
                        strokeWidth = 8.dp
                    )

                    Text(
                        text = podcastGenerationStatus ?: "Generating podcast...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Show player controls when audio is ready
                if (podcastAudioPath != null && !isPodcastGenerating) {
                    // Podcast icon
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Progress slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() else 0f,
                            onValueChange = { newValue ->
                                try {
                                    val seekPosition = newValue.toInt()
                                    mediaPlayer?.let { mp ->
                                        if (seekPosition >= 0 && seekPosition <= duration) {
                                            mp.seekTo(seekPosition)
                                            currentPosition = seekPosition
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PodcastScreen", "Seek error: ${e.message}")
                                }
                            },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Play/Pause button
                    FloatingActionButton(
                        onClick = {
                            mediaPlayer?.let {
                                if (isPlaying) {
                                    it.pause()
                                    isPlaying = false
                                } else {
                                    it.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status text
                    Text(
                        text = if (isPlaying) "Now Playing" else "Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Script dialog
        if (showScriptDialog && podcastScript != null) {
            AlertDialog(
                onDismissRequest = { showScriptDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Podcast Script")
                        IconButton(onClick = { showScriptDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                text = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = podcastScript!!,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScriptDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

/**
 * Format milliseconds to MM:SS format
 */
private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
