package com.group_7.studysage.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.data.model.DailyTaskItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing daily tasks in Firestore
 * Handles fetching, generating, and completing daily tasks
 * Tasks are stored under:
 * users/{userId}/dailyTasks/{date}
 * where {date} is in "yyyy-MM-dd" format
 * Each dailyTasks document contains a "tasks" array of task items
 * Each task item has:
 * - id: String
 * - title: String
 * - description: String
 * - xpReward: Int
 * - isCompleted: Boolean
 * - completedAt: Timestamp?
 * - taskType: String   
 * 
 * 
 * 
 */
class TasksRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val authRepository = AuthRepository(auth, firestore)

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    /**
     * Get the current date as a formatted string
     */
    private fun getCurrentDateString(): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Get today's tasks as a Flow from Firestore path users/{userId}/dailyTasks/{today's date}
     */
    fun getTodaysTasks(): Flow<List<DailyTaskItem>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            android.util.Log.e("TasksRepository", "getTodaysTasks: User not authenticated")
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        val today = getCurrentDateString()
        android.util.Log.d("TasksRepository", "Setting up listener for tasks on date: $today")

        val documentRef = firestore
            .collection("users")
            .document(userId)
            .collection("dailyTasks")
            .document(today)

        val listener = documentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("TasksRepository", "Error in tasks listener: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                android.util.Log.d("TasksRepository", "Tasks document exists")
                val tasks = snapshot.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                android.util.Log.d("TasksRepository", "Found ${tasks.size} tasks in document")

                val taskItems = tasks.mapNotNull { taskMap ->
                    try {
                        DailyTaskItem(
                            id = taskMap["id"] as? String ?: "",
                            title = taskMap["title"] as? String ?: "",
                            description = taskMap["description"] as? String ?: "",
                            xpReward = (taskMap["xpReward"] as? Long)?.toInt() ?: 0,
                            isCompleted = taskMap["isCompleted"] as? Boolean ?: false,
                            completedAt = taskMap["completedAt"] as? Timestamp,
                            taskType = taskMap["taskType"] as? String ?: "",
                            createdAt = taskMap["createdAt"] as? Timestamp ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("TasksRepository", "Error parsing task: ${e.message}", e)
                        null
                    }
                }
                android.util.Log.d("TasksRepository", "Sending ${taskItems.size} parsed tasks to flow")
                trySend(taskItems)
            } else {
                android.util.Log.w("TasksRepository", "Tasks document does not exist for $today")
                trySend(emptyList())
            }
        }

        awaitClose {
            android.util.Log.d("TasksRepository", "Removing tasks listener")
            listener.remove()
        }
    }

    /**
     * Generate 3 default daily tasks and save them to Firestore
     */
    suspend fun generateDailyTasks(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                android.util.Log.e("TasksRepository", "Cannot generate tasks: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }

            val today = getCurrentDateString()
            android.util.Log.d("TasksRepository", "Generating daily tasks for $today")

            val tasks = listOf(
                mapOf(
                    "id" to "task_1_$today",
                    "title" to "Complete Quiz",
                    "description" to "Complete a quiz",
                    "xpReward" to 50,
                    "isCompleted" to false,
                    "completedAt" to null,
                    "taskType" to "quiz",
                    "createdAt" to Timestamp.now()
                ),
                mapOf(
                    "id" to "task_2_$today",
                    "title" to "Study 30min",
                    "description" to "Study for 30 minutes",
                    "xpReward" to 50,
                    "isCompleted" to false,
                    "completedAt" to null,
                    "taskType" to "study",
                    "createdAt" to Timestamp.now()
                ),
                mapOf(
                    "id" to "task_3_$today",
                    "title" to "Review Flashcards",
                    "description" to "Review flashcards",
                    "xpReward" to 50,
                    "isCompleted" to false,
                    "completedAt" to null,
                    "taskType" to "flashcards",
                    "createdAt" to Timestamp.now()
                )
            )

            try {
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("dailyTasks")
                    .document(today)
                    .set(mapOf("tasks" to tasks))
                    .await()

                android.util.Log.d("TasksRepository", "✅ Successfully generated ${tasks.size} daily tasks")
                Result.success(Unit)
            } catch (firestoreException: Exception) {
                android.util.Log.e("TasksRepository", "Firestore error while generating tasks: ${firestoreException.message}", firestoreException)
                Result.failure(Exception("Failed to save tasks to database: ${firestoreException.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("TasksRepository", "Unexpected error in generateDailyTasks: ${e.message}", e)
            Result.failure(Exception("Failed to generate daily tasks: ${e.message}"))
        }
    }

    /**
     * Complete a task: mark as complete, set timestamp, and award XP
     */
    suspend fun completeTask(taskId: String, xpAmount: Int): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                android.util.Log.e("TasksRepository", "Cannot complete task: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }

            val today = getCurrentDateString()
            android.util.Log.d("TasksRepository", "Completing task $taskId with $xpAmount XP")

            val documentRef = firestore
                .collection("users")
                .document(userId)
                .collection("dailyTasks")
                .document(today)

            try {
                val snapshot = documentRef.get().await()

                if (!snapshot.exists()) {
                    android.util.Log.e("TasksRepository", "No tasks document found for today")
                    return Result.failure(Exception("No tasks found for today"))
                }

                val tasks = snapshot.get("tasks") as? List<Map<String, Any>>
                if (tasks == null) {
                    android.util.Log.e("TasksRepository", "Tasks field is null or invalid")
                    return Result.failure(Exception("Invalid tasks data"))
                }

                // Check if task exists
                val taskExists = tasks.any { it["id"] == taskId }
                if (!taskExists) {
                    android.util.Log.e("TasksRepository", "Task $taskId not found")
                    return Result.failure(Exception("Task not found"))
                }

                // Check if task is already completed
                val task = tasks.firstOrNull { it["id"] == taskId }
                if (task?.get("isCompleted") == true) {
                    android.util.Log.w("TasksRepository", "Task $taskId is already completed")
                    return Result.success(Unit) // Return success since task is already completed
                }

                val updatedTasks = tasks.map { task ->
                    if (task["id"] == taskId) {
                        task.toMutableMap().apply {
                            put("isCompleted", true)
                            put("completedAt", Timestamp.now())
                        }
                    } else {
                        task
                    }
                }

                documentRef.update("tasks", updatedTasks).await()
                android.util.Log.d("TasksRepository", "✅ Task $taskId marked as completed in Firestore")

                // Award XP to user
                try {
                    val xpResult = authRepository.awardXP(xpAmount)
                    if (xpResult.isSuccess) {
                        android.util.Log.d("TasksRepository", "✅ Awarded $xpAmount XP to user")
                    } else {
                        android.util.Log.e("TasksRepository", "Failed to award XP: ${xpResult.exceptionOrNull()?.message}")
                        // Don't fail the whole operation if XP award fails
                    }
                } catch (xpException: Exception) {
                    android.util.Log.e("TasksRepository", "Error awarding XP: ${xpException.message}", xpException)
                    // Don't fail the whole operation if XP award fails
                }

                Result.success(Unit)
            } catch (firestoreException: Exception) {
                android.util.Log.e("TasksRepository", "Firestore error while completing task: ${firestoreException.message}", firestoreException)
                Result.failure(Exception("Failed to update task in database: ${firestoreException.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("TasksRepository", "Unexpected error in completeTask: ${e.message}", e)
            Result.failure(Exception("Failed to complete task: ${e.message}"))
        }
    }

    /**
     * Check if tasks exist for today, if not generate them
     */
    suspend fun checkAndGenerateTasksForToday(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val today = getCurrentDateString()

            val document = firestore
                .collection("users")
                .document(userId)
                .collection("dailyTasks")
                .document(today)
                .get()
                .await()

            if (!document.exists()) {
                generateDailyTasks()
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

