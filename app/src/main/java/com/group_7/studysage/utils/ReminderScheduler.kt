package com.group_7.studysage.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.group_7.studysage.workers.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * ReminderScheduler
 *
 * Utility class to schedule and manage daily study reminder notifications
 * using WorkManager.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val DAILY_REMINDER_WORK_NAME = "daily_study_reminder"

    /**
     * Schedule a daily study reminder.
     * The reminder will repeat every 24 hours at the specified time.
     *
     * @param context Application context
     * @param timeString Time in HH:mm format (e.g., "09:00", "18:30")
     */
    fun scheduleDailyReminder(context: Context, timeString: String = "18:00") {
        try {
            Log.d(TAG, "Scheduling daily reminder for $timeString")

            // Calculate delay until next reminder time
            val delay = calculateInitialDelay(timeString)

            // Create constraints - only run when device is not in low battery mode
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Create PeriodicWorkRequest that runs every 24 hours
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag("study_reminder")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            // Enqueue work - replace existing if any
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
            )

            Log.d(TAG, "Daily reminder scheduled for $timeString")
            Log.d(TAG, "Initial delay: ${delay / 1000 / 60} minutes")

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily reminder", e)
        }
    }

    /**
     * Schedule a daily study reminder using hour of day.
     * Convenience method for backward compatibility.
     *
     * @param context Application context
     * @param hourOfDay Hour to send reminder (0-23)
     */
    fun scheduleDailyReminder(context: Context, hourOfDay: Int) {
        val timeString = String.format(java.util.Locale.US, "%02d:00", hourOfDay)
        scheduleDailyReminder(context, timeString)
    }

    /**
     * Cancel the daily study reminder.
     *
     * @param context Application context
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_REMINDER_WORK_NAME)
        Log.d(TAG, "Daily reminder cancelled")
    }

    /**
     * Calculate the initial delay until the next occurrence of the target time.
     *
     * This method parses a time string in HH:mm format and calculates how many
     * milliseconds from now until that time occurs. If the target time has already
     * passed today, it schedules for tomorrow.
     *
     * @param timeString Time in HH:mm format (e.g., "09:00", "18:30")
     * @return Delay in milliseconds until the target time
     *
     * Example:
     * - Current time: 15:30
     * - Target time: "18:00"
     * - Returns: delay for 2 hours 30 minutes from now
     *
     * - Current time: 20:00
     * - Target time: "18:00"
     * - Returns: delay for 22 hours (tomorrow at 18:00)
     */
    private fun calculateInitialDelay(timeString: String): Long {
        // Parse time string "HH:mm" into hour and minute components
        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        // Get current time in milliseconds since epoch
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set target time for today
        // We keep the current date but change the time to our target
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Check if target time has already passed today
        // If so, schedule for the same time tomorrow
        if (calendar.timeInMillis <= now) {
            // Add one day to schedule for tomorrow
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Calculate the delay: target time - current time
        // This gives us the number of milliseconds until the next occurrence
        return calendar.timeInMillis - now
    }

    /**
     * Check if the daily reminder is currently scheduled.
     *
     * @param context Application context
     * @return true if scheduled, false otherwise
     */
    fun isReminderScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(DAILY_REMINDER_WORK_NAME)
                .get()

            workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reminder status", e)
            false
        }
    }

    /**
     * Schedule or cancel the reminder based on user preference.
     *
     * @param context Application context
     * @param enabled Whether reminders should be enabled
     * @param hourOfDay Hour to send reminder (0-23, default 9 for 9 AM)
     */
    fun updateReminderSchedule(context: Context, enabled: Boolean, hourOfDay: Int = 9) {
        if (enabled) {
            scheduleDailyReminder(context, hourOfDay)
        } else {
            cancelDailyReminder(context)
        }
    }
}

