package com.group_7.studysage.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import java.io.InputStream

class HomeViewModel(
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _uploadStatus = mutableStateOf<String?>(null)
    val uploadStatus: State<String?> = _uploadStatus

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage


    init {

    }

    fun uploadAndProcessNote(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _uploadStatus.value = "Uploading file..."

            try {
                // Read file content
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val fileContent = inputStream?.readBytes()

                if (fileContent == null) {
                    _errorMessage.value = "Could not read file"
                    _isLoading.value = false
                    return@launch
                }

                _uploadStatus.value = "Processing with AI..."



            } catch (e: Exception) {
                _errorMessage.value = "Error processing file: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _uploadStatus.value = null
        _errorMessage.value = null
    }
}