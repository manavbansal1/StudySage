package com.group_7.studysage.data.models

data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: Int = 0,
    val difficulty: String = "",
    val explanation: String = ""
)
