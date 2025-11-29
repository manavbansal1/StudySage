package com.group_7.studysage.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.FlashcardRepository
import com.group_7.studysage.ui.screens.Flashcards.Flashcard
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FlashcardViewModel(
    private val repository: FlashcardRepository = FlashcardRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "FlashcardViewModel"
    }

    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val flashcards: StateFlow<List<Flashcard>> = _flashcards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _generationProgress = MutableStateFlow(0)
    val generationProgress: StateFlow<Int> = _generationProgress.asStateFlow()

    /**
     * Load flashcards for a note from Firestore
     */
    fun loadFlashcardsForNote(noteId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val result = repository.loadFlashcardsForNote(noteId)
                
                result.onSuccess { flashcards ->
                    _flashcards.value = flashcards
                    _isLoading.value = false
                    Log.d(TAG, "Loaded ${flashcards.size} flashcards for note: $noteId")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load flashcards", error)
                    _flashcards.value = emptyList()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading flashcards", e)
                _flashcards.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate flashcards using AI or fallback to Firebase note data
     */
    fun generateFlashcards(
        noteId: String,
        noteContent: String,
        numberOfCards: Int = 10,
        difficulty: String = "medium"
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _generationProgress.value = 0

                Log.d(TAG, "Starting flashcard generation for noteId: $noteId")
                Log.d(TAG, "Number of cards: $numberOfCards, Difficulty: $difficulty")

                withTimeout(30000L) { // Increased timeout to 30 seconds
                    if (noteContent.isBlank()) {
                        Log.d(TAG, "Content is blank, attempting to generate from Firebase note data.")
                        generateFromFirebaseNote(noteId, numberOfCards, difficulty)
                        return@withTimeout
                    }

                    // Try to generate with AI first
                    val result = repository.generateFlashcardsWithAI(
                        noteContent = noteContent,
                        numberOfCards = numberOfCards,
                        difficulty = difficulty,
                        onProgress = { progress ->
                            _generationProgress.value = progress
                        }
                    )

                    result.onSuccess { flashcardsData ->
                        val generatedFlashcards = flashcardsData.mapIndexed { index, card ->
                            Flashcard(
                                id = "${noteId}_${System.currentTimeMillis()}_$index",
                                question = card.question,
                                answer = card.answer,
                                difficulty = difficulty
                            )
                        }

                        _generationProgress.value = 100
                        _flashcards.value = generatedFlashcards
                        _isLoading.value = false
                        _errorMessage.value = null
                        Log.d(TAG, "Successfully generated ${generatedFlashcards.size} flashcards")
                        
                        // Save flashcards to Firestore
                        saveFlashcardsToFirestore(noteId, generatedFlashcards)
                    }.onFailure { error ->
                        // If AI fails, throw and catch below to trigger fallback
                        throw error
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Flashcard generation timed out", e)
                _errorMessage.value = "Generation timed out (30s limit). Please check your internet connection."
                _isLoading.value = false
                _generationProgress.value = 0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate flashcards with AI (Attempting fallback)", e)
                _errorMessage.value = "AI generation failed. Attempting to use existing note data..."
                _isLoading.value = false
                _generationProgress.value = 0

                // Try Firebase fallback
                try {
                    _isLoading.value = true
                    _errorMessage.value = null
                    generateFromFirebaseNote(noteId, numberOfCards, difficulty)
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Fallback also failed", fallbackError)
                    _errorMessage.value = "Failed to generate flashcards: Please ensure your note has content or key points."
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Generate flashcards from existing Firebase note data (fallback method)
     */
    private suspend fun generateFromFirebaseNote(
        noteId: String,
        numberOfCards: Int,
        difficulty: String = "medium"
    ) {
        try {
            val result = repository.generateFlashcardsFromNote(
                noteId = noteId,
                numberOfCards = numberOfCards,
                difficulty = difficulty,
                onProgress = { progress ->
                    _generationProgress.value = progress
                }
            )

            result.onSuccess { flashcards ->
                _flashcards.value = flashcards.mapIndexed { index, card ->
                    card.copy(id = "${noteId}_fallback_${System.currentTimeMillis()}_$index")
                }
                _isLoading.value = false
                _errorMessage.value = null
                Log.d(TAG, "Successfully generated ${flashcards.size} flashcards from Firebase data")
            }.onFailure { error ->
                _errorMessage.value = "Failed to load note data: ${error.message}"
                _isLoading.value = false
                _generationProgress.value = 0
                throw error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate from Firebase", e)
            _errorMessage.value = "Failed to load note data: ${e.message}"
            _isLoading.value = false
            _generationProgress.value = 0
            throw e
        }
    }

    /**
     * Shuffle the current flashcards
     */
    fun shuffleFlashcards() {
        _flashcards.value = _flashcards.value.shuffled()
    }

    /**
     * Save flashcards to Firestore in the background
     */
    private fun saveFlashcardsToFirestore(noteId: String, flashcards: List<Flashcard>) {
        viewModelScope.launch {
            try {
                val result = repository.saveFlashcards(noteId, flashcards)
                result.onSuccess {
                    Log.d(TAG, "Successfully saved ${flashcards.size} flashcards to Firestore")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to save flashcards to Firestore", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving flashcards", e)
            }
        }
    }

}