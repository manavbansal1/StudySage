package com.group_7.studysage.ui.screens.GameScreen.QuizGame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun QuizGameScreen(
    quizGameViewModel: QuizGameViewModel = viewModel(),
    pdfUri: String?
) {
    val questions by quizGameViewModel.questions.collectAsState()
    val currentQuestionIndex by quizGameViewModel.currentQuestionIndex.collectAsState()
    val score by quizGameViewModel.score.collectAsState()
    val isLoading by quizGameViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    if (pdfUri != null) {
        quizGameViewModel.loadQuestionsFromPdf(pdfUri, context)
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (currentQuestionIndex < questions.size) {
        val currentQuestion = questions[currentQuestionIndex]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = currentQuestion.questionText, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            currentQuestion.options.forEachIndexed { index, option ->
                Button(
                    onClick = { quizGameViewModel.answerQuestion(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = option)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Quiz Finished!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Your score: $score / ${questions.size}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
