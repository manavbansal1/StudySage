# StudySage

> AI-powered study companion for students to organize courses, process notes intelligently, and collaborate in study groups.

**Website:** [studysage.vercel.app](https://studysage.vercel.app)

---

## Overview

StudySage transforms how students manage their academic life by combining course organization, AI-powered note processing, and real-time group collaboration in a beautiful Material3 interface.

### Key Features

- **AI Note Processing** - Upload PDFs, Word docs, or text files and get instant AI-generated summaries, key points, and tags
- **Course Management** - Organize courses by semester with color-coded cards and semester/year filters
- **Study Groups** - Real-time messaging with image sharing, group invites, and role-based permissions
- **Modern UI** - Purple-gold theme with glass morphism effects and smooth animations
- **Cloud Sync** - All data synced across devices via Firebase
- **Secure Auth** - Email/password authentication with profile management

---

## Screenshots
```
[ Home Screen ]    [ Courses ]    [ Group Chat ]    [ Notes ]
                                             
```

---

## Architecture

### Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose, Material3 |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Language** | Kotlin |
| **Backend** | Firebase (Auth, Firestore, Storage) |
| **Image CDN** | Cloudinary |
| **AI Processing** | Custom API |
| **Navigation** | Jetpack Navigation Compose |
| **Async** | Kotlin Coroutines + Flow |

### Project Structure
```
com.group_7.studysage/
├── MainActivity.kt
├── data/repository/          # Data layer (5 repositories)
│   ├── AuthRepository.kt
│   ├── CourseRepository.kt
│   ├── GroupRepository.kt
│   ├── NotesRepository.kt
│   └── Course.kt                # Data models
├── navigation/                # Navigation graph
├── ui/
│   ├── screens/                 # 16 UI screens
│   │   ├── auth/               # Sign in/up
│   │   ├── HomeScreen.kt
│   │   ├── CourseScreen.kt
│   │   ├── GroupChatScreen.kt
│   │   ├── ProfileScreen.kt
│   │   └── ...
│   ├── viewmodels/             # 8 ViewModels
│   │   ├── AuthViewModel.kt
│   │   ├── CourseViewModel.kt
│   │   ├── GroupChatViewModel.kt
│   │   └── ...
│   └── theme/                  # Material3 theming
└── utils/                    # Utilities
    ├── CloudinaryUploader.kt
    ├── FileUtils.kt
    └── PermissionHandler.kt
```

### Architecture Diagram
```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  HomeScreen │ CourseScreen │ GroupScreen│
└──────────────┬──────────────────────────┘
               │ observe state
               ▼
┌─────────────────────────────────────────┐
│        ViewModel Layer (Logic)          │
│   8 ViewModels managing state & logic   │
└──────────────┬──────────────────────────┘
               │ suspend functions
               ▼
┌─────────────────────────────────────────┐
│      Repository Layer (Data)            │
│    5 Repositories abstracting data      │
└──────────────┬──────────────────────────┘
               │ async operations
               ▼
┌─────────────────────────────────────────┐
│   External Services (Firebase, etc.)    │
└─────────────────────────────────────────┘
```

---

## Features in Detail

### 1. AI-Powered Note Processing
- **Supported formats**: PDF, DOCX, TXT, MD, RTF
- **Max file size**: 10 MB
- **AI extracts**: Summary, key points, tags
- **Organization**: Link notes to courses or save as general notes

### 2. Course Management
- **CRUD operations**: Create, read, update, delete courses
- **Filters**: Semester (Spring/Summer/Fall/Winter) + Year
- **Color coding**: 12 predefined colors for visual organization
- **Archive**: Soft-delete courses without losing data

### 3. Study Groups
- **Real-time chat**: Live messaging with Firestore observers
- **Invites system**: Email-based group invitations
- **Member roles**: Admin/member permissions
- **Image sharing**: Upload via Cloudinary CDN
- **Group profiles**: Customizable name, description, picture

### 4. Profile Management
- **Edit profile**: Name and bio
- **Profile picture**: Upload via camera or gallery
- **Recent PDFs**: Track recently opened documents
- **Sign out**: Secure session management

---

## Firebase Structure

### Firestore Collections
```javascript
/users/{userId}
  ├── name, email, bio, profilePic
  ├── groups: [{ groupId, groupName, lastMessage, ... }]
  ├── groupInvites: [{ inviteId, groupId, ... }]
  └── recentlyOpenedPdfs: [...]

/courses/{courseId}
  ├── title, code, semester, year
  ├── instructor, description, color
  └── userId, createdAt, isArchived

/notes/{noteId}
  ├── title, summary, content
  ├── tags, keyPoints (AI-generated)
  └── userId, courseId, fileUrl

/groups/{groupId}
  ├── name, description, profilePic
  ├── members: [{ userId, name, role, ... }]
  └── /messages/{messageId}  ← subcollection
      ├── senderId, message, timestamp
      └── images: [...]
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 24+
- Firebase project
- Cloudinary account

### Setup Instructions

1. **Clone the repository**
```bash
   git clone https://github.com/yourusername/studysage.git
   cd studysage
```

2. **Configure Firebase**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Email/Password)
   - Create Firestore Database
   - Enable Firebase Storage
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Configure Cloudinary**
   - Create account at [cloudinary.com](https://cloudinary.com)
   - Get your cloud name and upload preset
   - Add to `build.gradle.kts`:
```kotlin
     buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"your_cloud_name\"")
     buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"your_preset\"")
```

4. **Build and Run**
```bash
   ./gradlew assembleDebug
```
   Or click ▶️ Run in Android Studio

---

## Project Stats

- **Total Files**: 36 Kotlin files
- **Lines of Code**: ~10,100
- **ViewModels**: 8
- **Repositories**: 5
- **UI Screens**: 16
- **Supported File Types**: 6 (PDF, DOCX, TXT, MD, RTF, images)

---

## Roadmap

- [ ] **Notifications** - Push notifications for messages and invites
- [ ] **Offline Mode** - Local caching with Room database
- [ ] **PDF Viewer** - In-app PDF reading with annotations
- [ ] **Voice Notes** - Audio recording and AI transcription
- [ ] **Study Analytics** - Progress tracking and insights
- [ ] **Calendar Sync** - Assignment deadline integration
- [ ] **Export Options** - Export notes as PDF or Markdown
- [ ] **Gamification** - XP system with badges and leaderboards

---

## Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## Known Issues

- GameScreen placeholder (gamification features not implemented)
- PermissionHandler needs implementation for camera/storage
- Image upload limited to 10MB

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contributors

**Group 7 - StudySage Development Team**

- Manav Bansal
- Kabir Singh Sidhu
- Ansh Tiwari
- Akaaljot Singh Mathoda
- Yadhu Choudhary

---

## Acknowledgments

- [Firebase](https://firebase.google.com) - Backend infrastructure
- [Cloudinary](https://cloudinary.com) - Image CDN
- [Material Design 3](https://m3.material.io) - Design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit

---

<div align="center">

[⭐ Star this repo](https://github.com/yourusername/studysage) • [🐛 Report Bug](https://github.com/yourusername/studysage/issues) • [✨ Request Feature](https://github.com/yourusername/studysage/issues)

</div>

