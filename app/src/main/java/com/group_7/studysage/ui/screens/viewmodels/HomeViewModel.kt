package com.group_7.studysage.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.data.repository.Note

class HomeViewModel(
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _uploadStatus = mutableStateOf<String?>(null)
    val uploadStatus: State<String?> = _uploadStatus

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _processedNote = mutableStateOf<Note?>(null)
    val processedNote: State<Note?> = _processedNote

    private val _recentNotes = mutableStateOf<List<Note>>(emptyList())
    val recentNotes: State<List<Note>> = _recentNotes

    private val _testResult = mutableStateOf<String?>(null)
    val testResult: State<String?> = _testResult

    init {
        loadRecentNotes()
    }

//    fun testFirebaseConnection() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _errorMessage.value = null
//
//            try {
//                val result = notesRepository.testFirebaseConnection()
//                result.onSuccess { message ->
//                    _testResult.value = "✅ $message"
//                }.onFailure { exception ->
//                    _testResult.value = "❌ Test failed: ${exception.message}"
//                }
//            } catch (e: Exception) {
//                _testResult.value = "❌ Test error: ${e.message}"
//            }
//
//            _isLoading.value = false
//        }
//    }

//    fun testGeminiConnection() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _errorMessage.value = null
//
//            try {
//                val result = notesRepository.testGeminiConnection()
//                result.onSuccess { message ->
//                    _testResult.value = message
//                }.onFailure { exception ->
//                    _testResult.value = exception.message ?: "❌ Gemini test failed"
//                }
//            } catch (e: Exception) {
//                _testResult.value = "❌ Gemini test error: ${e.message}"
//            }
//
//            _isLoading.value = false
//        }
//    }

    fun uploadAndProcessNote(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            clearMessages()

            try {
                val result = notesRepository.uploadNotesAndProcess(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    onProgress = { status ->
                        _uploadStatus.value = status
                    }
                )

                result.onSuccess { note ->
                    _processedNote.value = note
                    _uploadStatus.value = "Document processed successfully!"
                    loadRecentNotes() // Refresh the notes list
                }.onFailure { exception ->
                    _errorMessage.value = "Error processing file: ${exception.message}"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error processing file: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    private fun loadRecentNotes() {
        viewModelScope.launch {
            try {
                val notes = notesRepository.getUserNotes()
                _recentNotes.value = notes.take(5) // Show only last 5 notes
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    fun clearMessages() {
        _uploadStatus.value = null
        _errorMessage.value = null
    }

    fun clearProcessedNote() {
        _processedNote.value = null
    }
}