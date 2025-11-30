<div align="center">

<img src="https://img.shields.io/badge/StudySage-v1.0.0-9333EA?style=for-the-badge&logo=android&logoColor=white" alt="StudySage">

# StudySage

### *AI-Powered Study Companion with Real-Time Multiplayer Games*

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-BOM_2024.12-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=white" alt="Firebase">
  <img src="https://img.shields.io/badge/Ktor-3.3.1-087CFA?style=flat-square&logo=ktor&logoColor=white" alt="Ktor">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_7.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-FBBF24?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Build-Passing-10B981?style=flat-square" alt="Build">
</p>

---

**Transform your study experience with AI-powered content processing, collaborative study groups, and competitive multiplayer games.**

[**View Quick Setup →**](SETUP.md)

</div>


## Overview

StudySage is a comprehensive educational Android application that combines **AI-powered content processing**, **collaborative study groups**, and **real-time multiplayer games**. Built with modern Android technologies and a custom Kotlin/Ktor backend, StudySage creates an engaging, gamified learning environment.

> **Backend Access**: The multiplayer game server runs on a separate Kotlin/Ktor backend maintained in a private repository. The app's core features (notes, courses, groups, flashcards) work without the backend. For full multiplayer game functionality, email **bansalmanav39@gmail.com** for backend access.

<table>
<tr>
<td width="33%" align="center" valign="top">
<br>
<img src="https://api.iconify.design/material-symbols:auto-awesome.svg?color=%239333ea" width="48" height="48">
<h3 style="margin: 12px 0 8px 0;">AI-Powered</h3>
<p style="margin: 0; padding: 0 10px;">Auto-generate summaries, key points, and tags from your study materials using Gemini 1.5 Flash</p>
<br>
</td>
<td width="33%" align="center" valign="top">
<br>
<img src="https://api.iconify.design/material-symbols:groups.svg?color=%23fbbf24" width="48" height="48">
<h3 style="margin: 12px 0 8px 0;">Collaborative</h3>
<p style="margin: 0; padding: 0 10px;">Real-time study groups with live chat and image sharing</p>
<br>
</td>
<td width="33%" align="center" valign="top">
<br>
<img src="https://api.iconify.design/material-symbols:sports-esports.svg?color=%239333ea" width="48" height="48">
<h3 style="margin: 12px 0 8px 0;">Gamified</h3>
<p style="margin: 0; padding: 0 10px;">Compete in multiplayer games with XP system and leaderboards</p>
<br>
</td>
</tr>
</table>


## Key Features

<details open>
<summary><b><img src="https://api.iconify.design/material-symbols:auto-awesome.svg?color=%239333ea" width="18" height="18" style="vertical-align: middle; margin-right: 4px;"> AI-Powered Note Processing</b></summary>
<br>

Upload documents and let Gemini AI extract the important information:

- **Multi-Format Support**: PDF, DOCX, TXT, MD, RTF (up to 10MB)
- **Auto-Generation**: Summaries, key points, and smart tags
- **Dual Storage**: Firebase Storage + Cloudinary CDN
- **Recent Activity**: Track most-accessed PDFs with open counts
- **Course Linking**: Organize notes by course or standalone

</details>

<details>
<summary><b><img src="https://api.iconify.design/material-symbols:school.svg?color=%239333ea" width="18" height="18" style="vertical-align: middle; margin-right: 4px;"> Smart Course Management</b></summary>
<br>

Organize your academic life with color-coded courses:

- **Full CRUD**: Create, edit, archive, delete
- **Semester System**: Spring/Summer/Fall/Winter + Year
- **12 Color Themes**: Visual organization
- **Rich Metadata**: Code, instructor, credits, description
- **Archive System**: Soft-delete without data loss

</details>

<details>
<summary><b><img src="https://api.iconify.design/material-symbols:groups.svg?color=%23fbbf24" width="18" height="18" style="vertical-align: middle; margin-right: 4px;"> Real-Time Study Groups</b></summary>
<br>

Collaborate with classmates instantly:

- **Live Chat**: <100ms latency with Firestore snapshots
- **Image Sharing**: Cloudinary-powered uploads
- **Email Invites**: Send and manage invitations
- **Role System**: Admin and member permissions
- **Full History**: Complete chat with timestamps

