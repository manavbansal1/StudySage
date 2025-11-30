package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for CourseDialog to handle rotation and state persistence
 */
data class AddCourseUiState(
    val title: String = "",
    val code: String = "",
    val instructor: String = "",
    val description: String = "",
    val credits: String = "",
    val selectedColor: String = "#6200EE" // Default purple color
)

class AddCourseViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddCourseUiState())
    val uiState: StateFlow<AddCourseUiState> = _uiState.asStateFlow()
    
    fun setTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun setCode(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }
    
    fun setInstructor(instructor: String) {
        _uiState.value = _uiState.value.copy(instructor = instructor)
    }
    
    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun setCredits(credits: String) {
        _uiState.value = _uiState.value.copy(credits = credits)
    }
    
    fun setSelectedColor(color: String) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }
    
    fun initializeFromExistingCourse(
        title: String,
        code: String,
        instructor: String,
        description: String,
        credits: Int,
        color: String
    ) {
        _uiState.value = AddCourseUiState(
            title = title,
            code = code,
            instructor = instructor,
            description = description,
            credits = credits.toString(),
            selectedColor = color
        )
    }
    
    fun clearState() {
        _uiState.value = AddCourseUiState()
    }
}
