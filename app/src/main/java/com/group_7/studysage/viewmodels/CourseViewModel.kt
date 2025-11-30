package com.group_7.studysage.viewmodels

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import java.util.Calendar

data class CourseUiState(
    val courses: List<Course> = emptyList(),
    val allCourses: List<Course> = emptyList(), // Store all courses for filtering
    val selectedSemester: String = Semester.FALL.displayName,
    val selectedYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val availableYears: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false, // Added pull-to-refresh flag
    val error: String? = null,
    val message: String? = null,
    val isCreatingCourse: Boolean = false,
    val selectedCourse: CourseWithNotes? = null,
    val pendingOpenNoteId: String? = null, // NEW: hold a noteId to open when course loads
    val isShowingFullscreenOverlay: Boolean = false, // NEW: track if quiz/NFC screens are showing
    val isRestoringState: Boolean = false // NEW: track if we're restoring state after rotation
)

class CourseViewModel(
    private val courseRepository: CourseRepository = CourseRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val SELECTED_COURSE_ID_KEY = "selected_course_id"
    }

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("CourseViewModel", "========================================")
        android.util.Log.d("CourseViewModel", "🔧 CourseViewModel INITIALIZED")
        android.util.Log.d("CourseViewModel", "   SavedStateHandle instance: ${savedStateHandle.hashCode()}")
        android.util.Log.d("CourseViewModel", "   SavedStateHandle keys: ${savedStateHandle.keys()}")

        // Check if there's a saved course ID
        val savedCourseId = savedStateHandle.get<String>(SELECTED_COURSE_ID_KEY)
        android.util.Log.d("CourseViewModel", "   Saved course ID: ${savedCourseId ?: "null"}")

        if (savedCourseId != null) {
            android.util.Log.d("CourseViewModel", "✅ FOUND saved course ID - will restore after loading")
        } else {
            android.util.Log.d("CourseViewModel", "ℹ️ No saved course ID found - fresh start")
        }
        android.util.Log.d("CourseViewModel", "========================================")

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

    /**
     * Refresh the courses list used by pull-to-refresh.
     * Reuses existing loadCourses() and updates isRefreshing in uiState.
     */
    fun refreshCourses() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
                // Call existing loader (it uses its own coroutine)
                val loadDeferred = async { loadCourses() }
                loadDeferred.await()
                // Smooth UX
                delay(300)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to refresh courses: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Refresh a single course's details (course + notes) used on course detail screen.
     */
    fun refreshCourse(courseId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
                val loadDeferred = async { loadCourseWithNotes(courseId) }
                loadDeferred.await()
                delay(250)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to refresh course: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadCourseWithNotes(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Save the courseId to survive configuration changes
            android.util.Log.d("CourseViewModel", "========================================")
            android.util.Log.d("CourseViewModel", "💾 SAVING course ID to SavedStateHandle")
            android.util.Log.d("CourseViewModel", "   Course ID: $courseId")
            android.util.Log.d("CourseViewModel", "   SavedStateHandle instance: ${savedStateHandle.hashCode()}")
            android.util.Log.d("CourseViewModel", "   Before save - Keys: ${savedStateHandle.keys()}")

            savedStateHandle[SELECTED_COURSE_ID_KEY] = courseId

            android.util.Log.d("CourseViewModel", "   After save - Keys: ${savedStateHandle.keys()}")
            android.util.Log.d("CourseViewModel", "   Verification - Get value: ${savedStateHandle.get<String>(SELECTED_COURSE_ID_KEY)}")
            android.util.Log.d("CourseViewModel", "✅ Course ID saved successfully")
            android.util.Log.d("CourseViewModel", "========================================")

            try {
                android.util.Log.d("CourseViewModel", "📥 Loading course with notes: $courseId")
                val courseWithNotes = courseRepository.getCourseWithNotes(courseId)

                if (courseWithNotes == null) {
                    android.util.Log.e("CourseViewModel", "❌ Course not found: $courseId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRestoringState = false, // Clear restoration flag
                            error = "Course not found. It may have been deleted."
                        )
                    }
                    // Clear the saved state since the course doesn't exist
                    savedStateHandle.remove<String>(SELECTED_COURSE_ID_KEY)
                    return@launch
                }

                val noteCount = courseWithNotes.notes.size
                android.util.Log.d("CourseViewModel", "✅ Loaded course ${courseWithNotes.course.id} with $noteCount notes")

                if (noteCount == 0) {
                    android.util.Log.d("CourseViewModel", "ℹ️ Course has no notes yet - this is normal for new courses")
                }

                _uiState.update {
                    it.copy(
                        selectedCourse = courseWithNotes,
                        isLoading = false,
                        isRestoringState = false, // Clear restoration flag
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CourseViewModel", "❌ Failed to load course: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedCourse = null,
                        isRestoringState = false, // Clear restoration flag
                        error = "Failed to load course details: ${e.message}"
                    )
                }
                // Clear the saved state on error
                savedStateHandle.remove<String>(SELECTED_COURSE_ID_KEY)
            }
        }
    }

    // NEW: set a pending note id that should be opened when the course detail screen renders
    fun setPendingOpenNote(noteId: String?) {
        android.util.Log.d("CourseViewModel", "setPendingOpenNote: $noteId")
        _uiState.update { it.copy(pendingOpenNoteId = noteId) }
    }

    // NEW: clear pending open note
    fun clearPendingOpenNote() {
        android.util.Log.d("CourseViewModel", "clearPendingOpenNote")
        _uiState.update { it.copy(pendingOpenNoteId = null) }
    }

    // NEW: set fullscreen overlay showing state (for quiz/NFC screens)
    fun setFullscreenOverlay(isShowing: Boolean) {
        _uiState.update { it.copy(isShowingFullscreenOverlay = isShowing) }
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
        android.util.Log.d("CourseViewModel", "========================================")
        android.util.Log.d("CourseViewModel", "🧹 CLEARING selected course")
        android.util.Log.d("CourseViewModel", "   Called from: ${Thread.currentThread().stackTrace.getOrNull(3)?.methodName}")
        android.util.Log.d("CourseViewModel", "   Before clear - Keys: ${savedStateHandle.keys()}")
        android.util.Log.d("CourseViewModel", "   Before clear - Course ID: ${savedStateHandle.get<String>(SELECTED_COURSE_ID_KEY)}")

        savedStateHandle.remove<String>(SELECTED_COURSE_ID_KEY)

        android.util.Log.d("CourseViewModel", "   After clear - Keys: ${savedStateHandle.keys()}")
        android.util.Log.d("CourseViewModel", "   After clear - Course ID: ${savedStateHandle.get<String>(SELECTED_COURSE_ID_KEY)}")
        android.util.Log.d("CourseViewModel", "✅ SavedStateHandle cleared")
        android.util.Log.d("CourseViewModel", "========================================")

        _uiState.update { it.copy(selectedCourse = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    /**
     * Restore the selected course after configuration change (e.g., rotation)
     * Called when the course detail screen is recreated
     */
    fun restoreSelectedCourseIfNeeded() {
        android.util.Log.d("CourseViewModel", "========================================")
        android.util.Log.d("CourseViewModel", "🔄 RESTORE CHECK - restoreSelectedCourseIfNeeded() called")
        android.util.Log.d("CourseViewModel", "   SavedStateHandle instance: ${savedStateHandle.hashCode()}")
        android.util.Log.d("CourseViewModel", "   SavedStateHandle keys: ${savedStateHandle.keys()}")

        val savedCourseId = savedStateHandle.get<String>(SELECTED_COURSE_ID_KEY)
        android.util.Log.d("CourseViewModel", "   Saved course ID: ${savedCourseId ?: "null"}")

        val currentSelectedCourse = _uiState.value.selectedCourse
        android.util.Log.d("CourseViewModel", "   Current selectedCourse: ${currentSelectedCourse?.course?.id ?: "null"}")

        if (savedCourseId != null && currentSelectedCourse == null) {
            android.util.Log.d("CourseViewModel", "✅ RESTORING course after configuration change")
            android.util.Log.d("CourseViewModel", "   Course ID to restore: $savedCourseId")
            android.util.Log.d("CourseViewModel", "   Setting isRestoringState = true")

            // Set flag to prevent navigation away while restoring
            _uiState.update { it.copy(isRestoringState = true) }

            android.util.Log.d("CourseViewModel", "   Calling loadCourseWithNotes()...")
            android.util.Log.d("CourseViewModel", "========================================")
            loadCourseWithNotes(savedCourseId)
        } else if (savedCourseId == null) {
            android.util.Log.d("CourseViewModel", "ℹ️ No saved course ID - nothing to restore")
            android.util.Log.d("CourseViewModel", "========================================")
        } else if (currentSelectedCourse != null) {
            android.util.Log.d("CourseViewModel", "ℹ️ Course already loaded - no need to restore")
            android.util.Log.d("CourseViewModel", "========================================")
        }
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