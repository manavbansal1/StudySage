package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GamePlayViewModel(
    private val gameWebSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState

    private val gson = Gson()
    private var currentGroupId: String = ""
    private var currentSessionId: String = ""

    init {
        observeWebSocketMessages()
        observeConnectionState()
    }

    fun connect(groupId: String, sessionId: String) {
        currentGroupId = groupId
        currentSessionId = sessionId

        _gameUiState.value = _gameUiState.value.copy(isLoading = true, error = null)

        val userId = authViewModel.authRepository.currentUser?.uid ?: ""
        val userName = authViewModel.userProfile.value?.get("name") as? String ?: "Player"

        // For now, use a placeholder backend URL - update this with your actual backend URL
        val backendUrl = "ws://10.0.2.2:8080" // Android emulator localhost
        // For real device, use: "ws://YOUR_BACKEND_IP:8080"

        gameWebSocketManager.connect(backendUrl, groupId, sessionId, userId, userName)
    }

    fun submitAnswer(answerIndex: Int, timeElapsed: Long) {
        _gameUiState.value = _gameUiState.value.copy(
            selectedAnswerIndex = answerIndex,
            isAnswered = true
        )

        val userId = authViewModel.authRepository.currentUser?.uid ?: ""
        val questionId = _gameUiState.value.currentQuestion?.question?.id ?: ""

        // Format answer to match backend's GameAnswer format
        val answerData = mapOf(
            "playerId" to userId,
            "questionId" to questionId,
            "answer" to answerIndex,
            "timeElapsed" to timeElapsed
        )

        val message = mapOf(
            "type" to "SUBMIT_ANSWER",
            "data" to gson.toJson(answerData)
        )
        gameWebSocketManager.sendMessage(message)
    }

    fun setPlayerReady(isReady: Boolean) {
        val message = mapOf(
            "type" to "PLAYER_READY",
            "data" to isReady.toString()
        )
        gameWebSocketManager.sendMessage(message)
    }

    fun startGame() {
        val message = mapOf(
            "type" to "GAME_STARTING",
            "data" to null
        )
        gameWebSocketManager.sendMessage(message)
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            gameWebSocketManager.gameMessages.collect { message ->
                message?.let { handleGameMessage(it) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            gameWebSocketManager.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        _gameUiState.value = _gameUiState.value.copy(isLoading = false, error = null)
                    }
                    is ConnectionState.Error -> {
                        _gameUiState.value = _gameUiState.value.copy(
                            isLoading = false,
                            error = state.message
                        )
                    }
                    is ConnectionState.Disconnected -> {
                        // Handle disconnection if needed
                    }
                }
            }
        }
    }

    private fun handleGameMessage(message: com.group_7.studysage.data.websocket.GameMessage) {
        // Backend uses enum-based message types, handle both formats
        val messageType = message.type.uppercase().replace("-", "_")

        when (messageType) {
            "ROOM_UPDATE", "SESSION_UPDATE" -> handleSessionUpdate(message.data)
            "NEXT_QUESTION", "QUESTION" -> handleQuestion(message.data)
            "ANSWER_RESULT" -> handleAnswerResult(message.data)
            "SCORES_UPDATE", "LEADERBOARD_UPDATE" -> handleLeaderboardUpdate(message.data)
            "GAME_FINISHED" -> handleGameFinished(message.data)
            "PLAYER_JOINED" -> handlePlayerJoined(message.data)
            "PLAYER_LEFT" -> handlePlayerLeft(message.data)
            "GAME_STARTED" -> handleGameStarted(message.data)
            "GAME_STARTING" -> handleGameStarting(message.data)
            "ERROR" -> handleError(message.data?.get("message") as? String)
        }
    }

    private fun handlePlayerJoined(data: Map<String, Any>?) {
        // Refresh session state when a player joins
        data?.let {
            try {
                val sessionJson = gson.toJson(data)
                val session = gson.fromJson(sessionJson, GameSession::class.java)
                _gameUiState.value = _gameUiState.value.copy(currentSession = session)
            } catch (e: Exception) {
                println("Error parsing player joined: ${e.message}")
            }
        }
    }

    private fun handlePlayerLeft(data: Map<String, Any>?) {
        // Player left, the ROOM_UPDATE will follow
    }

    private fun handleGameStarted(data: Map<String, Any>?) {
        // Game has started, update status
        _gameUiState.value = _gameUiState.value.copy(isLoading = false)
    }

    private fun handleGameStarting(data: Map<String, Any>?) {
        // Game is starting (countdown), show loading
        _gameUiState.value = _gameUiState.value.copy(isLoading = true)
    }

    private fun handleSessionUpdate(data: Map<String, Any>?) {
        data?.let {
            try {
                val sessionJson = gson.toJson(data)
                val session = gson.fromJson(sessionJson, GameSession::class.java)

                val currentUserId = authViewModel.authRepository.currentUser?.uid ?: ""
                val isHost = session.players.values.any { it.id == currentUserId && it.isHost }

                _gameUiState.value = _gameUiState.value.copy(
                    currentSession = session,
                    isHost = isHost,
                    isLoading = false
                )
            } catch (e: Exception) {
                _gameUiState.value = _gameUiState.value.copy(
                    error = "Failed to parse session update: ${e.message}"
                )
            }
        }
    }

    private fun handleQuestion(data: Map<String, Any>?) {
        data?.let {
            try {
                val questionJson = gson.toJson(data)
                val questionData = gson.fromJson(questionJson, QuestionData::class.java)

                _gameUiState.value = _gameUiState.value.copy(
                    currentQuestion = questionData,
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    lastResult = null
                )
            } catch (e: Exception) {
                _gameUiState.value = _gameUiState.value.copy(
                    error = "Failed to parse question: ${e.message}"
                )
            }
        }
    }

    private fun handleAnswerResult(data: Map<String, Any>?) {
        data?.let {
            try {
                val resultJson = gson.toJson(data)
                val result = gson.fromJson(resultJson, AnswerResult::class.java)

                _gameUiState.value = _gameUiState.value.copy(
                    lastResult = result
                )
            } catch (e: Exception) {
                _gameUiState.value = _gameUiState.value.copy(
                    error = "Failed to parse answer result: ${e.message}"
                )
            }
        }
    }

    private fun handleLeaderboardUpdate(data: Map<String, Any>?) {
        data?.let {
            try {
                val leaderboardJson = gson.toJson(data["leaderboard"])
                val type = object : TypeToken<List<LeaderboardEntry>>() {}.type
                val leaderboard: List<LeaderboardEntry> = gson.fromJson(leaderboardJson, type)

                _gameUiState.value = _gameUiState.value.copy(
                    leaderboard = leaderboard
                )
            } catch (e: Exception) {
                _gameUiState.value = _gameUiState.value.copy(
                    error = "Failed to parse leaderboard: ${e.message}"
                )
            }
        }
    }

    private fun handleGameFinished(data: Map<String, Any>?) {
        data?.let {
            try {
                val resultsJson = gson.toJson(data)
                val finalResults = gson.fromJson(resultsJson, FinalResults::class.java)

                _gameUiState.value = _gameUiState.value.copy(
                    gameFinished = true,
                    finalResults = finalResults
                )
            } catch (e: Exception) {
                _gameUiState.value = _gameUiState.value.copy(
                    error = "Failed to parse final results: ${e.message}"
                )
            }
        }
    }

    private fun handleError(error: String?) {
        _gameUiState.value = _gameUiState.value.copy(
            error = error ?: "Unknown error occurred"
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameWebSocketManager.disconnect()
    }
}
