package com.group_7.studysage.ui.screens.nfc

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.group_7.studysage.MainActivity
import com.group_7.studysage.data.nfc.NFCPayload
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.utils.NFCManager
import com.group_7.studysage.viewmodels.NotesViewModel

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
                    title = { Text("Receive via NFC") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
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

    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var receivedPayload by remember { mutableStateOf<NFCPayload?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

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
            errorMessage = "NFC not available on this device"
            Log.e("NFC_RECEIVE", "NFC not available")
            return@LaunchedEffect
        }

        if (!nfcManager.isNfcEnabled()) {
            errorMessage = "Please enable NFC in your device settings"
            Log.e("NFC_RECEIVE", "NFC not enabled")
            return@LaunchedEffect
        }

        try {
            activity.enterReceiveMode()
            isListening = true
            Log.d("NFC_RECEIVE", "Ready to receive. Reader mode active.")
        } catch (e: Exception) {
            Log.e("NFC_RECEIVE", "Error entering receive mode", e)
            errorMessage = "Failed to start NFC receiving: ${e.message}"
        }
    }

    val receivedData by activity.nfcDataReceived

    LaunchedEffect(receivedData) {
        if (receivedData == null || !isListening) return@LaunchedEffect

        Log.d("NFC_RECEIVE", "Processing received data")

        val payload = receivedData

        if (payload != null) {
            Log.d("NFC_RECEIVE", "Successfully received: ${payload.noteTitle}")

            receivedPayload = payload
            isProcessing = true

            try {
                val note = Note(
                    id = "",
                    title = payload.noteTitle,
                    fileUrl = payload.fileUrl,
                    courseId = courseId,
                    originalFileName = payload.originalFileName,
                    fileType = payload.fileType,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // TODO: Save to Firestore
                // notesViewModel.addReceivedNote(note)

                Toast.makeText(
                    context,
                    "Note received: ${payload.noteTitle}",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("NFC_RECEIVE", "Note created and will be saved to course: $courseId")

            } catch (e: Exception) {
                Log.e("NFC_RECEIVE", "Error processing received note", e)
                errorMessage = "Failed to save received note: ${e.message}"
            } finally {
                isProcessing = false
                activity.consumeNfcData()
            }
        } else {
            Log.e("NFC_RECEIVE", "Received null payload")
            errorMessage = "Failed to read NFC data"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("NFC_RECEIVE", "Cleaning up receive screen")
            activity.exitNfcMode()
        }
    }

    BackHandler {
        Log.d("NFC_RECEIVE", "Back button pressed")
        activity.exitNfcMode()
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
                            imageVector = Icons.Default.ArrowBack,
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
                    receivedPayload != null -> "Note has been added to your course"
                    isProcessing -> "Adding note to your course..."
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
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
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
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Received Successfully",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.3f)
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
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
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

            if (errorMessage != null || receivedPayload != null) {
                Button(
                    onClick = {
                        activity.exitNfcMode()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (receivedPayload != null) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (receivedPayload != null) "Done" else "Go Back")
                }
            }
        }
    }
}