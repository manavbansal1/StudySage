package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.repository.GameRepository
import com.group_7.studysage.data.websocket.ConnectionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Game Lobby Screen
 * Manages:
 * - Fetching active game sessions
 * - Creating new game sessions
 * - Joining/leaving sessions
 * - Real-time lobby updates via WebSocket
 */
class GameLobbyViewModel(
    private val gameRepository: GameRepository = GameRepository()
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    // Current group ID
    private var currentGroupId: String = ""

    // Current user info
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    init {
        observeWebSocketUpdates()
    }

    /**
     * Set current user information
     */
    fun setUserInfo(userId: String, userName: String) {
        currentUserId = userId
        currentUserName = userName
    }

    /**
     * Set current group and load sessions
     */
    fun setGroup(groupId: String) {
        currentGroupId = groupId
        loadActiveSessions()
    }

    /**
     * Load active game sessions for the group
     */
    fun loadActiveSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = gameRepository.getActiveGameSessions(currentGroupId)

                if (response.success && response.data != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSessions = response.data,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSessions = emptyList(),
                            error = response.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sessions"
                    )
                }
            }
        }
    }

    /**
     * Create a new game session
     */
    fun createGameSession(
        documentId: String?,
        documentName: String?,
        gameType: GameType,
        settings: GameSettings
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            try {
                val response = gameRepository.createGameSession(
                    groupId = currentGroupId,
                    documentId = documentId,
                    documentName = documentName,
                    hostId = currentUserId,
                    hostName = currentUserName,
                    gameType = gameType,
                    settings = settings
                )

                if (response.success && response.data != null) {
                    val sessionId = response.data.gameSessionId

                    // Join the session immediately
                    joinSession(sessionId, isHost = true)

                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = response.message ?: "Failed to create session"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        error = e.message ?: "Failed to create session"
                    )
                }
            }
        }
    }

    /**
     * Join an existing game session
     */
    fun joinSession(sessionId: String, isHost: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }

            try {
                val response = gameRepository.joinGameSession(
                    groupId = currentGroupId,
                    sessionId = sessionId,
                    userId = currentUserId,
                    userName = currentUserName
                )

                if (response.success && response.data != null) {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            currentSession = response.data,
                            error = null
                        )
                    }

                    // Connect to WebSocket for real-time updates
                    connectToSession(sessionId)

                } else {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            error = response.message ?: "Failed to join session"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        error = e.message ?: "Failed to join session"
                    )
                }
            }
        }
    }

    /**
     * Leave current game session
     */
    fun leaveSession() {
        val sessionId = _uiState.value.currentSession?.id ?: return

        viewModelScope.launch {
            try {
                gameRepository.leaveGameSession(
                    groupId = currentGroupId,
                    sessionId = sessionId,
                    userId = currentUserId
                )

                // Disconnect WebSocket
                gameRepository.disconnectFromGame()

                _uiState.update {
                    it.copy(currentSession = null)
                }

                // Reload sessions list
                loadActiveSessions()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to leave session")
                }
            }
        }
    }

    /**
     * Connect to game session WebSocket
     */
    private fun connectToSession(sessionId: String) {
        gameRepository.connectToGameSession(
            groupId = currentGroupId,
            sessionId = sessionId,
            userId = currentUserId,
            userName = currentUserName
        )
    }

    /**
     * Send player ready status
     */
    fun setPlayerReady(isReady: Boolean) {
        gameRepository.sendPlayerReady(isReady)
    }

    /**
     * Start the game (host only)
     */
    fun startGame() {
        val sessionId = _uiState.value.currentSession?.id ?: return

        viewModelScope.launch {
            try {
                // Send via REST API
                gameRepository.startGame(
                    groupId = currentGroupId,
                    sessionId = sessionId,
                    hostId = currentUserId
                )

                // Also send via WebSocket for immediate response
                gameRepository.sendGameStarting()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to start game")
                }
            }
        }
    }

    /**
     * Observe WebSocket updates
     */
    private fun observeWebSocketUpdates() {
        // Room updates
        viewModelScope.launch {
            gameRepository.roomUpdate.collect { roomData ->
                roomData?.let {
                    _uiState.update { state ->
                        state.copy(currentSession = it)
                    }
                }
            }
        }

        // Player joined
        viewModelScope.launch {
            gameRepository.playerJoined.collect { data ->
                data?.let {
                    println("Player joined: ${it.player.name}, Total: ${it.totalPlayers}")
                }
            }
        }

        // Player left
        viewModelScope.launch {
            gameRepository.playerLeft.collect { playerId ->
                playerId?.let {
                    println("Player left: $it")
                }
            }
        }

        // Connection state
        viewModelScope.launch {
            gameRepository.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        println("✅ Connected to game session")
                    }
                    is ConnectionState.Disconnected -> {
                        println("❌ Disconnected from game session")
                    }
                    is ConnectionState.Error -> {
                        _uiState.update {
                            it.copy(error = state.message)
                        }
                    }
                    else -> {}
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
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        gameRepository.cleanup()
    }
}