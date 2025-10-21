package com.group_7.studysage.ui.screens.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.utils.CloudinaryUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: Map<String, Any>? = null,
    val isLoading: Boolean = false,
    val isUploadingImage: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = authRepository.getUserProfile()

            _uiState.update {
                it.copy(
                    userProfile = profile,
                    isLoading = false,
                    error = if (profile == null) "Failed to load profile" else null
                )
            }
        }
    }

    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val updates = mapOf(
                "name" to name,
                "bio" to bio
            )

            val result = authRepository.updateUserProfile(updates)

            result.onSuccess {
                loadUserProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Profile updated successfully"
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to update profile: ${exception.message}"
                    )
                }
            }
        }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }

            try {
                // Upload to Cloudinary
                val imageUrl = CloudinaryUploader.uploadImage(context, imageUri)

                if (imageUrl != null) {
                    // Update Firebase with the new image URL
                    val result = authRepository.updateProfileImage(imageUrl)

                    result.onSuccess {
                        loadUserProfile()
                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                message = "Profile picture updated successfully"
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                error = "Failed to update profile picture: ${exception.message}"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            error = "Failed to upload image to cloud"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingImage = false,
                        error = "Error uploading image: ${e.message}"
                    )
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update { ProfileUiState() }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}