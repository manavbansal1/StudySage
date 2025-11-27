package com.group_7.studysage.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
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
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NotesViewModel"
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

    init {
        // Initial load is handled by LaunchedEffect in the UI, which can provide courseId
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

    fun selectNote(note: Note) {
        Log.d(TAG, "Selecting note ${note.id}")
        _selectedNote.value = note
    }

    fun clearSelectedNote() {
        Log.d(TAG, "Clearing selected note")
        _selectedNote.value = null
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

            // Use Gemini API (use stable model instead of experimental)
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 8192 // Sufficient token limit for flashcard generation
                }
            )

            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text ?: throw Exception("Empty response from AI")

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
}
