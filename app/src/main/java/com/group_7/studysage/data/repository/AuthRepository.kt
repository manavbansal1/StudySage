package com.group_7.studysage.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

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
                "recentlyOpenedPdfs" to listOf<Map<String, Any>>(), // List of recently opened PDFs
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
                "groups" to listOf<Map<String, Any>>() // List of group summaries
            )

            firestore.collection("users").document(user.uid).set(userProfile).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun isUserSignedIn(): Boolean {
        return currentUser != null
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        return try {
            val userId = currentUser?.uid ?: return null
            val document = firestore.collection("users").document(userId).get().await()
            document.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            firestore.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileImage(imageUrl: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            firestore.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStreak(streakDays: Int): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            firestore.collection("users").document(userId)
                .update("streakDays", streakDays)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addXP(points: Int): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val currentXP = (profile?.get("xpPoints") as? Long)?.toInt() ?: 0
            val newXP = currentXP + points

            // Simple level calculation: every 100 XP = 1 level
            val newLevel = (newXP / 100) + 1

            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "xpPoints" to newXP,
                        "level" to newLevel
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== GROUP MANAGEMENT ====================

    /**
     * Add a group summary to user's profile
     * Each user stores a lightweight summary of their groups for quick access
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
            Result.failure(e)
        }
    }

    /**
     * Update the most recent message in user's group summary
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
            Result.failure(e)
        }
    }

    /**
     * Remove a group from user's profile
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
            Result.failure(e)
        }
    }

    /**
     * Get all groups for current user
     */
    suspend fun getUserGroups(): List<Map<String, Any>> {
        return try {
            val profile = getUserProfile()
            (profile?.get("groups") as? List<Map<String, Any>>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search for user by email
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
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== PDF MANAGEMENT ====================

    /**
     * Add a recently opened PDF to user's profile
     * Maintains a list of last 5 PDFs with most recent first
     */
    suspend fun addRecentlyOpenedPdf(
        pdfName: String,
        pdfUrl: String,
        subject: String,
        pageCount: Int,
        lastPage: Int = 0
    ): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()

            // Get existing PDFs list
            val recentPdfs = (profile?.get("recentlyOpenedPdfs") as? List<Map<String, Any>>) ?: emptyList()

            // Create new PDF entry
            val pdfEntry = mapOf(
                "pdfName" to pdfName,
                "pdfUrl" to pdfUrl,
                "subject" to subject,
                "pageCount" to pageCount,
                "lastPage" to lastPage,
                "openedAt" to System.currentTimeMillis(),
                "progress" to (lastPage.toFloat() / pageCount.toFloat())
            )

            // Remove if already exists (to update timestamp)
            val filteredPdfs = recentPdfs.filter { it["pdfUrl"] != pdfUrl }

            // Add new entry at start and keep only last 5
            val updatedPdfs = listOf(pdfEntry) + filteredPdfs.take(4)

            firestore.collection("users").document(userId)
                .update("recentlyOpenedPdfs", updatedPdfs)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recently opened PDFs
     */
    suspend fun getRecentlyOpenedPdfs(): List<Map<String, Any>> {
        return try {
            val profile = getUserProfile()
            (profile?.get("recentlyOpenedPdfs") as? List<Map<String, Any>>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update PDF progress (last page read)
     */
    suspend fun updatePdfProgress(pdfUrl: String, lastPage: Int, pageCount: Int): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val profile = getUserProfile()
            val recentPdfs = (profile?.get("recentlyOpenedPdfs") as? List<Map<String, Any>>) ?: emptyList()

            val updatedPdfs = recentPdfs.map { pdf ->
                if (pdf["pdfUrl"] == pdfUrl) {
                    pdf.toMutableMap().apply {
                        put("lastPage", lastPage)
                        put("progress", lastPage.toFloat() / pageCount.toFloat())
                        put("openedAt", System.currentTimeMillis())
                    }
                } else {
                    pdf
                }
            }

            firestore.collection("users").document(userId)
                .update("recentlyOpenedPdfs", updatedPdfs)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize user profile with sample PDFs (for testing)
     */
    suspend fun initializeSamplePdfs(): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))

            val samplePdfs = listOf(
                mapOf(
                    "pdfName" to "Introduction to Biology",
                    "pdfUrl" to "https://example.com/biology_intro.pdf",
                    "subject" to "Biology",
                    "pageCount" to 45,
                    "lastPage" to 28,
                    "openedAt" to System.currentTimeMillis() - 3600000, // 1 hour ago
                    "progress" to 0.62f
                ),
                mapOf(
                    "pdfName" to "Calculus Fundamentals",
                    "pdfUrl" to "https://example.com/calculus_basics.pdf",
                    "subject" to "Mathematics",
                    "pageCount" to 120,
                    "lastPage" to 45,
                    "openedAt" to System.currentTimeMillis() - 7200000, // 2 hours ago
                    "progress" to 0.38f
                ),
                mapOf(
                    "pdfName" to "World History Notes",
                    "pdfUrl" to "https://example.com/history_notes.pdf",
                    "subject" to "History",
                    "pageCount" to 80,
                    "lastPage" to 80,
                    "openedAt" to System.currentTimeMillis() - 86400000, // 1 day ago
                    "progress" to 1.0f
                ),
                mapOf(
                    "pdfName" to "Chemistry Lab Manual",
                    "pdfUrl" to "https://example.com/chem_lab.pdf",
                    "subject" to "Chemistry",
                    "pageCount" to 60,
                    "lastPage" to 15,
                    "openedAt" to System.currentTimeMillis() - 172800000, // 2 days ago
                    "progress" to 0.25f
                ),
                mapOf(
                    "pdfName" to "Physics Problem Sets",
                    "pdfUrl" to "https://example.com/physics_problems.pdf",
                    "subject" to "Physics",
                    "pageCount" to 95,
                    "lastPage" to 10,
                    "openedAt" to System.currentTimeMillis() - 259200000, // 3 days ago
                    "progress" to 0.11f
                )
            )

            firestore.collection("users").document(userId)
                .update("recentlyOpenedPdfs", samplePdfs)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}