package com.group_7.studysage.ui.screens.GroupsScreen

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.group_7.studysage.ui.viewmodels.GroupChatViewModel
import com.group_7.studysage.ui.viewmodels.GroupChatUiState
import com.group_7.studysage.data.repository.GroupMessage
import kotlinx.coroutines.delay
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    viewModel: GroupChatViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Define custom easing for the "pop" effect
    val BackOutEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
    val messages by viewModel.messages.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val inviteStatus by viewModel.inviteStatus.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }
    var showGroupDetails by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
        viewModel.loadMessages(groupId)
    }

    // Show invite status as snackbar
    LaunchedEffect(inviteStatus) {
        if (inviteStatus != null) {
            delay(3000)
            viewModel.clearInviteStatus()
        }
    }

    // Main container with theme background - fills entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is GroupChatUiState.Loading -> {
                ChatLoadingState()
            }

            is GroupChatUiState.Success -> {
                // Main chat layout - Column with header, messages, input
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header at top with status bar padding
                    GroupChatHeader(
                        groupName = state.groupName,
                        groupPic = state.groupPic,
                        memberCount = state.memberCount,
                        isAdmin = state.isAdmin,
                        onBackClick = onNavigateBack,
                        onGroupClick = { showGroupDetails = true },
                        onInviteClick = { showInviteDialog = true }
                    )

                    // Messages area
                    if (messages.isEmpty()) {
                        EmptyChatState(
                            groupName = state.groupName,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        MessagesList(
                            messages = messages,
                            currentUserId = currentUserId,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Message input at bottom
                    val context = LocalContext.current
                    MessageInputSection(
                        onSendMessage = { message ->
                            viewModel.sendMessage(groupId, message)
                        },
                        onSendAttachment = { uri, type ->
                            viewModel.uploadFile(
                                context = context,
                                fileUri = uri,
                                fileType = type,
                                onSuccess = { url ->
                                    if (type == "image") {
                                        viewModel.sendMessage(
                                            groupId = groupId,
                                            message = "",
                                            images = listOf(url)
                                        )
                                    } else {
                                        viewModel.sendMessage(
                                            groupId = groupId,
                                            message = "",
                                            attachments = listOf(
                                                com.group_7.studysage.data.repository.Attachment(
                                                    url = url,
                                                    type = type,
                                                    name = "Attachment" // Could extract real name if needed
                                                )
                                            )
                                        )
                                    }
                                },
                                onError = { error ->
                                    // Handle error (maybe show snackbar)
                                }
                            )
                        },
                        backOutEasing = BackOutEasing
                    )
                }

                // Group Details Overlay
                if (showGroupDetails) {
                    GroupDetailsOverlay(
                        groupId = groupId,
                        groupName = state.groupName,
                        groupPic = state.groupPic,
                        memberCount = state.memberCount,
                        members = state.members,
                        isAdmin = state.isAdmin,
                        currentUserId = currentUserId,
                        onDismiss = { showGroupDetails = false },
                        onLeaveGroup = {
                            viewModel.leaveGroup(groupId)
                            onNavigateBack()
                        },
                        onDeleteGroup = {
                            viewModel.deleteGroup(groupId)
                            onNavigateBack()
                        },
                        onRemoveMember = { userId ->
                            viewModel.removeMember(groupId, userId)
                        },
                        onPromoteToAdmin = { userId ->
                            viewModel.promoteToAdmin(groupId, userId)
                        },
                        onRemoveAllMembers = {
                            viewModel.removeAllMembers(groupId)
                        },
                        groupChatViewModel = viewModel
                    )
                }
            }

            is GroupChatUiState.Error -> {
                ErrorState(
                    message = state.message,
                    groupId = groupId,
                    viewModel = viewModel,
                    onRetry = {
                        viewModel.loadGroupData(groupId)
                        viewModel.loadMessages(groupId)
                    },
                    onBackClick = onNavigateBack
                )
            }
        }

        // Invite status snackbar
        inviteStatus?.let { status ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Invite dialog
        if (showInviteDialog) {
            InviteMemberDialog(
                onDismiss = { showInviteDialog = false },
                onConfirm = { email ->
                    viewModel.sendInviteByEmail(groupId, email)
                    showInviteDialog = false
                }
            )
        }
    }
}

