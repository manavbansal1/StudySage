package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.repository.GameRepository
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class QuizGenerationState(
    val isLoading: Boolean = false,
    val availableNotes: List<Note> = emptyList(),
    val selectedNote: Note? = null,
    val userPreferences: String = "",
    val generatedQuiz: Quiz? = null,
    val error: String? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val savedQuizId: String? = null
)

data class StandaloneGameState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val gameCode: String? = null
)

class GameViewModel(
    private val gameApiService: GameApiService,
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel,
    private val gameRepository: GameRepository = GameRepository(),
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _lobbyUiState = MutableStateFlow(LobbyUiState())
    val lobbyUiState: StateFlow<LobbyUiState> = _lobbyUiState.asStateFlow()

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    private val _quizGenerationState = MutableStateFlow(QuizGenerationState())
    val quizGenerationState: StateFlow<QuizGenerationState> = _quizGenerationState

    init {
        observeWebSocket()
    }

    // ============================================
    // LOBBY ACTIONS
    // ============================================

    fun loadActiveSessions(groupId: String) {
        viewModelScope.launch {
            _lobbyUiState.value = _lobbyUiState.value.copy(isLoading = true)
            val response = gameApiService.getActiveGameSessions(groupId)
            if (response.success && response.data != null) {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isLoading = false,
                    activeSessions = response.data
                )
            } else {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isLoading = false,
                    error = response.message
                )
            }
        }
    }

    fun createGame(
        groupId: String,
        gameType: GameType,
        settings: GameSettings,
        documentId: String? = null,
        documentName: String? = null
    ) {
        viewModelScope.launch {
            _lobbyUiState.value = _lobbyUiState.value.copy(isCreating = true)
            val currentUser = authViewModel.currentUser.value
            if (currentUser == null) {
                _lobbyUiState.value = _lobbyUiState.value.copy(isCreating = false, error = "User not logged in")
                return@launch
            }

            val response = gameApiService.createGameSession(
                groupId = groupId,
                documentId = documentId,
                documentName = documentName,
                hostId = currentUser.uid,
                hostName = currentUser.displayName?.takeIf { it.isNotEmpty() } ?: "Unknown",
                gameType = gameType,
                settings = settings
            )

            if (response.success && response.data != null) {
                joinGame(groupId, response.data.gameSessionId)
            } else {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isCreating = false,
                    error = response.message
                )
            }
        }
    }

    fun joinGame(groupId: String, sessionId: String) {
        viewModelScope.launch {
            _lobbyUiState.value = _lobbyUiState.value.copy(isJoining = true)
            val currentUser = authViewModel.currentUser.value
            if (currentUser == null) {
                _lobbyUiState.value = _lobbyUiState.value.copy(isJoining = false, error = "User not logged in")
                return@launch
            }

            val response = gameApiService.joinGameSession(
                groupId = groupId,
                sessionId = sessionId,
                userId = currentUser.uid,
                userName = currentUser.displayName?.takeIf { it.isNotEmpty() } ?: "Unknown"
            )

            if (response.success && response.data != null) {
                _lobbyUiState.value = _lobbyUiState.value.copy(isJoining = false, currentSession = response.data)
                _gameUiState.value = _gameUiState.value.copy(currentSession = response.data, isHost = response.data.hostId == currentUser.uid)
                connectToWebSocket(groupId, sessionId)
            } else {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isJoining = false,
                    error = response.message
                )
            }
        }
    }

    // ============================================
    // GAMEPLAY ACTIONS
    // ============================================

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

    // ============================================
    // WEBSOCKET HANDLING
    // ============================================

    private fun connectToWebSocket(groupId: String, sessionId: String) {
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
                    _gameUiState.value = _gameUiState.value.copy(currentSession = it)
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.nextQuestion
            .onEach { questionData ->
                _gameUiState.value = _gameUiState.value.copy(
                    currentQuestion = questionData,
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = questionData?.timeLimit ?: 0
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

    // ============================================
    // STANDALONE GAME OPERATIONS (No Group Dependency)
    // ============================================

    private val _standaloneGameState = MutableStateFlow(StandaloneGameState())
    val standaloneGameState: StateFlow<StandaloneGameState> = _standaloneGameState.asStateFlow()

    /**
     * Host a new standalone game
     */
    fun hostGame(
        gameType: GameType,
        contentSource: ContentSource,
        contentData: String?,
        topicDescription: String?
    ) {
        viewModelScope.launch {
            _standaloneGameState.value = StandaloneGameState(isLoading = true)

            try {
                val currentUser = authViewModel.currentUser.value
                if (currentUser == null) {
                    _standaloneGameState.value = StandaloneGameState(error = "You must be logged in to host a game")
                    return@launch
                }

                // Get user's name from Firestore profile
                val userName = authViewModel.userProfile.value?.get("name") as? String
                    ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                    ?: "Player"

                val response = gameApiService.hostGame(
                    hostId = currentUser.uid,
                    hostName = userName,
                    gameType = gameType,
                    contentSource = contentSource,
                    contentData = contentData,
                    topicDescription = topicDescription
                )

                if (response.success && response.data != null) {
                    _standaloneGameState.value = StandaloneGameState(
                        isLoading = false,
                        gameCode = response.data.gameCode
                    )
                } else {
                    _standaloneGameState.value = StandaloneGameState(
                        isLoading = false,
                        error = response.message ?: "Failed to create game"
                    )
                }
            } catch (e: Exception) {
                _standaloneGameState.value = StandaloneGameState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Join a standalone game by code
     */
    fun joinGame(gameCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _standaloneGameState.value = StandaloneGameState(isLoading = true)

            try {
                val currentUser = authViewModel.currentUser.value
                if (currentUser == null) {
                    _standaloneGameState.value = StandaloneGameState(error = "You must be logged in to join a game")
                    onResult(false)
                    return@launch
                }

                val response = gameApiService.joinGameByCode(
                    gameCode = gameCode,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Player"
                )

                if (response.success && response.data != null) {
                    _standaloneGameState.value = StandaloneGameState(isLoading = false)
                    onResult(true)
                } else {
                    _standaloneGameState.value = StandaloneGameState(
                        isLoading = false,
                        error = response.message ?: "Failed to join game"
                    )
                    onResult(false)
                }
            } catch (e: Exception) {
                _standaloneGameState.value = StandaloneGameState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
                onResult(false)
            }
        }
    }

    /**
     * Clear standalone game state
     */
    fun clearStandaloneGameState() {
        _standaloneGameState.value = StandaloneGameState()
    }

    // ============================================
    // QUIZ GENERATION (for QuizGenerationScreen)
    // ============================================

    /**
     * Load all notes available for quiz generation
     */
    fun loadAvailableNotes() {
        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(isLoading = true, error = null)

            try {
                val notes = notesRepository.getUserNotes()
                // Filter notes that have content
                val validNotes = notes.filter { it.content.isNotBlank() }
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    availableNotes = validNotes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading notes"
                )
            }
        }
    }

    /**
     * Set the selected note for quiz generation
     */
    fun setSelectedNote(note: Note) {
        _quizGenerationState.value = _quizGenerationState.value.copy(selectedNote = note)
    }

    /**
     * Update user preferences for quiz generation
     */
    fun setUserPreferences(preferences: String) {
        _quizGenerationState.value = _quizGenerationState.value.copy(userPreferences = preferences)
    }

    /**
     * Generate quiz from selected note and preferences
     */
    fun generateQuiz() {
        val state = _quizGenerationState.value
        val note = state.selectedNote

        if (note == null) {
            _quizGenerationState.value = state.copy(error = "Please select a note first")
            return
        }

        if (note.content.isBlank()) {
            _quizGenerationState.value = state.copy(error = "Selected note has no content")
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = state.copy(isGenerating = true, error = null)

            try {
                val result = gameRepository.generateQuizQuestions(
                    noteId = note.id,
                    noteTitle = note.title,
                    content = note.content,
                    userPreferences = state.userPreferences
                )

                result.fold(
                    onSuccess = { quiz ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            generatedQuiz = quiz,
                            isGenerating = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isGenerating = false,
                            error = error.message ?: "Failed to generate quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Error generating quiz"
                )
            }
        }
    }

    /**
     * Save generated quiz to Firestore
     */
    fun saveQuiz() {
        val quiz = _quizGenerationState.value.generatedQuiz

        if (quiz == null) {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                error = "No quiz to save"
            )
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                isSaving = true,
                error = null
            )

            try {
                val result = gameRepository.saveQuizToFirestore(quiz)

                result.fold(
                    onSuccess = { quizId ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            savedQuizId = quizId,
                            isSaving = false
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Error saving quiz"
                )
            }
        }
    }

    /**
     * Get quiz as JSON string for backend submission
     */
    fun getQuizJson(): String? {
        val quiz = _quizGenerationState.value.generatedQuiz
        return quiz?.let { gameRepository.quizToJson(it) }
    }

    /**
     * Reset quiz generation state
     */
    fun resetQuizGeneration() {
        _quizGenerationState.value = QuizGenerationState()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _quizGenerationState.value = _quizGenerationState.value.copy(error = null)
    }
}