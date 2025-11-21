package com.group_7.studysage.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.group_7.studysage.data.api.ApiResponse
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.model.QuizGenerationResponse
import com.group_7.studysage.data.models.*
import com.group_7.studysage.data.websocket.ConnectionState
import com.group_7.studysage.data.websocket.GameWebSocketManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing multiplayer game operations
 * Combines REST API calls and WebSocket real-time communication
 */
class GameRepository(
    private val apiService: GameApiService = GameApiService(),
    private val wsManager: GameWebSocketManager = GameWebSocketManager()
) {

    // ============================================
    // WebSocket State Flows (Real-time)
    // ============================================

    val connectionState: StateFlow<ConnectionState> = wsManager.connectionState
    val playerJoined: StateFlow<PlayerJoinedData?> = wsManager.playerJoined
    val playerLeft: StateFlow<String?> = wsManager.playerLeft
    val roomUpdate: StateFlow<GameSessionData?> = wsManager.roomUpdate
    val gameStarting: StateFlow<Int?> = wsManager.gameStarting
    val gameStarted: StateFlow<GameSessionData?> = wsManager.gameStarted
    val nextQuestion: StateFlow<QuestionData?> = wsManager.nextQuestion
    val nextFlashcard: StateFlow<FlashcardData?> = wsManager.nextFlashcard
    val answerResult: StateFlow<AnswerResultData?> = wsManager.answerResult
    val scoresUpdate: StateFlow<ScoresUpdateData?> = wsManager.scoresUpdate
    val gameFinished: StateFlow<GameResult?> = wsManager.gameFinished
    val chatMessage: StateFlow<ChatMessageData?> = wsManager.chatMessage
    val errorMessage: StateFlow<ErrorData?> = wsManager.errorMessage

    // ============================================
    // HEALTH & INFO
    // ============================================

    suspend fun checkHealth() = apiService.checkHealth()

    // ============================================
    // GAME SESSION MANAGEMENT (REST API)
    // ============================================

    /**
     * Create a new game session
     */
    suspend fun createGameSession(
        groupId: String,
        documentId: String?,
        documentName: String?,
        hostId: String,
        hostName: String,
        gameType: GameType,
        settings: GameSettings = GameSettings()
    ) = apiService.createGameSession(
        groupId = groupId,
        documentId = documentId,
        documentName = documentName,
        hostId = hostId,
        hostName = hostName,
        gameType = gameType,
        settings = settings
    )

    /**
     * Get all active game sessions in a group
     */
    suspend fun getActiveGameSessions(groupId: String) =
        apiService.getActiveGameSessions(groupId)

    /**
     * Get specific game session details
     */
    suspend fun getGameSession(groupId: String, sessionId: String) =
        apiService.getGameSession(groupId, sessionId)

    /**
     * Join a game session
     */
    suspend fun joinGameSession(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ) = apiService.joinGameSession(groupId, sessionId, userId, userName)

    /**
     * Leave a game session
     */
    suspend fun leaveGameSession(
        groupId: String,
        sessionId: String,
        userId: String
    ) = apiService.leaveGameSession(groupId, sessionId, userId)

    // ============================================
    // GAME CONTROL (REST API)
    // ============================================

    /**
     * Start a game (host only)
     */
    suspend fun startGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.startGame(groupId, sessionId, hostId)

    /**
     * Pause a game (host only)
     */
    suspend fun pauseGame(
        groupId: String,
        sessionId: String,
        hostId: String,
        reason: String? = null
    ) = apiService.pauseGame(groupId, sessionId, hostId, reason)

    /**
     * Resume a game (host only)
     */
    suspend fun resumeGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.resumeGame(groupId, sessionId, hostId)

    /**
     * End a game early (host only)
     */
    suspend fun endGame(
        groupId: String,
        sessionId: String,
        hostId: String
    ) = apiService.endGame(groupId, sessionId, hostId)

    // ============================================
    // GAME RESULTS & STATS (REST API)
    // ============================================

    /**
     * Get game results after game finishes
     */
    suspend fun getGameResults(groupId: String, sessionId: String) =
        apiService.getGameResults(groupId, sessionId)

    /**
     * Get user's game statistics
     */
    suspend fun getUserStats(userId: String) =
        apiService.getUserStats(userId)

    /**
     * Get user's game history
     */
    suspend fun getUserHistory(userId: String, limit: Int = 20) =
        apiService.getUserHistory(userId, limit)

    /**
     * Get group leaderboard
     */
    suspend fun getGroupLeaderboard(
        groupId: String,
        gameType: String? = null,
        limit: Int = 20
    ) = apiService.getGroupLeaderboard(groupId, gameType, limit)

    // ============================================
    // WEBSOCKET CONNECTION
    // ============================================

    /**
     * Connect to game session via WebSocket
     */
    fun connectToGameSession(
        groupId: String,
        sessionId: String,
        userId: String,
        userName: String
    ) {
        wsManager.connect(groupId, sessionId, userId, userName)
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnectFromGame() {
        wsManager.disconnect()
    }

    // ============================================
    // WEBSOCKET ACTIONS
    // ============================================

    /**
     * Send player ready status
     */
    fun sendPlayerReady(isReady: Boolean = true) {
        wsManager.sendPlayerReady(isReady)
    }

    /**
     * Send game starting signal (host only)
     */
    fun sendGameStarting() {
        wsManager.sendGameStarting()
    }

    /**
     * Submit answer for quiz question
     */
    fun submitQuizAnswer(
        playerId: String,
        questionId: String,
        answerIndex: Int,
        timeElapsed: Long
    ) {
        wsManager.submitAnswer(playerId, questionId, answerIndex, timeElapsed)
    }

    /**
     * Submit flashcard answer
     */
    fun submitFlashcardAnswer(
        playerId: String,
        flashcardId: String,
        isCorrect: Boolean,
        timeElapsed: Long
    ) {
        wsManager.submitFlashcardAnswer(playerId, flashcardId, isCorrect, timeElapsed)
    }

    /**
     * Submit match pair for Speed Match game
     */
    fun submitMatchPair(
        playerId: String,
        termId: String,
        definitionId: String,
        timeElapsed: Long
    ) {
        wsManager.submitMatchPair(playerId, termId, definitionId, timeElapsed)
    }

    /**
     * Send chat message
     */
    fun sendChatMessage(
        senderId: String,
        senderName: String,
        message: String,
        teamOnly: Boolean = false
    ) {
        wsManager.sendChatMessage(senderId, senderName, message, teamOnly)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        wsManager.cleanup()
    }

    // ============================================
    // QUIZ GENERATION (for QuizGenerationScreen)
    // ============================================

    private val firestore = Firebase.firestore
    private val gson = Gson()
    private val notesRepository = NotesRepository()

    /**
     * Generate quiz questions from note content using AI
     */
    suspend fun generateQuizQuestions(
        noteId: String,
        noteTitle: String,
        content: String,
        userPreferences: String
    ): Result<Quiz> {
        return try {
            // Use NotesRepository's AI generation with quiz-specific prompt
            val quizPrompt = """
                $userPreferences
                
                Based on the following content, generate a quiz with 5 multiple-choice questions.
                Each question must have exactly 4 options with only one correct answer.
                
                Return ONLY valid JSON in this exact format (no markdown, no code blocks, no extra text):
                {
                  "questions": [
                    {
                      "question": "question text here",
                      "options": [
                        {"text": "option 1", "isCorrect": false},
                        {"text": "option 2", "isCorrect": true},
                        {"text": "option 3", "isCorrect": false},
                        {"text": "option 4", "isCorrect": false}
                      ],
                      "explanation": "brief explanation of correct answer"
                    }
                  ]
                }
                
                Content:
                $content
            """.trimIndent()

            val aiResponse = notesRepository.generateAISummary(
                content = quizPrompt,
                wantsNewSummary = true,
                userPreferences = ""
            )

            // Clean up the JSON response more thoroughly
            var jsonResponse = aiResponse.trim()
            
            // Remove markdown code blocks
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
            }
            
            // Find the JSON object if there's extra text
            val jsonStart = jsonResponse.indexOf("{")
            val jsonEnd = jsonResponse.lastIndexOf("}") + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse = jsonResponse.substring(jsonStart, jsonEnd)
            }

            android.util.Log.d("GameRepository", "Cleaned JSON: $jsonResponse")

            val quizResponse = gson.fromJson(jsonResponse, QuizGenerationResponse::class.java)
            
            if (quizResponse.questions.isEmpty()) {
                return Result.failure(Exception("No questions were generated"))
            }
            
            val quiz = Quiz(
                quizId = "",
                noteId = noteId,
                noteTitle = noteTitle,
                userId = "",
                questions = quizResponse.questions,
                preferences = userPreferences,
                createdAt = System.currentTimeMillis(),
                totalQuestions = quizResponse.questions.size
            )

            Result.success(quiz)
        } catch (e: Exception) {
            android.util.Log.e("GameRepository", "Quiz generation error", e)
            Result.failure(Exception("Failed to generate quiz: ${e.message}"))
        }
    }

    /**
     * Save generated quiz to Firestore
     */
    suspend fun saveQuizToFirestore(quiz: Quiz): Result<String> {
        return try {
            val quizData = hashMapOf(
                "quizId" to quiz.quizId,
                "noteId" to quiz.noteId,
                "noteTitle" to quiz.noteTitle,
                "userId" to quiz.userId,
                "questions" to quiz.questions.map { question ->
                    hashMapOf(
                        "question" to question.question,
                        "options" to question.options.map { option ->
                            hashMapOf(
                                "text" to option.text,
                                "isCorrect" to option.isCorrect
                            )
                        },
                        "explanation" to question.explanation
                    )
                },
                "preferences" to quiz.preferences,
                "createdAt" to quiz.createdAt,
                "totalQuestions" to quiz.totalQuestions
            )

            val docRef = firestore.collection("quizzes").add(quizData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert quiz to JSON string
     */
    fun quizToJson(quiz: Quiz): String {
        return gson.toJson(quiz)
    }
}