@Composable
private fun GroupChatHeader(
    groupName: String,
    groupPic: String,
    memberCount: Int,
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onGroupClick: () -> Unit,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // Handle status bar padding internally
                .height(64.dp) // Standard toolbar height
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Group Avatar & Info Clickable Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGroupClick() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (groupPic.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(groupPic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Group Info
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$memberCount members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Invite Button (only for admins)
            if (isAdmin) {
                IconButton(
                    onClick = onInviteClick
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Member",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<GroupMessage>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Consistent spacing
    ) {
        items(
            items = messages.reversed(),
            key = { message -> message.messageId }
        ) { message ->
            // Animated entry for messages
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            ) {
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderId == currentUserId
                )
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun MessageBubble(
    message: GroupMessage,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for other users)
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isCurrentUser) 20.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isCurrentUser) {
                        SolidColor(Color(0xFF4A148C)) // Dark Purple
                    } else {
                        SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Images
                if (message.images.isNotEmpty()) {
                    message.images.forEach { imageUrl ->
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Sent image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Attachments
                if (message.attachments.isNotEmpty()) {
                    message.attachments.forEach { attachment ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // PDF Logo (Left)
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf, // Or Description if PictureAsPdf not available
                                    contentDescription = "PDF",
                                    tint = Color.Red,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // File Name (Middle)
                                Text(
                                    text = attachment.name.ifEmpty { "Document.pdf" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Download Button (Right)
                                IconButton(
                                    onClick = {
                                        try {
                                            uriHandler.openUri(attachment.url)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Message text
                if (message.message.isNotBlank()) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MessageInputSection(
    onSendMessage: (String) -> Unit,
    onSendAttachment: (Uri, String) -> Unit,
    backOutEasing: Easing,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }

    // Floating pill container
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding() // Handle nav bar padding
            .imePadding(), // Handle keyboard
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Add Attachment Button
            Box {
                var expanded by remember { mutableStateOf(false) }
                
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        onSendAttachment(it, "pdf")
                    }
                }

                val imageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        onSendAttachment(it, "image")
                    }
                }

                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Attachment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(180.dp),
                    offset = DpOffset(x = (-12).dp, y = 0.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Notes") },
                        onClick = {
                            expanded = false
                            launcher.launch("application/pdf")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Photos") },
                        onClick = {
                            expanded = false
                            imageLauncher.launch("image/*")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Games") },
                        onClick = {
                            expanded = false
                            // TODO: Handle Games attachment
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // Message input field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Type a message...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 5,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Animated Send button
            val isSendVisible = messageText.isNotBlank()
            val buttonScale by animateFloatAsState(
                targetValue = if (isSendVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 300, easing = backOutEasing),
                label = "scale"
            )
            val buttonWidth by animateDpAsState(
                targetValue = if (isSendVisible) 52.dp else 0.dp, // 44dp size + 8dp padding
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "width"
            )

            Box(
                modifier = Modifier.width(buttonWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText.trim())
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .scale(buttonScale) // Scale from center
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading messages...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyChatState(
    groupName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No messages yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start the conversation in $groupName",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    groupId: String,
    viewModel: GroupChatViewModel,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if this is a "removed from group" error
    val isRemovedError = message.contains("no longer a member", ignoreCase = true) ||
                         message.contains("removed from this group", ignoreCase = true) ||
                         message.contains("Group not found", ignoreCase = true)

    Box(modifier = modifier.fillMaxSize()) {
        // Back arrow in top left
        if (isRemovedError) {
            IconButton(
                onClick = {
                    // Remove group from user's profile before navigating back
                    viewModel.removeGroupFromUserProfile(groupId)
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Center content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isRemovedError) {
                // Show "Remove Group" button for removed users
                Button(
                    onClick = {
                        // Remove group from user's profile before navigating back
                        viewModel.removeGroupFromUserProfile(groupId)
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Group from List")
                }
            } else {
                // Show both buttons for other errors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go Back")
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isValidEmail by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Invite Member",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter the email address of the person you want to invite to this group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isValidEmail = true
                    },
                    label = { Text("Email Address") },
                    singleLine = true,
                    isError = !isValidEmail,
                    supportingText = {
                        if (!isValidEmail) {
                            Text("Please enter a valid email address")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        onConfirm(email)
                    } else {
                        isValidEmail = false
                    }
                },
                enabled = email.isNotBlank()
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GroupDetailsOverlay(
    groupId: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) { },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Group Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Group ID: $groupId",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Close",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        else -> {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            format.format(date)
        }
    }
}