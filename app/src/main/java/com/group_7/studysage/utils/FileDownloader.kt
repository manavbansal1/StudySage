package com.group_7.studysage.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.os.Environment
import android.util.Log


object FileDownloader {

    fun downloadFile(context: Context, url: String, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(url.toUri())
                .setTitle(fileName)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)

            Log.d("kss", "Download started for URL: $url")


        } catch (e: Exception) {

        }
    }

    fun downloadLocalFile(context: Context, sourceFilePath: String, fileName: String) {
        try {
            val sourceFile = java.io.File(sourceFilePath)
            if (!sourceFile.exists()) {

                return
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Check if file already exists and add a number suffix if needed
            var destinationFile = java.io.File(downloadsDir, fileName)
            var counter = 1
            val fileNameWithoutExt = fileName.substringBeforeLast(".")
            val fileExt = fileName.substringAfterLast(".")

            while (destinationFile.exists()) {
                val newFileName = "${fileNameWithoutExt}_$counter.$fileExt"
                destinationFile = java.io.File(downloadsDir, newFileName)
                counter++
            }

            // Copy file to Downloads directory
            sourceFile.copyTo(destinationFile, overwrite = false)

            Log.d("FileDownloader", "File downloaded to: ${destinationFile.absolutePath}")


        } catch (e: Exception) {
            Log.e("FileDownloader", "Download failed: ${e.message}", e)

        }
    }
}