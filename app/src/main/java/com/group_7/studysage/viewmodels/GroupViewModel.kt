package com.group_7.studysage.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.GroupRepository
import com.group_7.studysage.data.repository.GroupInvite
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GroupViewModel"
        private const val PREFS_NAME = "group_last_seen"
    }

    private val groupRepository: GroupRepository = GroupRepository(application.applicationContext)
    private val authRepository: AuthRepository = AuthRepository()
    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

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

    // Pull-to-refresh flag
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Operation status for user feedback
    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    // Real-time listener for group invites
    private var inviteListener: ListenerRegistration? = null
    
    // Real-time listener for group updates
    private var groupsListener: ListenerRegistration? = null

    init {
        loadGroups()
        startListeningToInvites() // Start real-time listening instead of one-time load
        startListeningToGroups() // Start real-time listening for group updates
    }

    /**
     * Start listening to real-time invite updates
     */
    private fun startListeningToInvites() {
        inviteListener = authRepository.listenToGroupInvites { invites ->
            Log.d(TAG, "Real-time invite update: ${'$'}{invites.size} pending invites")
            _pendingInvites.value = invites
            _pendingInviteCount.value = invites.size
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading

            try {
                val userGroups = authRepository.getUserGroups()

                val groupItems = userGroups.mapNotNull { groupSummary ->
                    val groupId = groupSummary["groupId"] as? String ?: ""
                    val groupProfile = groupRepository.getGroupProfile(groupId)

                    // Skip if group no longer exists (deleted)
                    if (groupProfile == null) {
                        Log.d(TAG, "Group $groupId no longer exists, skipping")
                        return@mapNotNull null
                    }

                    GroupItem(
                        groupId = groupId,
                        // Read from GLOBAL groupProfile instead of local groupSummary
                        groupName = groupProfile["name"] as? String ?: "",
                        groupPic = groupProfile["profilePic"] as? String ?: "",
                        lastMessage = groupProfile["lastMessage"] as? String ?: "",
                        lastMessageTime = (groupProfile["lastMessageTime"] as? Long) ?: 0L,
                        lastMessageSender = groupProfile["lastMessageSender"] as? String ?: "",
                        memberCount = (groupProfile["memberCount"] as? Long)?.toInt() ?: 0,
                        unreadCount = calculateUnreadCount(groupId, (groupProfile["lastMessageTime"] as? Long) ?: 0L),
                        isAdmin = checkIfAdminSync(groupId)
                    )
                }.sortedByDescending { it.lastMessageTime }

                _uiState.value = GroupUiState.Success(groupItems)
                Log.d(TAG, "Successfully loaded ${groupItems.size} groups")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load groups: ${e.message}", e)
                _uiState.value = GroupUiState.Error(e.message ?: "Failed to load groups")
            }
        }
    }

    /**
     * Refresh groups list used by pull-to-refresh in UI
     */
    fun refreshGroups() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val deferred = async { loadGroups() }
                deferred.await()
                delay(250)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh groups: ${'$'}{e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Backwards-compatible synchronous checkIfAdmin wrapper (small helper)
     */
    private fun checkIfAdminSync(groupId: String): Boolean {
        // We can't call suspend functions here; use blocking call pattern by launching a coroutine is expensive.
        // For now, default to false for quicker UI load. Real admin checks are handled elsewhere if needed.
        return false
    }

    /**
     * Start listening to real-time group updates from user's profile AND individual group documents
     * Updates UI whenever group data changes (new messages, etc.)
     */
    private fun startListeningToGroups() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot listen to groups: No user logged in")
            return
        }

        // First, listen to user's groups array to detect when they join/leave groups
        groupsListener = authRepository.firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user groups: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    viewModelScope.launch {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val userGroups = snapshot.get("groups") as? List<Map<String, Any>> ?: emptyList()
                            
                            // Set up listeners for each individual group
                            setupGroupListeners(userGroups.mapNotNull { it["groupId"] as? String })
                            
                            // Load initial group data
                            loadGroupsData(userGroups)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing user groups snapshot: ${e.message}", e)
                        }
                    }
                }
            }
    }

    // Store individual group listeners
    private val individualGroupListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * Set up listeners for individual group documents to detect message changes
     */
    private fun setupGroupListeners(groupIds: List<String>) {
        // Remove listeners for groups user is no longer in
        val currentGroupIds = groupIds.toSet()
        individualGroupListeners.keys.toList().forEach { groupId ->
            if (groupId !in currentGroupIds) {
                individualGroupListeners[groupId]?.remove()
                individualGroupListeners.remove(groupId)
                Log.d(TAG, "Removed listener for group $groupId")
            }
        }

        // Add listeners for new groups
        groupIds.forEach { groupId ->
            if (!individualGroupListeners.containsKey(groupId)) {
                val listener = authRepository.firestore.collection("groups")
                    .document(groupId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening to group $groupId: ${error.message}", error)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            // Group data changed (new message, etc.) - refresh all groups
                            viewModelScope.launch {
                                refreshGroupsData()
                            }
                            Log.d(TAG, "Group $groupId updated - refreshing groups page")
                        }
                    }
                individualGroupListeners[groupId] = listener
                Log.d(TAG, "Added listener for group $groupId")
            }
        }
    }

    /**
     * Load group data from user's groups array
     */
    private suspend fun loadGroupsData(userGroups: List<Map<String, Any>>) {
        try {
            val groupItems = userGroups.mapNotNull { groupSummary ->
                val groupId = groupSummary["groupId"] as? String ?: ""
                val groupProfile = groupRepository.getGroupProfile(groupId)

                // Skip if group no longer exists (deleted)
                if (groupProfile == null) {
                    Log.d(TAG, "Group $groupId no longer exists, skipping")
                    return@mapNotNull null
                }

                GroupItem(
                    groupId = groupId,
                    groupName = groupProfile["name"] as? String ?: "",
                    groupPic = groupProfile["profilePic"] as? String ?: "",
                    lastMessage = groupProfile["lastMessage"] as? String ?: "",
                    lastMessageTime = (groupProfile["lastMessageTime"] as? Long) ?: 0L,
                    lastMessageSender = groupProfile["lastMessageSender"] as? String ?: "",
                    memberCount = (groupProfile["memberCount"] as? Long)?.toInt() ?: 0,
                    unreadCount = calculateUnreadCount(groupId, (groupProfile["lastMessageTime"] as? Long) ?: 0L),
                    isAdmin = false
                )
            }.sortedByDescending { it.lastMessageTime }

            _uiState.value = GroupUiState.Success(groupItems)
            Log.d(TAG, "Loaded ${groupItems.size} groups")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading groups data: ${e.message}", e)
        }
    }

    /**
     * Refresh groups data (called when any group receives a message)
     */
    private suspend fun refreshGroupsData() {
        try {
            val userGroups = authRepository.getUserGroups()
            loadGroupsData(userGroups)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing groups: ${e.message}", e)
        }
    }

    /**
     * Calculate unread count for a group
     * Compares last message time with last seen time
     */
    private fun calculateUnreadCount(groupId: String, lastMessageTime: Long): Int {
        if (lastMessageTime == 0L) return 0
        
        val lastSeenTime = sharedPrefs.getLong("last_seen_$groupId", 0L)
        return if (lastMessageTime > lastSeenTime) 1 else 0
    }

    /**
     * Mark a group as read (update last seen time)
     * Called when user opens a group chat
     */
    fun markGroupAsRead(groupId: String) {
        val currentTime = System.currentTimeMillis()
        sharedPrefs.edit().putLong("last_seen_$groupId", currentTime).apply()
        
        // Refresh groups to update unread counts
        val currentState = _uiState.value
        if (currentState is GroupUiState.Success) {
            val updatedGroups = currentState.groups.map { group ->
                if (group.groupId == groupId) {
                    group.copy(unreadCount = 0)
                } else {
                    group
                }
            }
            _uiState.value = GroupUiState.Success(updatedGroups)
        }
        
        Log.d(TAG, "Marked group $groupId as read")
    }

    fun loadPendingInvites() {
        viewModelScope.launch {
            try {
                val invites = authRepository.getPendingInvites()
                _pendingInvites.value = invites
                _pendingInviteCount.value = invites.size
                Log.d(TAG, "Manually loaded ${'$'}{invites.size} pending invites")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load pending invites: ${'$'}{e.message}", e)
            }
        }
    }

    fun toggleInviteOverlay() {
        _showInviteOverlay.value = !_showInviteOverlay.value
    }

    fun acceptInvite(invite: GroupInvite) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Accepting invite for group: ${'$'}{invite.groupName}")

                val acceptResult = authRepository.acceptGroupInvite(invite.inviteId, invite.groupId)

                acceptResult.onSuccess {
                    val userProfile = authRepository.getUserProfile()
                    val userId = authRepository.currentUser?.uid ?: return@launch
                    val userName = userProfile?.get("name") as? String ?: "User"
                    val userProfilePic = userProfile?.get("profileImageUrl") as? String ?: ""

                    val addResult = groupRepository.addMemberToGroup(
                        groupId = invite.groupId,
                        userId = userId,
                        userName = userName,
                        userProfilePic = userProfilePic
                    )

                    addResult.onSuccess {
                        authRepository.addGroupToUserProfile(
                            groupId = invite.groupId,
                            groupName = invite.groupName,
                            groupPic = invite.groupPic
                        )

                        authRepository.deleteInvite(invite.inviteId)

                        loadGroups()

                        Log.d(TAG, "Successfully accepted invite and joined group: ${'$'}{invite.groupName}")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to add user to group: ${'$'}{error.message}", error)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to accept invite: ${'$'}{error.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting invite: ${'$'}{e.message}", e)
            }
        }
    }

    fun rejectInvite(invite: GroupInvite) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Rejecting invite for group: ${'$'}{invite.groupName}")

                val result = authRepository.rejectGroupInvite(invite.inviteId)

                result.onSuccess {
                    Log.d(TAG, "Successfully rejected invite for group: ${'$'}{invite.groupName}")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to reject invite: ${'$'}{error.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting invite: ${'$'}{e.message}", e)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating group: $name")
                _operationStatus.value = "Creating group..."

                val result = groupRepository.createGroup(name, description)

                result.onSuccess { groupId ->
                    authRepository.addGroupToUserProfile(
                        groupId = groupId,
                        groupName = name,
                        groupPic = ""
                    )

                    loadGroups()
                    _operationStatus.value = "Group '$name' created successfully"

                    Log.d(TAG, "Successfully created group: $name with ID: $groupId")
                    
                    // Clear success message after delay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _operationStatus.value = null
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "Failed to create group: ${error.message}", error)
                    val errorMsg = when {
                        error.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection"
                        error.message?.contains("permission", ignoreCase = true) == true ->
                            "Permission denied. Please sign in again"
                        else -> "Failed to create group: ${error.message}"
                    }
                    _uiState.value = GroupUiState.Error(errorMsg)
                    _operationStatus.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating group: ${e.message}", e)
                val errorMsg = "An error occurred: ${e.message}"
                _uiState.value = GroupUiState.Error(errorMsg)
                _operationStatus.value = errorMsg
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting group: $groupId")
                _operationStatus.value = "Deleting group..."

                val result = groupRepository.deleteGroup(groupId)

                result.onSuccess {
                    authRepository.removeGroupFromUserProfile(groupId)
                    loadGroups()
                    _operationStatus.value = "Group deleted successfully"
                    Log.d(TAG, "Successfully deleted group: $groupId")
                    
                    // Clear success message after delay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _operationStatus.value = null
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "Failed to delete group: ${error.message}", error)
                    val errorMsg = "Failed to delete group: ${error.message}"
                    _uiState.value = GroupUiState.Error(errorMsg)
                    _operationStatus.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting group: ${e.message}", e)
                val errorMsg = "An error occurred: ${e.message}"
                _uiState.value = GroupUiState.Error(errorMsg)
                _operationStatus.value = errorMsg
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser?.uid ?: return@launch

                Log.d(TAG, "User $userId leaving group: $groupId")
                _operationStatus.value = "Leaving group..."

                val result = groupRepository.removeMemberFromGroup(groupId, userId)

                result.onSuccess {
                    authRepository.removeGroupFromUserProfile(groupId)
                    loadGroups()
                    _operationStatus.value = "Left group successfully"
                    Log.d(TAG, "Successfully left group: $groupId")
                    
                    // Clear success message after delay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _operationStatus.value = null
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "Failed to leave group: ${error.message}", error)
                    val errorMsg = "Failed to leave group: ${error.message}"
                    _uiState.value = GroupUiState.Error(errorMsg)
                    _operationStatus.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving group: ${e.message}", e)
                val errorMsg = "An error occurred: ${e.message}"
                _uiState.value = GroupUiState.Error(errorMsg)
                _operationStatus.value = errorMsg
            }
        }
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


    override fun onCleared() {
        super.onCleared()
        inviteListener?.remove()
        groupsListener?.remove()
        // Remove all individual group listeners
        individualGroupListeners.values.forEach { it.remove() }
        individualGroupListeners.clear()
        Log.d(TAG, "GroupViewModel cleared, all listeners removed")
    }
}

