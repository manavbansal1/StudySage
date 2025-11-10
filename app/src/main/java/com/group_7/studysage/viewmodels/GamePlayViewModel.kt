import androidx.compose.runtime.collectAsState
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
