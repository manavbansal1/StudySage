package com.group_7.studysage.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State

class HomeViewModel : ViewModel() {

    private val _uploadStatus = mutableStateOf<String?>(null)
    val uploadStatus: State<String?> = _uploadStatus

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun updateUploadStatus(status: String) {
        _uploadStatus.value = status
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun clearUploadStatus() {
        _uploadStatus.value = null
    }

    // Future functions for file processing
    fun uploadFile(uri: android.net.Uri) {
        _isLoading.value = true
        // TODO: Implement Firebase upload
        // TODO: Send to Gemini AI for processing
        _isLoading.value = false
    }
}