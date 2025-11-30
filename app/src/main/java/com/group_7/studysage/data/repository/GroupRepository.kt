package com.group_7.studysage.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.group_7.studysage.utils.NotificationHelper
import com.group_7.studysage.utils.StudySageNotificationManager
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @GroupInvite data class to represent a group invitation
 * Includes details about the invite such as who invited, group info, timestamp, and status
 * Status can be "pending", "accepted", or "rejected"
 */
data class GroupInvite(
    val inviteId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val groupPic: String = "",
    val invitedBy: String = "",
    val invitedByName: String = "",
    val timestamp: Long = 0L,
    val status: String = "pending"
)

/**
 * @GroupMessage data class to represent a message in a group
 * Includes sender details, message content, timestamp, and optional images
 * Images are represented as a list of URLs
 */
data class GroupMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfilePic: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val images: List<String> = emptyList()
)

data class GroupMember(
    val userId: String = "",
    val name: String = "",
    val profilePic: String = "",
    val role: String = "member", // "admin" or "member"
    val joinedAt: Long = 0L
)

class GroupRepository(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "GroupRepository"
    }

    /**
     * Create a new group
     */
    suspend fun createGroup(
        name: String,
        description: String = "",
        profilePic: String = ""
    ): Result<String> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user logged in"))

            // Fetch user's actual name from Firestore profile
            val userProfile = try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user profile: ${e.message}", e)
                null
            }
            
            val userName = userProfile?.getString("name") ?: currentUser.displayName ?: "User"
            val userProfilePic = userProfile?.getString("profileImageUrl") ?: currentUser.photoUrl?.toString() ?: ""

            val groupId = firestore.collection("groups").document().id

            val groupProfile = mapOf(
                "groupId" to groupId,
                "name" to name,
                "description" to description,
                "profilePic" to profilePic,
                "adminId" to currentUser.uid,
                "createdAt" to System.currentTimeMillis(),
                "memberCount" to 1,
                "members" to listOf(
                    mapOf(
                        "userId" to currentUser.uid,
                        "name" to userName,
                        "profilePic" to userProfilePic,
                        "role" to "admin",
                        "joinedAt" to System.currentTimeMillis()
                    )
                ),
                "messages" to listOf<Map<String, Any>>(),
                "settings" to mapOf(
                    "allowMemberMessages" to true,
                    "allowImageSharing" to true,
                    "maxMembers" to 50
                )
            )

            firestore.collection("groups").document(groupId).set(groupProfile).await()

            Result.success(groupId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create group: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get group profile
     */
    suspend fun getGroupProfile(groupId: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("groups").document(groupId).get().await()
            document.data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch group profile: ${e.message}", e)
            null
        }
    }

    /**
     * Update group profile
     */
    suspend fun updateGroupProfile(groupId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId).update(updates).await()
            Log.d(TAG, "Group profile updated id=$groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update group profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update group profile picture
     */
    suspend fun updateGroupProfilePic(groupId: String, imageUrl: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .update("profilePic", imageUrl)
                .await()
            Log.d(TAG, "Group profile picture updated for id=$groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update group profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Add member to group (now only updates group, doesn't touch user profile)
     * User profile is updated when they accept the invite
     */
    suspend fun addMemberToGroup(
        groupId: String,
        userId: String,
        userName: String,
        userProfilePic: String
    ): Result<Unit> {
        return try {
            val newMember = mapOf(
                "userId" to userId,
                "name" to userName,
                "profilePic" to userProfilePic,
                "role" to "member",
                "joinedAt" to System.currentTimeMillis()
            )

            firestore.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "members" to FieldValue.arrayUnion(newMember),
                        "memberCount" to FieldValue.increment(1)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add member $userId to group $groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove member from group
     * Also removes the group from the user's database if removeFromProfile is true
     */
    suspend fun removeMemberFromGroup(
        groupId: String, 
        userId: String, 
        removeFromProfile: Boolean = true
    ): Result<Unit> {
        return try {
            // Get group data
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            // Remove user from group's members list
            val updatedMembers = members.filter { it["userId"] != userId }

            firestore.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "members" to updatedMembers,
                        "memberCount" to FieldValue.increment(-1)
                    )
                )
                .await()

            // Remove group from user's profile ONLY if requested
            if (removeFromProfile) {
                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    @Suppress("UNCHECKED_CAST")
                    val userGroups = userDoc.get("groups") as? MutableList<Map<String, Any>> ?: mutableListOf()
                    
                    val updatedUserGroups = userGroups.filter { it["groupId"] != groupId }
                    
                    firestore.collection("users").document(userId)
                        .update("groups", updatedUserGroups)
                        .await()
                        
                    Log.d(TAG, "Removed group $groupId from user $userId's profile")
                } catch (e: Exception) {
                    // Log but don't fail the whole operation if user update fails
                    Log.e(TAG, "Failed to remove group from user profile: ${e.message}", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member $userId from group $groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove ALL members from group except the admin
     * Used before deleting a group to ensure clean cleanup
     */
    suspend fun removeAllMembersFromGroup(groupId: String, adminId: String): Result<Unit> {
        return try {
            // Get all members
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()
            
            // Filter out the admin
            val membersToRemove = members.filter { it["userId"] != adminId }
            
            // Remove each member
            membersToRemove.forEach { member ->
                val userId = member["userId"] as? String
                if (userId != null) {
                    // Remove from group and their profile
                    removeMemberFromGroup(groupId, userId, removeFromProfile = true)
                }
            }
            
            Log.d(TAG, "Removed all members from group $groupId except admin $adminId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove all members from group $groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove group from user's profile ONLY
     * Used when the group itself has been deleted or user is removed
     */
    suspend fun removeGroupFromUserProfile(groupId: String, userId: String): Result<Unit> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            val userGroups = userDoc.get("groups") as? MutableList<Map<String, Any>> ?: mutableListOf()
            
            val updatedUserGroups = userGroups.filter { it["groupId"] != groupId }
            
            firestore.collection("users").document(userId)
                .update("groups", updatedUserGroups)
                .await()
                
            Log.d(TAG, "Removed group $groupId from user $userId's profile")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove group from user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete entire group (admin only)
     * 1. Removes all members from the group document
     * 2. Removes group from Admin's profile (so it disappears for them)
     * 3. KEEPS group in other members' profiles (so they see "Group not found" when clicking)
     * 4. Deletes all messages and the group document globally
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            
            // Get all members first
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()
            
            // Process each member
            members.forEach { member ->
                val userId = member["userId"] as? String
                if (userId != null) {
                    try {
                        // If it's the admin (current user), remove from their profile so it disappears
                        // If it's another member, KEEP in profile so they see "Group not found" when clicking
                        val isCurrentUser = userId == currentUser?.uid
                        
                        removeMemberFromGroup(groupId, userId, removeFromProfile = isCurrentUser)
                        
                        Log.d(TAG, "Processed member $userId deletion (removedFromProfile=$isCurrentUser)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process member $userId: ${e.message}", e)
                    }
                }
            }
            
            // Delete all messages from Firestore
            val messages = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .get()
                .await()

            messages.documents.forEach { it.reference.delete().await() }

            // Delete the group document globally from Firestore
            firestore.collection("groups").document(groupId).delete().await()
            
            Log.d(TAG, "Successfully deleted group $groupId globally")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete group $groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Promote member to admin
     */
    suspend fun promoteMemberToAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            val updatedMembers = members.map { member ->
                if (member["userId"] == userId) {
                    member.toMutableMap().apply { put("role", "admin") }
                } else {
                    member
                }
            }

            firestore.collection("groups").document(groupId)
                .update("members", updatedMembers)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to promote member $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message to group
     */
    suspend fun sendMessage(
        groupId: String,
        message: String,
        images: List<String> = emptyList()
    ): Result<String> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user logged in"))

            // VALIDATE: Check if user is still a member
            if (!isUserMember(groupId, currentUser.uid)) {
                return Result.failure(Exception("You are not a member of this group"))
            }

            // Fetch user's actual name from Firestore profile
            val userProfile = try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user profile: ${e.message}", e)
                null
            }
            
            val userName = userProfile?.getString("name") ?: currentUser.displayName ?: "User"
            val userProfilePic = userProfile?.getString("profileImageUrl") ?: currentUser.photoUrl?.toString() ?: ""

            val messageId = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .document().id

            val messageData = mapOf(
                "messageId" to messageId,
                "senderId" to currentUser.uid,
                "senderName" to userName,
                "senderProfilePic" to userProfilePic,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "images" to images
            )

            // Add message to subcollection
            firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .document(messageId)
                .set(messageData)
                .await()

            // Update group's last message info
            firestore.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageTime" to System.currentTimeMillis(),
                        "lastMessageSender" to userName
                    )
                )
                .await()

            // Notify group members about the new message
            notifyGroupMembers(
                groupId = groupId,
                senderId = currentUser.uid,
                senderName = userName,
                messageText = message
            )

            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to group $groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Notify group members about a new message.
     * This method runs asynchronously and doesn't block message sending.
     * Errors are logged but don't affect the message sending operation.
     *
     * @param groupId The ID of the group
     * @param senderId The ID of the user who sent the message
     * @param senderName The name of the user who sent the message
     * @param messageText The message content
     */
    private suspend fun notifyGroupMembers(
        groupId: String,
        senderId: String,
        senderName: String,
        messageText: String
    ) {
        try {
            Log.d(TAG, "Notifying group members for group: $groupId")

            // Get group details
            val groupDoc = firestore.collection("groups")
                .document(groupId)
                .get()
                .await()

            if (!groupDoc.exists()) {
                Log.w(TAG, "Group not found: $groupId")
                return
            }

            val groupName = groupDoc.getString("name") ?: "Group"

            // Get members list
            @Suppress("UNCHECKED_CAST")
            val membersList = groupDoc.get("members") as? List<Map<String, Any>> ?: emptyList()

            // Extract member IDs (exclude sender)
            val memberIds = membersList.mapNotNull { member ->
                val memberId = member["userId"] as? String
                if (memberId != null && memberId != senderId) {
                    memberId
                } else {
                    null
                }
            }

            Log.d(TAG, "Found ${memberIds.size} members to notify")

            // Truncate message for notification
            val truncatedMessage = NotificationHelper.truncateMessage(messageText, 100)

            // Send notification to each member
            memberIds.forEach { memberId ->
                try {
                    // Check if notifications are enabled for this user
                    val notificationsEnabled = try {
                        val userDoc = firestore.collection("users")
                            .document(memberId)
                            .get()
                            .await()
                        userDoc.getBoolean("settings.notificationsEnabled") ?: false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking notification settings for $memberId: ${e.message}")
                        false
                    }

                    // Send notification if user has notifications enabled
                    // Note: We send even when app is in foreground for group messages
                    if (notificationsEnabled) {
                        StudySageNotificationManager.showGroupMessage(
                            context = context,
                            groupName = groupName,
                            senderName = senderName,
                            message = truncatedMessage
                        )
                        Log.d(TAG, "✅ Notification sent to member: $memberId")
                    } else {
                        Log.d(TAG, "❌ Skipping notification for member: $memberId (notifications disabled in settings)")
                    }
                } catch (e: Exception) {
                    // Log but continue with other members
                    Log.e(TAG, "Error sending notification to member $memberId", e)
                }
            }

            Log.d(TAG, "Finished notifying group members")

        } catch (e: Exception) {
            // Don't throw - just log the error
            // This ensures message sending isn't affected by notification failures
            Log.e(TAG, "Error notifying group members: ${e.message}", e)
        }
    }


    /**
     * Check if user is a member of a group
     */
    suspend fun isUserMember(groupId: String, userId: String): Boolean {
        return try {
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()
            
            members.any { it["userId"] == userId }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check membership: ${e.message}", e)
            false
        }
    }

    /**
     * Check if user is admin of a group
     */
    suspend fun isUserAdmin(groupId: String, userId: String): Boolean {
        return try {
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            members.any {
                it["userId"] == userId && it["role"] == "admin"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to determine admin status: ${e.message}", e)
            false
        }
    }

    /**
     * Get all members of a group
     */
    suspend fun getGroupMembers(groupId: String): List<GroupMember> {
        return try {
            val groupData = getGroupProfile(groupId)
            @Suppress("UNCHECKED_CAST")
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            members.mapNotNull { member ->
                try {
                    GroupMember(
                        userId = member["userId"] as? String ?: "",
                        name = member["name"] as? String ?: "",
                        profilePic = member["profilePic"] as? String ?: "",
                        role = member["role"] as? String ?: "member",
                        joinedAt = (member["joinedAt"] as? Long) ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse group member: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch group members: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Observe group profile with real-time updates
     */
    fun observeGroupProfile(groupId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = firestore.collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Group profile listener error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.data)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe messages with real-time updates
     */
    fun observeMessages(groupId: String, limit: Int = 50): Flow<List<GroupMessage>> = callbackFlow {
        val listener = firestore.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Group messages listener error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val imagesList = (doc.get("images") as? List<String>) ?: emptyList()
                        GroupMessage(
                            messageId = doc.getString("messageId") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderProfilePic = doc.getString("senderProfilePic") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            images = imagesList
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}