</details>

<details>
<summary><b><img src="https://api.iconify.design/material-symbols:sports-esports.svg?color=%239333ea" width="18" height="18" style="vertical-align: middle; margin-right: 4px;"> Multiplayer Games</b></summary>
<br>

Learn while competing in real-time:

### <img src="https://api.iconify.design/material-symbols:flag.svg?color=%23ef4444" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Quiz Race
Competitive quiz with speed bonuses
- Time-limited questions (15-60s)
- Speed-based scoring formula
- Real-time leaderboards
- AI-generated from your notes

### <img src="https://api.iconify.design/material-symbols:grid-on.svg?color=%239333ea" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Study Tac Toe
Tic-tac-toe meets trivia
- Answer questions to claim squares
- Turn-based strategy
- 3-in-a-row wins
- Tests knowledge + tactics

**Backend**: WebSocket-powered (<50ms sync) • Kotlin/Ktor • 6-char room codes • Firebase persistence

</details>

<details>
<summary><b><img src="https://api.iconify.design/material-symbols:extension.svg?color=%23fbbf24" width="18" height="18" style="vertical-align: middle; margin-right: 4px;"> Additional Features</b></summary>
<br>

**AI Podcast Generation**:
- Convert notes to natural-sounding audio with Google Cloud TTS
- Multiple voice options and languages
- Playback controls: play, pause, seek, speed adjustment (0.5x - 2.0x)
- Offline download support for study on-the-go
- Background playback while using other apps

**Flashcard System**:
- Create custom flashcard decks with questions and answers
- Link flashcards to specific courses or topics
- Study mode with flip animation and progress tracking
- Shuffle mode for varied practice
- Mark difficult cards for focused review

**NFC Note Sharing**:
- Share notes instantly by tapping NFC-enabled phones
- Host Card Emulation (HCE) technology
- Secure APDU service for encrypted data transfer
- Works completely offline - no internet required
- Bidirectional: both send and receive capabilities

**XP & Gamification System**:
- Earn experience points for all activities (studying, games, streaks)
- Level progression formula: XP = Level² × 100
- Visual level badges and achievements
- Daily tasks with bonus XP rewards
- Leaderboards to compete with friends

**Daily Streaks & Tracking**:
- Track consecutive days of study activity
- Visual streak counter with fire icon
- Streak milestones and rewards
- Push notifications to maintain streaks
- Study time tracker with daily goals (30 min default)

**Smart Notifications**:
- Daily study reminders via WorkManager (scheduled at 9 AM)
- Real-time group message notifications via FCM
- Study group invitation alerts
- Streak reminder notifications
- Customizable notification preferences per category

</details>

<br>

## Tech Stack

<table>
<tr>
<td width="50%" valign="top">

<h3>Frontend</h3>

```kotlin
Language:      Kotlin 2.2.21
UI:            Jetpack Compose (BOM 2024.12)
Design:        Material Design 3
Architecture:  MVVM + Clean Architecture
State:         StateFlow + Coroutines
Navigation:    Navigation Compose 2.8.5
Image:         Coil 2.5.0
Network:       Retrofit + OkHttp + WebSocket
Async:         Kotlin Coroutines + Flow
```

**Min SDK**: 24 (Android 7.0) • **Target SDK**: 36

</td>
<td width="50%" valign="top">

<h3>Backend & Cloud</h3>

```yaml
Authentication:  Firebase Auth
Database:        Cloud Firestore (NoSQL)
Storage:         Firebase + Cloudinary CDN
AI:              Gemini 1.5 Flash (Vertex AI)
Game Server:     Kotlin/Ktor 3.3.1 (separate repo*)
Hosting:         Railway / Cloud Run
TTS:             Google Cloud Text-to-Speech
Real-time:       WebSocket + Firestore Snapshots
```

<sub>* Game server is in a private repository. Email **bansalmanav39@gmail.com** for access.</sub>

</td>
</tr>
</table>

<br>

## Architecture

### MVVM + Clean Architecture

