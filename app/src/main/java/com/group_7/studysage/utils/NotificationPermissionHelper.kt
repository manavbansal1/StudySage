package com.group_7.studysage.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * NotificationPermissionHelper
 *
 * Utility functions to handle notification permissions for Android 13+ (TIRAMISU).
 * Provides helper methods to check permission status and determine if rationale should be shown.
 */
object NotificationPermissionHelper {

    /**
     * Check if notification permission is granted.
     *
     * For Android 13+ (API 33+), checks the POST_NOTIFICATIONS permission.
     * For older Android versions, returns true as notifications don't require runtime permission.
     *
     * @param context Application or Activity context
     * @return true if permission is granted or not required, false otherwise
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications don't require permission before Android 13
            true
        }
    }

    /**
     * Check if we should show the permission rationale to the user.
     *
     * This returns true if:
     * - We're on Android 13+ AND
     * - The user has previously denied the permission (but not permanently)
     *
     * Use this to decide whether to show an explanation dialog before requesting permission.
     *
     * @param activity The Activity context
     * @return true if rationale should be shown, false otherwise
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // No rationale needed for older Android versions
            false
        }
    }

    /**
     * Check if the permission can be requested.
     *
     * Returns false if:
     * - Permission is already granted OR
     * - User has permanently denied the permission (Don't ask again)
     *
     * @param activity The Activity context
     * @return true if permission can be requested, false otherwise
     */
    fun canRequestPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // No permission needed for older versions
            return false
        }

        val isGranted = isNotificationPermissionGranted(activity)
        if (isGranted) {
            // Already granted, no need to request
            return false
        }

        // Can request if either:
        // 1. First time asking (shouldShowRationale = false) OR
        // 2. User denied but didn't check "Don't ask again" (shouldShowRationale = true)
        return true
    }

    /**
     * Get the notification permission string for Android 13+.
     * This is a convenience method to get the permission constant.
     *
     * @return The POST_NOTIFICATIONS permission string or null for older versions
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    /**
     * Check if we're on Android 13+ where notification permission is required.
     *
     * @return true if running on Android 13 or higher
     */
    fun requiresRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}

