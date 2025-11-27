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

    // Daily Tasks functionality - Must be initialized before init block
    private val tasksRepository: com.group_7.studysage.data.repository.TasksRepository =
        com.group_7.studysage.data.repository.TasksRepository(
            com.google.firebase.firestore.FirebaseFirestore.getInstance(),
            com.google.firebase.auth.FirebaseAuth.getInstance()
        )

    private val _dailyTasks = kotlinx.coroutines.flow.MutableStateFlow<List<com.group_7.studysage.data.model.DailyTaskItem>>(emptyList())
    val dailyTasks: kotlinx.coroutines.flow.StateFlow<List<com.group_7.studysage.data.model.DailyTaskItem>> = _dailyTasks

    private val _userTotalXP = kotlinx.coroutines.flow.MutableStateFlow(0)
    val userTotalXP: kotlinx.coroutines.flow.StateFlow<Int> = _userTotalXP

    private val _userLevel = kotlinx.coroutines.flow.MutableStateFlow(0)
    val userLevel: kotlinx.coroutines.flow.StateFlow<Int> = _userLevel

    // Study time tracking - tracks time when app is in foreground
    private val sharedPreferences = application.getSharedPreferences("StudyTimePrefs", Context.MODE_PRIVATE)
    private val _studyTimeToday = kotlinx.coroutines.flow.MutableStateFlow(0) // in seconds
    val studyTimeToday: kotlinx.coroutines.flow.StateFlow<Int> = _studyTimeToday
    private var studyStartTime: Long = 0
    private var isTrackingStudyTime = false
    private val STUDY_GOAL_SECONDS = 30 * 60 // 30 minutes in seconds
    private var studyTimerJob: kotlinx.coroutines.Job? = null
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadRecentNotes()
        loadUserProfile()
        loadRecentlyOpenedPdfs()
        loadCourses()
        loadDailyTasks()
        loadStudyTimeForToday()
    }

    // Load user profile from Firestore
    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val profile = authRepository.getUserProfile()
                _userFullName.value = (profile?.get("name") as? String) ?: "User"
                _userProfile.value = profile  // Store full profile data

                // Update XP and Level when profile is loaded
                _userTotalXP.value = (profile?.get("xpPoints") as? Long)?.toInt() ?: 0
                _userLevel.value = (profile?.get("level") as? Long)?.toInt() ?: 0
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
     * Clear all recently opened PDFs
     * Removes all items from both Firestore and local state
     */
    fun clearAllRecentlyOpened() {
        viewModelScope.launch {
            try {
                val result = authRepository.clearRecentlyOpened()
                result.onSuccess {
                    // Update local state immediately
                    _recentlyOpenedPdfs.value = emptyList()
                    android.util.Log.d("HomeViewModel", "Successfully cleared all recently opened PDFs")
                }.onFailure { exception ->
                    android.util.Log.e("HomeViewModel", "Failed to clear recently opened: ${exception.message}")
                    _errorMessage.value = "Failed to clear recent PDFs: ${exception.message}"
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error clearing recently opened: ${e.message}")
                _errorMessage.value = "Error clearing recent PDFs: ${e.message}"
            }
        }
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


    /**
     * Load daily tasks from repository
     * Checks and generates tasks for today if needed, then collects the flow
     */
    fun loadDailyTasks() {
        viewModelScope.launch {
            try {
                // Check and generate tasks if they don't exist for today
                val result = tasksRepository.checkAndGenerateTasksForToday()
                android.util.Log.d(TAG, "Check and generate tasks result: ${result.isSuccess}")

                // Collect the flow of daily tasks in the viewModelScope
                // This launches a separate coroutine to collect the flow continuously
                tasksRepository.getTodaysTasks().collect { tasks ->
                    _dailyTasks.value = tasks
                    android.util.Log.d(TAG, "Daily tasks loaded: ${tasks.size} tasks")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job was cancelled, this is normal during ViewModel cleanup
                android.util.Log.d(TAG, "Daily tasks collection cancelled (normal during cleanup)")
                throw e // Re-throw to properly cancel the coroutine
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading daily tasks: ${e.message}", e)
                _errorMessage.value = "Failed to load daily tasks"
            }
        }
    }

    /**
     * Toggle task completion status
     * Marks task as complete and awards XP
     */
    fun toggleTaskCompletion(taskId: String, xpAmount: Int) {
        viewModelScope.launch {
            try {
                val result = tasksRepository.completeTask(taskId, xpAmount)
                if (result.isSuccess) {
                    android.util.Log.d(TAG, "Task $taskId completed, awarded $xpAmount XP")
                    // Refresh user profile to get updated XP and level
                    loadUserProfile()
                    updateUserXPAndLevel()
                } else {
                    _errorMessage.value = "Failed to complete task"
                    android.util.Log.e(TAG, "Failed to complete task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error toggling task completion: ${e.message}", e)
                _errorMessage.value = "Error completing task"
            }
        }
    }

    /**
     * Check and complete task by type
     * Finds the first incomplete task matching the given type and completes it
     */
    fun checkAndCompleteTaskByType(taskType: String) {
        viewModelScope.launch {
            try {
                // Find the first incomplete task of the specified type
                val task = _dailyTasks.value.firstOrNull {
                    it.taskType == taskType && !it.isCompleted
                }

                if (task != null) {
                    // Complete the task
                    val result = tasksRepository.completeTask(task.id, task.xpReward)
                    if (result.isSuccess) {
                        android.util.Log.d(TAG, "Auto-completed task: ${task.title} (${task.xpReward} XP)")
                        // Refresh user profile to get updated XP and level
                        loadUserProfile()
                        updateUserXPAndLevel()
                    } else {
                        android.util.Log.e(TAG, "Failed to auto-complete task: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    android.util.Log.d(TAG, "No incomplete task found for type: $taskType")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error checking and completing task by type: ${e.message}", e)
            }
        }
    }

    /**
     * Update user XP and level from profile
     */
    private fun updateUserXPAndLevel() {
        viewModelScope.launch {
            try {
                val profile = authRepository.getUserProfile()
                _userTotalXP.value = (profile?.get("xpPoints") as? Long)?.toInt() ?: 0
                _userLevel.value = (profile?.get("level") as? Long)?.toInt() ?: 0
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updating XP and level: ${e.message}", e)
            }
        }
    }

    /**
     * Reset all user-specific data when user changes
     * This ensures complete data isolation between different user accounts
     */
    private fun resetUserSpecificData() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "🧹 resetUserSpecificData() called")
        android.util.Log.d(TAG, "   Before reset:")
        android.util.Log.d(TAG, "   - studyTimeToday: ${_studyTimeToday.value}")
        android.util.Log.d(TAG, "   - dailyTasks: ${_dailyTasks.value.size} tasks")
        android.util.Log.d(TAG, "   - userTotalXP: ${_userTotalXP.value}")
        android.util.Log.d(TAG, "   - userLevel: ${_userLevel.value}")
        android.util.Log.d(TAG, "   - userFullName: ${_userFullName.value}")

        _studyTimeToday.value = 0
        _dailyTasks.value = emptyList()
        _userTotalXP.value = 0
        _userLevel.value = 0
        _userFullName.value = "User"
        _userProfile.value = null
        _recentlyOpenedPdfs.value = emptyList()

        // Remove Firestore listener if it exists
        firestoreListener?.remove()
        firestoreListener = null

        android.util.Log.d(TAG, "   After reset:")
        android.util.Log.d(TAG, "   - studyTimeToday: ${_studyTimeToday.value}")
        android.util.Log.d(TAG, "   - dailyTasks: ${_dailyTasks.value.size} tasks")
        android.util.Log.d(TAG, "   - userTotalXP: ${_userTotalXP.value}")
        android.util.Log.d(TAG, "   - userLevel: ${_userLevel.value}")
        android.util.Log.d(TAG, "   - userFullName: ${_userFullName.value}")
        android.util.Log.d(TAG, "✅ User-specific data reset complete")
        android.util.Log.d(TAG, "========================================")
    }

    // ==================== STUDY TIME TRACKING ====================

    /**
     * Reload study time data - call this when user logs in/out
     * This ensures fresh data for the current user
     */
    fun reloadStudyTimeForCurrentUser() {
        android.util.Log.d(TAG, "🔄 Reloading study time for current user")

        // IMPORTANT: Reset study time to 0 BEFORE stopping tracking
        // This prevents old user's time from being synced to new user
        _studyTimeToday.value = 0

        // Stop current tracking
        stopStudyTimeTracking()

        // Reset all user-specific data completely
        resetUserSpecificData()

        // Reset the start time so new tracking starts fresh
        studyStartTime = System.currentTimeMillis()

        // Reload for current user
        loadStudyTimeForToday()

        // Restart tracking if app is active
        startStudyTimeTracking()
    }

    /**
     * Load study time for today from Firestore with REAL-TIME LISTENER
     * This will automatically detect changes made in Firebase Console
     */
    private fun loadStudyTimeForToday() {
        val today = getTodayDateString()

        viewModelScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                // Check if user changed - if so, clear old data
                val lastUserId = sharedPreferences.getString("lastUserId", "")
                if (userId != null && userId != lastUserId) {
                    android.util.Log.d(TAG, "👤 User changed from $lastUserId to $userId - clearing old study data")
                    // Clear old user's study time data
                    sharedPreferences.edit()
                        .clear()
                        .putString("lastUserId", userId)
                        .putString("lastStudyDate", today)
                        .putInt("studyTimeSeconds", 0)
                        .apply()

                    // Reset all user-specific data
                    resetUserSpecificData()
                }

                if (userId != null) {
                    // Set up REAL-TIME listener for Firestore changes
                    val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("studyProgress")
                        .document(today)

                    // Remove old listener if exists
                    firestoreListener?.remove()

                    // Add snapshot listener for real-time updates
                    firestoreListener = docRef.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e(TAG, "❌ Firestore listener error: ${error.message}")
                            loadFromLocalStorage(today)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val firestoreTime = (snapshot.getLong("studyTimeSeconds") ?: 0).toInt()

                            // Only update if value changed (avoid infinite loops)
                            if (_studyTimeToday.value != firestoreTime) {
                                _studyTimeToday.value = firestoreTime

                                // Update local cache with user ID
                                sharedPreferences.edit()
                                    .putString("lastUserId", userId)
                                    .putString("lastStudyDate", today)
                                    .putInt("studyTimeSeconds", firestoreTime)
                                    .apply()

                                android.util.Log.d(TAG, "🔄 Study time updated from Firestore: ${firestoreTime / 60}m ${firestoreTime % 60}s")

                                // Check if goal is reached (in case manual change completes it)
                                checkStudyGoalCompletion()
                            }
                        } else {
                            // Document doesn't exist yet, use local storage
                            android.util.Log.d(TAG, "📚 No Firestore data yet, using local storage")
                            loadFromLocalStorage(today)
                        }
                    }

                    android.util.Log.d(TAG, "👂 Real-time Firestore listener active for study time")
                } else {
                    // No user logged in, clear everything
                    android.util.Log.w(TAG, "⚠️ User not logged in, clearing all study data")
                    sharedPreferences.edit().clear().apply()
                    _studyTimeToday.value = 0
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error setting up Firestore listener: ${e.message}")
                loadFromLocalStorage(today)
            }
        }
    }

    /**
     * Load study time from SharedPreferences (local fallback)
     */
    private fun loadFromLocalStorage(today: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val lastUserId = sharedPreferences.getString("lastUserId", "")
        val lastStudyDate = sharedPreferences.getString("lastStudyDate", "")

        // Check if user changed
        if (userId != null && userId != lastUserId) {
            android.util.Log.d(TAG, "👤 User changed in local storage - resetting study time")
            sharedPreferences.edit()
                .clear()
                .putString("lastUserId", userId)
                .putString("lastStudyDate", today)
                .putInt("studyTimeSeconds", 0)
                .apply()

            // Reset all user-specific data
            resetUserSpecificData()
            return
        }

        if (lastStudyDate == today) {
            // Same day, load existing study time
            _studyTimeToday.value = sharedPreferences.getInt("studyTimeSeconds", 0)
            android.util.Log.d(TAG, "📚 Loaded study time from local: ${_studyTimeToday.value / 60} minutes")
        } else {
            // New day, reset study time
            _studyTimeToday.value = 0
            sharedPreferences.edit()
                .putString("lastUserId", userId ?: "")
                .putString("lastStudyDate", today)
                .putInt("studyTimeSeconds", 0)
                .apply()
            android.util.Log.d(TAG, "📅 New day detected, reset study time")
        }
    }

    /**
     * Start tracking study time when app comes to foreground
     * This should be called when the app becomes active
     */
    fun startStudyTimeTracking() {
        if (!isTrackingStudyTime) {
            isTrackingStudyTime = true
            studyStartTime = System.currentTimeMillis()

            // Start a periodic timer that updates every 10 seconds
            studyTimerJob = viewModelScope.launch {
                while (isTrackingStudyTime) {
                    kotlinx.coroutines.delay(10_000) // Update every 10 seconds
                    if (isTrackingStudyTime) {
                        updateStudyTime()
                    }
                }
            }

            android.util.Log.d(TAG, "📚 Study time tracking STARTED")
        }
    }

    /**
     * Stop tracking study time when app goes to background
     * This should be called when the app becomes inactive
     */
    fun stopStudyTimeTracking() {
        if (isTrackingStudyTime) {
            isTrackingStudyTime = false
            studyTimerJob?.cancel()
            studyTimerJob = null

            // Only update if there's actual time to save (prevent syncing 0 to new users)
            if (_studyTimeToday.value > 0) {
                // Final update when stopping
                updateStudyTime()
            }

            android.util.Log.d(TAG, "📚 Study time tracking STOPPED. Total today: ${_studyTimeToday.value / 60} minutes")
        }
    }

    /**
     * Update the study time accumulation and sync to Firestore
     */
    private fun updateStudyTime() {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = ((currentTime - studyStartTime) / 1000).toInt() // in seconds

        // Skip if session duration is 0 or negative (invalid state)
        if (sessionDuration <= 0) {
            android.util.Log.d(TAG, "⏭️ Skipping study time update (invalid session duration: $sessionDuration)")
            return
        }

        // Add session time to total
        val newTotalTime = _studyTimeToday.value + sessionDuration
        _studyTimeToday.value = newTotalTime

        // Save to SharedPreferences (local cache) with user ID
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        sharedPreferences.edit()
            .putString("lastUserId", userId ?: "")
            .putInt("studyTimeSeconds", newTotalTime)
            .apply()

        // Sync to Firestore
        syncStudyTimeToFirestore(newTotalTime)

        // Reset start time for next interval
        studyStartTime = currentTime

        val minutes = newTotalTime / 60
        val seconds = newTotalTime % 60
        android.util.Log.d(TAG, "⏱️ Study time updated: ${minutes}m ${seconds}s / 30m")

        // Check if study goal is reached
        checkStudyGoalCompletion()
    }

    /**
     * Sync study time to Firestore
     */
    private fun syncStudyTimeToFirestore(studyTimeSeconds: Int) {
        viewModelScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                if (userId == null) {
                    android.util.Log.w(TAG, "⚠️ Cannot sync to Firestore: User not logged in")
                    return@launch
                }

                val today = getTodayDateString()
                val studyData = mapOf(
                    "studyTimeSeconds" to studyTimeSeconds,
                    "date" to today,
                    "lastUpdated" to com.google.firebase.Timestamp.now(),
                    "userId" to userId
                )

                android.util.Log.d(TAG, "🔄 Attempting to sync study time to Firestore...")
                android.util.Log.d(TAG, "   User ID: $userId")
                android.util.Log.d(TAG, "   Date: $today")
                android.util.Log.d(TAG, "   Time: $studyTimeSeconds seconds (${studyTimeSeconds / 60} minutes)")

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("studyProgress")
                    .document(today)
                    .set(studyData)
                    .addOnSuccessListener {
                        android.util.Log.d(TAG, "✅ Study time synced to Firestore successfully!")
                        android.util.Log.d(TAG, "   Path: users/$userId/studyProgress/$today")
                        android.util.Log.d(TAG, "   Data: ${studyTimeSeconds}s")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e(TAG, "❌ Failed to sync to Firestore: ${e.message}")
                        android.util.Log.e(TAG, "   Error type: ${e.javaClass.simpleName}")
                        android.util.Log.e(TAG, "   User ID: $userId")
                        // Local data is still saved in SharedPreferences
                    }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error syncing study time: ${e.message}", e)
            }
        }
    }

    /**
     * Check if study goal (30 minutes) is reached and complete the task
     */
    private fun checkStudyGoalCompletion() {
        if (_studyTimeToday.value >= STUDY_GOAL_SECONDS) {
            // Check if task is already completed to avoid duplicate completions
            val studyTask = _dailyTasks.value.firstOrNull { it.taskType == "study" }
            if (studyTask != null && !studyTask.isCompleted) {
                android.util.Log.d(TAG, "🎉 Study goal reached (30 minutes)! Completing study task...")
                checkAndCompleteTaskByType("study")

                // Stop tracking since goal is reached
                stopStudyTimeTracking()
            }
        }
    }

    /**
     * Get today's date as a string (format: yyyy-MM-dd)
     */
    private fun getTodayDateString(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
    /**
     * Clean up when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopStudyTimeTracking()
        // Remove Firestore listener to prevent memory leaks
        firestoreListener?.remove()
        android.util.Log.d(TAG, "🧹 Cleaned up: Study tracking stopped, Firestore listener removed")
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}


