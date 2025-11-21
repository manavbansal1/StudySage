package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GamePlayViewModel(
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    init {
        observeWebSocket()
    }

    fun connect(groupId: String, sessionId: String) {
        val currentUser = authViewModel.currentUser.value
        currentUser?.let {
            webSocketManager.connect(
                groupId = groupId,
                sessionId = sessionId,
                userId = it.uid,
                userName = it.displayName ?: "Unknown"
            )
        }
    }

    /**
     * Connect to a standalone game using only the game code
     */
    fun connectToStandaloneGame(gameCode: String) {
        val currentUser = authViewModel.currentUser.value
        currentUser?.let {
            // Get user's name from Firestore profile
            val userName = authViewModel.userProfile.value?.get("name") as? String
                ?: it.displayName?.takeIf { it.isNotBlank() }
                ?: "Player"

            // For standalone games, use empty groupId or the game code itself
            webSocketManager.connect(
                groupId = "", // No group dependency
                sessionId = gameCode,
                userId = it.uid,
                userName = userName
            )
        }
    }

    fun setPlayerReady(isReady: Boolean) {
        webSocketManager.sendPlayerReady(isReady)
    }

    fun startGame() {
        if (_gameUiState.value.isHost) {
            webSocketManager.sendGameStarting()
        }
    }

    fun submitAnswer(answerIndex: Int, timeElapsed: Long) {
        val state = _gameUiState.value
        val question = state.currentQuestion?.question
        val player = authViewModel.currentUser.value

        if (question != null && player != null) {
            webSocketManager.submitAnswer(
                playerId = player.uid,
                questionId = question.id,
                answerIndex = answerIndex,
                timeElapsed = timeElapsed
            )
            _gameUiState.value = state.copy(isAnswered = true, selectedAnswerIndex = answerIndex)
        }
    }

    fun submitFlashcardAnswer(isCorrect: Boolean, timeElapsed: Long) {
        val state = _gameUiState.value
        val flashcard = state.currentFlashcard?.flashcard
        val player = authViewModel.currentUser.value

        if (flashcard != null && player != null) {
            webSocketManager.submitFlashcardAnswer(
                playerId = player.uid,
                flashcardId = flashcard.id,
                isCorrect = isCorrect,
                timeElapsed = timeElapsed
            )
            _gameUiState.value = state.copy(isAnswered = true)
        }
    }

    private fun observeWebSocket() {
        webSocketManager.connectionState
            .onEach { state ->
                _gameUiState.value = _gameUiState.value.copy(
                    isLoading = state is ConnectionState.Connecting,
                    error = if (state is ConnectionState.Error) state.message else null
                )
            }
            .launchIn(viewModelScope)

        webSocketManager.roomUpdate
            .onEach { session ->
                session?.let {
                    val currentUser = authViewModel.currentUser.value
                    val isHost = currentUser?.uid == it.hostId
                    _gameUiState.value = _gameUiState.value.copy(
                        currentSession = it,
                        isHost = isHost
                    )
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.nextQuestion
            .onEach { questionData ->
                _gameUiState.value = _gameUiState.value.copy(
                    currentQuestion = questionData,
                    currentFlashcard = null, // Clear flashcard when question comes
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = questionData?.timeLimit ?: 0
                )
            }
            .launchIn(viewModelScope)

        webSocketManager.nextFlashcard
            .onEach { flashcardData ->
                _gameUiState.value = _gameUiState.value.copy(
                    currentFlashcard = flashcardData,
                    currentQuestion = null, // Clear question when flashcard comes
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = flashcardData?.timeLimit ?: 0
                )
            }
            .launchIn(viewModelScope)

        webSocketManager.answerResult
            .onEach { result ->
                _gameUiState.value = _gameUiState.value.copy(lastResult = result)
            }
            .launchIn(viewModelScope)

        webSocketManager.scoresUpdate
            .onEach { scores ->
                scores?.let {
                    _gameUiState.value = _gameUiState.value.copy(leaderboard = it.leaderboard)
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.gameFinished
            .onEach { result ->
                result?.let {
                    _gameUiState.value = _gameUiState.value.copy(
                        gameFinished = true,
                        finalResults = it
                    )
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.chatMessage
            .onEach { message ->
                message?.let {
                    val updatedMessages = _gameUiState.value.chatMessages + it
                    _gameUiState.value = _gameUiState.value.copy(chatMessages = updatedMessages)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}