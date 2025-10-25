package com.group_7.studysage.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CourseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createCourse(course: Course): Result<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")

            val courseId = firestore.collection("courses").document().id
            val courseWithId = course.copy(id = courseId, userId = userId)

            firestore.collection("courses")
                .document(courseId)
                .set(courseWithId)
                .await()

            Log.d("CourseRepository", "Course created successfully with ID: $courseId")
            Result.success(courseWithId)

        } catch (e: Exception) {
            Log.e("CourseRepository", "Error creating course: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateCourse(course: Course): Result<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")

            if (course.userId != userId) {
                throw Exception("You can only update your own courses.")
            }

            val updatedCourse = course.copy(updatedAt = System.currentTimeMillis())

            firestore.collection("courses")
                .document(course.id)
                .set(updatedCourse)
                .await()

            Result.success(updatedCourse)

        } catch (e: Exception) {
            Log.e("CourseRepository", "Error updating course: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")

            // Check if course belongs to user
            val course = getCourseById(courseId)
            if (course?.userId != userId) {
                throw Exception("You can only delete your own courses.")
            }

            firestore.collection("courses").document(courseId).delete().await()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("CourseRepository", "Error deleting course: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserCourses(): List<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("courses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Course::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("CourseRepository", "Error parsing course: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching user courses: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCourseById(courseId: String): Course? {
        return try {
            val document = firestore.collection("courses").document(courseId).get().await()
            document.toObject(Course::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching course: ${e.message}")
            null
        }
    }

    suspend fun getCoursesBySemester(semester: String, year: String? = null): List<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            var query = firestore.collection("courses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("semester", semester)
                .whereEqualTo("isArchived", false)

            if (!year.isNullOrBlank()) {
                query = query.whereEqualTo("year", year)
            }

            val querySnapshot = query.orderBy("title").get().await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Course::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("CourseRepository", "Error parsing course: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching courses by semester: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCourseWithNotes(courseId: String): CourseWithNotes? {
        return try {
            val course = getCourseById(courseId) ?: return null
            val notes = getNotesForCourse(courseId)
            CourseWithNotes(course, notes as java.util.List<Note>)
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching course with notes: ${e.message}")
            null
        }
    }

    private suspend fun getNotesForCourse(courseId: String): List<Note> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Note::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("CourseRepository", "Error parsing note: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching notes for course: ${e.message}")
            emptyList()
        }
    }

    suspend fun archiveCourse(courseId: String): Result<Unit> {
        return try {
            val course = getCourseById(courseId) ?: throw Exception("Course not found")
            val archivedCourse = course.copy(isArchived = true, updatedAt = System.currentTimeMillis())
            updateCourse(archivedCourse)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error archiving course: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAvailableYears(): List<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("courses")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val years = querySnapshot.documents.mapNotNull { document ->
                document.getString("year")
            }.distinct().sorted()

            if (years.isEmpty()) {
                listOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString())
            } else {
                years
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching available years: ${e.message}")
            listOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString())
        }
    }
}