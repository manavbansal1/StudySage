package com.group_7.studysage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.repository.GameRepository
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class QuizGenerationState(
    val isLoading: Boolean = false,
    val availableNotes: List<Note> = emptyList(),
    val selectedNote: Note? = null,
    val userPreferences: String = "",
    val generatedQuiz: Quiz? = null,
    val error: String? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val savedQuizId: String? = null
)

class GameViewModel(
    private val gameRepository: GameRepository = GameRepository(),
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _showGameActionOverlay = MutableStateFlow(false)
    val showGameActionOverlay: StateFlow<Boolean> = _showGameActionOverlay

    private val _selectedGameTitle = MutableStateFlow("")
    val selectedGameTitle: StateFlow<String> = _selectedGameTitle

    private val _quizGenerationState = MutableStateFlow(QuizGenerationState())
    val quizGenerationState: StateFlow<QuizGenerationState> = _quizGenerationState

    fun setShowGameActionOverlay(show: Boolean) {
        _showGameActionOverlay.value = show
    }

    fun setSelectedGameTitle(title: String) {
        _selectedGameTitle.value = title
    }

    /**
     * Load all notes available for quiz generation
     */
    fun loadAvailableNotes() {
        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(isLoading = true, error = null)
            
            try {
                val notes = notesRepository.getUserNotes()
                // Filter notes that have content
                val validNotes = notes.filter { it.content.isNotBlank() }
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    availableNotes = validNotes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading notes"
                )
            }
        }
    }

    /**
     * Set the selected note for quiz generation
     */
    fun setSelectedNote(note: Note) {
        _quizGenerationState.value = _quizGenerationState.value.copy(selectedNote = note)
    }

    /**
     * Update user preferences for quiz generation
     */
    fun setUserPreferences(preferences: String) {
        _quizGenerationState.value = _quizGenerationState.value.copy(userPreferences = preferences)
    }

    /**
     * Generate quiz from selected note and preferences
     */
    fun generateQuiz() {
        val state = _quizGenerationState.value
        val note = state.selectedNote
        
        if (note == null) {
            _quizGenerationState.value = state.copy(error = "Please select a note first")
            return
        }

        if (note.content.isBlank()) {
            _quizGenerationState.value = state.copy(error = "Selected note has no content")
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = state.copy(isGenerating = true, error = null)
            
            try {
                val result = gameRepository.generateQuizQuestions(
                    noteId = note.id,
                    noteTitle = note.title,
                    content = note.content,
                    userPreferences = state.userPreferences
                )
                
                result.fold(
                    onSuccess = { quiz ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            generatedQuiz = quiz,
                            isGenerating = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isGenerating = false,
                            error = error.message ?: "Failed to generate quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Error generating quiz"
                )
            }
        }
    }

    /**
     * Save generated quiz to Firestore
     */
    fun saveQuiz() {
        val quiz = _quizGenerationState.value.generatedQuiz
        
        if (quiz == null) {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                error = "No quiz to save"
            )
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                isSaving = true, 
                error = null
            )
            
            try {
                val result = gameRepository.saveQuizToFirestore(quiz)
                
                result.fold(
                    onSuccess = { quizId ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            savedQuizId = quizId,
                            isSaving = false
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Error saving quiz"
                )
            }
        }
    }

    /**
     * Get quiz as JSON string for backend submission
     */
    fun getQuizJson(): String? {
        val quiz = _quizGenerationState.value.generatedQuiz
        return quiz?.let { gameRepository.quizToJson(it) }
    }

    /**
     * Reset quiz generation state
     */
    fun resetQuizGeneration() {
        _quizGenerationState.value = QuizGenerationState()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _quizGenerationState.value = _quizGenerationState.value.copy(error = null)
    }
}
