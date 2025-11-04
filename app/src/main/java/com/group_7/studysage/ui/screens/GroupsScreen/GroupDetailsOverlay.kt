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
    groupChatViewModel: GroupChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isUploadingImage by groupChatViewModel.isUploadingImage.collectAsState()
    val uploadError by groupChatViewModel.uploadError.collectAsState()

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Details",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3D2564))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
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
                                    showImageSourceDialog = true
                                }
                            }
                        )
                    }

                    // Upload Error
                    uploadError?.let { error ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp
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
                            color = Color.White,
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
                                onClick = { showLeaveConfirmation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Leave Group", fontSize = 16.sp)
                            }
                        }

                        // Delete Group Button (Admin only)
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Group", fontSize = 16.sp)
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
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Group Picture") },
            text = { Text("Choose image source") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF3D2564),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0B0C0)
        )
    }

    // Leave Confirmation
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            icon = {
                Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFEF4444))
            },
            title = { Text("Leave Group?") },
            text = { Text("Are you sure you want to leave $groupName?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmation = false
                        onLeaveGroup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF3D2564),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0B0C0)
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
            },
            title = { Text("Delete Group?") },
            text = {
                Text("This will permanently delete $groupName and all its messages. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteGroup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF3D2564),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0B0C0)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable(enabled = isAdmin && !isUploading) { onProfilePicClick() }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF9333EA),
                            Color(0xFF7C3AED)
                        )
                    )
                )
                .border(4.dp, Color(0xFF6B4BA6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(40.dp)
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
                            .background(Color.Black.copy(alpha = 0.4f)),
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
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9333EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Add picture",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = groupName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$memberCount members",
            fontSize = 14.sp,
            color = Color(0xFFB0B0C0)
        )

        if (isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF9333EA).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Admin",
                    fontSize = 12.sp,
                    color = Color(0xFF9333EA),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2564)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
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
                    .background(Color(0xFF9333EA)),
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
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCurrentUser) "$memberName (You)" else memberName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isThisUserAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF9333EA).copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "Admin",
                                fontSize = 10.sp,
                                color = Color(0xFF9333EA),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Actions (for admins, not on themselves)
            if (isAdmin && !isCurrentUser) {
                IconButton(
                    onClick = { showOptions = !showOptions }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }
        }

        // Member Actions Dropdown
        if (showOptions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                HorizontalDivider(color = Color(0xFF6B4BA6).copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(8.dp))

                if (!isThisUserAdmin) {
                    TextButton(
                        onClick = {
                            showOptions = false
                            onPromote()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFF9333EA)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Make Admin",
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                TextButton(
                    onClick = {
                        showOptions = false
                        onRemove()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Remove from Group",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

