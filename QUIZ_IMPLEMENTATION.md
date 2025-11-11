# Quiz Generation Feature - Implementation Summary

## Overview
A complete quiz generation system that allows users to:
1. Select a note from their library
2. Add optional preferences for quiz customization
3. Generate 10 multiple-choice questions using Gemini AI
4. Get properly formatted JSON output for backend submission
5. Save quizzes to Firestore database

## Architecture

### Data Models (`/data/model/Quiz.kt`)

```kotlin
// Single quiz option
QuizOption {
    text: String           // Option text
    isCorrect: Boolean     // Whether this is the correct answer
}

// Single quiz question
QuizQuestion {
    question: String       // Question text
    options: List<QuizOption>  // 4 options (A, B, C, D)
    explanation: String    // Explanation for correct answer
}

// Complete quiz
Quiz {
    quizId: String        // Firestore document ID
    noteId: String        // Reference to source note
    noteTitle: String     // Title of source note
    userId: String        // User who created quiz
    questions: List<QuizQuestion>  // 10 questions
    preferences: String   // User preferences used
    createdAt: Long       // Timestamp
    totalQuestions: Int   // Count (always 10)
}
```

All models use `@SerializedName` annotations for proper JSON serialization.

### Repository Layer (`GameRepository.kt`)

**Key Methods:**

1. **`generateQuizQuestions(noteId, noteTitle, content, userPreferences): Result<Quiz>`**
   - Sends content and preferences to Gemini AI
   - Requests strict JSON format response
   - Parses JSON into Quiz object
   - Validates response (ensures 10 questions, 4 options each, 1 correct answer)
   - Returns `Result.success(quiz)` or `Result.failure(error)`

2. **`saveQuizToFirestore(quiz: Quiz): Result<String>`**
   - Saves quiz to Firestore `quizzes` collection
   - Generates unique quiz ID
   - Returns quiz ID on success

3. **`quizToJson(quiz: Quiz): String`**
   - Converts Quiz object to JSON string
   - Ready for backend API submission

**AI Prompt Engineering:**
- Requests exactly 10 questions
- Each with 4 options (A, B, C, D)
- Only 1 correct answer per question
- Includes brief explanations
- Considers user preferences
- Returns pure JSON (no markdown formatting)

### ViewModel Layer (`GameViewModel.kt`)

**State Management:**
```kotlin
QuizGenerationState {
    isLoading: Boolean              // Loading notes
    availableNotes: List<Note>      // User's notes with content
    selectedNote: Note?             // Selected note for quiz
    userPreferences: String         // User input preferences
    generatedQuiz: Quiz?            // Generated quiz result
    error: String?                  // Error messages
    isGenerating: Boolean           // Generating quiz
    isSaving: Boolean              // Saving to database
    savedQuizId: String?           // Saved quiz ID
}
```

**Key Methods:**
- `loadAvailableNotes()` - Loads user notes, filters for content
- `setSelectedNote(note)` - Sets selected note
- `setUserPreferences(text)` - Updates preferences
- `generateQuiz()` - Triggers AI generation
- `saveQuiz()` - Saves to Firestore
- `getQuizJson()` - Gets JSON string for backend
- `resetQuizGeneration()` - Clears state for new quiz

### UI Layer (`QuizGenerationScreen.kt`)

**Main Screen Components:**

1. **Instructions Card**
   - Explains quiz generation process

2. **Note Selection Dropdown**
   - Shows all user notes with content
   - Displays note title and filename
   - Shows selected note info (title, content length)

3. **Preferences Input**
   - Multi-line text field (5 lines)
   - Optional - user can leave blank
   - Placeholder suggests examples

4. **Generate Button**
   - Disabled until note selected
   - Shows loading state during generation
   - Triggers quiz generation

**Quiz Result Screen Components:**

1. **Success Message**
   - Green card confirming generation
   - Shows question count

2. **Quiz Preview**
   - All 10 questions displayed
   - Options labeled A, B, C, D
   - Correct answer highlighted
   - Explanations shown below each question

3. **Action Buttons**
   - **Copy JSON to Clipboard** - Copies formatted JSON
   - **Save to Database** - Saves to Firestore
   - **Generate Another Quiz** - Resets state

