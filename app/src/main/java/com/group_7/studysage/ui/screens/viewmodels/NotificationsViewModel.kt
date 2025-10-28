package com.group_7.studysage.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class NotificationsViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationSettings()
    }

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val enabled = authRepository.getNotificationsEnabled()

            _uiState.update {
                it.copy(
                    notificationsEnabled = enabled,
                    isLoading = false
                )
            }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = authRepository.updateNotificationsEnabled(enabled)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        notificationsEnabled = enabled,
                        isLoading = false,
                        message = "Notification settings updated"
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

