package com.group_7.studysage.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.ui.viewmodels.GroupChatViewModel
import com.group_7.studysage.ui.viewmodels.GroupChatUiState
import com.group_7.studysage.data.repository.GroupMessage
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
    val messages by viewModel.messages.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val inviteStatus by viewModel.inviteStatus.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }
    var showGroupDetails by remember { mutableStateOf(false) } // ⭐ NEW

    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
        viewModel.loadMessages(groupId)
    }

    // Show invite status as snackbar
    LaunchedEffect(inviteStatus) {
        if (inviteStatus != null) {
            // You can show a snackbar here
            // After showing, clear the status
            kotlinx.coroutines.delay(3000)
            viewModel.clearInviteStatus()
        }
    }

    // Main container with gradient background - fills entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B4E),
                        Color(0xFF3D2B5E),
                        Color(0xFF2D1B4E)
                    )
                )
            )
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
                        memberCount = state.memberCount,
                        isAdmin = state.isAdmin,
                        onBackClick = onNavigateBack,
                        onGroupClick = { showGroupDetails = true }, // ⭐ NEW - Click to show details
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
                    MessageInputSection(
                        onSendMessage = { message ->
                            viewModel.sendMessage(groupId, message)
                        }
                    )
                }

                // Group Details Overlay - ⭐ UPDATED
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
                        groupChatViewModel = viewModel
                    )
                }
            }

            is GroupChatUiState.Error -> {
                ErrorState(
                    message = state.message,
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
                    color = Color(0xFF3D2564),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = Color.White,
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

        // Group details overlay
        if (showGroupDetails) {
            GroupDetailsOverlay(
                groupId = groupId,
                onDismiss = { showGroupDetails = false }
            )
        }
    }
}

@Composable
private fun GroupChatHeader(
    groupName: String,
    memberCount: Int,
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onGroupClick: () -> Unit, // ⭐ NEW
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Surface that extends to top of screen with status bar padding
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color(0xFF3D2564),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2D1B4E),
                                Color(0xFF1D0B3E)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Group Avatar with gradient border - ⭐ CLICKABLE
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onGroupClick() } // ⭐ NEW
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF9333EA),
                                Color(0xFF7C3AED),
                                Color(0xFFC084FC)
                            )
                        )
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF2D1B4E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Group Info - ⭐ CLICKABLE
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGroupClick() }, // ⭐ NEW
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = groupName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9333EA))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$memberCount members",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0C0),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Invite Button (only for admins)
            if (isAdmin) {
                IconButton(
                    onClick = onInviteClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9333EA),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Member",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
    ) {
        items(
            items = messages.reversed(),
            key = { message -> message.messageId }
        ) { message ->
            MessageBubble(
                message = message,
                isCurrentUser = message.senderId == currentUserId
            )
            Spacer(modifier = Modifier.height(8.dp))
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for other users)
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9333EA),
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        // Message bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isCurrentUser) 20.dp else 4.dp,
                topEnd = if (isCurrentUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = if (isCurrentUser)
                Color(0xFF9333EA)
            else
                Color(0xFF3D2564),
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Message text
                Text(
                    text = message.message,
                    fontSize = 15.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MessageInputSection(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color(0xFF3D2564),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Message input field
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2D1B4E),
                border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.3f))
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    maxLines = 5,
                    cursorBrush = SolidColor(Color(0xFF9333EA)),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Type a message...",
                                    fontSize = 15.sp,
                                    color = Color(0xFFB0B0C0).copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank())
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9333EA),
                                    Color(0xFF7C3AED)
                                )
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2D1B4E),
                                    Color(0xFF2D1B4E)
                                )
                            )
                    ),
                enabled = messageText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) Color.White else Color(0xFF6B4BA6),
                    modifier = Modifier.size(22.dp)
                )
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
                color = Color(0xFF9333EA),
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading messages...",
                fontSize = 14.sp,
                color = Color(0xFFB0B0C0)
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
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFF9333EA).copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No messages yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start the conversation in $groupName",
                fontSize = 14.sp,
                color = Color(0xFFB0B0C0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
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
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFFF6B6B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Error",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFFB0B0C0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3D2564)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back")
                }

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9333EA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
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
        title = { Text("Invite Member") },
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
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
    // TODO: Implement group details overlay
    // This is a placeholder implementation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(32.dp),
            color = Color(0xFF3D2564),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Group Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // TODO: Add actual group details here
                Text(
                    text = "Group ID: $groupId",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0C0)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End) // ⭐ FIX: Changed from Alignment.end to Alignment.End
                ) {
                    Text(
                        text = "Close",
                        color = Color(0xFF9333EA)
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
