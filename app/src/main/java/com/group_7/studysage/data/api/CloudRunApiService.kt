package com.group_7.studysage.data.api

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service to communicate with your Cloud Run backend
 * Handles both Gemini AI (via /chat) and Google Cloud TTS (via /tts)
 */
class CloudRunApiService(
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "CloudRunApiService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Generate content using Gemini AI through your Cloud Run backend
     */
    suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling Cloud Run /chat endpoint")

                val requestBody = JSONObject().apply {
                    put("message", prompt)
                }

                val request = Request.Builder()
                    .url("$baseUrl/chat")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Cloud Run error: ${response.code} - $errorBody")
                    throw Exception("Cloud Run API Error (${response.code}): $errorBody")
                }

                val responseText = response.body?.string() ?: throw Exception("Empty response")
                Log.d(TAG, "Cloud Run response received")

                // Parse Gemini response format
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")

                text
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error: ${e.message}", e)
                throw Exception("Failed to generate content: ${e.message}")
            }
        }
    }

    /**
     * Convert text to speech using Google Cloud TTS through your Cloud Run backend
     * Returns base64-encoded audio
     */
    suspend fun textToSpeech(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling Cloud Run /tts endpoint")

                val requestBody = JSONObject().apply {
                    put("text", text)
                }

                val request = Request.Builder()
                    .url("$baseUrl/tts")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "TTS error: ${response.code} - $errorBody")
                    throw Exception("TTS API Error (${response.code}): $errorBody")
                }

                val responseText = response.body?.string() ?: throw Exception("Empty response")
                Log.d(TAG, "TTS response received")

                val jsonResponse = JSONObject(responseText)
                jsonResponse.getString("audio")
            } catch (e: Exception) {
                Log.e(TAG, "TTS API error: ${e.message}", e)
                throw Exception("Failed to generate audio: ${e.message}")
            }
        }
    }
}
