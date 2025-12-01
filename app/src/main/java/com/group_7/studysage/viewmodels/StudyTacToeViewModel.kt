package com.group_7.studysage.ui.screens.GameScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import com.group_7.studysage.data.models.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for StudyTacToe game to manage UI state across configuration changes
 */
class StudyTacToeViewModel : ViewModel() {
    companion object {
        private const val TAG = "StudyTacToeViewModel"
    }

    // Dialog state
    private val _showQuestionDialog = MutableStateFlow(false)
    val showQuestionDialog: StateFlow<Boolean> = _showQuestionDialog.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Int?>(null)
    val selectedSquare: StateFlow<Int?> = _selectedSquare.asStateFlow()

    private val _selectedQuestionIndex = MutableStateFlow<Int?>(null)
    val selectedQuestionIndex: StateFlow<Int?> = _selectedQuestionIndex.asStateFlow()

    // Store the actual question data to survive rotation
    private val _currentQuestion = MutableStateFlow<QuizQuestion?>(null)
    val currentQuestion: StateFlow<QuizQuestion?> = _currentQuestion.asStateFlow()

    private val _waitingForAnswer = MutableStateFlow(false)
    val waitingForAnswer: StateFlow<Boolean> = _waitingForAnswer.asStateFlow()

    // Store the selected answer index to survive rotation
    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    // Track attempted squares per turn
    private val _attemptedSquares = MutableStateFlow<Set<Int>>(emptySet())
    val attemptedSquares: StateFlow<Set<Int>> = _attemptedSquares.asStateFlow()

    // Track current turn to reset attempted squares when turn changes
    private val _currentTurn = MutableStateFlow<String?>(null)

    // Result screen state
    private val _showResultScreen = MutableStateFlow(false)
    val showResultScreen: StateFlow<Boolean> = _showResultScreen.asStateFlow()

    /**
     * Open question dialog for a square with the question data
     */
    fun openQuestionDialog(squareIndex: Int, questionIndex: Int, question: QuizQuestion) {
        Log.d(TAG, "=== openQuestionDialog CALLED ===")
        Log.d(TAG, "Square: $squareIndex, QuestionIndex: $questionIndex")
        Log.d(TAG, "Question: ${question.question}")
        Log.d(TAG, "Previous state - showDialog: ${_showQuestionDialog.value}, waitingForAnswer: ${_waitingForAnswer.value}")

        _selectedSquare.value = squareIndex
        _selectedQuestionIndex.value = questionIndex
        _currentQuestion.value = question
        _showQuestionDialog.value = true
        _waitingForAnswer.value = true

        Log.d(TAG, "New state - showDialog: ${_showQuestionDialog.value}, waitingForAnswer: ${_waitingForAnswer.value}")
        Log.d(TAG, "Attempted squares: ${_attemptedSquares.value}")
    }

    /**
     * Close question dialog and reset related state
     */
    fun closeQuestionDialog() {
        Log.d(TAG, "=== closeQuestionDialog CALLED ===")
        Log.d(TAG, "Previous state - showDialog: ${_showQuestionDialog.value}, waitingForAnswer: ${_waitingForAnswer.value}")
        Log.d(TAG, "Previous selectedSquare: ${_selectedSquare.value}")

        _showQuestionDialog.value = false
        _selectedSquare.value = null
        _selectedQuestionIndex.value = null
        _currentQuestion.value = null
        _waitingForAnswer.value = false
        _selectedAnswerIndex.value = null

        Log.d(TAG, "New state - showDialog: ${_showQuestionDialog.value}, waitingForAnswer: ${_waitingForAnswer.value}")
        Log.d(TAG, "Attempted squares after close: ${_attemptedSquares.value}")
    }

    /**
     * Update the selected answer index
     */
    fun updateSelectedAnswer(answerIndex: Int?) {
        Log.d(TAG, "=== updateSelectedAnswer CALLED ===")
        Log.d(TAG, "Previous answer: ${_selectedAnswerIndex.value}, New answer: $answerIndex")
        _selectedAnswerIndex.value = answerIndex
    }

    /**
     * Mark a square as attempted
     */
    fun markSquareAttempted(squareIndex: Int) {
        Log.d(TAG, "=== markSquareAttempted CALLED ===")
        Log.d(TAG, "Marking square $squareIndex as attempted")
        Log.d(TAG, "Previous attempted squares: ${_attemptedSquares.value}")

        _attemptedSquares.value = _attemptedSquares.value + squareIndex

        Log.d(TAG, "New attempted squares: ${_attemptedSquares.value}")
    }

    /**
     * Reset attempted squares when turn changes
     */
    fun updateCurrentTurn(newTurn: String?) {
        Log.d(TAG, "=== updateCurrentTurn CALLED ===")
        Log.d(TAG, "Previous turn: ${_currentTurn.value}, New turn: $newTurn")

        if (_currentTurn.value != newTurn) {
            Log.d(TAG, "Turn changed! Resetting attempted squares")
            Log.d(TAG, "Previous attempted squares: ${_attemptedSquares.value}")

            _currentTurn.value = newTurn
            _attemptedSquares.value = emptySet()

            Log.d(TAG, "Attempted squares after reset: ${_attemptedSquares.value}")
        } else {
            Log.d(TAG, "Turn unchanged, keeping attempted squares: ${_attemptedSquares.value}")
        }
    }

    /**
     * Show the result screen
     */
    fun showResultScreen() {
        _showResultScreen.value = true
    }

    /**
     * Hide the result screen
     */
    fun hideResultScreen() {
        _showResultScreen.value = false
    }

    /**
     * Reset all state (useful when leaving the game)
     */
    fun resetState() {
        _showQuestionDialog.value = false
        _selectedSquare.value = null
        _selectedQuestionIndex.value = null
        _currentQuestion.value = null
        _waitingForAnswer.value = false
        _selectedAnswerIndex.value = null
        _attemptedSquares.value = emptySet()
        _currentTurn.value = null
        _showResultScreen.value = false
    }
}
