package com.group_7.studysage.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.data.repository.TasksRepository

/**
 * Worker that runs daily at midnight to generate new daily tasks
 */
class DailyTaskResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyTaskResetWorker"
        const val WORK_NAME = "daily_task_reset"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily task reset...")

            // Initialize repositories
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val tasksRepository = TasksRepository(firestore, auth)

            // Check if user is authenticated
            if (auth.currentUser == null) {
                Log.w(TAG, "No authenticated user, skipping task generation")
                return Result.success()
            }

            // Generate tasks for today if they don't exist
            val result = tasksRepository.checkAndGenerateTasksForToday()

            if (result.isSuccess) {
                Log.d(TAG, "✅ Daily tasks generated successfully")
                Result.success()
            } else {
                Log.e(TAG, "❌ Failed to generate daily tasks: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in daily task reset worker: ${e.message}", e)
            Result.failure()
        }
    }
}

