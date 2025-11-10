package com.group_7.studysage.data.repository

import com.group_7.studysage.data.api.ApiResponse
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing multiplayer game operations
 * Combines REST API calls and WebSocket real-time communication
 */
class GameRepository(
    private val apiService: GameApiService = GameApiService(),
    private val wsManager: GameWebSocketManager = GameWebSocketManager()
) {

    // ============================================
    // WebSocket State Flows (Real-time)
    // ============================================

    val connectionState: StateFlow<ConnectionState> = wsManager.connectionState
    val playerJoined: StateFlow<PlayerJoinedData?> = wsManager.playerJoined
    val playerLeft: StateFlow<String?> = wsManager.playerLeft
    val roomUpdate: StateFlow<GameSessionData?> = wsManager.roomUpdate
    val gameStarting: StateFlow<Int?> = wsManager.gameStarting
    val gameStarted: StateFlow<GameSessionData?> = wsManager.gameStarted
    val nextQuestion: StateFlow<QuestionData?> = wsManager.nextQuestion
    val nextFlashcard: StateFlow<FlashcardData?> = wsManager.nextFlashcard
    val answerResult: StateFlow<AnswerResultData?> = wsManager.answerResult
    val scoresUpdate: StateFlow<ScoresUpdateData?> = wsManager.scoresUpdate
    val gameFinished: StateFlow<GameResult?> = wsManager.gameFinished
    val chatMessage: StateFlow<ChatMessageData?> = wsManager.chatMessage
    val errorMessage: StateFlow<ErrorData?> = wsManager.errorMessage

    // ============================================
    // HEALTH & INFO
    // ============================================

    suspend fun checkHealth() = apiService.checkHealth()

    // ============================================
    // GAME SESSION MANAGEMENT (REST API)
    // ============================================

    /**
     * Create a new game session
     */
    suspend fun createGameSession(
        groupId: String,
        documentId: String?,
        documentName: String?,
        hostId: String,
        hostName: String,
        gameType: GameType,
        settings: GameSettings = GameSettings()
    ) = apiService.createGameSession(
        groupId = groupId,
        documentId = documentId,
        documentName = documentName,
        hostId = hostId,
        hostName = hostName,
        gameType = gameType,
        settings = settings
    )

    /**
     * Get all active game sessions in a group
     */
    suspend fun getActiveGameSessions(groupId: String) =
        apiService.getActiveGameSessions(groupId)

    /**
     * Get specific game session details
     */
    suspend fun getGameSession(groupId: String, sessionId: String) =
        apiService.getGameSession(groupId, sessionId)

    /**
     * Join a game session
     */
    suspend fun joinGameSession(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ) = apiService.joinGameSession(groupId, sessionId, userId, userName)

    /**
     * Leave a game session
     */
    suspend fun leaveGameSession(
        groupId: String,
        sessionId: String,
        userId: String
    ) = apiService.leaveGameSession(groupId, sessionId, userId)

    // ============================================
    // GAME CONTROL (REST API)
    // ============================================

    /**
     * Start a game (host only)
     */
    suspend fun startGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.startGame(groupId, sessionId, hostId)

    /**
     * Pause a game (host only)
     */
    suspend fun pauseGame(
        groupId: String,
        sessionId: String,
        hostId: String,
        reason: String? = null
    ) = apiService.pauseGame(groupId, sessionId, hostId, reason)

    /**
     * Resume a game (host only)
     */
    suspend fun resumeGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.resumeGame(groupId, sessionId, hostId)

    /**
     * End a game early (host only)
     */
    suspend fun endGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.endGame(groupId, sessionId, hostId)

    // ============================================
    // GAME RESULTS & STATS (REST API)
    // ============================================

    /**
     * Get game results after game finishes
     */
    suspend fun getGameResults(groupId: String, sessionId: String) =
        apiService.getGameResults(groupId, sessionId)

    /**
     * Get user's game statistics
     */
    suspend fun getUserStats(userId: String) =
        apiService.getUserStats(userId)

    /**
     * Get user's game history
     */
    suspend fun getUserHistory(userId: String, limit: Int = 20) =
        apiService.getUserHistory(userId, limit)

    /**
     * Get group leaderboard
     */
    suspend fun getGroupLeaderboard(
        groupId: String,
        gameType: String? = null,
        limit: Int = 20
    ) = apiService.getGroupLeaderboard(groupId, gameType, limit)

    // ============================================
    // WEBSOCKET CONNECTION
    // ============================================

    /**
     * Connect to game session via WebSocket
     */
    fun connectToGameSession(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ) {
        wsManager.connect(groupId, sessionId, userId, userName)
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnectFromGame() {
        wsManager.disconnect()
    }

    // ============================================
    // WEBSOCKET ACTIONS
    // ============================================

    /**
     * Send player ready status
     */
    fun sendPlayerReady(isReady: Boolean = true) {
        wsManager.sendPlayerReady(isReady)
    }

    /**
     * Send game starting signal (host only)
     */
    fun sendGameStarting() {
        wsManager.sendGameStarting()
    }

    /**
     * Submit answer for quiz question
     */
    fun submitQuizAnswer(
        playerId: String,
        questionId: String,
        answerIndex: Int,
        timeElapsed: Long
    ) {
        wsManager.submitAnswer(playerId, questionId, answerIndex, timeElapsed)
    }

    /**
     * Submit flashcard answer
     */
    fun submitFlashcardAnswer(
        playerId: String,
        flashcardId: String,
        isCorrect: Boolean,
        timeElapsed: Long
    ) {
        wsManager.submitFlashcardAnswer(playerId, flashcardId, isCorrect, timeElapsed)
    }

    /**
     * Submit match pair for Speed Match game
     */
    fun submitMatchPair(
        playerId: String,
        termId: String,
        definitionId: String,
        timeElapsed: Long
    ) {
        wsManager.submitMatchPair(playerId, termId, definitionId, timeElapsed)
    }

    /**
     * Send chat message
     */
    fun sendChatMessage(
        senderId: String,
        senderName: String,
        message: String,
        teamOnly: Boolean = false
    ) {
        wsManager.sendChatMessage(senderId, senderName, message, teamOnly)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        wsManager.cleanup()
    }
}