```
┌─────────────────────────────────────────┐
│   UI Layer (Jetpack Compose)           │
│   30+ Screens • Material Design 3      │
└──────────────┬──────────────────────────┘
               │ StateFlow<UiState>
               ▼
┌─────────────────────────────────────────┐
│   ViewModel Layer                       │
│   17 ViewModels • Business Logic        │
│   Threading: ~91 concurrent threads     │
└──────────────┬──────────────────────────┘
               │ suspend functions
               ▼
┌─────────────────────────────────────────┐
│   Repository Layer                      │
│   11 Repos • Single Source of Truth     │
│   Auth • Course • Notes • Game • Group  │
└──────────────┬──────────────────────────┘
               │ Dispatchers.IO
               ▼
┌─────────────────────────────────────────┐
│   External Services                     │
│   Firebase • Cloudinary • Ktor Backend  │
└─────────────────────────────────────────┘
```

### Threading Model

- **Main Thread**: UI rendering only
- **IO Dispatcher**: Firebase, network calls, file operations
- **Default Dispatcher**: Heavy computations, JSON parsing
- **WebSocket Pool**: Dedicated threads for real-time games

**Total: ~91 concurrent threads** for optimal performance

### Project Structure

```
StudySage/
├── app/src/main/java/com/group_7/studysage/
│   ├── data/
│   │   ├── repository/          # 11 Repositories
│   │   ├── api/                 # REST & WebSocket APIs
│   │   ├── model/               # 40+ Data classes
│   │   └── websocket/           # WebSocket manager
│   ├── viewmodels/              # 17 ViewModels
│   ├── ui/
│   │   ├── screens/             # 30+ Composable screens
│   │   ├── components/          # Reusable UI components
│   │   └── theme/               # Purple-gold theme
│   └── utils/                   # Helpers & utilities
```

> **Note**: The **Kotlin/Ktor game server backend** is maintained in a **separate private repository**. To request access for development or deployment, please email **bansalmanav39@gmail.com**.

<br>

## Multiplayer Games

### Game Architecture

```
Android App (WebSocket Client)
         │
         │ <50ms latency
         ▼
Ktor Backend (Game Server)*
  • Session management
  • Turn-based logic
  • AI question generation
         │
         │ Persistence
         ▼
Firebase Firestore
```

<sub>* The game server backend is maintained in a separate private repository. Email **bansalmanav39@gmail.com** for access.</sub>

### <img src="https://api.iconify.design/material-symbols:flag.svg?color=%23ef4444" width="22" height="22" style="vertical-align: middle; margin-right: 6px;">Quiz Race

**Fast-paced competitive quiz where speed matters**

**Flow**:
1. Host uploads study material → AI generates questions
2. Players join with 6-char code → WebSocket connections
3. Questions broadcast simultaneously → Players submit answers
4. Real-time validation & leaderboard updates

**Scoring**:
```kotlin
basePoints = 100
speedBonus = (timeLimit - timeElapsed) / 100
totalPoints = basePoints + speedBonus // if correct
```

**WebSocket Messages**:
- `NEXT_QUESTION` → All players (question data)
- `SUBMIT_ANSWER` → Server (answer + timestamp)
- `ANSWER_RESULT` → Individual player (points earned)
- `SCORES_UPDATE` → All players (leaderboard)

### <img src="https://api.iconify.design/material-symbols:grid-on.svg?color=%239333ea" width="22" height="22" style="vertical-align: middle; margin-right: 6px;">Study Tac Toe

**Strategic tic-tac-toe with knowledge checks**

**Turn Flow**:
```
Player selects square
       ↓
Server sends question
       ↓
Player answers
       ↓
┌─────────┬──────────┐
│ Correct │ Wrong    │
├─────────┼──────────┤
│ Claim   │ Forfeit  │
│ square  │ turn     │
└─────────┴──────────┘
       ↓
Check for win (3-in-a-row)
       ↓
Switch turns
```

**Board State Sync**:
```kotlin
// Real-time via WebSocket
BOARD_UPDATE {
  boardState: List  // ["", "X", "O", ...]
  currentTurn: String       // User ID
  lastMove: Int             // Square index
}

TURN_UPDATE {
  currentPlayerId: String
  playerSymbol: String      // "X" or "O"
}
```

