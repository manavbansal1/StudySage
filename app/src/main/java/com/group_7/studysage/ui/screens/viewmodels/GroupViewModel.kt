package com.group_7.studysage.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.GroupRepository
import com.group_7.studysage.data.repository.GroupInvite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

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

    private val _pendingInvites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val pendingInvites: StateFlow<List<GroupInvite>> = _pendingInvites.asStateFlow()

    private val _pendingInviteCount = MutableStateFlow(0)
    val pendingInviteCount: StateFlow<Int> = _pendingInviteCount.asStateFlow()

    private val _showInviteOverlay = MutableStateFlow(false)
    val showInviteOverlay: StateFlow<Boolean> = _showInviteOverlay.asStateFlow()

    init {
        loadGroups()
        loadPendingInvites()
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

    fun loadPendingInvites() {
        viewModelScope.launch {
            try {
                val invites = authRepository.getPendingInvites()
                _pendingInvites.value = invites
                _pendingInviteCount.value = invites.size
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun toggleInviteOverlay() {
        _showInviteOverlay.value = !_showInviteOverlay.value
    }

    fun acceptInvite(invite: GroupInvite) {
        viewModelScope.launch {
            try {
                // 1. Accept the invite (marks it as accepted in user's invites)
                val acceptResult = authRepository.acceptGroupInvite(invite.inviteId, invite.groupId)

                acceptResult.onSuccess {
                    // 2. Get user profile info
                    val userProfile = authRepository.getUserProfile()
                    val userId = authRepository.currentUser?.uid ?: return@launch
                    val userName = userProfile?.get("name") as? String ?: "User"
                    val userProfilePic = userProfile?.get("profileImageUrl") as? String ?: ""

                    // 3. Add user to group members
                    val addResult = groupRepository.addMemberToGroup(
                        groupId = invite.groupId,
                        userId = userId,
                        userName = userName,
                        userProfilePic = userProfilePic
                    )

                    addResult.onSuccess {
                        // 4. Add group to user's profile
                        authRepository.addGroupToUserProfile(
                            groupId = invite.groupId,
                            groupName = invite.groupName,
                            groupPic = invite.groupPic
                        )

                        // 5. Delete the invite after successful acceptance
                        authRepository.deleteInvite(invite.inviteId)

                        // 6. Reload both groups and invites
                        loadGroups()
                        loadPendingInvites()
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun rejectInvite(invite: GroupInvite) {
        viewModelScope.launch {
            try {
                val result = authRepository.rejectGroupInvite(invite.inviteId)

                result.onSuccess {
                    // Reload invites
                    loadPendingInvites()
                }
            } catch (e: Exception) {
                // Handle error
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