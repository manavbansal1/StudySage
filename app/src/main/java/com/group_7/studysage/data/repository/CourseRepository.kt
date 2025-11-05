package com.group_7.studysage.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository class for managing Course data in Firebase Firestore.
 * It provides methods to create, update, delete, and fetch courses,
 * as well as fetch courses with their associated notes.
 * It also includes methods to archive courses and get available years.
 * It has 2 dependencies: FirebaseFirestore and FirebaseAuth.
 * It uses Kotlin coroutines for asynchronous operations.
 * this is done to ensure that the UI remains responsive during data operations.
 *
 */
class CourseRepository {

    companion object {
        private const val TAG = "CourseRepository"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Creates a new course in Firestore.
     * It takes in a courese object and returns a Result wrapping the created course with its generated ID.
     * @param course The course object to be created.
     * @return Result<Course> The result of the operation, containing the created course or an
     * exception if the operation failed.
     */
    suspend fun createCourse(course: Course): Result<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")

            val courseId = firestore.collection("courses").document().id
            val courseWithId = course.copy(id = courseId, userId = userId)

            firestore.collection("courses")
                .document(courseId)
                .set(courseWithId)
                .await()

            Result.success(courseWithId)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating course: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing course in Firestore.
     * It takes in a course object and returns a Result wrapping the updated course.
     * @param course The course object to be updated.
     * @return Result<Course> The result of the operation, containing the updated course or an
     * exception if the operation failed.
     */
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
            Log.e(TAG, "Error updating course: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a course from Firestore.
     * It takes in a course ID and returns a Result indicating success or failure.
     * @param courseId The ID of the course to be deleted.
     * @return Result<Unit> The result of the operation, indicating success or an exception if the operation failed.
     */
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
            Log.e(TAG, "Error deleting course: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all courses for the currently authenticated user.
     * @return List<Course> A list of courses belonging to the user.
     * @throws Exception if there is an error during the fetch operation.
     * Its used to display the user's courses in the app.
     */
    suspend fun getUserCourses(): List<Course> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("courses")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Course::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing course: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user courses: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches a course by its ID.
     * @param courseId The ID of the course to fetch.
     * @return Course? The course object if found, or null if not found or an error occurs.
     * It is used to display course details in the app.
     */
    suspend fun getCourseById(courseId: String): Course? {
        return try {
            val document = firestore.collection("courses").document(courseId).get().await()
            document.toObject(Course::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching course: ${e.message}", e)
            null
        }
    }

    /**
     * NOT USED RN
     * :TODO
     */
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
                    Log.e(TAG, "Error parsing course: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching courses by semester: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches a course along with its associated notes.
     * @param courseId The ID of the course to fetch.
     * @return CourseWithNotes? The course with its notes if found, or null if not found or an error occurs.
     * It is used to display course details along with notes in the app.
     */
    suspend fun getCourseWithNotes(courseId: String): CourseWithNotes? {
        return try {
            val course = getCourseById(courseId) ?: return null
            val notes = getNotesForCourse(courseId)
            CourseWithNotes(course, notes as java.util.List<Note>)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching course with notes: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches notes associated with a specific course.
     * @param courseId The ID of the course whose notes are to be fetched.
     * @return List<Note> A list of notes associated with the course.
     * It is used internally to get notes for a course.
     * It is used to display notes under a specific course in the app.
     * It also has an option to order notes by creation date in descending order.
     */
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
                    Log.e(TAG, "Error parsing note: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notes for course: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Archives a course by setting its isArchived field to true.
     * @param courseId The ID of the course to be archived.
     * @return Result<Unit> The result of the operation, indicating success or an exception if the operation failed.
     * It is used to hide courses from the main view without deleting them.
     */
    suspend fun archiveCourse(courseId: String): Result<Unit> {
        return try {
            val course = getCourseById(courseId) ?: throw Exception("Course not found")
            val archivedCourse = course.copy(isArchived = true, updatedAt = System.currentTimeMillis())
            updateCourse(archivedCourse)
            Log.d(TAG, "Course archived id=$courseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error archiving course: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a list of distinct years for which the user has courses.
     * @return List<String> A list of years as strings.
     * If no years are found, it returns the current year as a default.
     * It is used to populate year filters in the UI.
     */
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
            Log.e(TAG, "Error fetching available years: ${e.message}", e)
            listOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString())
        }
    }
}
