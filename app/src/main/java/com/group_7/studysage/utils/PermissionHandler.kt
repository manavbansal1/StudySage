package com.group_7.studysage.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Requests all relevant runtime permissions for:
 *  - Camera
 *  - Microphone
 *  - Media (photos, videos, audio)
 *  - Files (legacy storage)
 *
 * Works on Android 6 → 14.  Silent + automatic.
 */
object PermissionHandler {

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    fun init(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        {}
    }

    fun requestAllPermissions(context: Context) {
        val permissions = mutableListOf<String>()

        // Always request camera and microphone
        permissions += Manifest.permission.CAMERA
        permissions += Manifest.permission.RECORD_AUDIO

        // Version-specific storage/media permissions
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+
                permissions += listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6–12 (includes Oreo)
                permissions += listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            else -> {
                // Android 5− grants at install time; nothing to request
                return
            }
        }

        // Ask only for permissions not already granted
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            permissionLauncher?.launch(notGranted.toTypedArray())
        }
    }
}
