package com.group_7.studysage.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.repository.GameRepository
import com.group_7.studysage.data.repository.Note
import com.group_7.studysage.data.repository.NotesRepository
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuizGenerationState(
    val isLoading: Boolean = false,
    val availableNotes: List<Note> = emptyList(),
    val selectedNote: Note? = null,
    val userPreferences: String = "",
    val generatedQuiz: Quiz? = null,
    val generatedQuestions: List<QuizQuestion> = emptyList(), // For temporary quiz
    val error: String? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val savedQuizId: String? = null
)

data class StandaloneGameState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val gameCode: String? = null
)

class GameViewModel(
    private val gameApiService: GameApiService,
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel,
    private val gameRepository: GameRepository = GameRepository(),
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _lobbyUiState = MutableStateFlow(LobbyUiState())
    val lobbyUiState: StateFlow<LobbyUiState> = _lobbyUiState.asStateFlow()

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    private val _quizGenerationState = MutableStateFlow(QuizGenerationState())
    val quizGenerationState: StateFlow<QuizGenerationState> = _quizGenerationState

    init {
        observeWebSocket()
    }

    private fun observeWebSocket() {
        webSocketManager.connectionState
            .onEach { state ->
                _gameUiState.value = _gameUiState.value.copy(
                    isLoading = state is ConnectionState.Connecting,
                    error = if (state is ConnectionState.Error) state.message else null
                )
            }
            .launchIn(viewModelScope)

        webSocketManager.roomUpdate
            .onEach { session ->
                session?.let {
                    _gameUiState.value = _gameUiState.value.copy(currentSession = it)
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.nextQuestion
            .onEach { questionData ->
                _gameUiState.value = _gameUiState.value.copy(
                    currentQuestion = questionData,
                    isAnswered = false,
                    selectedAnswerIndex = null,
                    timeRemaining = questionData?.timeLimit ?: 0
                )
            }
            .launchIn(viewModelScope)

        webSocketManager.answerResult
            .onEach { result ->
                _gameUiState.value = _gameUiState.value.copy(lastResult = result)
            }
            .launchIn(viewModelScope)

        webSocketManager.scoresUpdate
            .onEach { scores ->
                scores?.let {
                    _gameUiState.value = _gameUiState.value.copy(leaderboard = it.leaderboard)
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.gameFinished
            .onEach { result ->
                result?.let {
                    _gameUiState.value = _gameUiState.value.copy(
                        gameFinished = true,
                        finalResults = it
                    )
                }
            }
            .launchIn(viewModelScope)

        webSocketManager.chatMessage
            .onEach { message ->
                message?.let {
                    val updatedMessages = _gameUiState.value.chatMessages + it
                    _gameUiState.value = _gameUiState.value.copy(chatMessages = updatedMessages)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }

    // ============================================
    // STANDALONE GAME OPERATIONS (No Group Dependency)
    // ============================================

    private val _standaloneGameState = MutableStateFlow(StandaloneGameState())
    val standaloneGameState: StateFlow<StandaloneGameState> = _standaloneGameState.asStateFlow()
    /**
     * Load all notes available for quiz generation
     */
    fun loadAvailableNotes() {
        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(isLoading = true, error = null)

            try {
                val notes = notesRepository.getUserNotes()
                // Filter notes that have content
                val validNotes = notes.filter { it.content.isNotBlank() }
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    availableNotes = validNotes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading notes"
                )
            }
        }
    }

    /**
     * Set the selected note for quiz generation
     */
    fun setSelectedNote(note: Note) {
        _quizGenerationState.value = _quizGenerationState.value.copy(selectedNote = note)
    }

    /**
     * Update user preferences for quiz generation
     */
    fun setUserPreferences(preferences: String) {
        _quizGenerationState.value = _quizGenerationState.value.copy(userPreferences = preferences)
    }

    /**
     * Generate quiz from selected note and preferences
     */
    fun generateQuiz() {
        val state = _quizGenerationState.value
        val note = state.selectedNote

        if (note == null) {
            _quizGenerationState.value = state.copy(error = "Please select a note first")
            return
        }

        if (note.content.isBlank()) {
            _quizGenerationState.value = state.copy(error = "Selected note has no content")
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = state.copy(isGenerating = true, error = null)

            try {
                val result = gameRepository.generateQuizQuestions(
                    noteId = note.id,
                    noteTitle = note.title,
                    content = note.content,
                    userPreferences = state.userPreferences
                )

                result.fold(
                    onSuccess = { quiz ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            generatedQuiz = quiz,
                            isGenerating = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isGenerating = false,
                            error = error.message ?: "Failed to generate quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Error generating quiz"
                )
            }
        }
    }

    /**
     * Save generated quiz to Firestore
     */
    fun saveQuiz() {
        val quiz = _quizGenerationState.value.generatedQuiz

        if (quiz == null) {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                error = "No quiz to save"
            )
            return
        }

        viewModelScope.launch {
            _quizGenerationState.value = _quizGenerationState.value.copy(
                isSaving = true,
                error = null
            )

            try {
                val result = gameRepository.saveQuizToFirestore(quiz)

                result.fold(
                    onSuccess = { quizId ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            savedQuizId = quizId,
                            isSaving = false
                        )
                    },
                    onFailure = { error ->
                        _quizGenerationState.value = _quizGenerationState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _quizGenerationState.value = _quizGenerationState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Error saving quiz"
                )
            }
        }
    }

    /**
     * Get quiz as JSON string for backend submission
     */
    fun getQuizJson(): String? {
        val quiz = _quizGenerationState.value.generatedQuiz
        return quiz?.let { gameRepository.quizToJson(it) }
    }

    /**
     * Reset quiz generation state
     */
    fun resetQuizGeneration() {
        _quizGenerationState.value = QuizGenerationState()
    }

    /**
     * Clear quiz generation state (for temporary quizzes).
     * Resets the generated questions and error state while preserving other settings.
     */
    fun clearQuizGenerationState() {
        _quizGenerationState.update {
            QuizGenerationState(
                isGenerating = false,
                generatedQuestions = emptyList(),
                error = null
            )
        }
    }

    /**
     * Clear only the error state in quiz generation.
     * Used after showing error message to user.
     */
    fun clearError() {
        _quizGenerationState.update {
            it.copy(error = null)
        }
    }

    // ============================================
    // TEMPORARY QUIZ GENERATION FROM PDF
    // ============================================

    /**
     * Generate a temporary quiz from PDF without saving to database.
     * This is used for quick quiz generation that exists only in memory.
     *
     * @param context Application context for PDF processing
     * @param pdfUri URI of the PDF file to process
     * @param fileName Name of the PDF file for logging
     * @param userPreferences Optional user preferences for quiz generation
     */
    fun generateTempQuizFromPdf(
        context: Context,
        pdfUri: Uri,
        fileName: String,
        userPreferences: String
    ) {
        viewModelScope.launch {
            try {
                // Set loading state
                _quizGenerationState.update {
                    it.copy(
                        isGenerating = true,
                        error = null
                    )
                }

                Log.d(TAG, "🎯 Starting temporary quiz generation from PDF: $fileName")

                // Step 1: Extract text from PDF
                val pdfText = withContext(Dispatchers.IO) {
                    extractTextFromPdf(context, pdfUri)
                }

                if (pdfText.isBlank()) {
                    throw Exception("Could not extract text from PDF. The file may be scanned or image-based.")
                }

                Log.d(TAG, "✅ Extracted ${pdfText.length} characters from PDF")

                // Step 2: Generate quiz questions using AI
                val result = gameRepository.generateQuizQuestions(
                    noteId = "temp_${System.currentTimeMillis()}", // Temporary ID
                    noteTitle = fileName,
                    content = pdfText,
                    userPreferences = userPreferences
                )

                result.fold(
                    onSuccess = { quiz ->
                        if (quiz.questions.isEmpty()) {
                            throw Exception("Could not generate questions from this content. Please try a different PDF.")
                        }

                        Log.d(TAG, "✅ Generated ${quiz.questions.size} quiz questions")

                        // Step 3: Update state with generated quiz
                        // Note: We do NOT save this quiz to database - it's temporary
                        // Convert data.model.QuizQuestion to data.models.QuizQuestion
                        val convertedQuestions = quiz.questions.map { q ->
                            com.group_7.studysage.data.models.QuizQuestion(
                                id = "",
                                question = q.question,
                                options = q.options.map { it.text },
                                correctAnswer = q.options.indexOfFirst { it.isCorrect },
                                explanation = q.explanation
                            )
                        }

                        _quizGenerationState.update {
                            it.copy(
                                isGenerating = false,
                                generatedQuestions = convertedQuestions,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        throw Exception("Failed to generate quiz: ${error.message}")
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error generating temporary quiz: ${e.message}", e)
                _quizGenerationState.update {
                    it.copy(
                        isGenerating = false,
                        error = "Failed to generate quiz: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Extract text from PDF file using PDFBox.
     *
     * @param context Application context
     * @param uri URI of the PDF file
     * @return Extracted text content
     * @throws Exception if PDF cannot be read or processed
     */
    private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            // Initialize PDFBox
            PDFBoxResourceLoader.init(context)

            // Read PDF and extract text
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper().apply {
                            setSortByPosition(true)
                        }
                        stripper.getText(document)
                    }
                } ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PDF text: ${e.message}", e)
            throw Exception("Failed to read PDF: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GameViewModel"
    }
}