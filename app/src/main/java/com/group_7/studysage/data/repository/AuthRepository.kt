package com.group_7.studysage.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * @Class AuthRepository is used to handle all authentication and user profile related operations
 * using Firebase Authentication and Firestore as the database.
 * It has 2 main function parameters -> firebaseAuth and firestore which are instances of FirebaseAuth and FirebaseFirestore respectively.
 * It also has a property currentUser which returns the currently logged in FirebaseUser.
 * This helps us to perform various operations based on the current user.
 *
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Sign up a new user with email, password, and name
     * Initializes user profile in Firestore with default values
     * It returns a Result wrapping the FirebaseUser on success or an Exception on failure.
     *
     */
    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            val userProfile = mapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to email,
                "bio" to "Hey there! I'm using StudySage ✨",
                "profileImageUrl" to "", // empty until user uploads one
                "createdAt" to System.currentTimeMillis(),
                "lastLogin" to System.currentTimeMillis(),
                "level" to 1,
                "xpPoints" to 0,
                "streakDays" to 0,
                "quizStats" to mapOf(
                    "totalQuizzes" to 0,
                    "averageScore" to 0.0,
                    "highestScore" to 0.0
                ),
                "gameStats" to mapOf(
                    "totalGamesPlayed" to 0,
                    "wins" to 0,
                    "losses" to 0
                ),
                "preferences" to mapOf(
                    "darkMode" to false,
                    "notifications" to true
                ),
                "privacy" to mapOf(
                    "profileVisibility" to "everyone"
                ),
                "notifications" to mapOf(
                    "enabled" to true
                ),
                "groups" to listOf<Map<String, Any>>(), // List of group summaries
                "groupInvites" to listOf<Map<String, Any>>() // List of pending invites
            )
            firestore.collection("users").document(user.uid).set(userProfile).await()
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed for email=$email: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in an existing user with email and password
     * Updates lastLogin field in Firestore on successful login
     * It returns a Result wrapping the FirebaseUser on success or an Exception on failure.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            // Update lastLogin field
            result.user?.uid?.let { uid ->
                firestore.collection("users").document(uid)
                    .update("lastLogin", System.currentTimeMillis())
                    .await()
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed for email=$email: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Check if a user is currently signed in
     */
    fun isUserSignedIn(): Boolean {
        val signedIn = currentUser != null
        return signedIn
    }

    /**
     * Get user profile data from Firestore
     * Returns a Map of user profile fields or null if not found
     *
     */
    suspend fun getUserProfile(): Map<String, Any>? {
        return try {
            val userId = currentUser?.uid ?: return null
            val document = firestore.collection("users").document(userId).get().await()
            document.data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user profile: ${e.message}", e)
            null
        }
    }

    /**
     * Update user profile fields in Firestore
     * Accepts a Map of fields to update
     */
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            firestore.collection("users").document(userId).update(updates).await()
            Log.d(TAG, "User profile updated for uid=$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update user profile image URL in Firestore
     * It uses cloudinary to get the image URL after upload
     */
    suspend fun updateProfileImage(imageUrl: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            firestore.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile image: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Change user password with re-authentication for security
     * Returns Result<Unit> indicating success or failure with appropriate error messages
     * It uses the current password to re-authenticate before allowing password change
     * Uses firebase exceptions to handle specific error cases
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("No user logged in"))
            val email = user.email ?: return Result.failure(Exception("No email found"))
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Log.d(TAG, "Password updated successfully for uid=${user.uid}")
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Password change failed: invalid current password", e)
            Result.failure(Exception("Current password is incorrect"))
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            Log.e(TAG, "Password change failed: weak new password", e)
            Result.failure(Exception("New password is too weak. Please use at least 6 characters"))
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            Log.e(TAG, "Password change failed: recent login required", e)
            Result.failure(Exception("Please sign out and sign in again before changing password"))
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed: ${e.message}", e)
            Result.failure(Exception("Failed to change password: ${e.message}"))
        }
    }

    /**
     * Add a group summary to user's profile
     * Each user stores a lightweight summary of their groups for quick access
     * This includes groupId, groupName, groupPic, lastMessage, lastMessageTime, lastMessageSender, joinedAt
     * This is called when a user joins a new group
     * It returns Result<Unit> indicating success or failure
     */
    suspend fun addGroupToUserProfile(groupId: String, groupName: String, groupPic: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val groupSummary = mapOf(
                "groupId" to groupId,
                "groupName" to groupName,
                "groupPic" to groupPic,
                "lastMessage" to "",
                "lastMessageTime" to 0L,
                "lastMessageSender" to "",
                "joinedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId)
                .update("groups", FieldValue.arrayUnion(groupSummary))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add group $groupId to profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update the most recent message in user's group summary
     * Used so that the user always sees the current last message in their group list
     */
    suspend fun updateGroupLastMessage(
        groupId: String,
        message: String,
        senderName: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val groups = profile?.get("groups") as? List<Map<String, Any>> ?: emptyList()
            val updatedGroups = groups.map { group ->
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
            firestore.collection("users").document(userId)
                .update("groups", updatedGroups)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last message for group=$groupId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a group from user's profile
     * Used when a user leaves a group
     */
    suspend fun removeGroupFromUserProfile(groupId: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val groups = profile?.get("groups") as? List<Map<String, Any>> ?: emptyList()
            val updatedGroups = groups.filter { it["groupId"] != groupId }
            firestore.collection("users").document(userId)
                .update("groups", updatedGroups)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove group $groupId from profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all groups for current user
     * Returns a list of group summaries
     */
    suspend fun getUserGroups(): List<Map<String, Any>> {
        return try {
            val profile = getUserProfile()
            (profile?.get("groups") as? List<Map<String, Any>>) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user groups: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Search for user by email
     * Returns user profile data as Map or null if not found
     * Used for sending group invites to the user
     * if no user is found with the given email, it returns null
     */
    suspend fun getUserByEmail(email: String): Map<String, Any>? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            if (querySnapshot.documents.isNotEmpty()) {
                querySnapshot.documents.first().data
            } else {
                Log.d(TAG, "No user found for email=$email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by email=$email: ${e.message}", e)
            null
        }
    }

    /**
     * Send a group invite to a user
     * Uses recipient's email to find their user profile @func getUserByEmail
     * Checks if the user is already a member of the group or has a pending invite
     * If not, creates a new invite and adds it to the recipient's profile
     * Returns Result<Unit> indicating success or failure with appropriate error messages
     * The invite contains inviteId, groupId, groupName, groupPic, invitedBy, invitedByName, timestamp, status
     * Status is initially set to "pending"
     *
     */
    suspend fun sendGroupInvite(
        recipientEmail: String,
        groupId: String,
        groupName: String,
        groupPic: String
    ): Result<Unit> {
        return try {
            val currentUserId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val currentUserProfile = getUserProfile()
            val currentUserName = currentUserProfile?.get("name") as? String ?: "Unknown"
            val recipient = getUserByEmail(recipientEmail)
                ?: return Result.failure(Exception("User not found with email: $recipientEmail"))
            val recipientId = recipient["uid"] as? String
                ?: return Result.failure(Exception("Invalid user data"))
            // Check if user is already a member
            val recipientGroups = recipient["groups"] as? List<Map<String, Any>> ?: emptyList()
            val alreadyMember = recipientGroups.any { it["groupId"] == groupId }
            if (alreadyMember) {
                return Result.failure(Exception("User is already a member of this group"))
            }
            // Check if invite already exists
            val recipientInvites = recipient["groupInvites"] as? List<Map<String, Any>> ?: emptyList()
            val inviteExists = recipientInvites.any {
                it["groupId"] == groupId && it["status"] == "pending"
            }
            if (inviteExists) {
                return Result.failure(Exception("Invite already sent to this user"))
            }
            // Create new invite using a unique ID
            val inviteId = firestore.collection("users").document().id
            val invite = mapOf(
                "inviteId" to inviteId,
                "groupId" to groupId,
                "groupName" to groupName,
                "groupPic" to groupPic,
                "invitedBy" to currentUserId,
                "invitedByName" to currentUserName,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            // Add invite to recipient's profile
            firestore.collection("users").document(recipientId)
                .update("groupInvites", FieldValue.arrayUnion(invite))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send group invite to $recipientEmail: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all pending invites for current user
     * Returns a list of GroupInvite data class instances
     * GroupInvite contains inviteId, groupId, groupName, groupPic, invitedBy, invitedByName, timestamp, status
     * Filters invites to only include those with status "pending"
     */
    suspend fun getPendingInvites(): List<GroupInvite> {
        return try {
            val userId = currentUser?.uid ?: return emptyList()
            val profile = getUserProfile()
            val invites = profile?.get("groupInvites") as? List<Map<String, Any>> ?: emptyList()
            invites.filter { it["status"] == "pending" }.mapNotNull { invite ->
                try {
                    GroupInvite(
                        inviteId = invite["inviteId"] as? String ?: "",
                        groupId = invite["groupId"] as? String ?: "",
                        groupName = invite["groupName"] as? String ?: "",
                        groupPic = invite["groupPic"] as? String ?: "",
                        invitedBy = invite["invitedBy"] as? String ?: "",
                        invitedByName = invite["invitedByName"] as? String ?: "",
                        timestamp = (invite["timestamp"] as? Long) ?: 0L,
                        status = invite["status"] as? String ?: "pending"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse invite data: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pending invites: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Listen to real-time updates for group invites
     * Sets up a Firestore snapshot listener that triggers whenever invites change
     * Returns a ListenerRegistration that can be used to remove the listener
     * The callback receives a list of pending invites whenever they change
     *
     * Usage: val listener = authRepository.listenToGroupInvites { invites -> /* handle invites */ }
     * To stop listening: listener.remove()
     */
    fun listenToGroupInvites(onInvitesChanged: (List<GroupInvite>) -> Unit): ListenerRegistration? {
        val userId = currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot listen to invites: No user logged in")
            return null
        }

        return firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to group invites: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val invites = snapshot.get("groupInvites") as? List<Map<String, Any>> ?: emptyList()
                        val pendingInvites = invites.filter { it["status"] == "pending" }.mapNotNull { invite ->
                            try {
                                GroupInvite(
                                    inviteId = invite["inviteId"] as? String ?: "",
                                    groupId = invite["groupId"] as? String ?: "",
                                    groupName = invite["groupName"] as? String ?: "",
                                    groupPic = invite["groupPic"] as? String ?: "",
                                    invitedBy = invite["invitedBy"] as? String ?: "",
                                    invitedByName = invite["invitedByName"] as? String ?: "",
                                    timestamp = (invite["timestamp"] as? Long) ?: 0L,
                                    status = invite["status"] as? String ?: "pending"
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse invite in listener: ${e.message}", e)
                                null
                            }
                        }
                        onInvitesChanged(pendingInvites)
                        Log.d(TAG, "Group invites updated: ${pendingInvites.size} pending invites")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing invite snapshot: ${e.message}", e)
                        onInvitesChanged(emptyList())
                    }
                } else {
                    Log.d(TAG, "User document not found")
                    onInvitesChanged(emptyList())
                }
            }
    }

    /**
     * Accept a group invite
     */
    suspend fun acceptGroupInvite(inviteId: String, groupId: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val invites = profile?.get("groupInvites") as? List<Map<String, Any>> ?: emptyList()
            // Find and update the invite
            val updatedInvites = invites.map { invite ->
                if (invite["inviteId"] == inviteId) {
                    invite.toMutableMap().apply {
                        put("status", "accepted")
                    }
                } else {
                    invite
                }
            }
            // Update invites in user profile
            firestore.collection("users").document(userId)
                .update("groupInvites", updatedInvites)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to accept invite $inviteId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reject a group invite
     * Used if a user does not want to join the group andf wants to remove the invite
     */
    suspend fun rejectGroupInvite(inviteId: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val invites = profile?.get("groupInvites") as? List<Map<String, Any>> ?: emptyList()
            // Remove the invite
            val updatedInvites = invites.filter { it["inviteId"] != inviteId }
            // Update invites in user profile
            firestore.collection("users").document(userId)
                .update("groupInvites", updatedInvites)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject invite $inviteId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an invite
     * Used for cleanup so that accepted/rejected invites do not clutter the user's profile
     */
    suspend fun deleteInvite(inviteId: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val invites = profile?.get("groupInvites") as? List<Map<String, Any>> ?: emptyList()
            val updatedInvites = invites.filter { it["inviteId"] != inviteId }
            firestore.collection("users").document(userId)
                .update("groupInvites", updatedInvites)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete invite $inviteId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Used to update profile visibility setting
     * Possible values: "everyone", "friends", "only me"
     * Returns Result<Unit> indicating success or failure
     */
    suspend fun updateProfileVisibility(visibility: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
            firestore.collection("users")
                .document(userId)
                .update("privacy.profileVisibility", visibility)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile visibility: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get profile visibility setting
     * Returns the visibility setting as a String
     * Defaults to "everyone" if not set or on error
     */
    suspend fun getProfileVisibility(): String {
        return try {
            val userId = currentUser?.uid ?: return "everyone"
            val doc = firestore.collection("users").document(userId).get().await()
            val privacy = doc.get("privacy") as? Map<*, *>
            (privacy?.get("profileVisibility") as? String) ?: "everyone"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile visibility: ${e.message}", e)
            "everyone"
        }
    }

    /**
     * Update notifications enabled/disabled setting
     * Returns Result<Unit> indicating success or failure
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
            firestore.collection("users")
                .document(userId)
                .update("notifications.enabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notifications preference: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get notifications enabled/disabled setting
     * Returns true if enabled, false if disabled
     * Defaults to true if not set or on error
     */
    suspend fun getNotificationsEnabled(): Boolean {
        return try {
            val userId = currentUser?.uid ?: return true
            val doc = firestore.collection("users").document(userId).get().await()
            val notifications = doc.get("notifications") as? Map<*, *>
            (notifications?.get("enabled") as? Boolean) ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications preference: ${e.message}", e)
            true
        }
    }

    /**
     * Add a recently opened PDF to user's profile
     * Maintains a list of last 5 PDFs with most recent first
     */
    suspend fun addNoteToUserLibrary(
        noteId: String,
        fileName: String,
        fileUrl: String,
        subject: String,
        courseId: String
    ): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            // Get existing library
            val userLibrary = (profile?.get("userLibrary") as? List<Map<String, Any>>) ?: emptyList()
            // Create new note entry
            val noteEntry = mapOf(
                "noteId" to noteId,
                "fileName" to fileName,
                "fileUrl" to fileUrl,
                "subject" to subject,
                "courseId" to courseId,
                "addedAt" to System.currentTimeMillis()
            )
            // Add new entry to the library
            val updatedLibrary = listOf(noteEntry) + userLibrary
            firestore.collection("users").document(userId)
                .update("userLibrary", updatedLibrary)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add note $noteId to library: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get recently opened PDFs
     */
    suspend fun getUserLibrary(): List<Map<String, Any>> {
        return try {
            val profile = getUserProfile()
            (profile?.get("userLibrary") as? List<Map<String, Any>>) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user library: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Initialize user profile with sample PDFs (for testing)
     */
    suspend fun initializeSampleUserLibrary(): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val sampleLibrary = listOf(
                mapOf(
                    "noteId" to "sampleNote1",
                    "fileName" to "Introduction to Biology.pdf",
                    "fileUrl" to "https://example.com/biology_intro.pdf",
                    "subject" to "Biology",
                    "courseId" to "BIO101",
                    "addedAt" to System.currentTimeMillis() - 3600000 // 1 hour ago
                ),
                mapOf(
                    "noteId" to "sampleNote2",
                    "fileName" to "Calculus Fundamentals.pdf",
                    "fileUrl" to "https://example.com/calculus_basics.pdf",
                    "subject" to "Mathematics",
                    "courseId" to "MATH101",
                    "addedAt" to System.currentTimeMillis() - 7200000 // 2 hours ago
                )
            )
            firestore.collection("users").document(userId)
                .update("userLibrary", sampleLibrary)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sample library: ${e.message}", e)
            Result.failure(e)
        }
    }
}