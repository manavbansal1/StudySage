package com.group_7.studysage.ui.screens.GameScreen.QuizGame.data

data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)
