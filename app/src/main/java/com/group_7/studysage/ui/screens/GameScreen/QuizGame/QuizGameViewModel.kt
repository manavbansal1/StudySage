package com.group_7.studysage.ui.screens.GameScreen.QuizGame

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.BuildConfig
import com.group_7.studysage.data.api.CloudRunApiService
import com.group_7.studysage.ui.screens.GameScreen.QuizGame.data.Question
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class QuizGameViewModel : ViewModel() {
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Use Cloud Run API service for Gemini AI
    private val cloudRunApi = CloudRunApiService(BuildConfig.CLOUD_RUN_URL)

    fun loadQuestionsFromPdf(pdfUri: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val text = extractTextFromPdf(pdfUri, context)
            if (text.isNotEmpty()) {
                val generatedQuestions = generateQuestionsFromText(text)
                _questions.value = generatedQuestions
            }
            _isLoading.value = false
        }
    }

    private suspend fun extractTextFromPdf(pdfUri: String, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(pdfUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val textStripper = PDFTextStripper()
                val text = textStripper.getText(document)
                document.close()
                text
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    private suspend fun generateQuestionsFromText(text: String): List<Question> {
        val prompt = "Based on the following text, generate a JSON array of 5 multiple choice questions. Each question should have a 'questionText' field, an 'options' field with 4 string options, and a 'correctAnswerIndex' field with the index of the correct answer. Text: $text"
        val responseText = cloudRunApi.generateContent(prompt)
        return parseQuestionsFromJson(responseText)
    }

    private fun parseQuestionsFromJson(jsonString: String): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val questionText = jsonObject.getString("questionText")
                val optionsArray = jsonObject.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                val correctAnswerIndex = jsonObject.getInt("correctAnswerIndex")
                questions.add(Question(questionText, options, correctAnswerIndex))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return questions
    }

    fun answerQuestion(selectedOptionIndex: Int) {
        val currentQuestion = questions.value[currentQuestionIndex.value]
        if (selectedOptionIndex == currentQuestion.correctAnswerIndex) {
            _score.value++
        }
        _currentQuestionIndex.value++
    }
}
