import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameApiService: GameApiService,
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _lobbyUiState = MutableStateFlow(LobbyUiState())
    val lobbyUiState: StateFlow<LobbyUiState> = _lobbyUiState.asStateFlow()

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

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
                hostName = currentUser.displayName ?: "Unknown",
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
                userName = currentUser.displayName ?: "Unknown"
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
}
