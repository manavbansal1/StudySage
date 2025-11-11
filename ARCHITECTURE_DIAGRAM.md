# Quiz Generation System Architecture

## System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          USER INTERACTION                            │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         GameScreen.kt                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  User clicks "Quiz Game" card                                │   │
│  │  → Sets showQuizGenerationScreen = true                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    QuizGenerationScreen.kt                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. LaunchedEffect → loadAvailableNotes()                    │   │
│  │  2. User selects note from dropdown                          │   │
│  │  3. User enters preferences (optional)                       │   │
│  │  4. User clicks "Generate Quiz"                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       GameViewModel.kt                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  QuizGenerationState:                                        │   │
│  │  • isLoading: Boolean                                        │   │
│  │  • availableNotes: List<Note>                                │   │
│  │  • selectedNote: Note?                                       │   │
│  │  • userPreferences: String                                   │   │
│  │  • generatedQuiz: Quiz?                                      │   │
│  │  • isGenerating: Boolean                                     │   │
│  │  • error: String?                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Methods:                                                             │
│  • loadAvailableNotes() → NotesRepository                            │
│  • generateQuiz() → GameRepository                                   │
│  • saveQuiz() → GameRepository                                       │
│  • getQuizJson() → String                                            │
└─────────────────────────────────────────────────────────────────────┘
                    │                               │
                    ▼                               ▼
┌──────────────────────────────┐    ┌─────────────────────────────────┐
│    NotesRepository.kt        │    │     GameRepository.kt           │
│                              │    │                                 │
│  getUserNotes()              │    │  generateQuizQuestions()        │
│  ↓                           │    │  ↓                              │
│  Firebase Firestore          │    │  1. Limit content to 8000 chars│
│  ↓                           │    │  2. Build AI prompt             │
│  Returns: List<Note>         │    │  3. Call Gemini AI              │
│  (filtered for content)      │    │  4. Parse JSON response         │
└──────────────────────────────┘    │  5. Validate quiz structure     │
                                     │  6. Return Result<Quiz>         │
                                     │                                 │
                                     │  saveQuizToFirestore()          │
                                     │  ↓                              │
                                     │  Firebase Firestore             │
                                     │  ↓                              │
                                     │  Returns: Result<String>        │
                                     │                                 │
                                     │  quizToJson()                   │
                                     │  ↓                              │
                                     │  Gson serialization             │
                                     │  ↓                              │
                                     │  Returns: String (JSON)         │
                                     └─────────────────────────────────┘
                                                    │
                                                    ▼
                                     ┌─────────────────────────────────┐
                                     │      Gemini AI Service          │
                                     │                                 │
                                     │  Prompt:                        │
                                     │  • Content from note            │
                                     │  • User preferences             │
                                     │  • Strict JSON format rules     │
                                     │                                 │
                                     │  Response:                      │
                                     │  {                              │
                                     │    "quiz": [                    │
                                     │      {                          │
                                     │        "question": "...",       │
                                     │        "options": [...],        │
                                     │        "explanation": "..."     │
                                     │      }                          │
                                     │    ]                            │
                                     │  }                              │
                                     └─────────────────────────────────┘
                                                    │
                                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Data Models (Quiz.kt)                           │
│                                                                       │
│  QuizOption                     QuizQuestion                         │
│  ┌───────────────────┐         ┌────────────────────────┐          │
│  │ text: String      │         │ question: String       │          │
│  │ isCorrect: Boolean│         │ options: List<Option>  │          │
│  └───────────────────┘         │ explanation: String    │          │
│                                 └────────────────────────┘          │
│                                                                       │
│  Quiz                                                                 │
│  ┌────────────────────────────────────────────┐                     │
│  │ quizId: String                             │                     │
│  │ noteId: String                             │                     │
│  │ noteTitle: String                          │                     │
│  │ userId: String                             │                     │
│  │ questions: List<QuizQuestion>              │                     │
│  │ preferences: String                        │                     │
│  │ createdAt: Long                            │                     │
│  │ totalQuestions: Int                        │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                       │
│  All models use @SerializedName for JSON compatibility               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    OUTPUT OPTIONS                                    │
│                                                                       │
│  Option 1: Display in UI                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  QuizResultScreen shows:                                     │   │
│  │  • All questions and options                                 │   │
│  │  • Correct answers highlighted                               │   │
│  │  • Explanations                                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Option 2: Copy to Clipboard                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  JSON string copied for:                                     │   │
│  │  • Manual testing                                            │   │
│  │  • Backend submission                                        │   │
│  │  • Sharing/export                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Option 3: Save to Firestore                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stored in 'quizzes' collection                              │   │
│  │  • Retrievable later                                         │   │
│  │  • Associated with user                                      │   │
│  │  • Linked to source note                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Option 4: Submit to Backend                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  BackendApiService.kt:                                       │   │
│  │  • POST to your API endpoint                                 │   │
│  │  • JSON in request body                                      │   │
│  │  • Handle response                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## State Flow Diagram

