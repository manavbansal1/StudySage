package com.group_7.studysage.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.utils.StudySageNotificationManager
import kotlinx.coroutines.tasks.await

/**
 * DailyReminderWorker
 *
 * A WorkManager worker that sends daily study reminders to users.
 * Checks user preferences in Firestore before sending notifications.
 *
 * This worker:
 * 1. Verifies the user is authenticated
 * 2. Checks if notifications are enabled in user settings
 * 3. Sends a study reminder notification if enabled
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyReminderWorker"
        const val WORK_NAME = "daily_study_reminder"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "DailyReminderWorker started")

            // 1. Get current user ID from FirebaseAuth
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Log.w(TAG, "No user logged in, skipping reminder")
                return Result.success()
            }

            Log.d(TAG, "Checking notification settings for user: $userId")

            // 2. Check if notifications are enabled in Firestore
            val notificationsEnabled = checkNotificationSettings(userId)

            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled for user, skipping reminder")
                return Result.success()
            }

            Log.d(TAG, "Notifications enabled, sending reminder")

            // 3. Send notification
            StudySageNotificationManager.showStudyReminder(
                applicationContext,
                "Time to Study! 📚",
                "Review your notes and keep learning!"
            )

            Log.d(TAG, "Daily reminder sent successfully")

            // 4. Return success
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyReminderWorker", e)

            // Retry on failure (WorkManager will automatically handle retry with backoff)
            Result.retry()
        }
    }

    /**
     * Check if notifications are enabled in Firestore for the given user.
     *
     * @param userId The Firebase user ID
     * @return true if notifications are enabled, false otherwise
     */
    private suspend fun checkNotificationSettings(userId: String): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()

            // Query: users/{userId} document
            val userDoc = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.d(TAG, "User document not found, defaulting to disabled")
                // Default to disabled if user doesn't exist
                return false
            }

            // Get settings.notificationsEnabled field (default to false if not present)
            val notificationsEnabled = userDoc.getBoolean("settings.notificationsEnabled") ?: false

            Log.d(TAG, "Notifications enabled: $notificationsEnabled")

            notificationsEnabled

        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification settings", e)
            // On error, default to not sending (to be safe)
            false
        }
    }
}

