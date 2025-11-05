package com.group_7.studysage.utils

import kotlinx.coroutines.tasks.await

class ExtraFunctions {
    /**

    suspend fun updateUserAuthProfile(name: String, photoUrl: String): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("No user logged in"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(android.net.Uri.parse(photoUrl))
                .build()
            user.updateProfile(profileUpdates).await()
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


    /**
     * Get count of pending invites
    */
    suspend fun getPendingInviteCount(): Int {
    return try {
    val invites = getPendingInvites()
    invites.size
    } catch (e: Exception) {
    0
    }
    }

    /**
     * Update PDF progress
     * Used to track last opened page and progress percentage
     * Returns Result<Unit> indicating success or failure
     * Updates lastPage, progress (0.0 to 1.0), and openedAt timestamp
     * Finds the note by noteId in userLibrary and updates its fields
     * If noteId not found, no changes are made
    */
    suspend fun updateNoteProgress(noteId: String, lastPage: Int, pageCount: Int): Result<Unit> {
    return try {
    val userId = currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
    val profile = getUserProfile()
    val userLibrary = (profile?.get("userLibrary") as? List<Map<String, Any>>) ?: emptyList()

    val updatedLibrary = userLibrary.map { note ->
    if (note["noteId"] == noteId) {
    note.toMutableMap().apply {
    put("lastPage", lastPage)
    put("progress", lastPage.toFloat() / pageCount.toFloat())
    put("openedAt", System.currentTimeMillis())
    }
    } else {
    note
    }
    }

    firestore.collection("users").document(userId)
    .update("userLibrary", updatedLibrary)
    .await()

    Result.success(Unit)
    } catch (e: Exception) {
    Result.failure(e)
    }
    }

    */



}