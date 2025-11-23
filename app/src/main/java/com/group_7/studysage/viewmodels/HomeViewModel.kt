package com.group_7.studysage.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.data.repository.CourseRepository
import java.net.URLEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val notesRepository: NotesRepository = NotesRepository(application.applicationContext)
    private val authRepository: AuthRepository = AuthRepository()
    private val courseRepository: CourseRepository = CourseRepository()

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

    // Fetch user full name from Firebase Firestore
    private val _userFullName = mutableStateOf("User")
    val userFullName: State<String> = _userFullName

    private val _userProfile = mutableStateOf<Map<String, Any>?>(null)
    val userProfile: State<Map<String, Any>?> = _userProfile

    private val _isLoadingProfile = mutableStateOf(true)
    val isLoadingProfile: State<Boolean> = _isLoadingProfile

    private val _recentlyOpenedPdfs = mutableStateOf<List<Map<String, Any>>>(emptyList())
    val recentlyOpenedPdfs: State<List<Map<String, Any>>> = _recentlyOpenedPdfs

    // Store course ID to name mapping
    private val _courseNameMap = mutableStateOf<Map<String, String>>(emptyMap())

    // Pull-to-refresh state
    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    init {
        loadRecentNotes()
        loadUserProfile()
        loadRecentlyOpenedPdfs()
        loadCourses()
    }

    // Load user profile from Firestore
    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val profile = authRepository.getUserProfile()
                _userFullName.value = (profile?.get("name") as? String) ?: "User"
                _userProfile.value = profile  // Store full profile data
            } catch (e: Exception) {
                _userFullName.value = "User"
                _userProfile.value = null
                _errorMessage.value = "Failed to load profile"
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    // Public method to refresh user name
    fun refreshUserProfile() {
        loadUserProfile()
    }

    /**
     * Mark a note as opened/viewed
     * This updates the recently opened list with timestamp and open count
     */
    fun markNoteAsOpened(
        noteId: String,
        title: String,
        fileName: String,
        fileUrl: String,
        courseId: String
    ) {
        viewModelScope.launch {
            try {
                authRepository.addToRecentlyOpened(
                    noteId = noteId,
                    title = title,
                    fileName = fileName,
                    fileUrl = fileUrl,
                    courseId = courseId
                )
                // Refresh the recently opened list
                loadRecentlyOpenedPdfs()
            } catch (e: Exception) {
                // Log error but don't show to user - this is a background operation
                android.util.Log.e("HomeViewModel", "Failed to mark note as opened: ${e.message}")
            }
        }
    }

    /**
     * Load all courses to build course ID to name mapping
     */
    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = courseRepository.getUserCourses()
                val mapping = courses.associate { course ->
                    course.id to "${course.code} - ${course.title}"
                }
                _courseNameMap.value = mapping
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load courses: ${e.message}")
            }
        }
    }

    /**
     * Get course display name from course ID
     * Returns the course code and title, or the courseId if not found
     */
    fun getCourseName(courseId: String): String {
        return _courseNameMap.value[courseId] ?: courseId
    }

    // Updated method to accept courseId parameter
    fun uploadAndProcessNote(
        context: Context,
        uri: Uri,
        fileName: String,
        courseId: String? = null,
        userPreferences: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            clearMessages()

            try {
                // Validate file type
                val supportedTypes = listOf(".pdf", ".txt", ".doc", ".docx", ".md", ".rtf")
                val isSupported = supportedTypes.any { fileName.endsWith(it, ignoreCase = true) }

                if (!isSupported) {
                    _errorMessage.value = "Unsupported file type. Please upload PDF, TXT, DOC, or DOCX files."
                    _isLoading.value = false
                    return@launch
                }

                // Check file size (limit to 10MB). Use `use` so the InputStream is closed after reading.
                val fileSize = context.contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
                if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                    _errorMessage.value = "File too large. Please upload files smaller than 10MB."
                    _isLoading.value = false
                    return@launch
                }

                val result = notesRepository.uploadNotesAndProcess(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    courseId = courseId, // Pass the courseId
                    userPreferences = userPreferences, // Pass user preferences
                    onProgress = { status ->
                        _uploadStatus.value = status
                    }
                )

                result.onSuccess { note ->
                    _processedNote.value = note
                    _uploadStatus.value = "Document processed successfully!"
                    loadRecentNotes() // Refresh the notes list
                }.onFailure { exception ->
                    val errorMsg = when {
                        exception.message?.contains("authentication", ignoreCase = true) == true ->
                            "Please sign in to upload files"
                        exception.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection"
                        exception.message?.contains("API", ignoreCase = true) == true ->
                            "AI processing temporarily unavailable. Please try again"
                        exception.message?.contains("PDF", ignoreCase = true) == true ->
                            "PDF processing failed. Please try a different file"
                        else -> "Error processing file: ${exception.message}"
                    }
                    _errorMessage.value = errorMsg
                }

            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
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
                // Handle error silently for now, or show a subtle error
                _errorMessage.value = "Failed to load recent notes"
            }
        }
    }

    private fun loadRecentlyOpenedPdfs() {
        viewModelScope.launch {
            try {
                val result = authRepository.getRecentlyOpened(limit = 10)
                result.onSuccess { pdfs ->
                    _recentlyOpenedPdfs.value = pdfs
                }.onFailure { exception ->
                    android.util.Log.e("HomeViewModel", "Failed to load recently opened: ${exception.message}")
                    _recentlyOpenedPdfs.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading recently opened: ${e.message}")
                _recentlyOpenedPdfs.value = emptyList()
            }
        }
    }

    fun getNotesForCourseFromLibrary(courseId: String): List<Map<String, Any>> {
        return recentlyOpenedPdfs.value.filter { it["courseId"] == courseId }
    }

    /**
     * Open a PDF URL in an external app (browser or PDF viewer)
     */
    /**
     * Navigate to the course screen for a given courseId
     * Opens the course detail view where the user can see all notes
     */
    fun openCourse(courseId: String, courseViewModel: com.group_7.studysage.viewmodels.CourseViewModel, navController: androidx.navigation.NavController) {
        try {
            // Load the course with its notes
            courseViewModel.loadCourseWithNotes(courseId)
            // Navigate to the course screen
            navController.navigate("course")
        } catch (e: Exception) {
            _errorMessage.value = "Unable to open course: ${e.message}"
            android.util.Log.e("HomeViewModel", "Failed to open course: ${e.message}")
        }
    }

    // NEW overload: open course and specify a noteId to open inside that course
    fun openCourse(courseId: String, noteId: String?, courseViewModel: com.group_7.studysage.viewmodels.CourseViewModel, navController: androidx.navigation.NavController) {
        try {
            android.util.Log.d("HomeViewModel", "openCourse called: courseId=$courseId noteId=$noteId")
            // Tell the CourseViewModel which note should be opened when the course loads
            courseViewModel.setPendingOpenNote(noteId)
            // Load the course with its notes
            courseViewModel.loadCourseWithNotes(courseId)
            // Navigate to the course screen with optional query parameter (URL-encode the noteId)
            val route = if (noteId.isNullOrBlank()) {
                "course/$courseId"
            } else {
                val encoded = URLEncoder.encode(noteId, "UTF-8")
                "course/$courseId?noteId=$encoded"
            }
            navController.navigate(route)
        } catch (e: Exception) {
            _errorMessage.value = "Unable to open course: ${e.message}"
            android.util.Log.e("HomeViewModel", "Failed to open course with note: ${e.message}")
        }
    }

    fun refreshNotes() {
        loadRecentNotes()
    }

    fun clearMessages() {
        _uploadStatus.value = null
        _errorMessage.value = null
    }

    fun clearProcessedNote() {
        _processedNote.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Test connectivity methods (optional)
    fun testAIConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _testResult.value = "Testing AI connectivity..."

            try {
                // Simple test with a basic text file simulation
                val testContent = "This is a test document for AI processing."
                val summary = notesRepository.generateAISummary(testContent, false, "")

                if (summary.isNotBlank()) {
                    _testResult.value = "✅ AI connection successful"
                } else {
                    _testResult.value = "❌ AI connection failed"
                }
            } catch (e: Exception) {
                _testResult.value = "❌ AI test failed: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun retryProcessing(context: Context, uri: Uri, fileName: String, courseId: String? = null, userPreferences: String = "") {
        // Clear previous errors and try again
        clearMessages()
        uploadAndProcessNote(context, uri, fileName, courseId, userPreferences)
    }

    /**
     * Refresh all home screen data
     * Reloads user profile, recent notes, recently opened PDFs, and courses in parallel
     * Called when user performs pull-to-refresh gesture
     */
    fun refreshHomeData() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                android.util.Log.d(TAG, "🔄 Starting home data refresh...")

                // Launch all data loads in parallel for better performance using async
                val profileDeferred = async { loadUserProfile() }
                val notesDeferred = async { loadRecentNotes() }
                val pdfsDeferred = async { loadRecentlyOpenedPdfs() }
                val coursesDeferred = async { loadCourses() }

                // Wait for all parallel operations to complete
                profileDeferred.await()
                notesDeferred.await()
                pdfsDeferred.await()
                coursesDeferred.await()

                // Optional: Add small delay for better UX (shows refresh animation)
                kotlinx.coroutines.delay(300)

                android.util.Log.d(TAG, "✅ Home data refresh completed")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error refreshing home data: ${e.message}", e)
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                // Always reset refresh state, even if there's an error
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}