package com.group_7.studysage.data.api

import com.google.gson.Gson
import com.group_7.studysage.data.model.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Example service for submitting quizzes to your backend API
 * 
 * Usage:
 * val backendService = BackendApiService("https://your-api.com")
 * val result = backendService.submitQuiz(quiz)
 */
class BackendApiService(
    private val baseUrl: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    /**
     * Submit quiz to backend API
     * 
     * @param quiz The Quiz object to submit
     * @return Result with success message or error
     */
    suspend fun submitQuiz(quiz: Quiz): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Convert quiz to JSON
            val jsonBody = gson.toJson(quiz)
            
            // Create request body
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            // Build request
            val request = Request.Builder()
                .url("$baseUrl/api/quizzes")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                // Add your auth token if needed
                // .addHeader("Authorization", "Bearer $token")
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Result.success(responseBody)
            } else {
                Result.failure(IOException("Server returned ${response.code}: ${response.message}"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit quiz as raw JSON string
     * 
     * @param quizJson JSON string of the quiz
     * @return Result with success message or error
     */
    suspend fun submitQuizJson(quizJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = quizJson.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/api/quizzes")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Result.success(response.body?.string() ?: "Success")
            } else {
                Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Example usage in ViewModel:
 * 
 * class GameViewModel : ViewModel() {
 *     private val backendApi = BackendApiService("https://your-backend.com")
 *     
 *     fun submitQuizToBackend() {
 *         val quiz = _quizGenerationState.value.generatedQuiz ?: return
 *         
 *         viewModelScope.launch {
 *             _quizGenerationState.value = _quizGenerationState.value.copy(
 *                 isSubmitting = true
 *             )
 *             
 *             val result = backendApi.submitQuiz(quiz)
 *             
 *             result.fold(
 *                 onSuccess = { response ->
 *                     // Handle success
 *                     _quizGenerationState.value = _quizGenerationState.value.copy(
 *                         isSubmitting = false,
 *                         submitSuccess = true
 *                     )
 *                 },
 *                 onFailure = { error ->
 *                     // Handle error
 *                     _quizGenerationState.value = _quizGenerationState.value.copy(
 *                         isSubmitting = false,
 *                         error = error.message
 *                     )
 *                 }
 *             )
 *         }
 *     }
 * }
 */
