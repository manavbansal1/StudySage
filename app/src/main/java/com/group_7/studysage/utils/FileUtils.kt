package com.group_7.studysage.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.IOException

object FileUtils {

    data class FileInfo(
        val name: String,
        val size: Long,
        val mimeType: String?,
        val extension: String
    )

    /**
     * Get comprehensive file information from URI
     */
    fun getFileInfo(context: Context, uri: Uri): FileInfo? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "Unknown"
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                    val mimeType = context.contentResolver.getType(uri)
                    val extension = getFileExtension(name)

                    FileInfo(name, size, mimeType, extension)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get file extension from filename
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /**
     * Check if file type is supported
     */
    fun isSupportedFileType(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return supportedExtensions.contains(extension)
    }

    /**
     * Get human-readable file type description
     */
    fun getFileTypeDescription(fileName: String): String {
        return when (getFileExtension(fileName)) {
            "pdf" -> "PDF Document"
            "txt" -> "Text File"
            "doc" -> "Word Document"
            "docx" -> "Word Document"
            "md" -> "Markdown"
            "rtf" -> "Rich Text Format"
            "jpg", "jpeg" -> "JPEG Image"
            "png" -> "PNG Image"
            "gif" -> "GIF Image"
            "bmp" -> "Bitmap Image"
            "webp" -> "WebP Image"
            else -> "Document"
        }
    }

    /**
     * Get appropriate MIME type for file
     */
    fun getMimeType(fileName: String): String {
        val extension = getFileExtension(fileName)
        return when (extension) {
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "md" -> "text/markdown"
            "rtf" -> "application/rtf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
        }
    }

    /**
     * Format file size in human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    /**
     * Check if file size is within limits
     */
    fun isFileSizeValid(sizeBytes: Long): Boolean {
        return sizeBytes <= MAX_FILE_SIZE_BYTES
    }

    /**
     * Validate file for processing
     */
    fun validateFile(context: Context, uri: Uri): ValidationResult {
        val fileInfo = getFileInfo(context, uri)
            ?: return ValidationResult.Error("Could not read file information")

        // Check if file type is supported
        if (!isSupportedFileType(fileInfo.name)) {
            return ValidationResult.Error(
                "Unsupported file type. Supported formats: ${supportedExtensions.joinToString(", ")}"
            )
        }

        // Check file size
        if (!isFileSizeValid(fileInfo.size)) {
            return ValidationResult.Error(
                "File too large. Maximum size: ${formatFileSize(MAX_FILE_SIZE_BYTES)}"
            )
        }

        // Check if file is empty
        if (fileInfo.size == 0L) {
            return ValidationResult.Error("File is empty")
        }

        return ValidationResult.Success(fileInfo)
    }

    /**
     * Check if file can be processed by AI
     */
    fun canProcessWithAI(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return aiSupportedExtensions.contains(extension)
    }

    /**
     * Check if file is an image
     */
    fun isImageFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return imageExtensions.contains(extension)
    }

    /**
     * Check if file is a document
     */
    fun isDocumentFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return documentExtensions.contains(extension)
    }

    // Constants
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB

    private val supportedExtensions = setOf(
        "pdf", "txt", "doc", "docx", "md", "rtf",
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    )

    private val aiSupportedExtensions = setOf(
        "pdf", "txt", "doc", "docx", "md", "rtf",
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    )

    private val documentExtensions = setOf(
        "pdf", "txt", "doc", "docx", "md", "rtf"
    )

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    )

    sealed class ValidationResult {
        data class Success(val fileInfo: FileInfo) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}