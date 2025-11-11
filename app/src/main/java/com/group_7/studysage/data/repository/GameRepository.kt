package com.group_7.studysage.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.BuildConfig
import com.group_7.studysage.data.model.Quiz
import com.group_7.studysage.data.model.QuizQuestion
import com.group_7.studysage.data.model.QuizGenerationResponse
import kotlinx.coroutines.tasks.await


class GameRepository {

    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    private val notesRepository = NotesRepository() // Use working NotesRepository
    private val TAG = "GameRepository"



    suspend fun generateQuizQuestions(
        noteId: String,
        noteTitle: String,
        content: String,
        userPreferences: String
    ): Result<Quiz> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

            val limitedContent = if (content.length > 8000) {
                content.take(8000) + "..."
            } else content

            val fullPrompt = """
You are an expert educational quiz creator. Generate exactly 10 multiple-choice questions based on the provided content.

User Preferences: $userPreferences
Content: $limitedContent

IMPORTANT: Return ONLY valid JSON in this exact format, with no additional text before or after:
{
  "quiz": [
    {
      "question": "Question text here?",
      "options": [
        {"text": "Option A", "isCorrect": false},
        {"text": "Option B", "isCorrect": true},
        {"text": "Option C", "isCorrect": false},
        {"text": "Option D", "isCorrect": false}
      ],
      "explanation": "Brief explanation of why the correct answer is right"
    }
  ]
}

Requirements:
- Generate exactly 10 questions
- Each question must have exactly 4 options
- Only ONE option can have "isCorrect": true
- Questions should cover key concepts from the content
- Consider user preferences: $userPreferences
- Include brief explanations for correct answers
- Return ONLY the JSON object, no markdown, no code blocks, no additional text
            """.trimIndent()

            Log.d(TAG, "Generating quiz using NotesRepository AI...")
            
            // Use the working NotesRepository AI generation method
            val responseText = notesRepository.generateAISummary(
                content = fullPrompt,
                wantsNewSummary = true,
                userPreferences = "Generate quiz in JSON format only"
            )

            Log.d(TAG, "Received response: ${responseText.take(200)}...")

            if (responseText == "Error generating AI summary: Empty response") {
                throw Exception("No response from AI")
            }

            if (responseText.startsWith("Error generating")) {
                throw Exception(responseText)
            }

            // Clean the response - remove markdown code blocks if present
            val cleanedJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Parse the JSON response
            val quizResponse = try {
                gson.fromJson(cleanedJson, QuizGenerationResponse::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSON parsing error: ${e.message}")
                Log.e(TAG, "Response text: $cleanedJson")
                
                // Try to extract JSON from response if it's wrapped in text
                val jsonStart = cleanedJson.indexOf("{")
                val jsonEnd = cleanedJson.lastIndexOf("}") + 1
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val extractedJson = cleanedJson.substring(jsonStart, jsonEnd)
                    Log.d(TAG, "Trying extracted JSON: $extractedJson")
                    try {
                        gson.fromJson(extractedJson, QuizGenerationResponse::class.java)
                    } catch (e2: JsonSyntaxException) {
                        throw Exception("Failed to parse quiz JSON: ${e.message}")
                    }
                } else {
                    throw Exception("Failed to parse quiz JSON: ${e.message}")
                }
            }

            // Validate questions
            if (quizResponse.quiz.isEmpty()) {
                throw Exception("No questions generated")
            }

            if (quizResponse.quiz.size < 5) {
                throw Exception("Too few questions generated (${quizResponse.quiz.size})")
            }

            Log.d(TAG, "Successfully generated ${quizResponse.quiz.size} questions")

            // Create Quiz object
            val quiz = Quiz(
                quizId = "", // Will be set by backend
                noteId = noteId,
                noteTitle = noteTitle,
                userId = userId,
                questions = quizResponse.quiz,
                preferences = userPreferences,
                createdAt = System.currentTimeMillis()
            )

            Result.success(quiz)

        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed: ${e.message}", e)
            Result.failure(Exception("Failed to generate quiz: ${e.message}"))
        }
    }

    /**
     * Save quiz to Firestore
     */
    suspend fun saveQuizToFirestore(quiz: Quiz): Result<String> {
        return try {
            val quizRef = firestore.collection("quizzes").document()
            val quizWithId = quiz.copy(quizId = quizRef.id)
            
            val quizData = hashMapOf(
                "quizId" to quizWithId.quizId,
                "noteId" to quizWithId.noteId,
                "noteTitle" to quizWithId.noteTitle,
                "userId" to quizWithId.userId,
                "questions" to quizWithId.questions.map { question ->
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
                "preferences" to quizWithId.preferences,
                "createdAt" to quizWithId.createdAt,
                "totalQuestions" to quizWithId.totalQuestions
            )
            
            quizRef.set(quizData).await()
            Result.success(quizRef.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save quiz: ${e.message}", e)
            Result.failure(Exception("Failed to save quiz: ${e.message}"))
        }
    }

    /**
     * Convert Quiz to JSON string for backend submission
     */
    fun quizToJson(quiz: Quiz): String {
        return gson.toJson(quiz)
    }
}