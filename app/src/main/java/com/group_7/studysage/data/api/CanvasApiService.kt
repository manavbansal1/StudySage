package com.group_7.studysage.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class CanvasCourse(
    val id: Long,
    val name: String,
    val course_code: String? = null,
    val workflow_state: String? = null,
    val account_id: Long? = null,
    val start_at: String? = null,
    val end_at: String? = null,
    val enrollment_term_id: Long? = null
)

class CanvasApiService {
    
    companion object {
        private const val CANVAS_BASE_URL = "https://canvas.sfu.ca/api/v1"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getUserCourses(accessToken: String): Result<List<CanvasCourse>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$CANVAS_BASE_URL/courses?enrollment_state=active&per_page=100")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            
            if (response.isSuccessful) {
                val courses = json.decodeFromString<List<CanvasCourse>>(body)
                Result.success(courses)
            } else {
                Result.failure(Exception("Failed to fetch courses: ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToken(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$CANVAS_BASE_URL/users/self")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
