package com.group_7.studysage.data.websocket

import com.group_7.studysage.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket manager for real-time multiplayer game communication
 * Handles WebSocket connection, message sending/receiving for game sessions
 */
class GameWebSocketManager(
    private val baseUrl: String = "ws://10.0.2.2:8080"
) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Incoming messages
    private val _incomingMessages = MutableStateFlow<WebSocketMessage?>(null)
    val incomingMessages: StateFlow<WebSocketMessage?> = _incomingMessages

    // Specific message flows for easier handling
    private val _playerJoined = MutableStateFlow<PlayerJoinedData?>(null)
    val playerJoined: StateFlow<PlayerJoinedData?> = _playerJoined

    private val _playerLeft = MutableStateFlow<String?>(null)
    val playerLeft: StateFlow<String?> = _playerLeft

    private val _roomUpdate = MutableStateFlow<GameSessionData?>(null)
    val roomUpdate: StateFlow<GameSessionData?> = _roomUpdate

    private val _gameStarting = MutableStateFlow<Int?>(null)
    val gameStarting: StateFlow<Int?> = _gameStarting

    private val _gameStarted = MutableStateFlow<GameSessionData?>(null)
    val gameStarted: StateFlow<GameSessionData?> = _gameStarted

    private val _nextQuestion = MutableStateFlow<QuestionData?>(null)
    val nextQuestion: StateFlow<QuestionData?> = _nextQuestion

    private val _nextFlashcard = MutableStateFlow<FlashcardData?>(null)
    val nextFlashcard: StateFlow<FlashcardData?> = _nextFlashcard

    private val _answerResult = MutableStateFlow<AnswerResultData?>(null)
    val answerResult: StateFlow<AnswerResultData?> = _answerResult

    private val _scoresUpdate = MutableStateFlow<ScoresUpdateData?>(null)
    val scoresUpdate: StateFlow<ScoresUpdateData?> = _scoresUpdate

    private val _gameFinished = MutableStateFlow<GameResult?>(null)
    val gameFinished: StateFlow<GameResult?> = _gameFinished

    private val _chatMessage = MutableStateFlow<ChatMessageData?>(null)
    val chatMessage: StateFlow<ChatMessageData?> = _chatMessage

    private val _errorMessage = MutableStateFlow<ErrorData?>(null)
    val errorMessage: StateFlow<ErrorData?> = _errorMessage

    /**
     * Connect to game session WebSocket
     */
    fun connect(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ) {
        // Use standalone route if no groupId, otherwise use group-based route
        val url = if (groupId.isEmpty()) {
            "$baseUrl/api/games/sessions/$sessionId/ws?userId=$userId&userName=$userName"
        } else {
            "$baseUrl/api/games/groups/$groupId/sessions/$sessionId/ws?userId=$userId&userName=$userName"
        }

        val request = Request.Builder()
            .url(url)
            .build()

        _connectionState.value = ConnectionState.Connecting

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.Connected
                println("✅ WebSocket connected to game session")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingMessage(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                scope.launch {
                    handleIncomingMessage(bytes.utf8())
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnecting
                println("⚠️ WebSocket closing: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
                println("❌ WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
                println("❌ WebSocket error: ${t.message}")
            }
        })
    }

    /**
     * Handle incoming WebSocket messages
     */
    private fun handleIncomingMessage(messageText: String) {
        try {
            val message = json.decodeFromString<WebSocketMessage>(messageText)
            _incomingMessages.value = message

            // Route to specific flows based on message type
            when (message.type) {
                MessageType.PLAYER_JOINED -> {
                    message.data?.let {
                        _playerJoined.value = json.decodeFromString(it)
                    }
                }
                MessageType.PLAYER_LEFT -> {
                    _playerLeft.value = message.data
                }
                MessageType.ROOM_UPDATE -> {
                    message.data?.let {
                        _roomUpdate.value = json.decodeFromString(it)
                    }
                }
                MessageType.GAME_STARTING -> {
                    message.data?.let {
                        _gameStarting.value = it.toIntOrNull()
                    }
                }
                MessageType.GAME_STARTED -> {
                    message.data?.let {
                        _gameStarted.value = json.decodeFromString(it)
                    }
                }
                MessageType.NEXT_QUESTION -> {
                    message.data?.let {
                        _nextQuestion.value = json.decodeFromString(it)
                    }
                }
                MessageType.FLASHCARD_REVEALED -> {
                    message.data?.let {
                        _nextFlashcard.value = json.decodeFromString(it)
                    }
                }
                MessageType.ANSWER_RESULT -> {
                    message.data?.let {
                        _answerResult.value = json.decodeFromString(it)
                    }
                }
                MessageType.SCORES_UPDATE -> {
                    message.data?.let {
                        _scoresUpdate.value = json.decodeFromString(it)
                    }
                }
                MessageType.GAME_FINISHED -> {
                    message.data?.let {
                        _gameFinished.value = json.decodeFromString(it)
                    }
                }
                MessageType.CHAT_MESSAGE -> {
                    message.data?.let {
                        _chatMessage.value = json.decodeFromString(it)
                    }
                }
                MessageType.ERROR -> {
                    message.data?.let {
                        _errorMessage.value = json.decodeFromString(it)
                    }
                }
                else -> {
                    println("📩 Unhandled message type: ${message.type}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error parsing message: ${e.message}")
        }
    }

    // ============================================
    // SEND MESSAGES
    // ============================================

    /**
     * Send player ready status
     */
    fun sendPlayerReady(isReady: Boolean = true) {
        sendMessage(WebSocketMessage(
            type = MessageType.PLAYER_READY,
            data = isReady.toString()
        ))
    }

    /**
     * Send game starting signal (host only)
     */
    fun sendGameStarting() {
        sendMessage(WebSocketMessage(
            type = MessageType.GAME_STARTING,
            data = null
        ))
    }

    /**
     * Submit answer for quiz question
     */
    fun submitAnswer(
        playerId: String,
        questionId: String,
        answerIndex: Int,
        timeElapsed: Long
    ) {
        val answerData = GameAnswer(
            playerId = playerId,
            questionId = questionId,
            answer = answerIndex,
            timeElapsed = timeElapsed
        )

        val dataString = json.encodeToString(GameAnswer.serializer(), answerData)

        sendMessage(WebSocketMessage(
            type = MessageType.SUBMIT_ANSWER,
            data = dataString
        ))
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
        val answerData = GameAnswer(
            playerId = playerId,
            questionId = flashcardId,
            answer = if (isCorrect) 1 else 0,
            timeElapsed = timeElapsed,
            isCorrect = isCorrect
        )

        val dataString = json.encodeToString(GameAnswer.serializer(), answerData)

        sendMessage(WebSocketMessage(
            type = MessageType.SUBMIT_ANSWER,
            data = dataString
        ))
    }

    /**
     * Submit match pair (for Speed Match game)
     */
    fun submitMatchPair(
        playerId: String,
        termId: String,
        definitionId: String,
        timeElapsed: Long
    ) {
        val matchData = MatchPairSubmission(
            playerId = playerId,
            termId = termId,
            definitionId = definitionId,
            timeElapsed = timeElapsed
        )

        val dataString = json.encodeToString(MatchPairSubmission.serializer(), matchData)

        sendMessage(WebSocketMessage(
            type = MessageType.MATCH_PAIR,
            data = dataString
        ))
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
        val chatData = ChatMessageData(
            senderId = senderId,
            senderName = senderName,
            message = message,
            timestamp = System.currentTimeMillis(),
            teamOnly = teamOnly
        )

        val dataString = json.encodeToString(ChatMessageData.serializer(), chatData)

        sendMessage(WebSocketMessage(
            type = MessageType.CHAT_MESSAGE,
            data = dataString
        ))
    }

    /**
     * Generic message sender
     */
    private fun sendMessage(message: WebSocketMessage) {
        try {
            val messageString = json.encodeToString(WebSocketMessage.serializer(), message)
            webSocket?.send(messageString)
        } catch (e: Exception) {
            println("❌ Error sending message: ${e.message}")
        }
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
    }
}

/**
 * Connection state for WebSocket
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}