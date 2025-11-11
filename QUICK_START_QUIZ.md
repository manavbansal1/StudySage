# Quick Start Guide - Quiz Generation Feature

## 🎯 What's Been Implemented

A complete quiz generation system that:
- ✅ Lets users select any note from their library
- ✅ Accepts optional preferences for customization
- ✅ Generates 10 multiple-choice questions using Gemini AI
- ✅ Returns properly formatted JSON for your backend
- ✅ Saves quizzes to Firestore database
- ✅ Provides copy-to-clipboard functionality

## 🚀 How to Use

### For Users:
1. Open the app → Navigate to **Games** tab
2. Tap **Quiz Game** card
3. Select a note from the dropdown
4. (Optional) Add preferences like "Focus on key concepts, medium difficulty"
5. Tap **Generate Quiz**
6. View the generated quiz with all questions and answers
7. Copy JSON to clipboard or save to database

### For Developers (You):

#### Get Quiz JSON for Backend:
```kotlin
// In your code where you need the JSON
val gameViewModel: GameViewModel = viewModel()
val quizJson = gameViewModel.getQuizJson()

// quizJson is a String containing properly formatted JSON
// Send it to your backend API
```

#### Example JSON Output:
```json
{
  "quizId": "xyz123",
  "noteId": "note456",
  "noteTitle": "Machine Learning Basics",
  "userId": "user789",
  "preferences": "Focus on supervised learning",
  "createdAt": 1699564800000,
  "totalQuestions": 10,
  "questions": [
    {
      "question": "What is supervised learning?",
      "options": [
        {"text": "Learning with labeled data", "isCorrect": true},
        {"text": "Learning without data", "isCorrect": false},
        {"text": "Learning from unlabeled data", "isCorrect": false},
        {"text": "Learning from reinforcement", "isCorrect": false}
      ],
      "explanation": "Supervised learning uses labeled training data."
    }
    // ... 9 more questions
  ]
}
```

## 📡 Backend Integration

### Option 1: Use the Provided API Service
```kotlin
// Add to your ViewModel
private val backendApi = BackendApiService("https://your-backend-url.com")

fun submitToBackend() {
    viewModelScope.launch {
        val quiz = quizGenerationState.value.generatedQuiz ?: return@launch
        
        val result = backendApi.submitQuiz(quiz)
        result.fold(
            onSuccess = { response ->
                // Success! Quiz submitted
                Toast.makeText(context, "Quiz submitted!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                // Error occurred
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}
```

### Option 2: Use Raw JSON String
```kotlin
fun submitJsonToBackend() {
    val jsonString = gameViewModel.getQuizJson()
    
    // Use your existing API service
    yourApiService.post(
        endpoint = "/api/quizzes",
        body = jsonString,
        headers = mapOf("Content-Type" to "application/json")
    )
}
```

### Option 3: Manual Network Call
```kotlin
val quizJson = gameViewModel.getQuizJson()

// Using your preferred HTTP library
val request = Request.Builder()
    .url("https://your-backend.com/api/quizzes")
    .post(quizJson.toRequestBody("application/json".toMediaType()))
    .build()

client.newCall(request).execute()
```

## 🗂️ Data Structure

### Quiz Object Fields:
- `quizId`: String - Unique identifier (empty until saved)
- `noteId`: String - Reference to source note
- `noteTitle`: String - Title of the note used
- `userId`: String - Firebase user ID
- `questions`: Array - List of 10 questions
- `preferences`: String - User's input preferences
- `createdAt`: Long - Timestamp (milliseconds)
- `totalQuestions`: Int - Always 10

### Question Object Fields:
- `question`: String - The question text
- `options`: Array - 4 options (A, B, C, D)
- `explanation`: String - Why the correct answer is right

### Option Object Fields:
- `text`: String - Option text
- `isCorrect`: Boolean - True for correct answer

## 📝 Backend API Expectations

Your backend should accept:
- **Endpoint**: POST `/api/quizzes` (or your preferred route)
- **Content-Type**: `application/json`
- **Body**: The quiz JSON string
- **Response**: Success/error message

Example backend validation:
```javascript
// Node.js example
app.post('/api/quizzes', (req, res) => {
    const quiz = req.body;
    
    // Validate
    if (!quiz.questions || quiz.questions.length !== 10) {
        return res.status(400).json({ error: 'Must have 10 questions' });
    }
    
    // Save to your database
    database.quizzes.insert(quiz);
    
    res.json({ success: true, quizId: quiz.quizId });
});
```

