package com.group_7.studysage.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
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
                "name" to name,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
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
}

