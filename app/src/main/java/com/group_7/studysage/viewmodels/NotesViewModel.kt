package com.group_7.studysage.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.BuildConfig
import com.group_7.studysage.data.models.Flashcard
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.data.repository.PodcastRepository
import com.group_7.studysage.utils.FileDownloader
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class NotesViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NotesViewModel"
        private const val SELECTED_NOTE_ID_KEY = "selected_note_id"
        private const val SHOW_NOTE_OPTIONS_KEY = "show_note_options"
        // Note detail screen state keys
        private const val SHOW_AI_SUMMARY_KEY = "show_ai_summary"
        private const val SHOW_FLASHCARDS_KEY = "show_flashcards"
        private const val SHOW_NFC_SHARE_KEY = "show_nfc_share"
        private const val SHOW_PODCAST_KEY = "show_podcast"
        private const val NFC_SHARE_NOTE_ID_KEY = "nfc_share_note_id"
    }

    private val notesRepository: NotesRepository = NotesRepository(application.applicationContext)
    private val podcastRepository: PodcastRepository = PodcastRepository(application.applicationContext)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _processedNote = MutableStateFlow<Note?>(null)
    val processedNote: StateFlow<Note?> = _processedNote.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote.asStateFlow()

    private val _showNoteOptions = MutableStateFlow(false)
    val showNoteOptions: StateFlow<Boolean> = _showNoteOptions.asStateFlow()

    private val _isNoteDetailsLoading = MutableStateFlow(false)
    val isNoteDetailsLoading: StateFlow<Boolean> = _isNoteDetailsLoading.asStateFlow()

    private val _courseNotes = MutableStateFlow<List<Note>>(emptyList())
    val courseNotes: StateFlow<List<Note>> = _courseNotes.asStateFlow()

    private val _podcastGenerationStatus = MutableStateFlow<String?>(null)
    val podcastGenerationStatus: StateFlow<String?> = _podcastGenerationStatus.asStateFlow()

    private val _podcastAudioPath = MutableStateFlow<String?>(null)
    val podcastAudioPath: StateFlow<String?> = _podcastAudioPath.asStateFlow()

    private val _isPodcastGenerating = MutableStateFlow(false)
    val isPodcastGenerating: StateFlow<Boolean> = _isPodcastGenerating.asStateFlow()

    private val _podcastScript = MutableStateFlow<String?>(null)
    val podcastScript: StateFlow<String?> = _podcastScript.asStateFlow()
    // Temporary flashcard generation state (for quick action feature)
    data class TempFlashcardState(
        val isGenerating: Boolean = false,
        val generatedFlashcards: List<Flashcard> = emptyList(),
        val error: String? = null
    )

    private val _tempFlashcardState = MutableStateFlow(TempFlashcardState())
    val tempFlashcardState: StateFlow<TempFlashcardState> = _tempFlashcardState.asStateFlow()

    // Note detail screen states - preserved across rotation
    private val _showAiSummaryScreen = MutableStateFlow(false)
    val showAiSummaryScreen: StateFlow<Boolean> = _showAiSummaryScreen.asStateFlow()

    private val _showFlashcardsScreen = MutableStateFlow(false)
    val showFlashcardsScreen: StateFlow<Boolean> = _showFlashcardsScreen.asStateFlow()

    private val _showNfcShareDialog = MutableStateFlow(false)
    val showNfcShareDialog: StateFlow<Boolean> = _showNfcShareDialog.asStateFlow()

    private val _showPodcastScreen = MutableStateFlow(false)
    val showPodcastScreen: StateFlow<Boolean> = _showPodcastScreen.asStateFlow()

    // Note being shared via NFC - preserved across rotation
    private val _nfcShareNote = MutableStateFlow<Note?>(null)
    val nfcShareNote: StateFlow<Note?> = _nfcShareNote.asStateFlow()

    init {
        // Restore screen states from SavedStateHandle
        _showAiSummaryScreen.value = savedStateHandle.get<Boolean>(SHOW_AI_SUMMARY_KEY) ?: false
        _showFlashcardsScreen.value = savedStateHandle.get<Boolean>(SHOW_FLASHCARDS_KEY) ?: false
        _showNfcShareDialog.value = savedStateHandle.get<Boolean>(SHOW_NFC_SHARE_KEY) ?: false
        _showPodcastScreen.value = savedStateHandle.get<Boolean>(SHOW_PODCAST_KEY) ?: false

        Log.d(TAG, "NotesViewModel initialized - Screen states restored:")
        Log.d(TAG, "  AI Summary: ${_showAiSummaryScreen.value}")
        Log.d(TAG, "  Flashcards: ${_showFlashcardsScreen.value}")
        Log.d(TAG, "  NFC Share: ${_showNfcShareDialog.value}")
        Log.d(TAG, "  Podcast: ${_showPodcastScreen.value}")

        // Restore NFC share note if needed
        val nfcShareNoteId = savedStateHandle.get<String>(NFC_SHARE_NOTE_ID_KEY)
        if (nfcShareNoteId != null && _showNfcShareDialog.value) {
            Log.d(TAG, "  Restoring NFC share note: $nfcShareNoteId")
            loadNoteForNfcShare(nfcShareNoteId)
        }
    }

    fun loadNotes(courseId: String? = null) {
        Log.d(TAG, "Loading notes${courseId?.let { " for course $it" } ?: ""}")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userNotes = notesRepository.getUserNotes(courseId)
                if (courseId != null) {
                    _courseNotes.value = userNotes
                } else {
                    _notes.value = userNotes
                }
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

    fun selectNote(note: Note, showOptions: Boolean = true) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "📝 Selecting note: ${note.id}")
        Log.d(TAG, "   Note title: ${note.title}")
        Log.d(TAG, "   Show options: $showOptions")
        Log.d(TAG, "   SavedStateHandle instance: ${savedStateHandle.hashCode()}")

        // Save to SavedStateHandle to survive rotation
        savedStateHandle[SELECTED_NOTE_ID_KEY] = note.id
        savedStateHandle[SHOW_NOTE_OPTIONS_KEY] = showOptions

        Log.d(TAG, "   SavedStateHandle keys after save: ${savedStateHandle.keys()}")
        Log.d(TAG, "✅ Note ID saved to SavedStateHandle")
        Log.d(TAG, "========================================")

        _selectedNote.value = note
        _showNoteOptions.value = showOptions
    }

    fun clearSelectedNote() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🧹 Clearing selected note")
        Log.d(TAG, "   Before clear - Keys: ${savedStateHandle.keys()}")

        savedStateHandle.remove<String>(SELECTED_NOTE_ID_KEY)
        savedStateHandle.remove<Boolean>(SHOW_NOTE_OPTIONS_KEY)

        Log.d(TAG, "   After clear - Keys: ${savedStateHandle.keys()}")
        Log.d(TAG, "✅ SavedStateHandle cleared")
        Log.d(TAG, "========================================")

        _selectedNote.value = null
        _showNoteOptions.value = false
    }

    fun setShowNoteOptions(show: Boolean) {
        Log.d(TAG, "Setting showNoteOptions: $show")
        savedStateHandle[SHOW_NOTE_OPTIONS_KEY] = show
        _showNoteOptions.value = show
    }

    fun restoreSelectedNoteIfNeeded() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔄 RESTORE CHECK - restoreSelectedNoteIfNeeded() called")
        Log.d(TAG, "   SavedStateHandle instance: ${savedStateHandle.hashCode()}")
        Log.d(TAG, "   SavedStateHandle keys: ${savedStateHandle.keys()}")

        val savedNoteId = savedStateHandle.get<String>(SELECTED_NOTE_ID_KEY)
        val savedShowOptions = savedStateHandle.get<Boolean>(SHOW_NOTE_OPTIONS_KEY) ?: false

        Log.d(TAG, "   Saved note ID: ${savedNoteId ?: "null"}")
        Log.d(TAG, "   Saved show options: $savedShowOptions")
        Log.d(TAG, "   Current selected note: ${_selectedNote.value?.id ?: "null"}")

        if (savedNoteId != null && _selectedNote.value == null) {
            Log.d(TAG, "✅ RESTORING note after configuration change")
            Log.d(TAG, "   Note ID to restore: $savedNoteId")
            Log.d(TAG, "   Calling loadNoteById()...")
            Log.d(TAG, "========================================")

            loadNoteById(savedNoteId)
            _showNoteOptions.value = savedShowOptions
        } else if (savedNoteId == null) {
            Log.d(TAG, "ℹ️ No saved note ID - nothing to restore")
            Log.d(TAG, "========================================")
        } else {
            Log.d(TAG, "ℹ️ Note already loaded - no need to restore")
            Log.d(TAG, "========================================")
        }
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
                    loadNotes(courseId) // Refresh the notes list
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

    fun clearMessages() {
        _uploadStatus.value = null
        _errorMessage.value = null
    }

    fun clearProcessedNote() {
        _processedNote.value = null
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

    fun searchNotes(query: String, courseId: String? = null) {
        Log.d(TAG, "Searching notes with query=$query")
        if (query.isBlank()) {
            loadNotes(courseId)
            return
        }

        viewModelScope.launch {
            try {
                val allNotes = notesRepository.getUserNotes(courseId)
                val filteredNotes = allNotes.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.summary.contains(query, ignoreCase = true) ||
                            note.tags.any { it.contains(query, ignoreCase = true) } ||
                            note.keyPoints.any { it.contains(query, ignoreCase = true) }
                }
                if (courseId != null) {
                    _courseNotes.value = filteredNotes
                } else {
                    _notes.value = filteredNotes
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                Log.e(TAG, "Note search failed: ${e.message}", e)
            }
        }
    }

    fun updateNoteSummary(noteId: String, content: String, userPreferences: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newSummary = notesRepository.updateNoteSummary(noteId, content, userPreferences)
                _selectedNote.value = _selectedNote.value?.copy(summary = newSummary)
                // Refresh the notes list to reflect the change
                val currentNotes = _courseNotes.value.toMutableList()
                val index = currentNotes.indexOfFirst { it.id == noteId }
                if (index != -1) {
                    currentNotes[index] = currentNotes[index].copy(summary = newSummary)
                    _courseNotes.value = currentNotes
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update summary: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleNoteStar(noteId: String) {
        viewModelScope.launch {
            try {
                notesRepository.toggleNoteStar(noteId)
                    .onSuccess { newStarredState ->
                        // Update selected note
                        _selectedNote.value = _selectedNote.value?.copy(isStarred = newStarredState)

                        // Update in course notes list
                        val currentNotes = _courseNotes.value.toMutableList()
                        val index = currentNotes.indexOfFirst { it.id == noteId }
                        if (index != -1) {
                            currentNotes[index] = currentNotes[index].copy(isStarred = newStarredState)
                            _courseNotes.value = currentNotes
                        }

                        Log.d(TAG, "Note star toggled: $newStarredState")
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Failed to star note: ${exception.message}"
                        Log.e(TAG, "Failed to toggle star: ${exception.message}", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to star note: ${e.message}"
                Log.e(TAG, "Failed to toggle star: ${e.message}", e)
            }
        }
    }

    fun loadOrGeneratePodcast(
        noteId: String,
        content: String,
        noteTitle: String = ""
    ) {
        Log.d(TAG, "Loading or generating podcast for note $noteId")
        viewModelScope.launch {
            _errorMessage.value = null

            // Check if podcast already exists
            val existingPath = podcastRepository.getPodcastPath(noteId)
            if (existingPath != null) {
                Log.d(TAG, "Existing podcast found: $existingPath")
                _podcastAudioPath.value = existingPath
                _podcastGenerationStatus.value = "Podcast loaded"
                // Note: Script won't be available for existing podcasts unless we store it separately
            } else {
                Log.d(TAG, "No existing podcast found, generating new one")
                generatePodcast(noteId, content, noteTitle)
            }
        }
    }

    fun generatePodcast(
        noteId: String,
        content: String,
        noteTitle: String = ""
    ) {
        Log.d(TAG, "Generating podcast for note $noteId based on content length")
        viewModelScope.launch {
            _isPodcastGenerating.value = true
            _podcastGenerationStatus.value = "Initializing..."
            _errorMessage.value = null
            _podcastAudioPath.value = null // Clear current podcast path
            _podcastScript.value = null // Clear current script

            try {
                // Delete any existing podcast first
                podcastRepository.deletePodcast(noteId)

                val result = podcastRepository.generatePodcast(
                    noteId = noteId,
                    content = content,
                    noteTitle = noteTitle,
                    onProgress = { status ->
                        _podcastGenerationStatus.value = status
                    },
                    onScriptGenerated = { script ->
                        _podcastScript.value = script
                    }
                )

                result.onSuccess { audioPath ->
                    _podcastAudioPath.value = audioPath
                    _podcastGenerationStatus.value = "Podcast ready!"
                    Log.d(TAG, "Podcast generated successfully: $audioPath")
                }.onFailure { exception ->
                    val errorMsg = when {
                        exception.message?.contains("API key not configured") == true ->
                            "Please configure Google Cloud TTS API key in .env file"
                        exception.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection"
                        exception.message?.contains("timeout", ignoreCase = true) == true ->
                            "Request timeout. Please try again with a shorter note"
                        else -> "Failed to generate podcast: ${exception.message}"
                    }
                    _errorMessage.value = errorMsg
                    _podcastGenerationStatus.value = null
                    Log.e(TAG, "Podcast generation failed: ${exception.message}", exception)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _podcastGenerationStatus.value = null
                Log.e(TAG, "Podcast generation error: ${e.message}", e)
            }

            _isPodcastGenerating.value = false
        }
    }

    fun clearPodcastData() {
        Log.d(TAG, "Clearing podcast data")
        _podcastAudioPath.value = null
        _podcastGenerationStatus.value = null
        _isPodcastGenerating.value = false
        _podcastScript.value = null
    }

    fun downloadPodcast(context: Context, noteId: String, originalFileName: String) {
        Log.d(TAG, "Downloading podcast for note $noteId")
        val podcastPath = podcastRepository.getPodcastPath(noteId)
        if (podcastPath != null) {
            // Use original file name without extension, add _podcast suffix
            val baseFileName = if (originalFileName.contains(".")) {
                originalFileName.substringBeforeLast(".")
            } else {
                originalFileName
            }
            val fileName = "${baseFileName}_podcast.wav"
            FileDownloader.downloadLocalFile(context, podcastPath, fileName)
        } else {
            _errorMessage.value = "Podcast not found. Please generate it first."
            Log.e(TAG, "Podcast not found for note $noteId")
        }
    }

    // ============================================
    // TEMPORARY FLASHCARD GENERATION
    // ============================================

    fun generateTempFlashcardsFromPdf(
        context: Context,
        pdfUri: Uri,
        fileName: String,
        numberOfCards: Int,
        userPreferences: String
    ) {
        viewModelScope.launch {
            try {
                _tempFlashcardState.update { it.copy(isGenerating = true, error = null) }

                Log.d(TAG, "📚 Generating $numberOfCards flashcards from: $fileName")

                // Extract text from PDF
                val pdfText = withContext(Dispatchers.IO) {
                    extractTextFromPdf(context, pdfUri)
                }

                if (pdfText.isBlank()) {
                    throw Exception("Could not extract text from PDF")
                }

                Log.d(TAG, "✅ Extracted ${pdfText.length} characters")

                // Generate flashcards using AI
                val flashcards = generateFlashcardsWithAI(pdfText, numberOfCards, userPreferences)

                if (flashcards.isEmpty()) {
                    throw Exception("Could not generate flashcards from this content")
                }

                Log.d(TAG, "✅ Generated ${flashcards.size} flashcards")

                _tempFlashcardState.update {
                    it.copy(
                        isGenerating = false,
                        generatedFlashcards = flashcards,
                        error = null
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error generating flashcards: ${e.message}", e)
                _tempFlashcardState.update {
                    it.copy(
                        isGenerating = false,
                        error = "Failed to generate flashcards: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            PDFBoxResourceLoader.init(context)

            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper().apply {
                            sortByPosition = true
                        }
                        stripper.getText(document)
                    }
                } ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction error: ${e.message}", e)
            throw Exception("Failed to read PDF: ${e.message}")
        }
    }

    private suspend fun generateFlashcardsWithAI(
        text: String,
        numberOfCards: Int,
        userPreferences: String
    ): List<Flashcard> {
        return try {
            // Limit text to avoid API limits (around 8000 characters)
            val limitedText = if (text.length > 8000) text.take(8000) else text

            val prompt = """
                Generate exactly $numberOfCards flashcards from the following text.
                Create question-answer pairs that test key concepts, definitions, and important facts.
                
                ${if (userPreferences.isNotBlank()) "Focus on: $userPreferences" else ""}
                
                Return ONLY a valid JSON array with this exact format:
                [
                  {"question": "What is...", "answer": "It is..."},
                  {"question": "Define...", "answer": "..."}
                ]
                
                Text:
                $limitedText
            """.trimIndent()

            // Use Cloud Run backend instead of direct API
            val apiService = com.group_7.studysage.data.api.CloudRunApiService(BuildConfig.CLOUD_RUN_URL)
            val jsonText = apiService.generateContent(prompt)

            // Parse JSON response
            parseFlashcardsFromJson(jsonText)

        } catch (e: Exception) {
            Log.e(TAG, "AI generation error: ${e.message}", e)
            throw Exception("Failed to generate flashcards: ${e.message}")
        }
    }

    private fun parseFlashcardsFromJson(jsonText: String): List<Flashcard> {
        return try {
            // Remove markdown code blocks if present
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Parse JSON array
            val jsonArray = JSONArray(cleanJson)
            val flashcards = mutableListOf<Flashcard>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val question = obj.getString("question")
                val answer = obj.getString("answer")

                flashcards.add(
                    Flashcard(
                        id = "temp_$i",
                        question = question,
                        answer = answer,
                        category = null,
                        difficulty = "medium"
                    )
                )
            }

            flashcards
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}", e)
            throw Exception("Failed to parse flashcards: ${e.message}")
        }
    }

    fun clearTempFlashcardState() {
        _tempFlashcardState.update {
            TempFlashcardState()
        }
    }

    // ============================================
    // NOTE DETAIL SCREEN STATE MANAGEMENT
    // ============================================

    /**
     * Show/hide AI Summary screen
     * State is preserved across rotation via SavedStateHandle
     */
    fun setShowAiSummaryScreen(show: Boolean) {
        Log.d(TAG, "Setting showAiSummaryScreen: $show")
        savedStateHandle[SHOW_AI_SUMMARY_KEY] = show
        _showAiSummaryScreen.value = show
    }

    /**
     * Show/hide Flashcards screen
     * State is preserved across rotation via SavedStateHandle
     */
    fun setShowFlashcardsScreen(show: Boolean) {
        Log.d(TAG, "Setting showFlashcardsScreen: $show")
        savedStateHandle[SHOW_FLASHCARDS_KEY] = show
        _showFlashcardsScreen.value = show
    }

    /**
     * Show/hide NFC Share dialog
     * State is preserved across rotation via SavedStateHandle
     */
    fun setShowNfcShareDialog(show: Boolean) {
        Log.d(TAG, "Setting showNfcShareDialog: $show")
        savedStateHandle[SHOW_NFC_SHARE_KEY] = show
        _showNfcShareDialog.value = show
    }

    /**
     * Show/hide Podcast screen
     * State is preserved across rotation via SavedStateHandle
     */
    fun setShowPodcastScreen(show: Boolean) {
        Log.d(TAG, "Setting showPodcastScreen: $show")
        savedStateHandle[SHOW_PODCAST_KEY] = show
        _showPodcastScreen.value = show

        // Clear podcast data when closing the screen
        if (!show) {
            clearPodcastData()
        }
    }

    /**
     * Set note to share via NFC
     * State is preserved across rotation via SavedStateHandle
     */
    fun setNfcShareNote(note: Note?) {
        Log.d(TAG, "Setting NFC share note: ${note?.id ?: "null"}")
        if (note != null) {
            savedStateHandle[NFC_SHARE_NOTE_ID_KEY] = note.id
            _nfcShareNote.value = note
        } else {
            savedStateHandle.remove<String>(NFC_SHARE_NOTE_ID_KEY)
            _nfcShareNote.value = null
        }
    }

    /**
     * Load note for NFC sharing by ID
     * Called during restoration after rotation
     */
    private fun loadNoteForNfcShare(noteId: String) {
        if (noteId.isBlank()) return

        Log.d(TAG, "Loading note for NFC share: $noteId")
        viewModelScope.launch {
            try {
                val note = notesRepository.getNoteById(noteId)
                if (note != null) {
                    _nfcShareNote.value = note
                    Log.d(TAG, "NFC share note loaded: $noteId")
                } else {
                    Log.e(TAG, "Note not found for NFC share: $noteId")
                    // Clear the state if note not found
                    setShowNfcShareDialog(false)
                    setNfcShareNote(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load NFC share note: ${e.message}", e)
                // Clear the state on error
                setShowNfcShareDialog(false)
                setNfcShareNote(null)
            }
        }
    }

    /**
     * Clear all note detail screen states
     * Call this when closing the note detail or navigating away
     */
    fun clearAllNoteDetailScreens() {
        Log.d(TAG, "Clearing all note detail screen states")
        setShowAiSummaryScreen(false)
        setShowFlashcardsScreen(false)
        setShowNfcShareDialog(false)
        setShowPodcastScreen(false)
        setNfcShareNote(null)
    }
}
