package com.group_7.studysage.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.group_7.studysage.MainActivity
import com.group_7.studysage.R

/**
 * StudySageNotificationManager
 *
 * A centralized notification manager for StudySage app that handles:
 * - Study reminders
 * - Learning progress updates
 *
 * Automatically creates notification channels for Android O+ and manages
 * notification delivery with proper error handling and logging.
 */
object StudySageNotificationManager {

    private const val TAG = "StudySageNotificationManager"

    // Notification Channel IDs
    private const val CHANNEL_STUDY_REMINDERS = "study_reminders"
    private const val CHANNEL_LEARNING_PROGRESS = "learning_progress"

    // Channel Names
    private const val CHANNEL_NAME_STUDY = "Study Reminders"
    private const val CHANNEL_NAME_PROGRESS = "Learning Progress"

    // Channel Descriptions
    private const val CHANNEL_DESC_STUDY = "Notifications for study reminders and schedules"
    private const val CHANNEL_DESC_PROGRESS = "Updates on your learning progress and achievements"

    /**
     * Initialize notification channels.
     * Call this when the app starts or when needed.
     */
    fun init(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                if (notificationManager == null) {
                    Log.e(TAG, "NotificationManager is null. Cannot create channels.")
                    return
                }

                // Create Study Reminders Channel
                val studyChannel = NotificationChannel(
                    CHANNEL_STUDY_REMINDERS,
                    CHANNEL_NAME_STUDY,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC_STUDY
                    enableLights(true)
                    enableVibration(true)
                }

                // Create Learning Progress Channel
                val progressChannel = NotificationChannel(
                    CHANNEL_LEARNING_PROGRESS,
                    CHANNEL_NAME_PROGRESS,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_DESC_PROGRESS
                    enableLights(false)
                    enableVibration(false)
                }

                // Register all channels
                notificationManager.createNotificationChannel(studyChannel)
                notificationManager.createNotificationChannel(progressChannel)

                Log.d(TAG, "Notification channels created successfully")
            } else {
                Log.d(TAG, "Android version < O, notification channels not required")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channels", e)
        }
    }

    /**
     * Check if the app has notification permission (Android 13+).
     * For older versions, checks if notifications are enabled.
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Show a study reminder notification.
     *
     * @param context Application or Activity context
     * @param title The title of the reminder
     * @param message The reminder message
     */
    @SuppressLint("MissingPermission") // Permission is checked before calling notify()
    fun showStudyReminder(context: Context, title: String, message: String) {
        try {
            Log.d(TAG, "Showing study reminder: $title")

            // Check notification permission
            if (!hasNotificationPermission(context)) {
                Log.w(TAG, "Notification permission not granted. Cannot show notification.")
                return
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null. Cannot show notification.")
                return
            }

            // Create intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // Generate unique notification ID
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Study reminder notification shown successfully with ID: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing study reminder notification", e)
        }
    }

    /**
     * Show a note uploaded notification.
     *
     * @param context Application or Activity context
     * @param noteTitle The title of the uploaded note
     * @param courseTitle The course the note belongs to
     */
    @SuppressLint("MissingPermission") // Permission is checked before calling notify()
    fun showNoteUploaded(context: Context, noteTitle: String, courseTitle: String) {
        try {
            Log.d(TAG, "Showing note uploaded notification: $noteTitle")

            // Check notification permission
            if (!hasNotificationPermission(context)) {
                Log.w(TAG, "Notification permission not granted. Cannot show notification.")
                return
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null. Cannot show notification.")
                return
            }

            // Create intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("openCourse", courseTitle)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_LEARNING_PROGRESS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Note Uploaded Successfully")
                .setContentText("$noteTitle has been added to $courseTitle")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Your note \"$noteTitle\" has been successfully uploaded to $courseTitle")
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .build()

            // Generate unique notification ID
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Note uploaded notification shown successfully with ID: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing note uploaded notification", e)
        }
    }
}