## 🔧 Customization Options

### Change Number of Questions:
In `GameRepository.kt`, line ~50:
```kotlin
// Current: "generate exactly 10 multiple-choice questions"
// Change to: "generate exactly 15 multiple-choice questions"
```

### Change Number of Options:
In `GameRepository.kt`, line ~55:
```kotlin
// Current: "Each question must have exactly 4 options"
// Change to: "Each question must have exactly 5 options"
```

### Add More Preferences:
Create preset buttons in `QuizGenerationScreen.kt`:
```kotlin
Row {
    Button(onClick = { gameViewModel.setUserPreferences("Easy difficulty") }) {
        Text("Easy")
    }
    Button(onClick = { gameViewModel.setUserPreferences("Hard difficulty") }) {
        Text("Hard")
    }
}
```

## 🐛 Troubleshooting

### "No notes available"
- User needs to upload notes first
- Notes must have content (not empty files)

### "Failed to generate quiz"
- Check internet connection
- Verify Gemini API key is valid
- Check note content isn't too long (>8000 chars gets truncated)

### JSON parsing error
- Gemini occasionally returns malformed JSON
- Retry generation
- Check error logs for details

### Backend submission fails
- Verify backend URL is correct
- Check authentication headers if required
- Ensure backend accepts JSON content-type

## 📱 UI Screenshots Flow

1. **Games Screen** → Shows "Quiz Game" card
2. **Quiz Generation Screen** → Note selection + preferences
3. **Loading State** → "Generating Quiz..." with spinner
4. **Quiz Result Screen** → All questions displayed
5. **Success** → "Quiz JSON copied to clipboard" toast

## 🔐 Security Notes

- Quiz generation requires user authentication (Firebase)
- User can only access their own notes
- Gemini API key is in BuildConfig (not exposed to users)
- Backend submission should verify user ownership

## 📊 Firestore Structure

```
quizzes/
  ├── {quizId}/
      ├── quizId: "xyz123"
      ├── noteId: "note456"
      ├── noteTitle: "ML Basics"
      ├── userId: "user789"
      ├── questions: [...]
      ├── preferences: "Focus on..."
      ├── createdAt: 1699564800000
      └── totalQuestions: 10
```

## ⚡ Performance

- Quiz generation: ~5-15 seconds (depends on Gemini AI)
- Firestore save: <1 second
- Note loading: <1 second (cached after first load)

## 🎨 UI Customization

All UI components use Material3 theme colors:
- Primary color for buttons and highlights
- Surface colors for cards
- Error color for error messages

Modify in your app's theme to match your design.

## 📦 What's Included

### New Files:
- `Quiz.kt` - Data models with JSON annotations
- `QuizGenerationScreen.kt` - Complete UI
- `BackendApiService.kt` - Backend integration example
- `QUIZ_IMPLEMENTATION.md` - Detailed documentation

### Modified Files:
- `GameRepository.kt` - AI generation logic
- `GameViewModel.kt` - State management
- `GameScreen.kt` - Navigation setup

### No Additional Dependencies:
Everything uses existing dependencies already in your project!

## 🎓 Next Steps

1. **Test the feature**: 
   - Upload a note
   - Generate a quiz
   - Check the JSON output

2. **Integrate with backend**:
   - Use `BackendApiService.kt` or your own
   - Test submission
   - Handle responses

3. **Customize as needed**:
   - Adjust question count
   - Add difficulty levels
   - Style the UI

4. **Optional enhancements**:
   - Quiz playing interface
   - Score tracking
   - Leaderboards
   - Quiz sharing

## 💡 Tips

- **Best results**: Use notes with clear, well-structured content
- **Preferences work**: Try "Focus on definitions" or "Include examples"
- **Retry if needed**: AI generation isn't perfect - regenerate if unsatisfied
- **Copy JSON early**: Copy to clipboard before navigating away

## 🤝 Support

All code is well-commented and follows your existing patterns. If you need modifications, all logic is in these files:
- Repository: `GameRepository.kt`
- ViewModel: `GameViewModel.kt`  
- UI: `QuizGenerationScreen.kt`

Happy quiz generating! 🎉
