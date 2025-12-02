package com.group_7.studysage.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.data.api.CanvasApiService
import com.group_7.studysage.data.api.CanvasCourse
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Canvas LMS integration
 * Handles saving tokens, syncing courses, and fetching data from Firestore
 * 
 * - Save and retrieve Canvas access tokens
 * - Sync courses from Canvas to Firestore
 * - Validate Canvas tokens
 * - Disconnect Canvas and clean up data
 * - Fetch Canvas courses from Firestore
 * 
 * it requires a token to access the Canvas API
 *
 */


class CanvasRepository {
    
    companion object {
        private const val TAG = "CanvasRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_COURSES = "courses"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val canvasApi = CanvasApiService()
    
    /**
     * Save Canvas access token to Firestore
     */
    suspend fun saveCanvasToken(accessToken: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update("canvasAccessToken", accessToken)
                .await()
            
            Log.d(TAG, "Canvas token saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save Canvas token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get saved Canvas access token
     */
    suspend fun getCanvasToken(): Result<String?> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            val token = doc.getString("canvasAccessToken")
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Canvas token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch courses from Canvas and save to Firestore
     */
    suspend fun syncCanvasCourses(accessToken: String, semester: String = "Fall", year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()): Result<List<CanvasCourse>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Fetch courses from Canvas
            val coursesResult = canvasApi.getUserCourses(accessToken)
            
            coursesResult.onSuccess { courses ->
                Log.d(TAG, "Fetched ${courses.size} courses from Canvas")
                
                // Save each course to Firestore using the Course model structure
                val batch = firestore.batch()
                
                courses.forEach { course ->
                    val courseData = hashMapOf(
                        "id" to "${userId}_canvas_${course.id}",
                        "title" to course.name,
                        "code" to (course.course_code ?: ""),
                        "semester" to semester,
                        "year" to year,
                        "instructor" to "", // Canvas doesn't provide this in course list
                        "description" to "Imported from Canvas",
                        "color" to "#4CAF50", // Green color for Canvas courses
                        "credits" to 3, // Default credits
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis(),
                        "userId" to userId,
                        "isArchived" to false,
                        "source" to "canvas", // Extra field to identify Canvas courses
                        "canvasCourseId" to course.id
                    )
                    
                    val courseRef = firestore.collection(COLLECTION_COURSES)
                        .document("${userId}_canvas_${course.id}")
                    
                    batch.set(courseRef, courseData)
                }
                
                batch.commit().await()
                Log.d(TAG, "Saved ${courses.size} courses to Firestore")
                
                // Update user's canvas sync status
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .update(
                        mapOf(
                            "canvasLastSync" to System.currentTimeMillis(),
                            "canvasCoursesCount" to courses.size
                        )
                    )
                    .await()
            }
            
            coursesResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync Canvas courses", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get Canvas courses from Firestore
     */
    suspend fun getCanvasCourses(): Result<List<Map<String, Any>>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val snapshot = firestore.collection(COLLECTION_COURSES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("source", "canvas")
                .get()
                .await()
            
            val courses = snapshot.documents.mapNotNull { it.data }
            Log.d(TAG, "Retrieved ${courses.size} Canvas courses from Firestore")
            
            Result.success(courses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Canvas courses", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if Canvas token is valid
     */
    suspend fun validateCanvasToken(accessToken: String): Result<Boolean> {
        return canvasApi.validateToken(accessToken)
    }


fun getRandomColorHex(): String {
    val colors = listOf(
        "#F87171", // Red
        "#FBBF24", // Yellow
        "#34D399", // Green
        "#60A5FA", // Blue
        "#A78BFA", // Purple
        "#F472B6", // Pink
        "#F59E0B", // Amber
        "#10B981"  // Emerald
    )
    return colors.random()
}
    
    /**
     * Remove Canvas connection
     */
    suspend fun disconnectCanvas(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Remove token from user profile
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "canvasAccessToken" to null,
                        "canvasLastSync" to null,
                        "canvasCoursesCount" to 0
                    )
                )
                .await()
            
            // Delete Canvas courses
            val coursesToDelete = firestore.collection(COLLECTION_COURSES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("source", "canvas")
                .get()
                .await()
            
            val batch = firestore.batch()
            coursesToDelete.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Canvas disconnected successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect Canvas", e)
            Result.failure(e)
        }
    }

    /**
     * Sync selected courses to Firestore
     */
    suspend fun syncSelectedCourses(selectedCourses: List<CanvasCourse>, semester: String = "Fall", year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()): Result<List<CanvasCourse>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // First, delete existing Canvas courses to avoid duplicates
            val existingCourses = firestore.collection(COLLECTION_COURSES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("source", "canvas")
                .get()
                .await()
            
            val batch = firestore.batch()
            
            // Delete existing Canvas courses
            existingCourses.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Add selected courses to Firestore
            selectedCourses.forEach { course ->
                val courseData = hashMapOf(
                    "id" to "${userId}_canvas_${course.id}",
                    "title" to course.name,
                    "code" to (course.course_code ?: ""),
                    "semester" to semester,
                    "year" to year,
                    "instructor" to "", // Canvas doesn't provide this in course list
                    "description" to "Imported from Canvas",
                    "color" to getRandomColorHex(), // Random color for each course
                    "credits" to 3, // Default credits
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "userId" to userId,
                    "isArchived" to false,
                    "source" to "canvas", 
                    "canvasCourseId" to course.id
                )
                
                val courseRef = firestore.collection(COLLECTION_COURSES)
                    .document("${userId}_canvas_${course.id}")
                
                batch.set(courseRef, courseData)
            }
            
            batch.commit().await()
            Log.d(TAG, "Saved ${selectedCourses.size} selected courses to Firestore")
            
            // Update user's canvas sync status
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "canvasLastSync" to System.currentTimeMillis(),
                        "canvasCoursesCount" to selectedCourses.size
                    )
                )
                .await()
            
            Result.success(selectedCourses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync selected Canvas courses", e)
            Result.failure(e)
        }
    }

    suspend fun addToUserCoursesDatabase(course: CanvasCourse){
        try {
            val userId = auth.currentUser?.uid ?: return

            // Parse semester and year from current date
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            val defaultSemester = when (currentMonth) {
                0, 1, 2, 3 -> "Spring"
                4, 5, 6, 7 -> "Summer"
                8, 9, 10 -> "Fall"
                else -> "Winter"
            }

            val courseData = hashMapOf(
                "id" to "${userId}_canvas_${course.id}",
                "title" to course.name,
                "code" to (course.course_code ?: ""),
                "semester" to defaultSemester,
                "year" to currentYear,
                "instructor" to "",
                "description" to "Imported from Canvas",
                "color" to getRandomColorHex(),
                "credits" to 3,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "userId" to userId,
                "isArchived" to false,
                "source" to "canvas",
                "canvasCourseId" to course.id
            )

            val courseRef = firestore.collection(COLLECTION_COURSES)
                .document("${userId}_canvas_${course.id}")

            courseRef.set(courseData).await()
            Log.d(TAG, "Course ${course.name} added to user courses database")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add course ${course.name} to user courses database", e)
        }
    }
}
