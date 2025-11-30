package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for TempQuizGenerationScreen to handle rotation and state persistence
 */
data class TempQuizUiState(
    val selectedPdfUri: String? = null,
    val selectedFileName: String = "",
    val quizPreferences: String = "",
    val showPlayQuizScreen: Boolean = false,
    val showSuccessMessage: Boolean = false
)

class TempQuizViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(TempQuizUiState())
    val uiState: StateFlow<TempQuizUiState> = _uiState.asStateFlow()
    
    fun setSelectedPdf(uri: String, fileName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPdfUri = uri,
            selectedFileName = fileName
        )
    }
    
    fun setQuizPreferences(preferences: String) {
        _uiState.value = _uiState.value.copy(quizPreferences = preferences)
    }
    
    fun setShowPlayQuizScreen(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPlayQuizScreen = show)
    }
    
    fun setShowSuccessMessage(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSuccessMessage = show)
    }
    
    fun clearState() {
        _uiState.value = TempQuizUiState()
    }
}
