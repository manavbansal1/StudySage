package com.group_7.studysage.ui.screens.Courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.CourseRepository
import com.group_7.studysage.data.repository.CourseWithNotes
import com.group_7.studysage.data.repository.Semester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CourseUiState(
    val courses: List<Course> = emptyList(),
    val allCourses: List<Course> = emptyList(), // Store all courses for filtering
    val selectedSemester: String = Semester.FALL.displayName,
    val selectedYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val availableYears: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isCreatingCourse: Boolean = false,
    val selectedCourse: CourseWithNotes? = null
)

class CourseViewModel(
    private val courseRepository: CourseRepository = CourseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        loadAvailableYears()
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val allCourses = courseRepository.getUserCourses()

                // Filter courses based on selected semester and year
                val filteredCourses = allCourses.filter { course ->
                    course.semester == _uiState.value.selectedSemester &&
                            course.year == _uiState.value.selectedYear
                }

                _uiState.update {
                    it.copy(
                        allCourses = allCourses,
                        courses = filteredCourses,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load courses: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadCourseWithNotes(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val courseWithNotes = courseRepository.getCourseWithNotes(courseId)
                _uiState.update {
                    it.copy(
                        selectedCourse = courseWithNotes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load course details: ${e.message}"
                    )
                }
            }
        }
    }

    fun createCourse(
        title: String,
        code: String,
        instructor: String,
        description: String,
        credits: Int,
        color: String
    ) {
        if (title.isBlank() || code.isBlank()) {
            _uiState.update { it.copy(error = "Course title and code are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCourse = true, error = null) }

            val course = Course(
                title = title.trim(),
                code = code.trim().uppercase(),
                semester = _uiState.value.selectedSemester,
                year = _uiState.value.selectedYear,
                instructor = instructor.trim(),
                description = description.trim(),
                credits = credits,
                color = color
            )

            courseRepository.createCourse(course)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCreatingCourse = false,
                            message = "Course created successfully!"
                        )
                    }
                    loadCourses() // Refresh the list
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isCreatingCourse = false,
                            error = "Failed to create course: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            courseRepository.updateCourse(course)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Course updated successfully!"
                        )
                    }
                    loadCourses()
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to update course: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            courseRepository.deleteCourse(courseId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Course deleted successfully!"
                        )
                    }
                    loadCourses()
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete course: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun setSemesterFilter(semester: String) {
        _uiState.update { it.copy(selectedSemester = semester) }
        // Filter existing courses instead of reloading from repository
        filterCourses()
    }

    fun setYearFilter(year: String) {
        _uiState.update { it.copy(selectedYear = year) }
        // Filter existing courses instead of reloading from repository
        filterCourses()
    }

    private fun filterCourses() {
        val currentState = _uiState.value
        val filteredCourses = currentState.allCourses.filter { course ->
            course.semester == currentState.selectedSemester &&
                    course.year == currentState.selectedYear
        }
        _uiState.update { it.copy(courses = filteredCourses) }
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            try {
                val years = courseRepository.getAvailableYears()
                _uiState.update { it.copy(availableYears = years) }
            } catch (e: Exception) {
                // Use current year as fallback
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                _uiState.update { it.copy(availableYears = listOf(currentYear)) }
            }
        }
    }

    fun clearSelectedCourse() {
        _uiState.update { it.copy(selectedCourse = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun archiveCourse(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            courseRepository.archiveCourse(courseId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Course archived successfully!"
                        )
                    }
                    loadCourses()
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to archive course: ${exception.message}"
                        )
                    }
                }
        }
    }
}