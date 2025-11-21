package com.group_7.studysage.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single option in a quiz question
 */
data class QuizOption(
    @SerializedName("text")
    val text: String,
    
    @SerializedName("isCorrect")
    val isCorrect: Boolean
)

/**
 * Represents a single quiz question with multiple options
 */
data class QuizQuestion(
    @SerializedName("question")
    val question: String,
    
    @SerializedName("options")
    val options: List<QuizOption>,
    
    @SerializedName("explanation")
    val explanation: String = ""
)

/**
 * Represents a complete quiz with metadata and questions
 */
data class Quiz(
    @SerializedName("quizId")
    val quizId: String = "",
    
    @SerializedName("noteId")
    val noteId: String,
    
    @SerializedName("noteTitle")
    val noteTitle: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("questions")
    val questions: List<QuizQuestion>,
    
    @SerializedName("preferences")
    val preferences: String = "",
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @SerializedName("totalQuestions")
    val totalQuestions: Int = questions.size
)

/**
 * Response wrapper for quiz generation from Gemini AI
 */
data class QuizGenerationResponse(
    @SerializedName("questions")
    val questions: List<QuizQuestion>
)
