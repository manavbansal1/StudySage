package com.group_7.studysage.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.BuildConfig

data class Quiz(
    val quizId: String = "",
    val title: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val fileUrl: String = "",
    val courseId: String = "",
    val questionsAndAnswers: Map<String, String> = emptyMap(),
    val totalQuestions : Int = 0
)

class QuizRepository {

    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Primary model for content generation
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 32
            topP = 1f
            maxOutputTokens = 4096
        },
    )

    // Fallback model
    private val fallbackModel = GenerativeModel(
        modelName = "gemini-1.0-pro",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 32
            topP = 1f
            maxOutputTokens = 2048
        },
    )

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun generateQuizQuestion(prompt: String): String {
        return try {

            return "";

        }
        catch (e: Exception) {
            "Error generating quiz question: ${e.message}"
        }

        return "";
    }




}