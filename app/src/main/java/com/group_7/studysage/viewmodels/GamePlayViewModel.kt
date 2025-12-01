package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class  GamePlayViewModel(
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    private var timerJob: Job? = null
    private var questionStartTime: Long = 0L

    init {
        observeWebSocket()
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

        if (question != null && player != null && !state.isAnswered) {
            // Stop the timer
            stopTimer()
            
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

    fun submitTacToeMove(squareIndex: Int, answerIndex: Int, boardState: List<String>) {
        val state = _gameUiState.value
        val player = authViewModel.currentUser.value

        if (player != null) {
            android.util.Log.d("GamePlayViewModel", "Submitting TacToe move: square=$squareIndex, answer=$answerIndex, boardState=$boardState")
            
            // Submit the answer with square index as question ID
            webSocketManager.submitAnswer(
                playerId = player.uid,
                questionId = "square_$squareIndex",
                answerIndex = answerIndex,
                timeElapsed = 0
            )
            // Send board update to sync with other players
            webSocketManager.sendBoardUpdate(boardState)
            
            android.util.Log.d("GamePlayViewModel", "Board update sent to server")
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
                    android.util.Log.d("GamePlayViewModel", "============ ROOM_UPDATE RECEIVED ============")
                    android.util.Log.d("GamePlayViewModel", "Session ID: ${it.id}")
                    android.util.Log.d("GamePlayViewModel", "Game Type: ${it.gameType}")
                    android.util.Log.d("GamePlayViewModel", "Game Status: ${it.status}")
                    android.util.Log.d("GamePlayViewModel", "Board State: ${it.boardState}")
                    android.util.Log.d("GamePlayViewModel", "Board State Size: ${it.boardState?.size ?: 0}")
                    android.util.Log.d("GamePlayViewModel", "Current Turn: ${it.currentTurn}")
                    android.util.Log.d("GamePlayViewModel", "Players: ${it.players.keys}")
                    android.util.Log.d("GamePlayViewModel", "Questions Count: ${it.questions.size}")
                    android.util.Log.d("GamePlayViewModel", "=========================================")

                    val currentUser = authViewModel.currentUser.value
                    val isHost = currentUser?.uid == it.hostId
                    _gameUiState.value = _gameUiState.value.copy(
                        currentSession = it,
                        isHost = isHost
                    )
                    android.util.Log.d("GamePlayViewModel", "GameUiState updated - new boardState: ${_gameUiState.value.currentSession?.boardState}")
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.gameStarted
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
                // Stop any existing timer
                stopTimer()
                
                _gameUiState.value = _gameUiState.value.copy(
                    currentQuestion = questionData,
                    currentFlashcard = null, // Clear flashcard when question comes
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = questionData?.timeLimit ?: 0
                )
                
                // Start countdown timer if question exists
                if (questionData != null) {
                    questionStartTime = System.currentTimeMillis()
                    startTimer(questionData.timeLimit)
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.nextFlashcard
            .onEach { flashcardData ->
                // Stop timer for flashcards too
                stopTimer()
                
                _gameUiState.value = _gameUiState.value.copy(
                    currentFlashcard = flashcardData,
                    currentQuestion = null, // Clear question when flashcard comes
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = flashcardData?.timeLimit ?: 0
                )
                
                // Start timer for flashcards if needed
                if (flashcardData != null) {
                    questionStartTime = System.currentTimeMillis()
                    startTimer(flashcardData.timeLimit)
                }
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

        webSocketManager.boardUpdate
            .onEach { boardState ->
                boardState?.let {
                    android.util.Log.d("GamePlayViewModel", "BOARD_UPDATE received: $it")
                    // Update the session with new board state
                    _gameUiState.value.currentSession?.let { session ->
                        android.util.Log.d("GamePlayViewModel", "Current session exists, updating with new board state")
                        val updatedSession = session.copy(boardState = it)
                        _gameUiState.value = _gameUiState.value.copy(
                            currentSession = updatedSession
                        )
                        android.util.Log.d("GamePlayViewModel", "Session updated with board state: ${updatedSession.boardState}")
                    } ?: android.util.Log.e("GamePlayViewModel", "ERROR: Current session is null, cannot update board state!")
                } ?: android.util.Log.e("GamePlayViewModel", "ERROR: Board state in update is null!")
            }
            .launchIn(viewModelScope)

        webSocketManager.turnUpdate
            .onEach { newTurnPlayerId ->
                newTurnPlayerId?.let {
                    // Update the session with new current turn
                    _gameUiState.value.currentSession?.let { session ->
                        _gameUiState.value = _gameUiState.value.copy(
                            currentSession = session.copy(currentTurn = it)
                        )
                        android.util.Log.d("GamePlayViewModel", "Turn updated to: $it")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Start countdown timer for a question
     */
    private fun startTimer(initialTime: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = initialTime
            while (timeLeft > 0 && !_gameUiState.value.isAnswered) {
                delay(1000)
                timeLeft--
                _gameUiState.value = _gameUiState.value.copy(timeRemaining = timeLeft)
            }
            
            // If timer reached 0 and user hasn't answered, auto-submit wrong answer
            if (timeLeft == 0 && !_gameUiState.value.isAnswered) {
                autoSubmitWrongAnswer()
            }
        }
    }

    /**
     * Stop the countdown timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Auto-submit a wrong answer when time runs out
     */
    private fun autoSubmitWrongAnswer() {
        val state = _gameUiState.value
        val question = state.currentQuestion?.question
        val player = authViewModel.currentUser.value

        if (question != null && player != null && !state.isAnswered) {
            // Find a wrong answer index (any index that's not the correct answer)
            val wrongAnswerIndex = if (question.correctAnswer == 0) 1 else 0
            val timeElapsed = System.currentTimeMillis() - questionStartTime
            
            webSocketManager.submitAnswer(
                playerId = player.uid,
                questionId = question.id,
                answerIndex = wrongAnswerIndex,
                timeElapsed = timeElapsed
            )
            
            _gameUiState.value = state.copy(
                isAnswered = true, 
                selectedAnswerIndex = wrongAnswerIndex,
                timeRemaining = 0
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        webSocketManager.disconnect()
    }
}