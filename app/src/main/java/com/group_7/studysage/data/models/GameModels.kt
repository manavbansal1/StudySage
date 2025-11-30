package com.group_7.studysage.data.models

import kotlinx.serialization.Serializable

// ============================================
// GAME TYPES & STATUS
// ============================================

@Serializable
enum class GameType {
    QUIZ_RACE,
    FLASHCARD_BATTLE,
    STUDY_TAC_TOE,
    SPEED_MATCH,
    SURVIVAL_MODE,
    SPEED_QUIZ
}

@Serializable
enum class GameStatus {
    WAITING,
    IN_PROGRESS,
}

@Serializable
enum class ContentSource {
    PDF,
    TEXT
}

@Serializable
enum class MessageType {
    // Connection
    PLAYER_JOINED,
    PLAYER_LEFT,
    PLAYER_READY,

    // Game flow
    GAME_STARTING,
    GAME_STARTED,
    NEXT_QUESTION,
    TURN_UPDATE, // For turn-based games like STUDY_TAC_TOE
    BOARD_UPDATE, // For syncing board state in STUDY_TAC_TOE
    GAME_FINISHED,

    // Answers
    SUBMIT_ANSWER,
    ANSWER_RESULT,
    SCORES_UPDATE,

    // Flashcard Battle specific
    FLASHCARD_REVEALED,

    // Speed Match specific
    MATCH_PAIR,
    // Chat
    CHAT_MESSAGE,

    // Room management
    ROOM_UPDATE,

    // Errors
    ERROR,
}

// ============================================
// GAME CONTENT MODELS
// ============================================

@Serializable
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int, // Index of correct option
    val timeLimit: Int = 30, // seconds
    val points: Int = 100,
    val difficulty: String = "medium",
    val explanation: String? = null
)

@Serializable
data class Flashcard(
    val id: String,
    val question: String,
    val answer: String,
    val category: String? = null,
    val difficulty: String = "medium"
)

@Serializable
data class MatchPair(
    val id: String,
    val term: String,
    val definition: String,
    val matched: Boolean = false
)

// ============================================
// PLAYER & TEAM MODELS
// ============================================

@Serializable
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val answeredQuestions: List<String> = emptyList(),
    val correctAnswers: Int = 0,
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val teamId: String? = null,
    val streakCount: Int = 0,
    val profilePic: String? = null
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    val memberIds: List<String> = emptyList(),
    val score: Int = 0,
    val color: String = "#4CAF50"
)

// ============================================
// GAME SESSION MODELS
// ============================================

@Serializable
data class GameSettings(
    val questionTimeLimit: Int = 30,
    val numberOfQuestions: Int = 10,
    val pointsPerCorrectAnswer: Int = 100,
    val pointsForSpeed: Boolean = true,
    val speedBonusMultiplier: Double = 1.5,
    val streakBonus: Boolean = true,
    val streakBonusPoints: Int = 50,
    val allowSpectators: Boolean = true,
    val isPrivate: Boolean = false,
    val difficulty: String = "medium",
    val teamMode: Boolean = false,
    val numberOfTeams: Int = 2,
    val autoStartWhenReady: Boolean = false,
    val showCorrectAnswers: Boolean = true,
    val showLeaderboardAfterQuestion: Boolean = true
)