```
┌──────────────┐
│ Initial Load │
└──────────────┘
        │
        ▼
┌────────────────────────┐
│ isLoading = true       │
│ Call loadAvailableNotes│
└────────────────────────┘
        │
        ▼
┌─────────────────────────────────┐
│ availableNotes = List<Note>     │
│ isLoading = false               │
│ [Dropdown shows notes]          │
└─────────────────────────────────┘
        │
        ▼ User selects note
┌─────────────────────────────────┐
│ selectedNote = Note             │
│ [Generate button enabled]       │
└─────────────────────────────────┘
        │
        ▼ User enters preferences (optional)
┌─────────────────────────────────┐
│ userPreferences = String        │
└─────────────────────────────────┘
        │
        ▼ User clicks Generate
┌─────────────────────────────────┐
│ isGenerating = true             │
│ [Shows loading spinner]         │
└─────────────────────────────────┘
        │
        ▼ AI generates quiz
┌─────────────────────────────────┐
│ Success:                        │
│ • generatedQuiz = Quiz          │
│ • isGenerating = false          │
│ • [Shows QuizResultScreen]      │
│                                 │
│ Failure:                        │
│ • error = String                │
│ • isGenerating = false          │
│ • [Shows error toast]           │
└─────────────────────────────────┘
        │
        ▼ User saves quiz (optional)
┌─────────────────────────────────┐
│ isSaving = true                 │
└─────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────┐
│ savedQuizId = String            │
│ isSaving = false                │
│ [Shows success message]         │
└─────────────────────────────────┘
```

## Component Interaction

```
┌────────────────────────────────────────────────────────────────┐
│                       UI LAYER                                  │
│                                                                  │
│  GameScreen ──────────────► QuizGenerationScreen               │
│      │                              │                           │
│      │                              │                           │
│      │                              ▼                           │
│      │                     QuizResultScreen                     │
│      │                                                          │
└──────┼──────────────────────────────┼───────────────────────────┘
       │                              │
       │                              │ observes state
       │                              │
┌──────┼──────────────────────────────┼───────────────────────────┐
│      │         VIEWMODEL LAYER      │                           │
│      │                              │                           │
│      └──────────────► GameViewModel ◄─────────────┐            │
│                           │ StateFlow              │            │
│                           │                        │            │
│                           ▼                        │            │
│                  QuizGenerationState               │            │
│                                                    │            │
└────────────────────────────┼───────────────────────┼────────────┘
                             │ calls                 │
                             │                       │
┌────────────────────────────┼───────────────────────┼────────────┐
│         REPOSITORY LAYER   │                       │            │
│                            ▼                       │            │
│                   GameRepository ◄─────────────────┘            │
│                            │                                    │
│                            ├──► generateQuizQuestions()         │
│                            ├──► saveQuizToFirestore()           │
│                            └──► quizToJson()                    │
│                                                                  │
│                   NotesRepository                               │
│                            │                                    │
│                            └──► getUserNotes()                  │
│                                                                  │
└────────────────────────────┼───────────────────────┼────────────┘
                             │                       │
                             ▼                       ▼
┌────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES                            │
│                                                                  │
│    Gemini AI Service          Firebase Firestore               │
│    • Generate questions       • Store quizzes                   │
│    • Return JSON             • Retrieve notes                   │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

## Data Transformation Flow

```
User's Note Content
        ↓
┌──────────────────┐
│ String content   │ (from Firestore)
└──────────────────┘
        ↓
┌──────────────────┐
│ + preferences    │ (user input)
└──────────────────┘
        ↓
┌──────────────────┐
│ AI Prompt        │ (formatted request)
└──────────────────┘
        ↓
   Gemini AI
        ↓
┌──────────────────┐
│ JSON Response    │ (raw string)
└──────────────────┘
        ↓
   Gson Parser
        ↓
┌──────────────────┐
│ QuizQuestion[]   │ (Kotlin objects)
└──────────────────┘
        ↓
┌──────────────────┐
│ Quiz object      │ (complete with metadata)
└──────────────────┘
        ↓
    ┌───┴────┐
    ↓        ↓
Display    Convert
in UI      to JSON
           ↓
        Backend
```

## Error Handling Flow

```
Any Operation
     │
     ▼
Try-Catch Block
     │
     ├──► Success ──► Update state ──► Show result
     │
     └──► Exception
           │
           ├──► Network error ──► error = "Check connection"
           ├──► Parse error ──► error = "Invalid response"
           ├──► Auth error ──► error = "Not authenticated"
           └──► Other ──► error = exception.message
                │
                ▼
           Show toast
                │
                ▼
           Clear error after shown
```

This architecture ensures:
✅ Clean separation of concerns
✅ Testable components
✅ Reusable code
✅ Easy to maintain
✅ Scalable for future features
