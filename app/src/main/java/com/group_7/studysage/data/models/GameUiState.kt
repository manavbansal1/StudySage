package com.group_7.studysage.data.models

data class GameUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val gameFinished: Boolean = false,
    val currentQuestion: QuestionData? = null,
    val currentSession: GameSession? = null,
    val isHost: Boolean = false,
    val finalResults: FinalResults? = null,
    val lastResult: AnswerResult? = null,
    val isAnswered: Boolean = false,
    val selectedAnswerIndex: Int? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList()
)

data class QuestionData(
    val question: QuizQuestion,
    val questionNumber: Int,
    val totalQuestions: Int,
    val timeLimit: Int = 30
)

data class GameSession(
    val id: String = "",
    val name: String = "",
    val gameType: GameType = GameType.QUIZ_RACE,
    val players: Map<String, Player> = emptyMap(),
    val maxPlayers: Int = 8,
    val currentQuestionIndex: Int = 0,
    val status: String = "waiting"
)

data class FinalResults(
    val players: List<Player> = emptyList(),
    val winner: Player? = null
)

data class AnswerResult(
    val isCorrect: Boolean = false,
    val points: Int = 0,
    val correctAnswerIndex: Int = 0
)