### Navigation (`GameScreen.kt`)

- Quiz Game card now opens `QuizGenerationScreen`
- Back button returns to games list
- Resets state when returning

## Usage Flow

### User Journey:

1. **User taps "Quiz Game" card**
   → Opens QuizGenerationScreen

2. **Screen loads user's notes**
   → Shows dropdown with available notes

3. **User selects a note**
   → Shows note info card

4. **User enters preferences (optional)**
   → Examples: "Focus on key concepts", "Medium difficulty"

5. **User taps "Generate Quiz"**
   → Shows loading state
   → Calls Gemini AI
   → Parses JSON response

6. **Quiz generated successfully**
   → Shows QuizResultScreen
   → Displays all questions with answers
   → Shows action buttons

7. **User can:**
   - Copy JSON to clipboard → For backend submission
   - Save to Firestore → Stores in database
   - Generate new quiz → Resets and starts over

## JSON Format Example

```json
{
  "quizId": "abc123",
  "noteId": "note456",
  "noteTitle": "Introduction to Machine Learning",
  "userId": "user789",
  "preferences": "Focus on supervised learning, medium difficulty",
  "createdAt": 1699564800000,
  "totalQuestions": 10,
  "questions": [
    {
      "question": "What is supervised learning?",
      "options": [
        {"text": "Learning with labeled data", "isCorrect": true},
        {"text": "Learning without any data", "isCorrect": false},
        {"text": "Learning from unlabeled data", "isCorrect": false},
        {"text": "Learning from reinforcement", "isCorrect": false}
      ],
      "explanation": "Supervised learning uses labeled training data where each example has an input and desired output."
    }
    // ... 9 more questions
  ]
}
```

## Backend Integration

### To send quiz to your backend:

```kotlin
// In your backend API service
val quizJson = gameViewModel.getQuizJson()
if (quizJson != null) {
    // Send to your backend API
    yourBackendApi.submitQuiz(quizJson)
}
```

The JSON is already properly formatted and includes:
- All questions and options
- Correct answer flags
- User preferences
- Metadata (note info, timestamps)

## Error Handling

- **No notes available**: Shows message in dropdown
- **Note has no content**: Filtered out from list
- **AI generation fails**: Shows error toast, can retry
- **JSON parsing fails**: Logs error, shows user-friendly message
- **Firestore save fails**: Shows error, can retry
- **Network issues**: Handled by try-catch with error messages

## Testing Checklist

- [ ] Load screen with no notes
- [ ] Load screen with multiple notes
- [ ] Select different notes
- [ ] Generate with empty preferences
- [ ] Generate with detailed preferences
- [ ] Copy JSON to clipboard
- [ ] Save to Firestore
- [ ] Generate multiple quizzes in one session
- [ ] Test back navigation
- [ ] Test error scenarios (no internet, etc.)

## Future Enhancements

1. **Quiz Difficulty Selection**: Easy, Medium, Hard presets
2. **Question Count Selection**: Allow 5, 10, 15, or 20 questions
3. **Question Type Options**: True/False, Fill-in-blank, etc.
4. **Quiz History**: View previously generated quizzes
5. **Play Quiz**: Interactive quiz-taking interface
6. **Quiz Statistics**: Track scores, completion rates
7. **Share Quiz**: Generate shareable links
8. **Export Options**: PDF, CSV formats

## Files Modified/Created

### Created:
- `/app/src/main/java/com/group_7/studysage/data/model/Quiz.kt`
- `/app/src/main/java/com/group_7/studysage/ui/screens/GameScreen/QuizGenerationScreen.kt`

### Modified:
- `/app/src/main/java/com/group_7/studysage/data/repository/GameRepository.kt`
- `/app/src/main/java/com/group_7/studysage/viewmodels/GameViewModel.kt`
- `/app/src/main/java/com/group_7/studysage/ui/screens/GameScreen/GameScreen.kt`

## Dependencies Used

- **Gson**: JSON serialization/deserialization
- **Gemini AI**: Quiz question generation
- **Firebase Firestore**: Quiz storage
- **Jetpack Compose**: UI components
- **Coroutines**: Async operations
- **StateFlow**: State management

All dependencies already present in `build.gradle.kts`.
