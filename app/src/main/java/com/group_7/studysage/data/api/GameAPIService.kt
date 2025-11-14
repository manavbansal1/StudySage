package com.group_7.studysage.data.api

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
class GameApiService(private val baseUrl: String = "http://10.0.2.2:8080") {

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

    // ============================================
    // HEALTH CHECK
    // ============================================

    /**
     * Check backend health status
     * GET /api/games/health
     */
    suspend fun checkHealth(): ApiResponse<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/games/health")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val healthResponse = json.decodeFromString<HealthResponse>(body)
                ApiResponse(success = true, data = healthResponse)
            } else {
                ApiResponse(success = false, message = "Health check failed")
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
                val errorResponse = json.decodeFromString<ApiResponse<CreateGameSessionResponse>>(body)
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
     * Get specific game session details
     * GET /api/games/groups/{groupId}/sessions/{sessionId}
     */
    suspend fun getGameSession(
        groupId: String,
        sessionId: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to fetch session")
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
            val requestBody = JoinGameRequest(
                userId = userId,
                userName = userName
            )

            val jsonBody = json.encodeToString(JoinGameRequest.serializer(), requestBody)

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

    /**
     * Leave a game session
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/leave
     */
    suspend fun leaveGameSession(
        groupId: String,
        sessionId: String,
        userId: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = LeaveGameRequest(userId = userId)
            val jsonBody = json.encodeToString(LeaveGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/leave")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to leave game")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to leave game")
        }
    }

    // ============================================
    // GAME CONTROL
    // ============================================

    /**
     * Start a game (host only)
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/start
     */
    suspend fun startGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = StartGameRequest(hostId = hostId)
            val jsonBody = json.encodeToString(StartGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/start")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to start game")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to start game")
        }
    }

    /**
     * Pause a game (host only)
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/pause
     */
    suspend fun pauseGame(
        groupId: String,
        sessionId: String,
        hostId: String,
        reason: String? = null
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = PauseGameRequest(hostId = hostId, reason = reason)
            val jsonBody = json.encodeToString(PauseGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/pause")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to pause game")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to pause game")
        }
    }

    /**
     * Resume a game (host only)
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/resume
     */
    suspend fun resumeGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = ResumeGameRequest(hostId = hostId)
            val jsonBody = json.encodeToString(ResumeGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/resume")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to resume game")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to resume game")
        }
    }

    /**
     * End a game early (host only)
     * POST /api/games/groups/{groupId}/sessions/{sessionId}/end
     */
    suspend fun endGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ): ApiResponse<GameSessionData> = withContext(Dispatchers.IO) {
        try {
            val requestBody = EndGameRequest(hostId = hostId)
            val jsonBody = json.encodeToString(EndGameRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/end")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameSessionData>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to end game")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to end game")
        }
    }

    // ============================================
    // GAME RESULTS & STATS
    // ============================================

    /**
     * Get game results
     * GET /api/games/groups/{groupId}/sessions/{sessionId}/results
     */
    suspend fun getGameResults(
        groupId: String,
        sessionId: String
    ): ApiResponse<GameResult> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/games/groups/$groupId/sessions/$sessionId/results")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<GameResult>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to fetch results")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    /**
     * Get user game statistics
     * GET /api/games/users/{userId}/stats
     */
    suspend fun getUserStats(userId: String): ApiResponse<GameStats> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/games/users/$userId/stats")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")

                if (response.isSuccessful) {
                    val result = json.decodeFromString<ApiResponse<GameStats>>(body)
                    result
                } else {
                    ApiResponse(success = false, message = "Failed to fetch stats")
                }
            } catch (e: Exception) {
                ApiResponse(success = false, message = e.message ?: "Unknown error")
            }
        }

    /**
     * Get user game history
     * GET /api/games/users/{userId}/history?limit={limit}
     */
    suspend fun getUserHistory(
        userId: String,
        limit: Int = 20
    ): ApiResponse<List<GameHistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/games/users/$userId/history?limit=$limit")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<List<GameHistoryEntry>>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to fetch history")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    /**
     * Get group leaderboard
     * GET /api/games/groups/{groupId}/leaderboard?gameType={gameType}&limit={limit}
     */
    suspend fun getGroupLeaderboard(
        groupId: String,
        gameType: String? = null,
        limit: Int = 20
    ): ApiResponse<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("$baseUrl/api/games/groups/$groupId/leaderboard?limit=$limit")
                if (gameType != null) {
                    append("&gameType=$gameType")
                }
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (response.isSuccessful) {
                val result = json.decodeFromString<ApiResponse<List<LeaderboardEntry>>>(body)
                result
            } else {
                ApiResponse(success = false, message = "Failed to fetch leaderboard")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error")
        }
    }
}

// ============================================
// REQUEST/RESPONSE DATA CLASSES
// ============================================

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: Long,
    val supportedGameTypes: List<String>
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
data class JoinGameRequest(
    val userId: String,
    val userName: String
)

@Serializable
data class LeaveGameRequest(
    val userId: String
)

@Serializable
data class StartGameRequest(
    val hostId: String
)

@Serializable
data class PauseGameRequest(
    val hostId: String,
    val reason: String? = null
)

@Serializable
data class ResumeGameRequest(
    val hostId: String
)

@Serializable
data class EndGameRequest(
    val hostId: String
)

@Serializable
data class GameHistoryEntry(
    val gameSessionId: String,
    val gameType: GameType,
    val finalScore: Int,
    val rank: Int,
    val playedAt: Long,
    val duration: Long
)