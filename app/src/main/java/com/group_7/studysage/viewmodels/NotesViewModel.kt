package com.group_7.studysage.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.utils.FileDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "NotesViewModel"
    }

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNoteDetailsLoading = MutableStateFlow(false)
    val isNoteDetailsLoading: StateFlow<Boolean> = _isNoteDetailsLoading.asStateFlow()

    init {
        // Initial load is handled by LaunchedEffect in the UI, which can provide courseId
    }

    fun loadNotes(courseId: String? = null) {
        Log.d(TAG, "Loading notes${courseId?.let { " for course $it" } ?: ""}")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userNotes = notesRepository.getUserNotes(courseId)
                _notes.value = userNotes
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load notes: ${e.message}"
                Log.e(TAG, "Failed to load notes: ${e.message}", e)
            }

            _isLoading.value = false
        }
    }

    fun setErrorMessage(message: String) {
        Log.d(TAG, "Setting error message: $message")
        _errorMessage.value = message
    }

    fun selectNote(note: Note) {
        Log.d(TAG, "Selecting note ${note.id}")
        _selectedNote.value = note
    }

    fun clearSelectedNote() {
        Log.d(TAG, "Clearing selected note")
        _selectedNote.value = null
    }

    fun loadNoteById(noteId: String) {
        if (noteId.isBlank()) return

        Log.d(TAG, "Loading note by id=$noteId")
        viewModelScope.launch {
            _isNoteDetailsLoading.value = true
            try {
                val note = notesRepository.getNoteById(noteId)
                if (note != null) {
                    _selectedNote.value = note
                    _errorMessage.value = null
                    Log.d(TAG, "Note loaded for id=$noteId")
                } else {
                    _errorMessage.value = "Note details not available."
                    Log.e(TAG, "No note found for id=$noteId")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load note: ${e.message}"
                Log.e(TAG, "Failed to load note: ${e.message}", e)
            } finally {
                _isNoteDetailsLoading.value = false
            }
        }
    }

    fun deleteNote(noteId: String) {
        Log.d(TAG, "Deleting note id=$noteId")
        viewModelScope.launch {
            _isLoading.value = true

            notesRepository.deleteNote(noteId)
                .onSuccess {
                    loadNotes() // Refresh the list
                    _selectedNote.value = null // Clear selection if deleted note was selected
                    Log.d(TAG, "Note deleted id=$noteId")
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to delete note: ${exception.message}"
                    Log.e(TAG, "Failed to delete note: ${exception.message}", exception)
                }

            _isLoading.value = false
        }
    }

    fun uploadNote(
        context: Context,
        uri: Uri,
        fileName: String,
        courseId: String?,
        onUploadProgress: (String) -> Unit,
        onUploadComplete: () -> Unit
    ) {
        Log.d(TAG, "Uploading note $fileName for courseId=$courseId")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                notesRepository.uploadNotesAndProcess(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    courseId = courseId,
                    onProgress = onUploadProgress
                )
                    .onSuccess { newNote ->
                        _notes.value = listOf(newNote) + _notes.value // Add new note to the top
                        _errorMessage.value = null
                        onUploadComplete()
                        Log.d(TAG, "Note upload succeeded id=${newNote.id}")
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Upload failed: ${exception.message}"
                        onUploadComplete()
                        Log.e(TAG, "Note upload failed: ${exception.message}", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.message}"
                onUploadComplete()
                Log.e(TAG, "Unexpected upload failure: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }

    fun downloadNote(context: Context, note: Note) {
        Log.d(TAG, "Downloading note ${note.id}")
        if (note.fileUrl.isNotBlank()) {
            FileDownloader.downloadFile(context, note.fileUrl, note.originalFileName)
        } else {
            _errorMessage.value = "File URL is not available."
            Log.e(TAG, "Download failed: missing file URL for note ${note.id}")
        }
    }

    fun searchNotes(query: String) {
        Log.d(TAG, "Searching notes with query=$query")
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
                Log.e(TAG, "Note search failed: ${e.message}", e)
            }
        }
    }

    fun clearError() {
        Log.d(TAG, "Clearing note errors")
        _errorMessage.value = null
    }
}
