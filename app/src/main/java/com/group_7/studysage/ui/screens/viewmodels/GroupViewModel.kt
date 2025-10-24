package com.group_7.studysage.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class GroupItem(
    val groupId: String,
    val groupName: String,
    val groupPic: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val lastMessageSender: String,
    val memberCount: Int,
    val unreadCount: Int = 0,
    val isAdmin: Boolean = false
)

sealed class GroupUiState {
    object Loading : GroupUiState()
    data class Success(val groups: List<GroupItem>) : GroupUiState()
    data class Error(val message: String) : GroupUiState()
}

class GroupViewModel(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading

            try {
                // Get groups from user profile
                val userGroups = authRepository.getUserGroups()

                // Convert to GroupItem format
                val groupItems = userGroups.map { groupSummary ->
                    val groupId = groupSummary["groupId"] as? String ?: ""
                    val groupProfile = groupRepository.getGroupProfile(groupId)

                    GroupItem(
                        groupId = groupId,
                        groupName = groupSummary["groupName"] as? String ?: "",
                        groupPic = groupSummary["groupPic"] as? String ?: "",
                        lastMessage = groupSummary["lastMessage"] as? String ?: "",
                        lastMessageTime = (groupSummary["lastMessageTime"] as? Long) ?: 0L,
                        lastMessageSender = groupSummary["lastMessageSender"] as? String ?: "",
                        memberCount = (groupProfile?.get("memberCount") as? Long)?.toInt() ?: 0,
                        unreadCount = 0, // Implement unread logic later
                        isAdmin = checkIfAdmin(groupId)
                    )
                }.sortedByDescending { it.lastMessageTime }

                _uiState.value = GroupUiState.Success(groupItems)
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "Failed to load groups")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            try {
                val result = groupRepository.createGroup(name, description)

                result.onSuccess { groupId ->
                    // Add group to user profile
                    authRepository.addGroupToUserProfile(
                        groupId = groupId,
                        groupName = name,
                        groupPic = ""
                    )

                    // Reload groups
                    loadGroups()
                }

                result.onFailure { error ->
                    _uiState.value = GroupUiState.Error(error.message ?: "Failed to create group")
                }
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val result = groupRepository.deleteGroup(groupId)

                result.onSuccess {
                    // Remove from user profile
                    authRepository.removeGroupFromUserProfile(groupId)

                    // Reload groups
                    loadGroups()
                }

                result.onFailure { error ->
                    _uiState.value = GroupUiState.Error(error.message ?: "Failed to delete group")
                }
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser?.uid ?: return@launch

                // Remove user from group
                val result = groupRepository.removeMemberFromGroup(groupId, userId)

                result.onSuccess {
                    // Remove from user profile
                    authRepository.removeGroupFromUserProfile(groupId)

                    // Reload groups
                    loadGroups()
                }

                result.onFailure { error ->
                    _uiState.value = GroupUiState.Error(error.message ?: "Failed to leave group")
                }
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun addMemberByEmail(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                // Search for user by email
                val user = authRepository.getUserByEmail(email)

                if (user == null) {
                    _uiState.value = GroupUiState.Error("User not found with email: $email")
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

                    // Add group to user's profile using Firestore directly
                    // We need to update the other user's profile, not current user
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

                    // Reload groups to show updated member count
                    loadGroups()
                }

                result.onFailure { error ->
                    _uiState.value = GroupUiState.Error(error.message ?: "Failed to add member")
                }
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private suspend fun checkIfAdmin(groupId: String): Boolean {
        val userId = authRepository.currentUser?.uid ?: return false
        return groupRepository.isUserAdmin(groupId, userId)
    }

    fun getFilteredGroups(): List<GroupItem> {
        val currentState = _uiState.value
        if (currentState !is GroupUiState.Success) return emptyList()

        val query = _searchQuery.value
        if (query.isEmpty()) return currentState.groups

        return currentState.groups.filter {
            it.groupName.contains(query, ignoreCase = true)
        }
    }
}