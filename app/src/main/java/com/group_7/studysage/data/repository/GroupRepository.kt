package com.group_7.studysage.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
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
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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
                        "name" to (currentUser.displayName ?: "User"),
                        "profilePic" to (currentUser.photoUrl?.toString() ?: ""),
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
            null
        }
    }

    /**
     * Update group profile
     */
    suspend fun updateGroupProfile(groupId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    /**
     * Remove member from group
     */
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupData = getGroupProfile(groupId)
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            val updatedMembers = members.filter { it["userId"] != userId }

            firestore.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "members" to updatedMembers,
                        "memberCount" to FieldValue.increment(-1)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Promote member to admin
     */
    suspend fun promoteMemberToAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupData = getGroupProfile(groupId)
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

            val messageId = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .document().id

            val messageData = mapOf(
                "messageId" to messageId,
                "senderId" to currentUser.uid,
                "senderName" to (currentUser.displayName ?: "User"),
                "senderProfilePic" to (currentUser.photoUrl?.toString() ?: ""),
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
                        "lastMessageSender" to (currentUser.displayName ?: "User")
                    )
                )
                .await()

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get messages for a group (with pagination)
     */
    suspend fun getMessages(groupId: String, limit: Int = 50): List<GroupMessage> {
        return try {
            val snapshot = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    GroupMessage(
                        messageId = doc.getString("messageId") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        senderProfilePic = doc.getString("senderProfilePic") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        images = (doc.get("images") as? List<String>) ?: emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a message (admin only)
     */
    suspend fun deleteMessage(groupId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete entire group (admin only)
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            // Delete all messages first
            val messages = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .get()
                .await()

            messages.documents.forEach { it.reference.delete().await() }

            // Delete the group
            firestore.collection("groups").document(groupId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is admin of a group
     */
    suspend fun isUserAdmin(groupId: String, userId: String): Boolean {
        return try {
            val groupData = getGroupProfile(groupId)
            val members = groupData?.get("members") as? List<Map<String, Any>> ?: emptyList()

            members.any {
                it["userId"] == userId && it["role"] == "admin"
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all members of a group
     */
    suspend fun getGroupMembers(groupId: String): List<GroupMember> {
        return try {
            val groupData = getGroupProfile(groupId)
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
                    null
                }
            }
        } catch (e: Exception) {
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
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        GroupMessage(
                            messageId = doc.getString("messageId") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderProfilePic = doc.getString("senderProfilePic") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            images = (doc.get("images") as? List<String>) ?: emptyList()
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