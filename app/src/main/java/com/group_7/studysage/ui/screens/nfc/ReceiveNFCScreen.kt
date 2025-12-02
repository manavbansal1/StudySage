package com.group_7.studysage.ui.screens.nfc

import android.util.Log

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.MainActivity
import com.group_7.studysage.data.nfc.NFCPayload
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.utils.NFCManager
import com.group_7.studysage.viewmodels.NotesViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveNFCScreen(
    courseId: String,
    onBack: () -> Unit,
    notesViewModel: NotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    if (activity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Receive via NFC", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Unable to access NFC functionality",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        return
    }

    val nfcManager = remember { NFCManager(activity) }

    // Use ViewModel states instead of local remember states to survive rotation
    val nfcState by notesViewModel.nfcReceiveState.collectAsState()
    val isListening = nfcState.isListening
    val errorMessage = nfcState.errorMessage
    val receivedPayloadJson = nfcState.receivedPayloadJson
    val isProcessing = nfcState.isProcessing
    val noteAdded = nfcState.noteAdded

    // Parse received payload from JSON
    val receivedPayload = remember(receivedPayloadJson) {
        receivedPayloadJson?.let { json ->
            try {
                // Parse JSON back to NFCPayload
                // This is a simple approach - you might want to use a proper JSON library
                com.google.gson.Gson().fromJson(json, NFCPayload::class.java)
            } catch (e: Exception) {
                Log.e("NFC_RECEIVE", "Error parsing payload JSON", e)
                null
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        Log.d("NFC_RECEIVE", "Initializing receive screen for course: $courseId")

        if (!nfcManager.isNfcAvailable()) {
            notesViewModel.updateNfcReceiveState(errorMessage = "NFC not available on this device")
            Log.e("NFC_RECEIVE", "NFC not available")
            return@LaunchedEffect
        }

        if (!nfcManager.isNfcEnabled()) {
            notesViewModel.updateNfcReceiveState(errorMessage = "Please enable NFC in your device settings")
            Log.e("NFC_RECEIVE", "NFC not enabled")
            return@LaunchedEffect
        }

        try {
            activity.enterReceiveMode()
            notesViewModel.updateNfcReceiveState(isListening = true)
            Log.d("NFC_RECEIVE", "Ready to receive. Reader mode active.")
        } catch (e: Exception) {
            Log.e("NFC_RECEIVE", "Error entering receive mode", e)
            notesViewModel.updateNfcReceiveState(errorMessage = "Failed to start NFC receiving: ${e.message}")
        }
    }

    val receivedData by activity.nfcDataReceived

    LaunchedEffect(receivedData) {
        if (receivedData == null || !isListening) return@LaunchedEffect

        Log.d("NFC_RECEIVE", "Processing received data")

        val payload = receivedData

        if (payload != null) {
            Log.d("NFC_RECEIVE", "Successfully received: ${payload.noteTitle}")
            // Convert payload to JSON string for SavedStateHandle compatibility
            val payloadJson = try {
                com.google.gson.Gson().toJson(payload)
            } catch (e: Exception) {
                Log.e("NFC_RECEIVE", "Error converting payload to JSON", e)
                null
            }
            notesViewModel.updateNfcReceiveState(
                receivedPayloadJson = payloadJson,
                isProcessing = false
            )
            activity.consumeNfcData()
        } else {
            Log.e("NFC_RECEIVE", "Received null payload")
            notesViewModel.updateNfcReceiveState(errorMessage = "Failed to read NFC data")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("NFC_RECEIVE", "Cleaning up receive screen")
            activity.exitNfcMode()
            notesViewModel.clearNfcReceiveState()
        }
    }

    BackHandler {
        Log.d("NFC_RECEIVE", "Back button pressed")
        activity.exitNfcMode()
        notesViewModel.clearNfcReceiveState()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive via NFC") },
                navigationIcon = {
                    IconButton(onClick = {
                        activity.exitNfcMode()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (isListening && receivedPayload == null) scale else 1f)
                    .background(
                        color = when {
                            receivedPayload != null -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            isListening -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (receivedPayload != null)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when {
                        receivedPayload != null -> Color(0xFF4CAF50)
                        isListening -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = when {
                    errorMessage != null -> "Unable to Receive"
                    receivedPayload != null -> "Note Received!"
                    isProcessing -> "Processing..."
                    isListening -> "Ready to Receive"
                    else -> "Preparing..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    receivedPayload != null -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = when {
                    errorMessage != null -> errorMessage!!
                    noteAdded -> "Note has been added to your course"
                    receivedPayload != null -> "Tap 'Add to Course' to save this note"
                    isProcessing -> "Receiving note..."
                    isListening -> "Hold your device back-to-back with the sending device.\n\nThis device is ready to receive data."
                    else -> "Setting up NFC..."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (receivedPayload != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (noteAdded) 
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (noteAdded) Icons.Default.CheckCircle else Icons.Default.Nfc,
                                contentDescription = null,
                                tint = if (noteAdded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (noteAdded) "Added Successfully" else "Received Note",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (noteAdded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = if (noteAdded) 
                                Color(0xFF4CAF50).copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Title:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = receivedPayload!!.noteTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "File:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = receivedPayload!!.originalFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Type:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = receivedPayload!!.fileType,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (noteAdded)
                                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isListening && receivedPayload == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Keep devices touching for 2-3 seconds. Make sure the other device is in 'Share' mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Show Add to Course button when note is received but not yet added
            if (receivedPayload != null && !noteAdded) {
                val scope = rememberCoroutineScope()
                
                Button(
                    onClick = {
                        notesViewModel.updateNfcReceiveState(isProcessing = true)
                        scope.launch {
                            try {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    ?: throw Exception("User not logged in")
                                
                                val firestore = FirebaseFirestore.getInstance()
                                val noteId = firestore.collection("notes").document().id
                                
                                val note = Note(
                                    id = noteId,
                                    title = receivedPayload!!.noteTitle,
                                    originalFileName = receivedPayload!!.originalFileName,
                                    content = receivedPayload!!.content,
                                    summary = receivedPayload!!.summary,
                                    keyPoints = emptyList(),
                                    tags = receivedPayload!!.tags,
                                    userId = userId,
                                    fileUrl = receivedPayload!!.fileUrl,
                                    fileType = receivedPayload!!.fileType,
                                    courseId = courseId
                                )

                                // Save to Firestore
                                firestore.collection("notes")
                                    .document(noteId)
                                    .set(note)
                                    .await()

                                notesViewModel.updateNfcReceiveState(noteAdded = true, isProcessing = false)



                                Log.d("NFC_RECEIVE", "Note added to course: $courseId")
                            } catch (e: Exception) {
                                Log.e("NFC_RECEIVE", "Error adding note", e)

                                notesViewModel.updateNfcReceiveState(isProcessing = false)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isProcessing) "Adding..." else "Add to Course")
                }
            }

            // Show Done button after note is added or if there's an error
            if (errorMessage != null || noteAdded) {
                Button(
                    onClick = {
                        activity.exitNfcMode()
                        notesViewModel.clearNfcReceiveState()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (noteAdded) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (noteAdded) "Done" else "Go Back")
                }
            }
        }
    }
}