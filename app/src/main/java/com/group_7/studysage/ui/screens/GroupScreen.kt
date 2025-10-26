package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.ui.viewmodels.GroupViewModel
import com.group_7.studysage.ui.viewmodels.GroupUiState
import com.group_7.studysage.ui.viewmodels.GroupItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onGroupClick: (String) -> Unit  // ← Matches your navigation parameter
) {
    val viewModel: GroupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val pendingInviteCount by viewModel.pendingInviteCount.collectAsState()
    val showInviteOverlay by viewModel.showInviteOverlay.collectAsState()
    val pendingInvites by viewModel.pendingInvites.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Groups") },
                    actions = {
                        // Bell icon with badge for invites
                        BadgedBox(
                            badge = {
                                if (pendingInviteCount > 0) {
                                    Badge {
                                        Text(pendingInviteCount.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.toggleInviteOverlay() }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Group Invites"
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Group"
                    )
                }
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is GroupUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GroupUiState.Success -> {
                    if (state.groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No groups yet. Create one!")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.groups) { group ->
                                GroupItemCard(
                                    group = group,
                                    onClick = { onGroupClick(group.groupId) }  // ← Uses your parameter
                                )
                            }
                        }
                    }
                }

                is GroupUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadGroups() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // Invite overlay
        if (showInviteOverlay) {
            GroupInviteOverlay(
                invites = pendingInvites,
                onAccept = { invite ->
                    viewModel.acceptInvite(invite)
                },
                onReject = { invite ->
                    viewModel.rejectInvite(invite)
                },
                onDismiss = { viewModel.toggleInviteOverlay() }
            )
        }

        // Create group dialog
        if (showCreateGroupDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateGroupDialog = false },
                onConfirm = { name, description ->
                    viewModel.createGroup(name, description)
                    showCreateGroupDialog = false
                }
            )
        }
    }
}

@Composable
private fun GroupItemCard(
    group: GroupItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.titleMedium
            )
            if (group.lastMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${group.lastMessageSender}: ${group.lastMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}