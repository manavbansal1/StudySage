package com.group_7.studysage.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.group_7.studysage.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object CloudinaryUploader {

    // Cloudinary credentials from BuildConfig To Connect with the Data Base
    private const val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    private const val UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    private const val CLOUDINARY_IMAGE_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
    private const val CLOUDINARY_RAW_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/raw/upload"

    //Setting Time Outs For api calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileType: String, // e.g., "image", "raw"
        folder: String, // e.g., "studysage/notes"
        resourceType: String // e.g., "image", "raw"
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Validate Cloudinary configuration
            if (CLOUD_NAME.isBlank() || UPLOAD_PRESET.isBlank()) {
                android.util.Log.e("CloudinaryUploader", "❌ Missing Cloudinary credentials. CLOUD_NAME or UPLOAD_PRESET is empty.")
                return@withContext null
            }

            // Convert URI to File
            val file = uriToFile(context, fileUri) ?: run {
                return@withContext null
            }

            // Validate file size (max 10MB for images, 50MB for other files)
            val maxSize = if (resourceType == "image") 10 * 1024 * 1024 else 50 * 1024 * 1024
            if (file.length() > maxSize) {
                file.delete()
                return@withContext null
            }

            val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

            // Validate image type for image uploads
            if (resourceType == "image" && !mimeType.startsWith("image/")) {
                file.delete()
                return@withContext null
            }

            // Determine the correct Cloudinary URL based on fileType
            val uploadUrl = when (resourceType) {
                "image" -> CLOUDINARY_IMAGE_UPLOAD_URL
                "raw" -> CLOUDINARY_RAW_UPLOAD_URL
                else -> CLOUDINARY_IMAGE_UPLOAD_URL // Default to image upload
            }

            // Requesting For File
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder) // Dynamic folder
                .addFormDataPart("resource_type", resourceType) // Specify resource type
                .build()

            // Create request
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val jsonObject = JSONObject(it)
                    val secureUrl = jsonObject.getString("secure_url")
                    val withAttachment = secureUrl.replace("/upload/", "/upload/fl_attachment/")
                    val downloadUrl = withAttachment.replace(Regex("/v[0-9]+/"), "/")

                    // Clean up temporary file
                    file.delete()

                    android.util.Log.d("CloudinaryUploader", "✅ Cloudinary upload successful: $downloadUrl")
                    return@withContext downloadUrl
                }
            } else {
                // Log detailed error
                val errorBody = response.body?.string()
                val errorMessage = try {
                    errorBody?.let { JSONObject(it).optString("error", "Unknown error") }
                } catch (e: Exception) {
                    errorBody
                }
                
                android.util.Log.e("CloudinaryUploader", "❌ Upload failed. Code: ${response.code}, Error: $errorMessage")
                
                when (response.code) {
                    401 -> android.util.Log.e("CloudinaryUploader", "❌ Invalid credentials. Check CLOUD_NAME and UPLOAD_PRESET.")
                    400 -> android.util.Log.e("CloudinaryUploader", "❌ Bad request. Check parameters.")
                    413 -> android.util.Log.e("CloudinaryUploader", "❌ File too large.")
                    else -> android.util.Log.e("CloudinaryUploader", "❌ Upload failed with code ${response.code}")
                }
            }

            // Clean up temporary file
            file.delete()

            return@withContext null

        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("CloudinaryUploader", "❌ Cloudinary Error: No internet connection", e)
            return@withContext null
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("CloudinaryUploader", "❌ Cloudinary Error: Upload timeout. Please try again", e)
            return@withContext null
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryUploader", "❌ Cloudinary Error: ${e.message}", e)
            e.printStackTrace()
            return@withContext null
        }
    }

    // Below is the function to Convert URI to File
    private fun uriToFile(context: Context, uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Get file name from URI
            val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg"

            // Create temporary file
            val tempFile = File(context.cacheDir, fileName)
            tempFile.createNewFile()

            // Copy input stream to file
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            inputStream.close()

            return tempFile

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Below is the function to get the Image when requested using its name
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }

        return fileName
    }

    /**
     * Upload a PDF file to Cloudinary specifically for game quiz generation
     * @param context Android context
     * @param pdfUri URI of the PDF file
     * @return The secure URL of the uploaded PDF, or null if upload failed
     */
    suspend fun uploadPdfForGame(context: Context, pdfUri: Uri): String? {
        return uploadFile(
            context = context,
            fileUri = pdfUri,
            fileType = "raw",
            folder = "studysage/game_pdfs",
            resourceType = "raw"
        )
    }

    // Below is the function to Delete User pfp
    suspend fun deleteImage(publicId: String): Boolean = withContext(Dispatchers.IO) {
        false
    }
}