**Backend Endpoints**:
```http
POST   /api/games/groups/{groupId}/sessions          # Create
GET    /api/games/groups/{groupId}/sessions          # List
POST   /api/games/.../sessions/{id}/join             # Join
POST   /api/games/.../sessions/{id}/start            # Start
WS     /ws/game/{sessionId}                          # Play
```

<br>

## Firebase Schema

```javascript
firestore/
├── users/{userId}
│   ├── name, email, bio, profilePicUrl
│   ├── xpPoints, level, dailyStreak
│   └── recentlyOpenedPdfs: [{noteId, title, lastOpenedAt}]
│
├── courses/{courseId}
│   ├── title, code, semester, year
│   └── instructor, credits, color, isArchived
│
├── notes/{noteId}
│   ├── title, content, fileUrl
│   ├── summary, tags, keyPoints  # AI-generated
│   └── userId, courseId
│
├── groups/{groupId}
│   ├── name, description, profilePic
│   ├── members: [{userId, role, joinedAt}]
│   └── messages/{messageId}
│       └── senderId, message, images, timestamp
│
└── gameSessions/{sessionId}
    ├── gameType, hostId, status, players
    ├── questions, settings, currentQuestionIndex
    └── boardState, currentTurn  # Study Tac Toe
```

<br>

## Design System

### Color Palette

```kotlin
// Primary Colors
Purple Primary:   #652497  // Deep rich purple
Purple Secondary: #9333EA  // Vibrant purple
Gold Tertiary:    #FBBF24  // Contrasting gold

// Light Theme
Background:       #FCFCFF  // Clean white
Surface:          #FFFFFF  // Pure white cards
Text:             #1B1921  // Dark purple-black

// Dark Theme  
Background:       #1A1721  // Very dark purple
Surface:          #2C2A3A  // Dark purple-grey
Text:             #F5F3FF  // Off-white
```

### UI Highlights

- **Material Design 3** custom purple-gold theme
- **Glassmorphism** effects on navigation
- **Smooth animations** for transitions
- **Dark/Light themes** based on system
- **Responsive layouts** for all screen sizes

<br>

##  Team

<div align="center">

| Developer | Role | GitHub |
|-----------|------|--------|
| **Manav Bansal** | Backend & WebSocket Integration | [@manavbansal1](https://github.com/manavbansal1) |
| **Kabir Singh Sidhu** | AI Integration & Game UI | [@kabirsinghsidhu](https://github.com/kab1rs1dhu) |
| **Ansh Tiwari** | UI/UX Design & NFC | [@anshtiwari](https://github.com/CandyRagi) |
| **Akaaljot Singh Mathoda** | Notifications & Quick Actions | [@akaaljotmathoda](https://github.com/Jassa47) |
| **Yadhu Choudhary** | Podcasts, Flashcards & Content | [@yadhuchoudhary](https://github.com/yadhuuu1110) |

</div>

<br>

## License

MIT License - Copyright (c) 2025 Group 7 - StudySage Development Team

See [LICENSE](LICENSE) file for details.


## Acknowledgments

**Technologies**: [Firebase](https://firebase.google.com) • [Cloudinary](https://cloudinary.com) • [Google Gemini](https://ai.google.dev) • [Ktor](https://ktor.io) • [Jetpack Compose](https://developer.android.com/jetpack/compose)

**Inspiration**: Notion (AI processing) • Discord (real-time chat) • Duolingo (gamification)

---


<div align="center">

<div align="center">

<p>
<img src="https://api.iconify.design/material-symbols:mail.svg?color=%239333ea" width="18" height="18" style="vertical-align: middle; margin-right: 6px;"><b>bansalmanav39@gmail.com</b>
</p>
</p>

### ⭐ Star this repo if you found it helpful!

<img src="https://img.shields.io/badge/Status-Active_Development-10B981?style=flat-square" alt="Status">
<img src="https://img.shields.io/badge/Version-1.0.0-9333EA?style=flat-square" alt="Version">
<img src="https://img.shields.io/badge/Made_with-❤️-ef4444?style=flat-square" alt="Love">

</div>
