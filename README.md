<div align="center">

# 📚 StudySage

### AI-Powered Study Companion for the Modern Student

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-green.svg)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Material%20Design-3-blue.svg)](https://m3.material.io)
[![Firebase](https://img.shields.io/badge/Firebase-BOM%2033.6-orange.svg)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Website:** [studysage.vercel.app](https://studysage.vercel.app)

[Features](#-features) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## 📖 Overview

**StudySage** transforms how students manage their academic life by combining course organization, AI-powered note processing, real-time collaboration, and gamified learning—all in a beautiful Material Design 3 interface.

Whether you're organizing courses, generating AI summaries from lecture notes, competing in multiplayer study games, or collaborating with study groups in real-time, StudySage has you covered.

### 🎯 Why StudySage?

- ✅ **All-in-One Platform** - Courses, notes, groups, and games in one app
- 🤖 **AI-Powered** - Automatic summaries, key points, tags, and podcast generation
- ⚡ **Real-Time Collaboration** - Live chat and multiplayer games with WebSocket technology
- 🎮 **Gamification** - XP system, streaks, leaderboards, and 6 game modes
- 📱 **Modern UI** - Glass morphism, smooth animations, purple-gold theme
- ☁️ **Cloud Sync** - All data synced across devices via Firebase
- 📲 **NFC Sharing** - Share notes physically by tapping phones together

---

## ✨ Features

### 🧠 AI-Powered Note Processing
Upload documents and let AI do the heavy lifting:
- **Supported Formats**: PDF, DOCX, TXT, MD, RTF (up to 10MB)
- **AI Extraction**: Automatic summaries, key points, and tag generation
- **Powered by**: Firebase Vertex AI (Gemini) via Google Cloud Run backend
- **Organization**: Link notes to courses or save standalone
- **Storage**: Firebase Storage + Cloudinary CDN for fast access
- **Recently Opened**: Track your most accessed PDFs

### 🎓 Smart Course Management
Keep your academic life organized:
- **CRUD Operations**: Create, edit, archive, and delete courses
- **Semester Filtering**: Spring, Summer, Fall, Winter + Year selection
- **Color Coding**: 12 beautiful colors for visual organization
- **Metadata**: Course code, instructor, credits, description
- **Note Linking**: Associate notes directly with courses
- **Archive System**: Soft-delete courses without losing data

### 👥 Real-Time Study Groups
Collaborate with classmates effortlessly:
- **Live Chat**: Real-time messaging powered by Firestore snapshots
- **Image Sharing**: Upload and share images via Cloudinary CDN
- **Invitations**: Email-based group invite system
- **Roles**: Admin and member permissions
- **Group Profiles**: Customizable name, description, and profile picture
- **Message History**: Full chat history with timestamps

### 🎮 Multiplayer Study Games
Make studying fun with 6 competitive game modes:

1. **Quiz Race** 🏁 - Competitive quiz answering with speed bonuses
2. **Flashcard Battle** ⚔️ - Fast-paced flashcard competition
3. **Study Tac Toe** ❌⭕ - Tic-tac-toe with quiz questions to claim squares
4. **Speed Match** ⚡ - Match terms with definitions against the clock
5. **Survival Mode** 💀 - Answer questions continuously without mistakes
6. **Speed Quiz** 🚀 - Rapid-fire questions for maximum XP

**Game Features**:
- Real-time WebSocket connections for instant gameplay
- Team mode support for collaborative play
- Global leaderboards and rankings
- Streak bonuses and combo multipliers
- XP and leveling system
- Custom game lobbies with configurable settings
- Spectator mode for watching ongoing games

### 🎙️ AI Podcast Generation
Turn your notes into audio content:
- **Text-to-Speech**: Convert notes to natural-sounding podcasts
- **Google Cloud TTS**: High-quality AI narration
- **Playback Controls**: Play, pause, seek, and speed controls
- **Cloud Run Backend**: Scalable serverless processing

### 🃏 Flashcard System
Master concepts through spaced repetition:
- **Create & Edit**: Build custom flashcard decks
- **Link to Courses**: Organize by course or topic
- **Study Mode**: Flip cards and track progress
- **Game Integration**: Use in Flashcard Battle mode

### 📲 NFC Note Sharing
Share notes physically by tapping phones:
- **Host Card Emulation (HCE)**: Act as an NFC card
- **Reader Mode**: Read from other NFC-enabled devices
- **Secure Transfer**: APDU service for data exchange
- **Instant Sharing**: No internet required

### 🔔 Smart Notifications & Reminders
Never miss important study sessions:
- **Daily Study Reminders**: WorkManager scheduled at 9 AM
- **Group Messages**: Real-time push notifications
- **Invite Alerts**: Get notified of study group invitations
- **Streak Tracking**: Daily streak counter with reminders

### 👤 Profile Management
Personalize your experience:
- **Edit Profile**: Name, bio, and profile picture
- **Profile Picture**: Upload via camera or gallery
- **Recent Activity**: Track recently opened PDFs
- **Privacy Settings**: Manage notification preferences
- **Secure Auth**: Email/password with Firebase Authentication

---

## 🛠️ Tech Stack

### Frontend
| Component | Technology |
|-----------|-----------|
| **Language** | Kotlin 2.2.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.12.01) |
| **Design System** | Material Design 3 |
| **Navigation** | Navigation Compose 2.8.5 |
| **Image Loading** | Coil 2.5.0 |
| **Async** | Kotlin Coroutines 1.7.3 + Flow |
| **Serialization** | Kotlinx Serialization 1.6.0 |

### Backend & Cloud
| Component | Technology |
|-----------|-----------|
| **Authentication** | Firebase Auth |
| **Database** | Cloud Firestore (NoSQL) |
| **File Storage** | Firebase Storage + Cloudinary CDN |
| **AI Processing** | Firebase Vertex AI (Gemini) |
| **Custom Backend** | Google Cloud Run (serverless) |
| **TTS** | Google Cloud Text-to-Speech |
| **Real-Time Games** | WebSocket via custom API |
| **Analytics** | Firebase Analytics |

### Networking & APIs
| Component | Technology |
|-----------|-----------|
| **HTTP Client** | OkHttp 4.12.0 |
| **REST API** | Retrofit 2.9.0 |
| **WebSocket** | OkHttp WebSocket |
| **JSON Parsing** | Gson 2.10.1 |

### Additional Libraries
- **PDF Processing**: PDFBox Android 2.0.27.0
- **Permissions**: Accompanist Permissions 0.34.0
- **Swipe Refresh**: Accompanist Swipe Refresh 0.32.0
- **Background Tasks**: WorkManager 2.9.0
- **NFC**: Android HCE (Host Card Emulation)

### Build Tools
- **Build System**: Gradle 8.13 with Kotlin DSL
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36
- **Compile SDK**: 36
- **AGP**: 8.11.1

---

## 🏗️ Architecture

### MVVM Pattern
StudySage follows the **Model-View-ViewModel (MVVM)** architecture for clean separation of concerns:

```
┌─────────────────────────────────────────────────┐
│           UI Layer (Jetpack Compose)            │
│  30+ Composable Screens with Material3 Design   │
│    HomeScreen │ CourseScreen │ GameScreen │ ... │
└───────────────────┬─────────────────────────────┘
                    │ observes StateFlow/State
                    ▼
┌─────────────────────────────────────────────────┐
│        ViewModel Layer (Business Logic)         │
│     17 ViewModels managing UI state & logic     │
│  AuthViewModel │ GameViewModel │ NotesViewModel │
└───────────────────┬─────────────────────────────┘
                    │ calls suspend functions
                    ▼
┌─────────────────────────────────────────────────┐
│        Repository Layer (Data Abstraction)      │
│  11 Repositories providing single source of truth│
│   AuthRepo │ CourseRepo │ GameRepo │ NotesRepo  │
└───────────────────┬─────────────────────────────┘
                    │ async operations via Coroutines
                    ▼
┌─────────────────────────────────────────────────┐
│     External Services (Firebase, APIs, etc.)    │
│  Firestore │ Storage │ Cloud Run │ WebSocket    │
└─────────────────────────────────────────────────┘
```

### Project Structure

```
app/src/main/java/com/group_7/studysage/
├── MainActivity.kt                      # App entry point
│
├── data/                                # Data Layer
│   ├── repository/                      # 11 Repositories
│   │   ├── AuthRepository.kt           # Authentication & profiles
│   │   ├── CourseRepository.kt         # Course CRUD operations
│   │   ├── NotesRepository.kt          # Notes & AI processing
│   │   ├── GroupRepository.kt          # Study groups & chat
│   │   ├── GameRepository.kt           # Game sessions
│   │   ├── PodcastRepository.kt        # Podcast generation
│   │   ├── Flashcardrepository.kt      # Flashcard management
│   │   └── QuizRepository.kt           # Quiz data
│   ├── api/                             # API Definitions
│   │   ├── CloudRunApiService.kt       # AI & TTS endpoints
│   │   ├── GameAPIService.kt           # Game backend
│   │   └── ApiConfig.kt                # API configuration
│   ├── model/                           # Data Models
│   │   ├── Course.kt                   # Course entity
│   │   ├── Quiz.kt                     # Quiz models
│   │   └── GameModels.kt               # 40+ game data classes
│   ├── websocket/                       # Real-Time Connections
│   │   └── WebSocketManager.kt         # WebSocket handler
│   └── nfc/                             # NFC Data Transfer
│       └── NFCPayload.kt               # NFC payload structures
│
├── viewmodels/                          # ViewModel Layer (17 ViewModels)
│   ├── AuthViewModel.kt                # Auth state management
│   ├── HomeViewModel.kt                # Home screen logic
│   ├── CourseViewModel.kt              # Course operations
│   ├── NotesViewModel.kt               # Notes processing
│   ├── GroupViewModel.kt               # Group management
│   ├── GroupChatViewModel.kt           # Real-time chat
│   ├── GameViewModel.kt                # Game state
│   ├── GameLobbyViewModel.kt           # Lobby management
│   ├── GamePlayViewModel.kt            # Gameplay logic
│   ├── FlashcardViewModel.kt           # Flashcard state
│   ├── ProfileViewModel.kt             # Profile editing
│   └── ...                             # + 6 more ViewModels
│
├── ui/screens/                          # UI Layer (30+ Screens)
│   ├── auth/                            # Authentication
│   │   ├── SignInScreen.kt
│   │   └── SignUpScreen.kt
│   ├── HomeScreen/                      # Dashboard
│   │   └── HomeScreen.kt               # Quick actions & stats
│   ├── CourseScreen/                    # Course Management
│   │   ├── CourseScreen.kt             # Course list
│   │   ├── CourseDetailsScreen.kt      # Course details
│   │   └── ...                         # Dialogs & forms
│   ├── GroupsScreen/                    # Study Groups
│   │   ├── GroupScreen.kt              # Groups list
│   │   ├── GroupChatScreen.kt          # Real-time chat
│   │   ├── GroupDetailsScreen.kt       # Group info
│   │   └── ...                         # Invites & settings
│   ├── GameScreen/                      # Multiplayer Games
│   │   ├── GameScreen.kt               # Game mode selection
│   │   ├── GameLobbyScreen.kt          # Pre-game lobby
│   │   ├── QuizRaceScreen.kt           # Quiz Race mode
│   │   ├── FlashcardBattleScreen.kt    # Flashcard Battle
│   │   ├── StudyTacToeScreen.kt        # Study Tac Toe
│   │   ├── SpeedMatchScreen.kt         # Speed Match
│   │   └── ...                         # + more game screens
│   ├── ProfileScreen/                   # User Profile
│   │   ├── ProfileScreen.kt            # Profile editor
│   │   ├── NotificationsScreen.kt      # Notification settings
│   │   └── PrivacyScreen.kt            # Privacy settings
│   ├── podcast/                         # AI Podcasts
│   │   └── PodcastScreen.kt            # Podcast player
│   ├── flashcards/                      # Flashcards
│   │   └── FlashcardScreen.kt          # Flashcard viewer
│   ├── nfc/                             # NFC Sharing
│   │   ├── NFCSendScreen.kt            # Send via NFC
│   │   └── NFCReceiveScreen.kt         # Receive via NFC
│   └── ...                             # + more screens
│
├── navigation/                          # Navigation
│   └── StudySageNavigation.kt          # Navigation graph (558 lines)
│
├── theme/                               # Material3 Theming
│   ├── Color.kt                        # Purple-gold color scheme
│   ├── Theme.kt                        # Light/dark themes
│   └── Type.kt                         # Typography
│
├── utils/                               # Utilities (13 classes)
│   ├── CloudinaryUploader.kt           # Image CDN uploads
│   ├── FileUtils.kt                    # File operations
│   ├── PDFTextExtractor.kt             # PDF parsing
│   ├── StudySageNotificationManager.kt # Push notifications
│   ├── ReminderScheduler.kt            # Daily reminders
│   └── ...                             # + 8 more utilities
│
├── services/                            # Background Services
│   └── NfcHostApduService.kt           # NFC HCE service
│
└── workers/                             # Background Workers
    └── DailyReminderWorker.kt          # WorkManager tasks
```

### Database Schema (Firestore)

```javascript
// User Profile
/users/{userId}
  ├── name: string
  ├── email: string
  ├── bio: string
  ├── profilePic: string (URL)
  ├── groups: array<GroupInfo>
  ├── groupInvites: array<InviteInfo>
  ├── recentlyOpenedPdfs: array<PdfInfo>
  ├── dailyStreak: number
  └── createdAt: timestamp

// Courses
/courses/{courseId}
  ├── title: string
  ├── code: string
  ├── semester: enum (Spring/Summer/Fall/Winter)
  ├── year: number
  ├── instructor: string
  ├── description: string
  ├── credits: number
  ├── color: string
  ├── userId: string
  ├── isArchived: boolean
  └── createdAt: timestamp

// Notes
/notes/{noteId}
  ├── title: string
  ├── summary: string (AI-generated)
  ├── content: string
  ├── tags: array<string> (AI-generated)
  ├── keyPoints: array<string> (AI-generated)
  ├── fileUrl: string
  ├── fileType: string
  ├── userId: string
  ├── courseId: string (optional)
  └── createdAt: timestamp

// Study Groups
/groups/{groupId}
  ├── name: string
  ├── description: string
  ├── profilePic: string (URL)
  ├── members: array<MemberInfo>
  │   ├── userId: string
  │   ├── name: string
  │   ├── role: enum (Admin/Member)
  │   └── joinedAt: timestamp
  ├── createdBy: string
  ├── createdAt: timestamp
  └── /messages/{messageId}  ← Subcollection
      ├── senderId: string
      ├── senderName: string
      ├── message: string
      ├── images: array<string> (URLs)
      └── timestamp: timestamp

// Flashcards
/flashcards/{flashcardId}
  ├── question: string
  ├── answer: string
  ├── userId: string
  ├── courseId: string (optional)
  └── createdAt: timestamp

// Game Sessions (managed via external API)
// Stored on Cloud Run backend, not in Firestore
```

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have:

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or higher
- **Android SDK** with API 24-36
- **Git** for version control
- **Firebase Account** (free tier works)
- **Cloudinary Account** (free tier works)
- **Google Cloud Account** (for Cloud Run - optional)

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/StudySage.git
cd StudySage
```

#### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project named "StudySage"
3. Add an Android app with package name: `com.group_7.studysage`
4. Download `google-services.json`
5. Place it in `app/` directory

**Enable Firebase Services:**
- **Authentication** → Sign-in method → Email/Password → Enable
- **Firestore Database** → Create database → Start in test mode
- **Storage** → Get started → Start in test mode
- **Vertex AI** → Enable Firebase Vertex AI for Gemini

**Firestore Security Rules** (update later for production):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### 3. Cloudinary Setup

1. Create account at [Cloudinary](https://cloudinary.com)
2. Go to Dashboard → Settings → Upload
3. Create an **unsigned upload preset**
4. Note your **Cloud Name** and **Upload Preset**

#### 4. Environment Variables

Create a `.env` file in the project root:

```env
# Cloud Run API (optional - for AI podcasts)
CLOUD_RUN_URL=https://your-cloud-run-url.run.app

# Cloudinary (required)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_UPLOAD_PRESET=your_upload_preset

# Resend API (optional - for email invites)
RESEND_API_KEY=your_resend_api_key
```

Update `app/build.gradle.kts` to load these:
```kotlin
android {
    defaultConfig {
        // Load .env file
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val (key, value) = line.split("=")
                buildConfigField("String", key, "\"$value\"")
            }
        }
    }
}
```

#### 5. Build and Run

**Option A: Android Studio**
1. Open project in Android Studio
2. Let Gradle sync complete
3. Connect Android device or start emulator
4. Click ▶️ **Run** button

**Option B: Command Line**
```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

#### 6. Optional: Cloud Run Backend Setup

For AI podcasts and advanced features, deploy the backend:

```bash
# Navigate to backend directory (if you have one)
cd backend

# Deploy to Cloud Run
gcloud run deploy studysage-api \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated

# Note the URL and add to .env file
```

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| **Total Kotlin Files** | 84 |
| **Lines of Code** | ~29,000 |
| **ViewModels** | 17 |
| **Repositories** | 11 |
| **UI Screens** | 30+ |
| **Utility Classes** | 13 |
| **Data Models** | 40+ |
| **Game Modes** | 6 |
| **Supported File Types** | PDF, DOCX, TXT, MD, RTF |
| **Dependencies** | 80+ |

---

## 🎨 UI/UX Highlights

### Design System
- **Material Design 3** with custom purple-gold theme
- **Glass morphism** effects on bottom navigation
- **Smooth animations** for screen transitions
- **Dark/Light themes** based on system preferences
- **Responsive layouts** for different screen sizes

### Color Palette
```kotlin
Primary: Purple (#6200EE)
Secondary: Gold (#FFD700)
Tertiary: Teal (#03DAC6)
Background: Dynamic (Light/Dark)
Surface: Elevated with blur effects
```

### Accessibility
- High contrast color ratios
- Large touch targets (48dp minimum)
- Screen reader support
- Keyboard navigation ready

---

## 🔒 Security Features

- **Firebase Authentication** with secure email/password
- **Firestore Security Rules** for data access control
- **ProGuard/R8** code obfuscation in release builds
- **Environment variables** for sensitive keys (not hardcoded)
- **Network Security Config** with HTTPS enforcement
- **Firebase Storage Rules** for file upload permissions
- **Input validation** on all user inputs
- **SQL injection prevention** (Firestore NoSQL)

---

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`

---

## 📱 Supported Devices

- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 14+ (API 36)
- **Screen Sizes**: Phones and tablets
- **Orientations**: Portrait and landscape
- **NFC**: Required for NFC sharing feature

---

## 🗺️ Roadmap

### ✅ Completed Features
- [x] User authentication and profiles
- [x] Course management with semesters
- [x] AI note processing (summaries, tags)
- [x] Real-time study groups and chat
- [x] 6 multiplayer game modes
- [x] AI podcast generation
- [x] Flashcard system
- [x] NFC note sharing
- [x] Daily streak tracking
- [x] Push notifications

### 🚧 In Progress
- [ ] Performance optimizations
- [ ] Accessibility improvements
- [ ] Comprehensive test coverage

### 🔮 Future Enhancements
- [ ] **Offline Mode** - Local caching with Room database
- [ ] **PDF Annotations** - Highlight and annotate in-app
- [ ] **Voice Notes** - Audio recording with AI transcription
- [ ] **Study Analytics** - Progress tracking and insights
- [ ] **Calendar Integration** - Sync with Google Calendar
- [ ] **Export Options** - Export notes as PDF/Markdown
- [ ] **Collaborative Notes** - Real-time co-editing
- [ ] **AR Study Mode** - Augmented reality flashcards
- [ ] **Widget Support** - Home screen widgets
- [ ] **Wear OS App** - Study reminders on smartwatches

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### How to Contribute

1. **Fork the repository**
   ```bash
   git clone https://github.com/yourusername/StudySage.git
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/AmazingFeature
   ```

3. **Make your changes**
   - Follow Kotlin coding conventions
   - Write meaningful commit messages
   - Add tests for new features
   - Update documentation

4. **Commit your changes**
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```

5. **Push to the branch**
   ```bash
   git push origin feature/AmazingFeature
   ```

6. **Open a Pull Request**
   - Describe your changes in detail
   - Reference any related issues
   - Ensure CI checks pass

### Code Style Guidelines
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions small and focused
- Prefer composition over inheritance

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** feat, fix, docs, style, refactor, test, chore

**Example:**
```
feat(games): add new Survival Mode game

- Implement continuous quiz gameplay
- Add streak multiplier system
- Integrate with leaderboard API

Closes #42
```

---

## 🐛 Known Issues

- ~~Notification permissions on Android 13+~~ ✅ Fixed
- Game WebSocket reconnection needs improvement
- Large PDF files (>10MB) not supported
- Image upload limited to 10MB
- Dark theme has minor contrast issues in some screens

See [Issues](https://github.com/yourusername/StudySage/issues) for full list.

---

## 🔧 Troubleshooting

### Build Fails

**Issue:** `google-services.json not found`
```bash
Solution: Download from Firebase Console and place in app/ directory
```

**Issue:** `BuildConfig fields not generated`
```bash
Solution: Create .env file with required variables and sync Gradle
```

### Runtime Errors

**Issue:** Firebase initialization error
```bash
Solution: Ensure google-services.json is in app/ and plugin is applied
```

**Issue:** Cloudinary upload fails
```bash
Solution: Check CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET in .env
```

### NFC Not Working

**Issue:** NFC sharing doesn't work
```bash
Solution: Ensure both devices have NFC enabled and Android Beam permissions
```

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Group 7 - StudySage Development Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

[Full MIT License text...]
```

---

## 👥 Team

**Group 7 - StudySage Development Team**

| Name | Role | GitHub |
|------|------|--------|
| Manav Bansal | Lead Developer | [@manavbansal1](https://github.com/manavbansal1) |
| Kabir Singh Sidhu | Backend & AI | [@kabirsinghsidhu](https://github.com/kabirsinghsidhu) |
| Ansh Tiwari | UI/UX & Frontend | [@anshtiwari](https://github.com/anshtiwari) |
| Akaaljot Singh Mathoda | Full Stack | [@akaaljotmathoda](https://github.com/akaaljotmathoda) |
| Yadhu Choudhary | Games & Multiplayer | [@yadhuchoudhary](https://github.com/yadhuchoudhary) |

---

## 🙏 Acknowledgments

### Technologies
- [Firebase](https://firebase.google.com) - Backend infrastructure
- [Cloudinary](https://cloudinary.com) - Image CDN and uploads
- [Google Cloud](https://cloud.google.com) - Cloud Run serverless backend
- [Gemini AI](https://ai.google.dev) - AI-powered summaries
- [Material Design 3](https://m3.material.io) - Design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit

### Inspiration
- Modern note-taking apps for organizational patterns
- Duolingo for gamification mechanics
- Discord for real-time chat UX
- Notion for AI-powered content processing

### Special Thanks
- Android Developers community for support
- Kotlin community for excellent documentation
- Firebase team for comprehensive SDKs
- All our beta testers and contributors

---

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/StudySage/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/StudySage/discussions)
- **Email**: studysage.team@gmail.com
- **Website**: [studysage.vercel.app](https://studysage.vercel.app)

---

## 📈 Project Status

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)

**Current Version:** 1.0.0
**Last Updated:** January 2025
**Status:** Active Development

---

<div align="center">

### ⭐ Star us on GitHub — it motivates us a lot!

[⭐ Star this repo](https://github.com/yourusername/StudySage) • [🐛 Report Bug](https://github.com/yourusername/StudySage/issues) • [✨ Request Feature](https://github.com/yourusername/StudySage/issues) • [💬 Discussions](https://github.com/yourusername/StudySage/discussions)

---

Made with ❤️ by Group 7

</div>
