package com.group_7.studysage.data.repository

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