package com.group_7.studysage.data.api

import android.util.Log
import com.group_7.studysage.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * API Service for StudySage multiplayer games backend
 * Handles all REST API calls for game session management
 */
class GameApiService(private val baseUrl: String = ApiConfig.BASE_HTTP_URL) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Host a new standalone game
     * POST /api/games/host
     */
    suspend fun hostGame(
        hostId: String,
        hostName: String,
        gameType: GameType,
        contentSource: ContentSource,
        contentData: String? = null,
        topicDescription: String? = null,
        settings: GameSettings = GameSettings()
    ): ApiResponse<HostGameResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = HostGameRequest(
                hostId = hostId,
                hostName = hostName,
                gameType = gameType,
                contentSource = contentSource,
                contentData = contentData,
                topicDescription = topicDescription,
                settings = settings
            )

            val jsonBody = json.encodeToString(HostGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/host")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val apiResponse = json.decodeFromString<ApiResponse<HostGameResponse>>(body)
                apiResponse
            } else {
                ApiResponse(success = false, message = "Failed to host game: ${response.code}")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    /**
     * Join a game by code
     * POST /api/games/join
     */
    suspend fun joinGameByCode(
        gameCode: String,
        userId: String,
        userName: String
    ): ApiResponse<JoinGameResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JoinGameRequest(
                gameCode = gameCode,
                userId = userId,
                userName = userName
            )

            val jsonBody = json.encodeToString(JoinGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/join")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val apiResponse = json.decodeFromString<ApiResponse<JoinGameResponse>>(body)
                apiResponse
            } else {
                ApiResponse(success = false, message = "Failed to join game: ${response.code}")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    // ============================================
    // GAME SESSION MANAGEMENT
    // ============================================

    /**
     * Create a new game session within a group
     * POST /api/games/groups/{groupId}/sessions
     */
    suspend fun createGameSession(
        groupId: String,
        documentId: String?,
        documentName: String?,
        hostId: String,
        hostName: String,
        gameType: GameType,
        settings: GameSettings
    ): ApiResponse<CreateGameSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = CreateGameSessionRequest(
                documentId = documentId,
                documentName = documentName,
                hostId = hostId,
                hostName = hostName,
                gameType = gameType,
                settings = settings
            )

            val jsonBody = json.encodeToString(
                CreateGameSessionRequest.serializer(),
                requestBody
            )

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<CreateGameSessionResponse>>(body)
                result
            } else {
                val errorResponse =
                    json.decodeFromString<ApiResponse<CreateGameSessionResponse>>(body)
                errorResponse
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to create game session")
        }
    }

    /**
     * Get all active game sessions in a group
     * GET /api/games/groups/{groupId}/sessions
     */
    suspend fun getActiveGameSessions(groupId: String): ApiResponse<List<GameSessionData>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/games/groups/$groupId/sessions")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")

                if (response.isSuccessful) {
                    val result = json.decodeFromString<ApiResponse<List<GameSessionData>>>(body)
                    result
                } else {
                    ApiResponse(success = false, message = "Failed to fetch sessions")
                }
            } catch (e: Exception) {
                ApiResponse(success = false, message = e.message ?: "Unknown error")
            }
        }

    /**
     * Join a game session
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/join
     */
    suspend fun joinGameSession(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JoinGameSessionRequest(
                userId = userId,
                userName = userName
            )

            val jsonBody = json.encodeToString(JoinGameSessionRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/join")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                val errorResponse = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                errorResponse
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to join game")
        }
    }

    @Serializable
    data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    @Serializable
    data class CreateGameSessionRequest(
        val documentId: String?,
        val documentName: String?,
        val hostId: String,
        val hostName: String,
        val gameType: GameType,
        val settings: GameSettings
    )

    @Serializable
    data class CreateGameSessionResponse(
        val gameSessionId: String,
        val groupId: String
    )

    @Serializable
    data class JoinGameSessionRequest(
        val userId: String,
        val userName: String
    )
}