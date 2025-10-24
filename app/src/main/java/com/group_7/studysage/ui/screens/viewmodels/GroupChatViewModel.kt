package com.group_7.studysage.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.GroupRepository
import com.group_7.studysage.data.repository.GroupMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class GroupChatUiState {
    object Loading : GroupChatUiState()
    data class Success(
        val groupName: String,
        val groupPic: String,
        val memberCount: Int,
        val isAdmin: Boolean,
        val members: List<Map<String, Any>>
    ) : GroupChatUiState()
    data class Error(val message: String) : GroupChatUiState()
}

class GroupChatViewModel(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupChatUiState>(GroupChatUiState.Loading)
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val messages: StateFlow<List<GroupMessage>> = _messages.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    init {
        _currentUserId.value = authRepository.currentUser?.uid ?: ""
    }

    fun loadGroupData(groupId: String) {
        viewModelScope.launch {
            _uiState.value = GroupChatUiState.Loading

            try {
                val groupProfile = groupRepository.getGroupProfile(groupId)

                if (groupProfile == null) {
                    _uiState.value = GroupChatUiState.Error("Group not found")
                    return@launch
                }

                val groupName = groupProfile["name"] as? String ?: "Unknown Group"
                val groupPic = groupProfile["profilePic"] as? String ?: ""
                val memberCount = (groupProfile["memberCount"] as? Long)?.toInt() ?: 0
                val members = groupProfile["members"] as? List<Map<String, Any>> ?: emptyList()

                val currentUserId = authRepository.currentUser?.uid ?: ""
                val isAdmin = groupRepository.isUserAdmin(groupId, currentUserId)

                _uiState.value = GroupChatUiState.Success(
                    groupName = groupName,
                    groupPic = groupPic,
                    memberCount = memberCount,
                    isAdmin = isAdmin,
                    members = members
                )
            } catch (e: Exception) {
                _uiState.value = GroupChatUiState.Error(e.message ?: "Failed to load group data")
            }
        }
    }

    fun loadMessages(groupId: String) {
        viewModelScope.launch {
            try {
                val messagesList = groupRepository.getMessages(groupId, limit = 100)
                _messages.value = messagesList.reversed() // Show oldest first
            } catch (e: Exception) {
                // Handle error silently or show notification
            }
        }
    }

    fun sendMessage(groupId: String, message: String, images: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val result = groupRepository.sendMessage(groupId, message, images)

                result.onSuccess {
                    // Update last message in user's group summary
                    val currentUser = authRepository.currentUser
                    val userName = currentUser?.displayName ?: "You"

                    authRepository.updateGroupLastMessage(
                        groupId = groupId,
                        message = message,
                        senderName = userName,
                        timestamp = System.currentTimeMillis()
                    )

                    // Update all group members' summaries
                    updateAllMembersGroupSummary(groupId, message, userName)

                    // Reload messages
                    loadMessages(groupId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addMemberByEmail(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                // Search for user by email
                val user = authRepository.getUserByEmail(email)

                if (user == null) {
                    _uiState.value = GroupChatUiState.Error("User not found with email: $email")
                    // Reload previous state after showing error
                    loadGroupData(groupId)
                    return@launch
                }

                val userId = user["uid"] as? String ?: return@launch
                val userName = user["name"] as? String ?: "Unknown User"
                val userProfilePic = user["profileImageUrl"] as? String ?: ""

                // Add user to group
                val result = groupRepository.addMemberToGroup(groupId, userId, userName, userProfilePic)

                result.onSuccess {
                    // Get group info
                    val groupProfile = groupRepository.getGroupProfile(groupId)
                    val groupName = groupProfile?.get("name") as? String ?: ""
                    val groupPic = groupProfile?.get("profilePic") as? String ?: ""

                    // Add group to user's profile
                    val groupSummary = mapOf(
                        "groupId" to groupId,
                        "groupName" to groupName,
                        "groupPic" to groupPic,
                        "lastMessage" to "",
                        "lastMessageTime" to 0L,
                        "lastMessageSender" to "",
                        "joinedAt" to System.currentTimeMillis()
                    )

                    // Update the target user's profile
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userGroups = user["groups"] as? MutableList<Map<String, Any>> ?: mutableListOf()
                    userGroups.add(groupSummary)

                    firestore.collection("users").document(userId)
                        .update("groups", userGroups)
                        .await()

                    // Reload group data
                    loadGroupData(groupId)
                }

                result.onFailure { error ->
                    _uiState.value = GroupChatUiState.Error(error.message ?: "Failed to add member")
                    loadGroupData(groupId)
                }
            } catch (e: Exception) {
                _uiState.value = GroupChatUiState.Error(e.message ?: "An error occurred")
                loadGroupData(groupId)
            }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = groupRepository.removeMemberFromGroup(groupId, userId)

                result.onSuccess {
                    // Remove group from user's profile
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val userGroups = userDoc.get("groups") as? MutableList<Map<String, Any>> ?: mutableListOf()

                    val updatedGroups = userGroups.filter { it["groupId"] != groupId }
                    firestore.collection("users").document(userId)
                        .update("groups", updatedGroups)
                        .await()

                    // Reload group data
                    loadGroupData(groupId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun promoteToAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = groupRepository.promoteMemberToAdmin(groupId, userId)

                result.onSuccess {
                    // Reload group data
                    loadGroupData(groupId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser?.uid ?: return@launch

                // Remove user from group
                groupRepository.removeMemberFromGroup(groupId, userId)

                // Remove from user profile
                authRepository.removeGroupFromUserProfile(groupId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                // Get all members first
                val members = groupRepository.getGroupMembers(groupId)

                // Remove group from all members' profiles
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                members.forEach { member ->
                    val userDoc = firestore.collection("users").document(member.userId).get().await()
                    val userGroups = userDoc.get("groups") as? MutableList<Map<String, Any>> ?: mutableListOf()

                    val updatedGroups = userGroups.filter { it["groupId"] != groupId }
                    firestore.collection("users").document(member.userId)
                        .update("groups", updatedGroups)
                        .await()
                }

                // Delete the group
                groupRepository.deleteGroup(groupId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun updateAllMembersGroupSummary(groupId: String, message: String, senderName: String) {
        try {
            val members = groupRepository.getGroupMembers(groupId)
            val timestamp = System.currentTimeMillis()
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            members.forEach { member ->
                val userDoc = firestore.collection("users").document(member.userId).get().await()
                val userGroups = userDoc.get("groups") as? MutableList<Map<String, Any>> ?: mutableListOf()

                val updatedGroups = userGroups.map { group ->
                    if (group["groupId"] == groupId) {
                        group.toMutableMap().apply {
                            put("lastMessage", message)
                            put("lastMessageTime", timestamp)
                            put("lastMessageSender", senderName)
                        }
                    } else {
                        group
                    }
                }

                firestore.collection("users").document(member.userId)
                    .update("groups", updatedGroups)
                    .await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}