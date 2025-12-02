/**
 * Handles the multiplayer quiz game stuff. Think Kahoot but with your own study notes.
 * You can challenge friends using questions generated from your uploaded materials.
 * 
 * - Creates game lobbies with custom settings
 * - Uses AI to generate quiz questions from your notes
 * - Manages real-time gameplay with WebSockets
 * - Keeps track of scores and player stats
 * - Handles different game modes and difficulties
 * 
 * Game flow is pretty simple:
 * 1. Someone creates a game and shares the code
 * 2. Others join with the code
 * 3. AI generates questions from selected notes
 * 4. Everyone answers in real-time with live leaderboards
 * 5. Results get saved to your profile
 * 6. In the case that any player or even bothj do not answer the question, they will be marked as incorrect and no points will be awarded.
 * 
 * Uses WebSockets for real-time updates and caches questions to avoid spamming the AI.
 */
package com.group_7.studysage.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
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
    private val wsManager: GameWebSocketManager = GameWebSocketManager()
) {
    val errorMessage: StateFlow<ErrorData?> = wsManager.errorMessage


    private val firestore = Firebase.firestore
    private val gson = Gson()
    private val notesRepository = NotesRepository()

    /**
     * Generate quiz questions from note content using AI
     * returns in the specific Quiz format
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