@Serializable
data class GameSessionData(
    val id: String,
    val name: String,
    val gameType: GameType,
    val hostId: String,
    val hostName: String,
    val hostProfilePic: String? = null,
    val gameCode: String, // Unique code for joining
    val players: Map<String, Player> = emptyMap(),
    val teams: Map<String, Team> = emptyMap(),
    val maxPlayers: Int = 2,
    val status: GameStatus = GameStatus.WAITING,
    val currentQuestionIndex: Int = 0,
    val currentTurn: String? = null, // Player ID whose turn it is (for turn-based games)
    val boardState: List<String>? = null, // For STUDY_TAC_TOE: 9 elements, empty string or "X" or "O"
    val questions: List<QuizQuestion> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val matchPairs: List<MatchPair> = emptyList(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val pausedAt: Long? = null,
    val settings: GameSettings = GameSettings(),
    val contentSource: ContentSource = ContentSource.TEXT,
    val contentData: String? = null, // PDF URL or text content
    val topicDescription: String? = null, // User-provided topic description
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class GameAnswer(
    val playerId: String,
    val questionId: String,
    val answer: Int, // Selected option index or 0/1 for boolean
    val timeElapsed: Long, // milliseconds
    val isCorrect: Boolean = false,
    val pointsEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================
// WEBSOCKET MESSAGE MODELS
// ============================================

@Serializable
data class WebSocketMessage(
    val type: MessageType,
    val data: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PlayerJoinedData(
    val player: Player,
    val totalPlayers: Int
)


@Serializable
data class QuestionData(
    val question: QuizQuestion,
    val questionNumber: Int,
    val totalQuestions: Int,
    val timeLimit: Int
)

@Serializable
data class FlashcardData(
    val flashcard: Flashcard,
    val flashcardNumber: Int,
    val totalFlashcards: Int,
    val timeLimit: Int,
    val showAnswer: Boolean = false
)

@Serializable
data class AnswerResultData(
    val playerId: String,
    val playerName: String,
    val isCorrect: Boolean,
    val points: Int,
    val timeElapsed: Long,
    val correctAnswer: Int? = null,
    val explanation: String? = null,
    val streakBonus: Int = 0
)

@Serializable
data class ScoresUpdateData(
    val scores: Map<String, Int>,
    val teamScores: Map<String, Int> = emptyMap(),
    val leaderboard: List<LeaderboardEntry>
)

@Serializable
data class LeaderboardEntry(
    val playerId: String,
    val playerName: String,
    val profilePic: String? = null,
    val score: Int,
    val rank: Int,
    val teamId: String? = null,
    val streakCount: Int = 0
)

@Serializable
data class MatchPairSubmission(
    val playerId: String,
    val termId: String,
    val definitionId: String,
    val timeElapsed: Long
)

@Serializable
data class ChatMessageData(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val teamOnly: Boolean = false
)

@Serializable
data class ErrorData(
    val code: String,
    val message: String,
    val details: String? = null
)



@Serializable
data class GameResult(
    val roomId: String,
    val gameType: GameType,
    val players: List<PlayerResult>,
    val teams: List<TeamResult> = emptyList(),
    val duration: Long,
    val totalQuestions: Int,
    val startTime: Long,
    val endTime: Long,
    val winner: String? = null,
    val winningTeam: String? = null
)

@Serializable
data class PlayerResult(
    val playerId: String,
    val playerName: String,
    val profilePic: String? = null,
    val finalScore: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val averageTime: Long,
    val fastestAnswer: Long,
    val longestStreak: Int,
    val rank: Int,
    val teamId: String? = null,
    val xpEarned: Int = 0
)

@Serializable
data class TeamResult(
    val teamId: String,
    val teamName: String,
    val finalScore: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val rank: Int,
    val memberIds: List<String>
)

// ============================================
// UI STATE MODELS
// ============================================

data class GameUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentSession: GameSessionData? = null,
    val currentQuestion: QuestionData? = null,
    val currentFlashcard: FlashcardData? = null,
    val currentMatchPairs: List<MatchPair> = emptyList(),
    val selectedAnswerIndex: Int? = null,
    val timeRemaining: Int = 0,
    val isAnswered: Boolean = false,
    val lastResult: AnswerResultData? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val chatMessages: List<ChatMessageData> = emptyList(),
    val isHost: Boolean = false,
    val gameFinished: Boolean = false,
    val finalResults: GameResult? = null
)

data class LobbyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSessions: List<GameSessionData> = emptyList(),
    val currentSession: GameSessionData? = null,
    val isJoining: Boolean = false,
    val isCreating: Boolean = false
)

// ============================================
// REQUEST/RESPONSE MODELS FOR NEW STANDALONE API
// ============================================

@Serializable
data class HostGameRequest(
    val hostId: String,
    val hostName: String,
    val gameType: GameType,
    val contentSource: ContentSource,
    val contentData: String? = null, // PDF URL or text content
    val topicDescription: String? = null, // User-provided description of topics
    val settings: GameSettings = GameSettings()
)

@Serializable
data class HostGameResponse(
    val gameCode: String,
    val sessionId: String,
    val message: String = "Game created successfully"
)

@Serializable
data class JoinGameRequest(
    val gameCode: String,
    val userId: String,
    val userName: String
)

@Serializable
data class JoinGameResponse(
    val sessionId: String,
    val gameCode: String,
    val session: GameSessionData,
    val message: String = "Joined game successfully"
)

@Serializable
data class GlobalLeaderboardEntry(
    val userId: String,
    val userName: String,
    val profilePic: String? = null,
    val totalGames: Int = 0,
    val gamesWon: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Double = 0.0,
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val rank: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)