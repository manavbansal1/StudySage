package com.group_7.studysage.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.api.CanvasCourse
import com.group_7.studysage.data.repository.CanvasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CanvasUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val courses: List<CanvasCourse> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastSyncTime: Long? = null
)

class CanvasViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "CanvasViewModel"
    }
    
    private val repository = CanvasRepository()
    
    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()
    
    init {
        checkCanvasConnection()
    }
    
    /**
     * Check if Canvas is already connected
     */
    private fun checkCanvasConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val tokenResult = repository.getCanvasToken()
            tokenResult.onSuccess { token ->
                if (token != null) {
                    // Validate the token
                    val validResult = repository.validateCanvasToken(token)
                    validResult.onSuccess { isValid ->
                        _uiState.value = _uiState.value.copy(
                            isConnected = isValid,
                            isLoading = false
                        )
                        
                        if (isValid) {
                            loadCanvasCourses()
                        }
                    }.onFailure {
                        _uiState.value = _uiState.value.copy(
                            isConnected = false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnected = false,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Save Canvas access token and sync courses
     */
    fun connectCanvas(accessToken: String, semester: String = "Fall", year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            Log.d(TAG, "Connecting to Canvas...")
            
            // Save token
            val saveResult = repository.saveCanvasToken(accessToken)
            
            saveResult.onSuccess {
                Log.d(TAG, "Token saved, syncing courses...")
                
                // Sync courses with semester and year
                val syncResult = repository.syncCanvasCourses(accessToken, semester, year)
                
                syncResult.onSuccess { courses ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConnected = true,
                        courses = courses,
                        successMessage = "Successfully synced ${courses.size} courses from Canvas!",
                        lastSyncTime = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Successfully synced ${courses.size} courses")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to sync courses: ${error.message}"
                    )
                    Log.e(TAG, "Failed to sync courses", error)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save Canvas token: ${error.message}"
                )
                Log.e(TAG, "Failed to save token", error)
            }
        }
    }
    
    /**
     * Load Canvas courses from Firestore
     */
    private fun loadCanvasCourses() {
        viewModelScope.launch {
            val result = repository.getCanvasCourses()
            
            result.onSuccess { coursesData ->
                val courses = coursesData.mapNotNull { data ->
                    try {
                        CanvasCourse(
                            id = (data["canvasCourseId"] as? Number)?.toLong() ?: 0L,
                            name = data["name"] as? String ?: "",
                            course_code = data["courseCode"] as? String,
                            workflow_state = data["workflowState"] as? String
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _uiState.value = _uiState.value.copy(courses = courses)
            }
        }
    }
    
    /**
     * Refresh Canvas courses
     */
    fun refreshCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val tokenResult = repository.getCanvasToken()
            
            tokenResult.onSuccess { token ->
                if (token != null) {
                    val syncResult = repository.syncCanvasCourses(token)
                    
                    syncResult.onSuccess { courses ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            courses = courses,
                            successMessage = "Courses refreshed successfully!",
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to refresh: ${error.message}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No Canvas token found"
                    )
                }
            }
        }
    }
    
    /**
     * Disconnect Canvas
     */
    fun disconnectCanvas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = repository.disconnectCanvas()
            
            result.onSuccess {
                _uiState.value = CanvasUiState(
                    isConnected = false,
                    successMessage = "Canvas disconnected successfully"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to disconnect: ${error.message}"
                )
            }
        }
    }
    
    /**
     * Fetch courses from Canvas without syncing
     */
    fun fetchCanvasCourses(accessToken: String, semester: String = "Fall", year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            Log.d(TAG, "Fetching Canvas courses...")
            
            // Save token first
            val saveResult = repository.saveCanvasToken(accessToken)
            
            saveResult.onSuccess {
                // Just fetch courses from API without syncing to Firestore
                val apiService = com.group_7.studysage.data.api.CanvasApiService()
                val coursesResult = apiService.getUserCourses(accessToken)
                
                coursesResult.onSuccess { courses ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConnected = true,
                        courses = courses,
                        successMessage = "Found ${courses.size} courses. Select the ones you want to sync.",
                        lastSyncTime = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Successfully fetched ${courses.size} courses")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch courses: ${error.message}"
                    )
                    Log.e(TAG, "Failed to fetch courses", error)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save Canvas token: ${error.message}"
                )
                Log.e(TAG, "Failed to save token", error)
            }
        }
    }
    
    /**
     * Sync selected courses to Firestore
     */
    fun syncSelectedCourses(courseIds: List<String>, semester: String = "Fall", year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            Log.d(TAG, "Syncing selected courses...")
            
            val selectedCourses = _uiState.value.courses.filter { courseIds.contains(it.id.toString()) }
            
            if (selectedCourses.isNotEmpty()) {
                val syncResult = repository.syncSelectedCourses(selectedCourses, semester, year)
                
                syncResult.onSuccess { syncedCourses ->
                    // Update the UI state to show only the synced courses
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        courses = syncedCourses, // Update to show only synced courses
                        successMessage = "Successfully synced ${syncedCourses.size} selected courses!",
                        lastSyncTime = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Successfully synced ${syncedCourses.size} selected courses")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to sync selected courses: ${error.message}"
                    )
                    Log.e(TAG, "Failed to sync selected courses", error)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No courses selected"
                )
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
