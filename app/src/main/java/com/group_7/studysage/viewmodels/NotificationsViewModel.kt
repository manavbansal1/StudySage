package com.group_7.studysage.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.utils.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * NotificationsUiState
 *
 * Data class representing the UI state for notification settings.
 *
 * @param notificationsEnabled Whether notifications are enabled
 * @param dailyReminderTime Time for daily reminders in HH:mm format
 * @param isLoading Loading state for fetching settings
 * @param isSaving Saving state for updating settings
 * @param error Error message if any operation fails
 * @param message Success message to display to user
 */
data class NotificationsUiState(
    val notificationsEnabled: Boolean = false,
    val dailyReminderTime: String = "18:00",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * NotificationsViewModel
 *
 * ViewModel for managing notification settings.
 * Handles loading and updating notification preferences from Firestore.
 *
 * @param authRepository Repository for authentication and user settings
 */
class NotificationsViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationsViewModel"
    }

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "NotificationsViewModel initialized")
        loadNotificationSettings()
    }

    /**
     * Load notification settings from Firestore.
     * Updates UI state with fetched values or error.
     */
    fun loadNotificationSettings() {
        viewModelScope.launch {
            Log.d(TAG, "Loading notification settings")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val enabled = authRepository.getNotificationsEnabled()
                val time = authRepository.getReminderTime()

                Log.d(TAG, "Settings loaded - Enabled: $enabled, Time: $time")

                _uiState.update {
                    it.copy(
                        notificationsEnabled = enabled,
                        dailyReminderTime = time,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification settings", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load settings: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update notifications enabled/disabled setting.
     * Also schedules or cancels WorkManager reminders based on the setting.
     *
     * @param context Application context for WorkManager
     * @param enabled Whether notifications should be enabled
     */
    fun updateNotificationsEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Updating notifications enabled: $enabled")
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }

            authRepository.updateNotificationsEnabled(enabled).onSuccess {
                try {
                    // Schedule or cancel reminders based on enabled state
                    if (enabled) {
                        Log.d(TAG, "Scheduling daily reminders at ${_uiState.value.dailyReminderTime}")
                        ReminderScheduler.scheduleDailyReminder(
                            context,
                            _uiState.value.dailyReminderTime
                        )
                    } else {
                        Log.d(TAG, "Canceling daily reminders")
                        ReminderScheduler.cancelDailyReminder(context)
                    }

                    Log.d(TAG, "Notifications enabled updated successfully")
                    _uiState.update {
                        it.copy(
                            notificationsEnabled = enabled,
                            isSaving = false,
                            message = "Settings updated"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling/canceling reminders", e)
                    _uiState.update {
                        it.copy(
                            notificationsEnabled = enabled,
                            isSaving = false,
                            error = "Settings updated but reminder scheduling failed: ${e.message}"
                        )
                    }
                }
            }.onFailure { exception ->
                Log.e(TAG, "Error updating notifications enabled", exception)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message
                    )
                }
            }
        }
    }

    /**
     * Update daily reminder time.
     * If notifications are enabled, reschedules reminders with the new time.
     *
     * @param context Application context for WorkManager
     * @param time Time in HH:mm format (e.g., "18:00")
     */
    fun updateReminderTime(context: Context, time: String) {
        viewModelScope.launch {
            Log.d(TAG, "Updating reminder time: $time")
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }

            authRepository.updateReminderTime(time).onSuccess {
                try {
                    // Reschedule with new time if notifications are enabled
                    if (_uiState.value.notificationsEnabled) {
                        Log.d(TAG, "Rescheduling reminders with new time: $time")
                        ReminderScheduler.cancelDailyReminder(context)
                        ReminderScheduler.scheduleDailyReminder(context, time)
                    } else {
                        Log.d(TAG, "Notifications disabled, skipping reschedule")
                    }

                    Log.d(TAG, "Reminder time updated successfully")
                    _uiState.update {
                        it.copy(
                            dailyReminderTime = time,
                            isSaving = false,
                            message = "Reminder time updated"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders", e)
                    _uiState.update {
                        it.copy(
                            dailyReminderTime = time,
                            isSaving = false,
                            error = "Time updated but rescheduling failed: ${e.message}"
                        )
                    }
                }
            }.onFailure { exception ->
                Log.e(TAG, "Error updating reminder time", exception)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message
                    )
                }
            }
        }
    }

    /**
     * Clear any displayed message or error.
     * Used after user acknowledges the message.
     */
    fun clearMessage() {
        Log.d(TAG, "Clearing messages")
        _uiState.update { it.copy(message = null, error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "NotificationsViewModel cleared")
    }
}

