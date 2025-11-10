package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Game Play Screen
 * Manages:
 * - Game state and flow for all game modes
 * - Timer management
 * - Answer submission
 * - Real-time score updates
 * - Game completion
 */
class GamePlayViewModel(
    private val gameRepository: GameRepository = GameRepository()
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Current session info
    private var currentGroupId: String = ""
    private var currentSessionId: String = ""
    private var currentUserId: String = ""
    private var currentGameType: GameType = GameType.QUIZ_RACE

    // Timer management
    private var timerJob: Job? = null
    private var questionStartTime: Long = 0

    init {
        observeGameUpdates()
    }

    /**
     * Initialize game session
     */
    fun initializeGame(
        groupId: String,
        sessionId: String,
        userId: String,
        gameType: GameType
    ) {
        currentGroupId = groupId
        currentSessionId = sessionId
        currentUserId = userId
        currentGameType = gameType

        // Connect to WebSocket if not already connected
        gameRepository.connectToGameSession(groupId, sessionId, userId, "Player")

        // Load initial game state
        loadGameSession()
    }

    /**
     * Load current game session state
     */
    private fun loadGameSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val response = gameRepository.getGameSession(currentGroupId, currentSessionId)

                if (response.success && response.data != null) {
                    val session = response.data
                    val isHost = session.hostId == currentUserId

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentSession = session,
                            isHost = isHost
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load game"
                    )
                }
            }
        }
    }

    /**
     * Observe real-time game updates
     */
    private fun observeGameUpdates() {
        // Game started
        viewModelScope.launch {
            gameRepository.gameStarted.collect { session ->
                session?.let {
                    _uiState.update { state ->
                        state.copy(currentSession = it)
                    }
                }
            }
        }

        // Game starting countdown
        viewModelScope.launch {
            gameRepository.gameStarting.collect { countdown ->
                countdown?.let {
                    println("Game starting in $it...")
                }
            }
        }

        // Next question (Quiz modes)
        viewModelScope.launch {
            gameRepository.nextQuestion.collect { questionData ->
                questionData?.let {
                    handleNextQuestion(it)
                }
            }
        }

        // Next flashcard (Flashcard Battle)
        viewModelScope.launch {
            gameRepository.nextFlashcard.collect { flashcardData ->
                flashcardData?.let {
                    handleNextFlashcard(it)
                }
            }
        }

        // Answer result
        viewModelScope.launch {
            gameRepository.answerResult.collect { result ->
                result?.let {
                    handleAnswerResult(it)
                }
            }
        }

        // Scores update
        viewModelScope.launch {
            gameRepository.scoresUpdate.collect { scoresData ->
                scoresData?.let {
                    _uiState.update { state ->
                        state.copy(leaderboard = it.leaderboard)
                    }
                }
            }
        }

        // Game finished
        viewModelScope.launch {
            gameRepository.gameFinished.collect { result ->
                result?.let {
                    handleGameFinished(it)
                }
            }
        }

        // Chat messages
        viewModelScope.launch {
            gameRepository.chatMessage.collect { message ->
                message?.let {
                    _uiState.update { state ->
                        state.copy(
                            chatMessages = state.chatMessages + it
                        )
                    }
                }
            }
        }

        // Errors
        viewModelScope.launch {
            gameRepository.errorMessage.collect { error ->
                error?.let {
                    _uiState.update { state ->
                        state.copy(error = it.message)
                    }
                }
            }
        }
    }

    /**
     * Handle next question
     */
    private fun handleNextQuestion(questionData: QuestionData) {
        _uiState.update {
            it.copy(
                currentQuestion = questionData,
                selectedAnswerIndex = null,
                isAnswered = false,
                timeRemaining = questionData.timeLimit,
                lastResult = null
            )
        }

        questionStartTime = System.currentTimeMillis()
        startTimer(questionData.timeLimit)
    }

    /**
     * Handle next flashcard
     */
    private fun handleNextFlashcard(flashcardData: FlashcardData) {
        _uiState.update {
            it.copy(
                currentFlashcard = flashcardData,
                isAnswered = false,
                timeRemaining = flashcardData.timeLimit,
                lastResult = null
            )
        }

        questionStartTime = System.currentTimeMillis()
        startTimer(flashcardData.timeLimit)
    }

    /**
     * Handle answer result
     */
    private fun handleAnswerResult(result: AnswerResultData) {
        _uiState.update {
            it.copy(
                lastResult = result,
                isAnswered = true
            )
        }

        stopTimer()
    }

    /**
     * Handle game finished
     */
    private fun handleGameFinished(result: GameResult) {
        _uiState.update {
            it.copy(
                gameFinished = true,
                finalResults = result
            )
        }

        stopTimer()
    }

    /**
     * Start question timer
     */
    private fun startTimer(initialTime: Int) {
        stopTimer() // Stop any existing timer

        timerJob = viewModelScope.launch {
            var remaining = initialTime

            while (remaining > 0) {
                _uiState.update { it.copy(timeRemaining = remaining) }
                delay(1000)
                remaining--
            }

            _uiState.update { it.copy(timeRemaining = 0) }

            // Auto-submit if not answered
            if (!_uiState.value.isAnswered) {
                submitTimeout()
            }
        }
    }

    /**
     * Stop timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Select an answer
     */
    fun selectAnswer(answerIndex: Int) {
        _uiState.update { it.copy(selectedAnswerIndex = answerIndex) }
    }

    /**
     * Submit quiz answer
     */
    fun submitQuizAnswer() {
        val state = _uiState.value
        val selectedIndex = state.selectedAnswerIndex ?: return
        val questionId = state.currentQuestion?.question?.id ?: return

        if (state.isAnswered) return

        val timeElapsed = System.currentTimeMillis() - questionStartTime

        gameRepository.submitQuizAnswer(
            playerId = currentUserId,
            questionId = questionId,
            answerIndex = selectedIndex,
            timeElapsed = timeElapsed
        )

        _uiState.update { it.copy(isAnswered = true) }
        stopTimer()
    }

    /**
     * Submit flashcard answer
     */
    fun submitFlashcardAnswer(isCorrect: Boolean) {
        val state = _uiState.value
        val flashcardId = state.currentFlashcard?.flashcard?.id ?: return

        if (state.isAnswered) return

        val timeElapsed = System.currentTimeMillis() - questionStartTime

        gameRepository.submitFlashcardAnswer(
            playerId = currentUserId,
            flashcardId = flashcardId,
            isCorrect = isCorrect,
            timeElapsed = timeElapsed
        )

        _uiState.update { it.copy(isAnswered = true) }
        stopTimer()
    }

    /**
     * Submit match pair (Speed Match)
     */
    fun submitMatchPair(termId: String, definitionId: String) {
        val timeElapsed = System.currentTimeMillis() - questionStartTime

        gameRepository.submitMatchPair(
            playerId = currentUserId,
            termId = termId,
            definitionId = definitionId,
            timeElapsed = timeElapsed
        )
    }

    /**
     * Submit timeout (no answer selected)
     */
    private fun submitTimeout() {
        val state = _uiState.value

        when (currentGameType) {
            GameType.QUIZ_RACE,
            GameType.SPEED_QUIZ,
            GameType.TEAM_TRIVIA,
            GameType.SURVIVAL_MODE -> {
                val questionId = state.currentQuestion?.question?.id ?: return
                val timeElapsed = System.currentTimeMillis() - questionStartTime

                // Submit answer index -1 to indicate timeout
                gameRepository.submitQuizAnswer(
                    playerId = currentUserId,
                    questionId = questionId,
                    answerIndex = -1,
                    timeElapsed = timeElapsed
                )
            }
            GameType.FLASHCARD_BATTLE -> {
                val flashcardId = state.currentFlashcard?.flashcard?.id ?: return
                val timeElapsed = System.currentTimeMillis() - questionStartTime

                gameRepository.submitFlashcardAnswer(
                    playerId = currentUserId,
                    flashcardId = flashcardId,
                    isCorrect = false,
                    timeElapsed = timeElapsed
                )
            }
            else -> {}
        }

        _uiState.update { it.copy(isAnswered = true) }
    }

    /**
     * Send chat message
     */
    fun sendChatMessage(message: String, teamOnly: Boolean = false) {
        gameRepository.sendChatMessage(
            senderId = currentUserId,
            senderName = "Player", // Should get from auth
            message = message,
            teamOnly = teamOnly
        )
    }

    /**
     * Pause game (host only)
     */
    fun pauseGame(reason: String? = null) {
        if (!_uiState.value.isHost) return

        viewModelScope.launch {
            try {
                gameRepository.pauseGame(
                    groupId = currentGroupId,
                    sessionId = currentSessionId,
                    hostId = currentUserId,
                    reason = reason
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to pause game")
                }
            }
        }
    }

    /**
     * Resume game (host only)
     */
    fun resumeGame() {
        if (!_uiState.value.isHost) return

        viewModelScope.launch {
            try {
                gameRepository.resumeGame(
                    groupId = currentGroupId,
                    sessionId = currentSessionId,
                    hostId = currentUserId
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to resume game")
                }
            }
        }
    }

    /**
     * End game early (host only)
     */
    fun endGameEarly() {
        if (!_uiState.value.isHost) return

        viewModelScope.launch {
            try {
                gameRepository.endGame(
                    groupId = currentGroupId,
                    sessionId = currentSessionId,
                    hostId = currentUserId
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to end game")
                }
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Cleanup
     */
    override fun onCleared() {
        super.onCleared()
        stopTimer()
        gameRepository.cleanup()
    }
}