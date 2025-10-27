package com.group_7.studysage.ui.screens.Courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userNotes = notesRepository.getUserNotes()
                _notes.value = userNotes
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load notes: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun selectNote(note: Note) {
        _selectedNote.value = note
    }

    fun clearSelectedNote() {
        _selectedNote.value = null
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            notesRepository.deleteNote(noteId)
                .onSuccess {
                    loadNotes() // Refresh the list
                    _selectedNote.value = null // Clear selection if deleted note was selected
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to delete note: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun searchNotes(query: String) {
        if (query.isBlank()) {
            loadNotes()
            return
        }

        viewModelScope.launch {
            try {
                val allNotes = notesRepository.getUserNotes()
                val filteredNotes = allNotes.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.summary.contains(query, ignoreCase = true) ||
                            note.tags.any { it.contains(query, ignoreCase = true) } ||
                            note.keyPoints.any { it.contains(query, ignoreCase = true) }
                }
                _notes.value = filteredNotes
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}