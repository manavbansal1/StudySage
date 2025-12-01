package com.group_7.studysage.ui.screens.GroupsScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.group_7.studysage.ui.viewmodels.GroupChatViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupDetailsOverlay(
    groupId: String,
    groupName: String,
    groupPic: String,
    memberCount: Int,
    members: List<Map<String, Any>>,
    isAdmin: Boolean,
    currentUserId: String,
    onDismiss: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteToAdmin: (String) -> Unit,
    onRemoveAllMembers: () -> Unit,
    groupChatViewModel: GroupChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isUploadingImage by groupChatViewModel.isUploadingImage.collectAsState()
    val uploadError by groupChatViewModel.uploadError.collectAsState()
    val uploadSuccess by groupChatViewModel.uploadSuccess.collectAsState()

    // Dialog states preserved across rotation via ViewModel
    val showImageSourceDialog by groupChatViewModel.showImageSourceDialog.collectAsState()
    val showLeaveConfirmation by groupChatViewModel.showLeaveConfirmation.collectAsState()
    val showDeleteConfirmation by groupChatViewModel.showDeleteConfirmation.collectAsState()
    val showRemoveMembersRequiredDialog by groupChatViewModel.showRemoveMembersRequiredDialog.collectAsState()

    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    // Auto-dismiss success message
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess != null) {
            kotlinx.coroutines.delay(3000)
            groupChatViewModel.clearUploadSuccess()
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingImageUri = it
            scope.launch {
                groupChatViewModel.uploadGroupProfilePicture(
                    context = context,
                    imageUri = it,
                    groupId = groupId
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Group Profile Picture Section
                    item {
                        GroupProfileSection(
                            groupPic = groupPic,
                            groupName = groupName,
                            memberCount = memberCount,
                            isAdmin = isAdmin,
                            isUploading = isUploadingImage,
                            onProfilePicClick = {
                                if (isAdmin) {
                                    groupChatViewModel.setShowImageSourceDialog(true)
                                }
                            }
                        )
                    }

                    // Upload Error with Retry
                    uploadError?.let { error ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Upload Failed",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = error,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    // Retry button if we have a pending image
                                    pendingImageUri?.let { uri ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { groupChatViewModel.clearUploadError() }
                                            ) {
                                                Text("Dismiss")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        groupChatViewModel.uploadGroupProfilePicture(
                                                            context = context,
                                                            imageUri = uri,
                                                            groupId = groupId
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Upload Success
                    uploadSuccess?.let { message ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF4CAF50).copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = message,
                                        color = Color(0xFF1B5E20),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Members Section Header
                    item {
                        Text(
                            text = "Members ($memberCount)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Members List
                    items(members) { member ->
                        MemberItem(
                            member = member,
                            isCurrentUser = member["userId"] == currentUserId,
                            isAdmin = isAdmin,
                            onRemove = {
                                onRemoveMember(member["userId"] as? String ?: "")
                            },
                            onPromote = {
                                onPromoteToAdmin(member["userId"] as? String ?: "")
                            }
                        )
                    }

                    // Action Buttons
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Leave Group Button
                        if (!isAdmin) {
                            Button(
                                onClick = { groupChatViewModel.setShowLeaveConfirmation(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Leave Group",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Delete Group Button (Admin only)
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (memberCount > 1) {
                                        groupChatViewModel.setShowRemoveMembersRequiredDialog(true)
                                    } else {
                                        groupChatViewModel.setShowDeleteConfirmation(true)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Delete Group",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { groupChatViewModel.setShowImageSourceDialog(false) },
            title = { Text("Change Group Picture") },
            text = { Text("Choose image source") },
            confirmButton = {
                TextButton(onClick = {
                    groupChatViewModel.setShowImageSourceDialog(false)
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupChatViewModel.setShowImageSourceDialog(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Leave Confirmation
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { groupChatViewModel.setShowLeaveConfirmation(false) },
            icon = {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Leave Group?") },
            text = { Text("Are you sure you want to leave $groupName?") },
            confirmButton = {
                Button(
                    onClick = {
                        groupChatViewModel.setShowLeaveConfirmation(false)
                        onLeaveGroup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupChatViewModel.setShowLeaveConfirmation(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Remove Members Required Dialog
    if (showRemoveMembersRequiredDialog) {
        AlertDialog(
            onDismissRequest = { groupChatViewModel.setShowRemoveMembersRequiredDialog(false) },
            icon = {
                Icon(Icons.Default.GroupRemove, null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Remove Members First") },
            text = {
                Text("You must remove all other members before you can delete this group.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupChatViewModel.setShowRemoveMembersRequiredDialog(false)
                        onRemoveAllMembers()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove All Members")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupChatViewModel.setShowRemoveMembersRequiredDialog(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { groupChatViewModel.setShowDeleteConfirmation(false) },
            icon = {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete Group?") },
            text = {
                Text("This will permanently delete $groupName and all its messages. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupChatViewModel.setShowDeleteConfirmation(false)
                        onDeleteGroup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupChatViewModel.setShowDeleteConfirmation(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GroupProfileSection(
    groupPic: String,
    groupName: String,
    memberCount: Int,
    isAdmin: Boolean,
    isUploading: Boolean,
    onProfilePicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .clickable(enabled = isAdmin && !isUploading) { onProfilePicClick() }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            } else if (groupPic.isNotEmpty()) {
                AsyncImage(
                    model = groupPic,
                    contentDescription = "Group Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Camera overlay for admins
                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change picture",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Add picture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = groupName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = "$memberCount members",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun MemberItem(
    member: Map<String, Any>,
    isCurrentUser: Boolean,
    isAdmin: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit
) {
    val memberName = member["name"] as? String ?: "Unknown"
    val memberRole = member["role"] as? String ?: "member"
    val memberPic = member["profilePic"] as? String ?: ""
    val isThisUserAdmin = memberRole == "admin"

    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Member Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (memberPic.isNotEmpty()) {
                    AsyncImage(
                        model = memberPic,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = memberName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Member Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCurrentUser) "$memberName (You)" else memberName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isThisUserAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = if (isThisUserAdmin) "Group Admin" else "Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            if (isAdmin && !isCurrentUser) {
                Box {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        if (!isThisUserAdmin) {
                            DropdownMenuItem(
                                text = { Text("Promote to Admin") },
                                onClick = {
                                    showOptions = false
                                    onPromote()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Shield, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Group", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOptions = false
                                onRemove()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            } else if (isThisUserAdmin) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Admin",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}