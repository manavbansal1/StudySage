package com.group_7.studysage.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * NotificationHelper
 *
 * Utility object for managing notification-related operations.
 * Provides methods to check app state, format messages, and determine if notifications should be sent.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    /**
     * Check if the app is currently in the background.
     *
     * This method queries the ActivityManager to determine if the app's process
     * is in the foreground or background.
     *
     * @param context Application or Activity context
     * @return true if app is in background, false if in foreground
     */
    fun isAppInBackground(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

            if (activityManager == null) {
                Log.w(TAG, "ActivityManager is null, assuming app is in background")
                return true
            }

            val appProcesses = activityManager.runningAppProcesses

            if (appProcesses == null) {
                Log.w(TAG, "No running app processes found, assuming app is in background")
                return true
            }

            val packageName = context.packageName

            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName == packageName
                ) {
                    Log.d(TAG, "App is in foreground")
                    return false // App is in foreground
                }
            }

            Log.d(TAG, "App is in background")
            return true // App is in background

        } catch (e: Exception) {
            Log.e(TAG, "Error checking app background state", e)
            return true // On error, assume background to avoid spamming notifications
        }
    }

    /**
     * Truncate a message to a maximum length.
     * Useful for notification message previews.
     *
     * @param message The message to truncate
     * @param maxLength Maximum length of the message (default: 100)
     * @return Truncated message with "..." appended if truncated
     */
    fun truncateMessage(message: String, maxLength: Int = 100): String {
        return try {
            if (maxLength <= 0) {
                Log.w(TAG, "Invalid maxLength: $maxLength, returning empty string")
                return ""
            }

            if (message.length > maxLength) {
                val truncated = message.take(maxLength) + "..."
                Log.d(TAG, "Message truncated from ${message.length} to ${truncated.length} characters")
                truncated
            } else {
                message
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error truncating message", e)
            message // Return original message on error
        }
    }

    /**
     * Determine if a notification should be sent to the user.
     *
     * Checks two conditions:
     * 1. App is in background (don't send notifications if user is actively using the app)
     * 2. User has notifications enabled in Firestore settings
     *
     * @param context Application or Activity context
     * @param userId Firebase user ID
     * @return true if notification should be sent, false otherwise
     */
    suspend fun shouldNotifyUser(context: Context, userId: String): Boolean {
        return try {
            Log.d(TAG, "Checking if should notify user: $userId")

            // Check if app is in background
            if (!isAppInBackground(context)) {
                Log.d(TAG, "App is in foreground, skipping notification")
                return false
            }

            // Check if user has notifications enabled
            val notificationsEnabled = try {
                val firestore = FirebaseFirestore.getInstance()
                val doc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val enabled = doc.getBoolean("settings.notificationsEnabled") ?: false
                Log.d(TAG, "User notifications enabled: $enabled")
                enabled

            } catch (e: Exception) {
                Log.e(TAG, "Error checking notification settings for user $userId", e)
                false // Default to not sending on error
            }

            notificationsEnabled

        } catch (e: Exception) {
            Log.e(TAG, "Error in shouldNotifyUser", e)
            false // Default to not sending on error
        }
    }

    /**
     * Check if user has notifications enabled in Firestore.
     * Helper method that only checks the Firestore setting without checking app state.
     *
     * @param userId Firebase user ID
     * @return true if notifications are enabled, false otherwise
     */
    suspend fun areNotificationsEnabled(userId: String): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            doc.getBoolean("settings.notificationsEnabled") ?: false

        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification settings for user $userId", e)
            false
        }
    }

    /**
     * Format a timestamp to a human-readable relative time string.
     * Useful for notification time display.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted string like "2 minutes ago", "1 hour ago", etc.
     */
    fun formatRelativeTime(timestamp: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            when {
                diff < 0 -> "just now"
                diff < 60_000 -> "just now"
                diff < 3600_000 -> "${diff / 60_000} minutes ago"
                diff < 86400_000 -> "${diff / 3600_000} hours ago"
                diff < 604800_000 -> "${diff / 86400_000} days ago"
                else -> "${diff / 604800_000} weeks ago"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting relative time", e)
            "recently"
        }
    }
}

