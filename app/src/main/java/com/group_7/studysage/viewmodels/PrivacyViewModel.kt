package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val profileVisibility: String = "everyone",  // "everyone" | "groups" | "private"
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class PrivacyViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        loadPrivacySettings()
    }

    private fun loadPrivacySettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val visibility = authRepository.getProfileVisibility()

            _uiState.update {
                it.copy(
                    profileVisibility = visibility,
                    isLoading = false
                )
            }
        }
    }

    fun updateProfileVisibility(visibility: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = authRepository.updateProfileVisibility(visibility)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        profileVisibility = visibility,
                        isLoading = false,
                        message = "Privacy settings updated"
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

