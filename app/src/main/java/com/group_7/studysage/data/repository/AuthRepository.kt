package com.group_7.studysage.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
                )
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
}