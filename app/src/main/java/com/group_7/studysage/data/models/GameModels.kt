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
    STARTING,
    IN_PROGRESS,
    PAUSED,
    FINISHED,
    CANCELLED
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
    PLAYER_RECONNECTED,

    // Team management
    TEAM_ASSIGNED,
    TEAM_UPDATE,

    // Game flow
    GAME_STARTING,
    GAME_STARTED,
    GAME_PAUSED,
    GAME_RESUMED,
    NEXT_QUESTION,
    QUESTION_TIMEOUT,
    TURN_UPDATE, // For turn-based games like STUDY_TAC_TOE
    GAME_FINISHED,

    // Answers
    SUBMIT_ANSWER,
    ANSWER_RESULT,
    SCORES_UPDATE,
    STREAK_UPDATE,

    // Flashcard Battle specific
    FLASHCARD_REVEALED,
    FLASHCARD_FLIP,
    FLASHCARD_CORRECT,
    FLASHCARD_INCORRECT,

    // Speed Match specific
    MATCH_PAIR,
    MATCH_RESULT,
    MATCH_UPDATE,

    // Chat
    CHAT_MESSAGE,

    // Room management
    ROOM_UPDATE,
    SETTINGS_UPDATE,
    HOST_CHANGED,

    // Errors
    ERROR,
    WARNING
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
    val maxPlayers: Int = 8,
    val status: GameStatus = GameStatus.WAITING,
    val currentQuestionIndex: Int = 0,
    val currentTurn: String? = null, // Player ID whose turn it is (for turn-based games)
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
data class TeamAssignedData(
    val playerId: String,
    val teamId: String,
    val teamName: String
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
data class StreakUpdateData(
    val playerId: String,
    val streakCount: Int,
    val bonusPoints: Int
)

@Serializable
data class MatchPairSubmission(
    val playerId: String,
    val termId: String,
    val definitionId: String,
    val timeElapsed: Long
)

@Serializable
data class MatchResultData(
    val playerId: String,
    val isCorrect: Boolean,
    val points: Int,
    val termId: String,
    val definitionId: String
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
data class GamePausedData(
    val pausedBy: String,
    val pausedAt: Long,
    val reason: String? = null
)

@Serializable
data class HostChangedData(
    val oldHostId: String,
    val newHostId: String,
    val newHostName: String
)

// ============================================
// GAME RESULT MODELS
// ============================================

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

@Serializable
data class GameStats(
    val userId: String,
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalXp: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Int = 0,
    val longestStreak: Int = 0,
    val favoriteGameType: GameType? = null,
    val lastPlayedAt: Long? = null
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

@Serializable
data class LeaderboardResponse(
    val entries: List<GlobalLeaderboardEntry>,
    val totalPlayers: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)