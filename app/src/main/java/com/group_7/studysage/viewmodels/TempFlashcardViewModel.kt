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
    val showSuccessMessage: Boolean = false,
    val currentCardIndex: Int = 0,
    val flipStates: Map<Int, Boolean> = emptyMap()
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
    
    fun setCurrentCardIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentCardIndex = index)
    }

    fun toggleFlipState(index: Int) {
        val currentFlipStates = _uiState.value.flipStates.toMutableMap()
        val currentState = currentFlipStates[index] ?: false
        currentFlipStates[index] = !currentState
        _uiState.value = _uiState.value.copy(flipStates = currentFlipStates)
    }

    fun clearState() {
        _uiState.value = TempFlashcardUiState()
    }
}
