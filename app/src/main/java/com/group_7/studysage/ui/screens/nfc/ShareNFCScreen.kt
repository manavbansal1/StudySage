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
import com.group_7.studysage.MainActivity
import com.group_7.studysage.data.nfc.NFCPayload
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.utils.NFCManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareNFCScreen(
    note: Note,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    if (activity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Share via NFC") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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

    var isReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val payload = remember {
        NFCPayload(
            noteId = note.id,
            noteTitle = note.title,
            fileUrl = note.fileUrl,
            originalFileName = note.originalFileName,
            fileType = note.fileType,
            content = note.content,
            tags = note.tags
        )
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
        Log.d("NFC_SHARE", "Initializing share screen for note: ${note.title}")

        if (!nfcManager.isNfcAvailable()) {
            errorMessage = "NFC not available on this device"
            Log.e("NFC_SHARE", "NFC not available")
            return@LaunchedEffect
        }

        if (!nfcManager.isNfcEnabled()) {
            errorMessage = "Please enable NFC in your device settings"
            Log.e("NFC_SHARE", "NFC not enabled")
            return@LaunchedEffect
        }

        try {
            nfcManager.prepareDataForSending(payload)
            activity.enterSendMode()
            isReady = true
            Log.d("NFC_SHARE", "Ready to share. HCE service active.")
        } catch (e: Exception) {
            Log.e("NFC_SHARE", "Error preparing NFC share", e)
            errorMessage = "Failed to prepare NFC sharing: ${e.message}"
        }
    }

    val writeSuccess by activity.nfcWriteSuccess

    LaunchedEffect(writeSuccess) {
        if (writeSuccess) {
            successMessage = "Note shared successfully!"
            Log.d("NFC_SHARE", "Write success confirmed")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("NFC_SHARE", "Cleaning up share screen")
            activity.exitNfcMode()
        }
    }

    BackHandler {
        Log.d("NFC_SHARE", "Back button pressed")
        activity.exitNfcMode()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share via NFC") },
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
                    .scale(if (isReady && successMessage == null) scale else 1f)
                    .background(
                        color = when {
                            successMessage != null -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            isReady -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (successMessage != null)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when {
                        successMessage != null -> Color(0xFF4CAF50)
                        isReady -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = when {
                    errorMessage != null -> "Unable to Share"
                    successMessage != null -> "Shared Successfully!"
                    isReady -> "Ready to Share"
                    else -> "Preparing..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    successMessage != null -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = when {
                    errorMessage != null -> errorMessage!!
                    successMessage != null -> "Note has been transferred to the other device"
                    isReady -> "Hold your device back-to-back with the receiving device.\n\nThis device is ready to send data."
                    else -> "Setting up NFC..."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isReady) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sharing:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
                                text = note.title,
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
                                text = note.originalFileName,
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
                                        text = note.fileType,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isReady && successMessage == null) {
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
                            text = "Keep devices touching for 2-3 seconds. Make sure the other device is in 'Receive' mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (errorMessage != null || successMessage != null) {
                Button(
                    onClick = {
                        activity.exitNfcMode()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (successMessage != null) "Done" else "Go Back")
                }
            }
        }
    }
}