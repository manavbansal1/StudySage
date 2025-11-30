package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for TempFlashcardsGenerationScreen to handle rotation and state persistence
 */
data class TempFlashcardUiState(
    val selectedPdfUri: String? = null,
    val selectedFileName: String = "",
    val flashcardPreferences: String = "",
    val numberOfCards: Int = 10,
    val showFlashcardStudyScreen: Boolean = false,
    val showSuccessMessage: Boolean = false
)

class TempFlashcardViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(TempFlashcardUiState())
    val uiState: StateFlow<TempFlashcardUiState> = _uiState.asStateFlow()
    
    fun setSelectedPdf(uri: String, fileName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPdfUri = uri,
            selectedFileName = fileName
        )
    }
    
    fun setFlashcardPreferences(preferences: String) {
        _uiState.value = _uiState.value.copy(flashcardPreferences = preferences)
    }
    
    fun setNumberOfCards(count: Int) {
        _uiState.value = _uiState.value.copy(numberOfCards = count)
    }
    
    fun setShowFlashcardStudyScreen(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFlashcardStudyScreen = show)
    }
    
    fun setShowSuccessMessage(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSuccessMessage = show)
    }
    
    fun clearState() {
        _uiState.value = TempFlashcardUiState()
    }
}
