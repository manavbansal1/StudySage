package com.group_7.studysage.ui.screens.podcast

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var volume by remember { mutableStateOf(1.0f) }

    val isPodcastGenerating by notesViewModel.isPodcastGenerating.collectAsState()
    val podcastGenerationStatus by notesViewModel.podcastGenerationStatus.collectAsState()
    val podcastAudioPath by notesViewModel.podcastAudioPath.collectAsState()
    val errorMessage by notesViewModel.errorMessage.collectAsState()
    val podcastScript by notesViewModel.podcastScript.collectAsState()

    // Check for existing podcast or generate new one when screen opens
    LaunchedEffect(Unit) {
        notesViewModel.loadOrGeneratePodcast(
            noteId = note.id,
            content = note.content,
            noteTitle = note.title
        )
    }

    // Initialize media player when audio path is available
    LaunchedEffect(podcastAudioPath) {
        podcastAudioPath?.let { path ->
            try {
                // Stop playback if currently playing
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                }

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
                        // Apply current playback speed and volume
                        try {
                            this.playbackParams = this.playbackParams.setSpeed(playbackSpeed)
                            this.setVolume(volume, volume)
                        } catch (e: Exception) {
                            android.util.Log.e("PodcastScreen", "Error setting playback params: ${e.message}")
                        }
                        // Sync isPlaying state with actual MediaPlayer state
                        isPlaying = this.isPlaying
                        android.util.Log.d("PodcastScreen", "Audio prepared, duration: ${this.duration}ms, isPlaying: ${this.isPlaying}")
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
                    setOnSeekCompleteListener { mp ->
                        // Resume playing after seek if it was playing
                        if (isPlaying && !mp.isPlaying) {
                            try {
                                android.util.Log.d("PodcastScreen", "Resuming after seek")
                                mp.start()
                            } catch (e: Exception) {
                                android.util.Log.e("PodcastScreen", "Error resuming after seek: ${e.message}")
                            }
                        } else {
                            android.util.Log.d("PodcastScreen", "Seek complete, not auto-starting (isPlaying=$isPlaying)")
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

    // Update playback position and sync state
    LaunchedEffect(mediaPlayer) {
        while (true) {
            delay(100)
            mediaPlayer?.let {
                try {
                    // Sync isPlaying state with actual MediaPlayer state
                    val actuallyPlaying = it.isPlaying
                    if (actuallyPlaying != isPlaying) {
                        android.util.Log.d("PodcastScreen", "State mismatch detected! UI: $isPlaying, Player: $actuallyPlaying - syncing to player state")
                        isPlaying = actuallyPlaying
                    }

                    if (actuallyPlaying) {
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Podcast",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        notesViewModel.clearPodcastData()
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Show regenerate button when podcast exists and not generating
                    if (podcastAudioPath != null && !isPodcastGenerating) {
                        IconButton(onClick = {
                            notesViewModel.generatePodcast(
                                noteId = note.id,
                                content = note.content,
                                noteTitle = note.title
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate Podcast",
                                tint = Color.White
                            )
                        }
                    }
                    // Show download button when podcast is available
                    if (podcastAudioPath != null && !isPodcastGenerating) {
                        IconButton(onClick = {
                            notesViewModel.downloadPodcast(context, note.id, note.originalFileName)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Podcast",
                                tint = Color.White
                            )
                        }
                    }
                    // Show script button when script is available
                    if (podcastScript != null) {
                        IconButton(onClick = { showScriptDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "View Script",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Note title
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Show error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
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
                    Card(
                        modifier = Modifier.size(200.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Podcasts,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress slider
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
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
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Volume control button
                                IconButton(
                                    onClick = { showVolumeDialog = true },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Volume",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Skip backward 10 seconds
                                IconButton(
                                    onClick = {
                                        mediaPlayer?.let {
                                            val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                                            it.seekTo(newPosition)
                                            currentPosition = newPosition
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Skip back 10 seconds",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

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
                                    modifier = Modifier.size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Skip forward 10 seconds
                                IconButton(
                                    onClick = {
                                        mediaPlayer?.let {
                                            val newPosition = (currentPosition + 10000).coerceAtMost(duration)
                                            it.seekTo(newPosition)
                                            currentPosition = newPosition
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Skip forward 10 seconds",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Playback speed button
                                IconButton(
                                    onClick = { showSpeedDialog = true },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Playback speed",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status text
                            Text(
                                text = if (isPlaying) "Now Playing" else "Paused",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Volume dialog
        if (showVolumeDialog) {
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text("Volume") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = volume,
                            onValueChange = { newVolume ->
                                volume = newVolume
                                mediaPlayer?.let {
                                    try {
                                        it.setVolume(newVolume, newVolume)
                                    } catch (e: Exception) {
                                        android.util.Log.e("PodcastScreen", "Error setting volume: ${e.message}")
                                    }
                                }
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", style = MaterialTheme.typography.bodySmall)
                            Text("100%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVolumeDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Playback speed dialog
        if (showSpeedDialog) {
            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = {
                    Text(
                        "Playback Speed",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speedOptions.forEach { speed ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        mediaPlayer?.let {
                                            try {
                                                it.playbackParams = it.playbackParams.setSpeed(speed)
                                            } catch (e: Exception) {
                                                android.util.Log.e("PodcastScreen", "Error setting speed: ${e.message}")
                                            }
                                        }
                                        showSpeedDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (playbackSpeed == speed)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", speed)}x",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                        color = if (playbackSpeed == speed)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (playbackSpeed == speed) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Close")
                    }
                }
            )
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

    // Loading dialog while generating podcast - Using "Uploading Note" animation style
    if (isPodcastGenerating) {
        Dialog(onDismissRequest = { /* Can't dismiss while generating */ }) {
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
                        text = "Generating Podcast",
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = podcastGenerationStatus ?: "Processing your document...",
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

/**
 * Format milliseconds to MM:SS format
 */
private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}