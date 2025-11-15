package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.models.GameSettings
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.data.models.LobbyUiState
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameLobbyViewModel(
    private val gameApiService: GameApiService,
    private val authViewModel: AuthViewModel,
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _lobbyUiState = MutableStateFlow(LobbyUiState())
    val lobbyUiState: StateFlow<LobbyUiState> = _lobbyUiState.asStateFlow()

    private val _availableNotes = MutableStateFlow<List<Note>>(emptyList())
    val availableNotes: StateFlow<List<Note>> = _availableNotes.asStateFlow()

    private val _isLoadingNotes = MutableStateFlow(false)
    val isLoadingNotes: StateFlow<Boolean> = _isLoadingNotes.asStateFlow()

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

    fun loadAvailableNotes() {
        viewModelScope.launch {
            _isLoadingNotes.value = true
            try {
                val notes = notesRepository.getUserNotes()
                _availableNotes.value = notes.filter { it.content.isNotBlank() }
            } catch (e: Exception) {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    error = "Failed to load notes: ${e.message}"
                )
            } finally {
                _isLoadingNotes.value = false
            }
        }
    }

    fun createGame(
        groupId: String,
        gameType: GameType,
        settings: GameSettings,
        documentId: String? = null,
        documentName: String? = null,
        onGameCreated: (String) -> Unit
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
                onGameCreated(response.data.gameSessionId)
            } else {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isCreating = false,
                    error = response.message
                )
            }
        }
    }

    fun joinGame(groupId: String, sessionId: String, onJoined: () -> Unit) {
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
                onJoined()
            } else {
                _lobbyUiState.value = _lobbyUiState.value.copy(
                    isJoining = false,
                    error = response.message
                )
            }
        }
    }
}