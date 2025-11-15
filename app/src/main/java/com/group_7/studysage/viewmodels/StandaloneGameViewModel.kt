package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.models.ContentSource
import com.group_7.studysage.data.models.GameType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StandaloneGameUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val gameCode: String? = null
)

class StandaloneGameViewModel : ViewModel() {
    private val apiService = GameApiService()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(StandaloneGameUiState())
    val uiState: StateFlow<StandaloneGameUiState> = _uiState.asStateFlow()

    /**
     * Host a new game
     */
    fun hostGame(
        gameType: GameType,
        contentSource: ContentSource,
        contentData: String?,
        topicDescription: String?
    ) {
        viewModelScope.launch {
            _uiState.value = StandaloneGameUiState(isLoading = true)

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = StandaloneGameUiState(error = "You must be logged in to host a game")
                    return@launch
                }

                val response = apiService.hostGame(
                    hostId = currentUser.uid,
                    hostName = currentUser.displayName ?: "Player",
                    gameType = gameType,
                    contentSource = contentSource,
                    contentData = contentData,
                    topicDescription = topicDescription
                )

                if (response.success && response.data != null) {
                    _uiState.value = StandaloneGameUiState(
                        isLoading = false,
                        gameCode = response.data.gameCode
                    )
                } else {
                    _uiState.value = StandaloneGameUiState(
                        isLoading = false,
                        error = response.message ?: "Failed to create game"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = StandaloneGameUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Join a game by code
     */
    fun joinGame(gameCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = StandaloneGameUiState(isLoading = true)

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = StandaloneGameUiState(error = "You must be logged in to join a game")
                    onResult(false)
                    return@launch
                }

                val response = apiService.joinGameByCode(
                    gameCode = gameCode,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Player"
                )

                if (response.success && response.data != null) {
                    _uiState.value = StandaloneGameUiState(isLoading = false)
                    onResult(true)
                } else {
                    _uiState.value = StandaloneGameUiState(
                        isLoading = false,
                        error = response.message ?: "Failed to join game"
                    )
                    onResult(false)
                }
            } catch (e: Exception) {
                _uiState.value = StandaloneGameUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
                onResult(false)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear game code
     */
    fun clearGameCode() {
        _uiState.value = _uiState.value.copy(gameCode = null)
    }
}
