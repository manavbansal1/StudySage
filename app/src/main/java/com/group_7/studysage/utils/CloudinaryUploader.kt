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
    private const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    //Setting Time Outs For api calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Convert URI to File
            val file = uriToFile(context, imageUri) ?: return@withContext null

            // Requesting For Image
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", "studysage/profiles") // Optional: organize in folders
                .build()

            // Create request
            val request = Request.Builder()
                .url(CLOUDINARY_URL)
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val jsonObject = JSONObject(it)
                    val secureUrl = jsonObject.getString("secure_url")

                    // Clean up temporary file
                    file.delete()

                    return@withContext secureUrl
                }
            } else {
                // Log error
                val errorBody = response.body?.string()
                println("Cloudinary upload failed: ${response.code} - $errorBody")
            }

            // Clean up temporary file
            file.delete()

            return@withContext null

        } catch (e: Exception) {
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

    // Below is the function to Delete User pfp
    suspend fun deleteImage(publicId: String): Boolean = withContext(Dispatchers.IO) {
        false
    }
}