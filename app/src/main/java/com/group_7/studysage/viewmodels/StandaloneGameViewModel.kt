package com.group_7.studysage.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.models.ContentSource
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.utils.CloudinaryUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StandaloneGameUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val gameCode: String? = null,
    val uploadProgress: String? = null
)

class StandaloneGameViewModel : ViewModel() {
    private val apiService = GameApiService()
    private val auth = FirebaseAuth.getInstance()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(StandaloneGameUiState())
    val uiState: StateFlow<StandaloneGameUiState> = _uiState.asStateFlow()

    /**
     * Host a new game
     */
    fun hostGame(
        context: Context,
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

                // Handle PDF upload to Cloudinary if contentSource is PDF
                val finalContentData = if (contentSource == ContentSource.PDF && contentData != null) {
                    _uiState.value = StandaloneGameUiState(
                        isLoading = true,
                        uploadProgress = "Uploading PDF to cloud..."
                    )

                    val pdfUri = Uri.parse(contentData)
                    val cloudinaryUrl = CloudinaryUploader.uploadPdfForGame(context, pdfUri)

                    if (cloudinaryUrl == null) {
                        _uiState.value = StandaloneGameUiState(
                            isLoading = false,
                            error = "Failed to upload PDF. Please try again."
                        )
                        return@launch
                    }

                    _uiState.value = StandaloneGameUiState(
                        isLoading = true,
                        uploadProgress = "Creating game session..."
                    )

                    cloudinaryUrl
                } else {
                    contentData
                }

                // Fetch user's name from Firestore
                val userProfile = authRepository.getUserProfile()
                val userName = userProfile?.get("name") as? String ?: "Player"

                val response = apiService.hostGame(
                    hostId = currentUser.uid,
                    hostName = userName,
                    gameType = gameType,
                    contentSource = contentSource,
                    contentData = finalContentData,
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

                // Fetch user's name from Firestore
                val userProfile = authRepository.getUserProfile()
                val userName = userProfile?.get("name") as? String ?: "Player"

                val response = apiService.joinGameByCode(
                    gameCode = gameCode,
                    userId = currentUser.uid,
                    userName = userName
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

    /**
     * Refresh the game screen
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            // Add a small delay to show the refresh animation
            kotlinx.coroutines.delay(1000)
            
            // Clear any existing game code and errors
            _uiState.value = _uiState.value.copy(
                gameCode = null,
                error = null,
                isRefreshing = false
            )
        }
    }
}
