package com.group_7.studysage.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.GroupRepository
import com.group_7.studysage.data.repository.GroupMessage
import com.group_7.studysage.data.repository.Attachment
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
    application: Application
) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository = GroupRepository(application.applicationContext)
    private val authRepository: AuthRepository = AuthRepository()

    private val _uiState = MutableStateFlow<GroupChatUiState>(GroupChatUiState.Loading)
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val messages: StateFlow<List<GroupMessage>> = _messages.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _inviteStatus = MutableStateFlow<String?>(null)
    val inviteStatus: StateFlow<String?> = _inviteStatus.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    // Persist message text across rotation
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    fun setMessageText(text: String) {
        _messageText.value = text
    }

    // Persist invite email across rotation
    private val _inviteEmail = MutableStateFlow("")
    val inviteEmail: StateFlow<String> = _inviteEmail.asStateFlow()

    fun setInviteEmail(email: String) {
        _inviteEmail.value = email
    }

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _uploadSuccess = MutableStateFlow<String?>(null)
    val uploadSuccess: StateFlow<String?> = _uploadSuccess.asStateFlow()

    // Dialog states - preserved across rotation
    private val _showImageSourceDialog = MutableStateFlow(false)
    val showImageSourceDialog: StateFlow<Boolean> = _showImageSourceDialog.asStateFlow()

    private val _showLeaveConfirmation = MutableStateFlow(false)
    val showLeaveConfirmation: StateFlow<Boolean> = _showLeaveConfirmation.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    private val _showRemoveMembersRequiredDialog = MutableStateFlow(false)
    val showRemoveMembersRequiredDialog: StateFlow<Boolean> = _showRemoveMembersRequiredDialog.asStateFlow()

    init {
        _currentUserId.value = authRepository.currentUser?.uid ?: ""
    }

    fun loadGroupData(groupId: String) {
        viewModelScope.launch {
            _uiState.value = GroupChatUiState.Loading

            try {
                val currentUserId = authRepository.currentUser?.uid ?: ""
                
                // Set up real-time listener instead of one-time read
                groupRepository.observeGroupProfile(groupId).collect { groupProfile ->
                    if (groupProfile == null) {
                        _uiState.value = GroupChatUiState.Error("Group not found")
                        return@collect
                    }

                    // CHECK: Is user still a member?
                    val isMember = groupRepository.isUserMember(groupId, currentUserId)
                    if (!isMember) {
                        _uiState.value = GroupChatUiState.Error("You are no longer a member of this group")
                        return@collect
                    }

                    val groupName = groupProfile["name"] as? String ?: "Unknown Group"
                    val groupPic = groupProfile["profilePic"] as? String ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val members = groupProfile["members"] as? List<Map<String, Any>> ?: emptyList()
                    val memberCount = members.size

                    val isAdmin = groupRepository.isUserAdmin(groupId, currentUserId)

                    _uiState.value = GroupChatUiState.Success(
                        groupName = groupName,
                        groupPic = groupPic,
                        memberCount = memberCount,
                        isAdmin = isAdmin,
                        members = members
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = GroupChatUiState.Error(e.message ?: "Failed to load group data")
            }
        }
    }

    fun loadMessages(groupId: String) {
        viewModelScope.launch {
            try {
                // Set up real-time listener for messages
                groupRepository.observeMessages(groupId, limit = 100).collect { messagesList ->
                    _messages.value = messagesList.reversed()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendMessage(groupId: String, message: String, images: List<String> = emptyList(), attachments: List<Attachment> = emptyList()) {
        viewModelScope.launch {
            try {
                val result = groupRepository.sendMessage(groupId, message, images, attachments)

                result.onSuccess {
                    // Last message is now updated globally in GroupRepository.sendMessage()
                    // All members will see the update via real-time listener
                    // No need to update individual user documents
                    
                    // Reload messages
                    loadMessages(groupId)
                }
                
                result.onFailure { error ->
                    // Handle error - user might have been removed
                    if (error.message?.contains("not a member", ignoreCase = true) == true) {
                        // User was removed, update UI state
                        _uiState.value = GroupChatUiState.Error("You are no longer a member of this group")
                    }
                    Log.e("GroupChatViewModel", "Failed to send message: ${error.message}")
                }
            } catch (e: Exception) {
                // Handle error
                Log.e("GroupChatViewModel", "Error sending message: ${e.message}", e)
            }
        }
    }

    /**
     * Upload a file (PDF, etc.) to Cloudinary
     */
    fun uploadFile(
        context: android.content.Context,
        fileUri: android.net.Uri,
        fileType: String, // "pdf", etc.
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isUploadingImage.value = true // Reuse existing loading state or create a new one
                
                // Upload to Cloudinary
                // For PDFs, we use "raw" resource type usually
                val resourceType = if (fileType == "pdf") "raw" else "auto"
                val folder = "studysage/chat_attachments"
                
                val url = com.group_7.studysage.utils.CloudinaryUploader.uploadFile(
                    context = context,
                    fileUri = fileUri,
                    fileType = "raw", 
                    folder = folder,
                    resourceType = resourceType
                )

                _isUploadingImage.value = false

                if (url != null) {
                    onSuccess(url)
                } else {
                    onError("Failed to upload file")
                }
            } catch (e: Exception) {
                _isUploadingImage.value = false
                onError(e.message ?: "Unknown error during upload")
            }
        }
    }

    /**
     * Send invite to user by email instead of adding directly
     */
    fun sendInviteByEmail(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                _inviteStatus.value = "Sending invite..."

                val currentState = _uiState.value
                if (currentState !is GroupChatUiState.Success) {
                    _inviteStatus.value = "Error: Group data not loaded"
                    return@launch
                }

                // Send the invite
                val result = authRepository.sendGroupInvite(
                    recipientEmail = email,
                    groupId = groupId,
                    groupName = currentState.groupName,
                    groupPic = currentState.groupPic
                )

                result.onSuccess {
                    _inviteStatus.value = "Invite sent successfully to $email"
                }

                result.onFailure { error ->
                    _inviteStatus.value = "Error: ${error.message}"
                }
            } catch (e: Exception) {
                _inviteStatus.value = "Error: ${e.message}"
            }
        }
    }

    fun clearInviteStatus() {
        _inviteStatus.value = null
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = groupRepository.removeMemberFromGroup(groupId, userId)

                result.onSuccess {
                    // Check if the removed user is the current user
                    val currentUserId = authRepository.currentUser?.uid
                    if (userId == currentUserId) {
                        // Current user was removed, show error
                        _uiState.value = GroupChatUiState.Error("You have been removed from this group")
                    } else {
                        // Reload group data for remaining members
                        loadGroupData(groupId)
                    }
                }
            } catch (e: Exception) {
                // Handle error
                Log.e("GroupChatViewModel", "Error removing member: ${e.message}", e)
            }
        }
    }

    fun removeAllMembers(groupId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.currentUser?.uid ?: return@launch
                val result = groupRepository.removeAllMembersFromGroup(groupId, currentUserId)

                result.onSuccess {
                    // Reload group data to reflect changes
                    loadGroupData(groupId)
                }
                
                result.onFailure { error ->
                    Log.e("GroupChatViewModel", "Failed to remove all members: ${error.message}", error)
                }
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error removing all members: ${e.message}", e)
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

    /**
     * Remove group from current user's profile
     * Used when user is removed from group and wants to clean up their local list
     */


    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                // Get all members first
                val members = groupRepository.getGroupMembers(groupId)

                // Remove group from all members' profiles
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                members.forEach { member ->
                    val userDoc = firestore.collection("users").document(member.userId).get().await()
                    @Suppress("UNCHECKED_CAST")
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

    /**
     * Upload group profile picture to Cloudinary
     */
    fun uploadGroupProfilePicture(
        context: android.content.Context,
        imageUri: android.net.Uri,
        groupId: String
    ) {
        viewModelScope.launch {
            try {
                _isUploadingImage.value = true
                _uploadError.value = null
                _uploadSuccess.value = null

                // Validate file size before upload (max 5MB for better UX)
                val fileSize = try {
                    context.contentResolver.openInputStream(imageUri)?.use { it.available() } ?: 0
                } catch (e: Exception) {
                    0
                }

                if (fileSize > 5 * 1024 * 1024) {
                    _isUploadingImage.value = false
                    _uploadError.value = "Image too large. Please select an image under 5MB"
                    return@launch
                }

                if (fileSize == 0) {
                    _isUploadingImage.value = false
                    _uploadError.value = "Unable to read image file. Please try another image"
                    return@launch
                }

                // Upload to Cloudinary
                val imageUrl = com.group_7.studysage.utils.CloudinaryUploader.uploadFile(
                    context = context,
                    fileUri = imageUri,
                    fileType = "image",
                    folder = "studysage/groups/$groupId",
                    resourceType = "image"
                )

                if (imageUrl != null) {
                    // Update group profile in Firebase
                    val updateResult = groupRepository.updateGroupProfilePic(groupId, imageUrl)

                    updateResult.onSuccess {
                        // Profile pic is now global in groups collection
                        // All members will see the update via real-time listener
                        _isUploadingImage.value = false
                        _uploadSuccess.value = "Group picture updated successfully"
                    }

                    updateResult.onFailure { error ->
                        _isUploadingImage.value = false
                        _uploadError.value = "Failed to update group: ${error.message}"
                    }
                } else {
                    _isUploadingImage.value = false
                    // Check for common Cloudinary errors
                    _uploadError.value = when {
                        com.group_7.studysage.BuildConfig.CLOUDINARY_CLOUD_NAME.isBlank() -> 
                            "Image upload not configured. Please contact support"
                        else -> "Failed to upload image. Please check your internet connection and try again"
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _isUploadingImage.value = false
                _uploadError.value = "No internet connection. Please check your network and try again"
            } catch (e: Exception) {
                _isUploadingImage.value = false
                _uploadError.value = "Upload failed: ${e.message ?: "Unknown error"}"
            }
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun clearUploadSuccess() {
        _uploadSuccess.value = null
    }

    // Dialog state management functions
    fun setShowImageSourceDialog(show: Boolean) {
        _showImageSourceDialog.value = show
    }

    fun setShowLeaveConfirmation(show: Boolean) {
        _showLeaveConfirmation.value = show
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _showDeleteConfirmation.value = show
    }

    fun setShowRemoveMembersRequiredDialog(show: Boolean) {
        _showRemoveMembersRequiredDialog.value = show
    }

    fun removeGroupFromUserProfile(groupId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            groupRepository.removeGroupFromUserProfile(groupId, userId)
        }
    }
}