<div align="center">

<img src="https://img.shields.io/badge/Quick_Setup-StudySage-9333EA?style=for-the-badge&logo=rocket&logoColor=white" alt="Quick Setup">

# Quick Setup Guide

### Get StudySage running in 15 minutes

<p align="center">
  <img src="https://api.iconify.design/material-symbols:rocket-launch.svg?color=%239333ea" width="48" height="48">
</p>

[← Back to README](README.md)

</div>

<br>

## <img src="https://api.iconify.design/material-symbols:checklist.svg?color=%239333ea" width="26" height="26" style="vertical-align: middle;"> Prerequisites

Before you begin, ensure you have:

- <img src="https://api.iconify.design/devicon:androidstudio.svg" width="16" height="16"> **Android Studio** Hedgehog (2023.1.1) or later
- <img src="https://api.iconify.design/logos:java.svg" width="16" height="16"> **JDK 17** or higher
- <img src="https://api.iconify.design/logos:android-icon.svg" width="16" height="16"> **Android SDK** with API levels 24-36
- <img src="https://api.iconify.design/logos:git-icon.svg" width="16" height="16"> **Git** for version control
- <img src="https://api.iconify.design/logos:firebase.svg" width="16" height="16"> **Firebase Account** (free tier)
- <img src="https://api.iconify.design/simple-icons:cloudinary.svg?color=%233448c5" width="16" height="16"> **Cloudinary Account** (free tier)

> **Optional**: For multiplayer game features, you'll need access to the backend repository. Email **bansalmanav39@gmail.com** to request access.

<br>

## <img src="https://api.iconify.design/material-symbols:download.svg?color=%23fbbf24" width="26" height="26" style="vertical-align: middle;"> Step 1: Clone Repository

```bash
git clone https://github.com/manavbansal1/StudySage.git
cd StudySage
```

<br>

## <img src="https://api.iconify.design/logos:firebase.svg" width="26" height="26" style="vertical-align: middle;"> Step 2: Firebase Setup

### 2.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **"Add project"** → Name it **"StudySage"**
3. Disable Google Analytics (optional)
4. Click **"Create project"**

### 2.2 Add Android App

1. Click **"Add app"** → Select <img src="https://api.iconify.design/logos:android-icon.svg" width="16" height="16"> **Android**
2. **Package name**: `com.group_7.studysage`
3. **App nickname**: StudySage
4. Click **"Register app"**
5. **Download** `google-services.json`
6. **Place** it in `StudySage/app/` directory

### 2.3 Enable Firebase Services

#### <img src="https://api.iconify.design/material-symbols:lock.svg?color=%239333ea" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Authentication

```
Firebase Console → Build → Authentication → Get Started
→ Sign-in method → Email/Password → Enable → Save
```

#### <img src="https://api.iconify.design/material-symbols:database.svg?color=%239333ea" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Firestore Database

```
Firebase Console → Build → Firestore Database → Create database
→ Start in test mode → Select region (us-central1) → Enable
```

#### <img src="https://api.iconify.design/material-symbols:folder.svg?color=%239333ea" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Storage

```
Firebase Console → Build → Storage → Get Started
→ Start in test mode → Done
```

#### <img src="https://api.iconify.design/material-symbols:auto-awesome.svg?color=%23fbbf24" width="20" height="20" style="vertical-align: middle; margin-right: 4px;"> Vertex AI (Gemini)

```
Firebase Console → Build → Vertex AI in Firebase
→ Get Started → Enable APIs
```

### 2.4 Security Rules (Development)

**Firestore Rules**:
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

**Storage Rules**:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

> ⚠️ **Important**: Update these rules for production deployment!

<br>

## <img src="https://api.iconify.design/simple-icons:cloudinary.svg?color=%233448c5" width="26" height="26" style="vertical-align: middle;"> Step 3: Cloudinary Setup

1. Create account at [cloudinary.com](https://cloudinary.com)
2. Go to **Dashboard** → **Settings** → **Upload**
3. Create **unsigned upload preset**:
   - Scroll to "Upload presets"
   - Click "Add upload preset"
   - **Signing Mode**: Unsigned
   - **Preset name**: `studysage_uploads`
   - Click **"Save"**
4. Note your **Cloud Name** from dashboard

<br>

## <img src="https://api.iconify.design/material-symbols:settings.svg?color=%239333ea" width="26" height="26" style="vertical-align: middle;"> Step 4: Configure API Keys

Create `local.properties` file in root directory:

```properties
# Android SDK (auto-generated by Android Studio)
sdk.dir=/path/to/Android/Sdk

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME=your_cloud_name_here
CLOUDINARY_UPLOAD_PRESET=studysage_uploads

# Backend API URL
# For Android Emulator:
BACKEND_API_URL=http://10.0.2.2:8080

# For Physical Device (replace with your IP):
# BACKEND_API_URL=http://192.168.x.x:8080

# For Production (Railway deployment):
# BACKEND_API_URL=https://your-app.railway.app
```

> 📝 **Note**: `local.properties` is in `.gitignore` - your keys are safe!

<br>

## <img src="https://api.iconify.design/logos:ktor-icon.svg" width="26" height="26" style="vertical-align: middle;"> Step 5: Backend Setup (Game Server)

> **Important**: The Kotlin/Ktor game server backend is maintained in a **separate private repository**. To request access, please email **bansalmanav39@gmail.com** with your use case (development, testing, or deployment).

Once you have access to the backend repository:

### 5.1 Get Firebase Admin SDK Key

1. Firebase Console → **Project Settings** (gear icon)
2. **Service accounts** tab
3. Click **"Generate new private key"**
4. Save JSON file as `firebase-credentials.json`

### 5.2 Clone Backend Repository

```bash
# After receiving access, clone the backend repository
git clone <backend-repo-url>
cd studysage-backend
```

### 5.3 Configure Backend

Create `.env` file in `studysage-backend/` directory:

```bash
# Firebase Admin SDK
FIREBASE_SERVICE_ACCOUNT_PATH=/absolute/path/to/firebase-credentials.json

# Gemini AI (get from ai.google.dev)
GEMINI_API_KEY=your_gemini_api_key_here

# Server Configuration
PORT=8080
```

### 5.4 Run Backend Locally

```bash
cd studysage-backend
./gradlew clean build
./gradlew run
```

Server starts at `http://localhost:8080`

**Test health endpoint**:
```bash
curl http://localhost:8080/api/games/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T12:00:00Z"
}
```

<br>

## <img src="https://api.iconify.design/material-symbols:build.svg?color=%23fbbf24" width="26" height="26" style="vertical-align: middle;"> Step 6: Build & Run App

### Option A: Android Studio

1. Open **Android Studio**
2. **File** → **Open** → Select `StudySage` folder
3. Wait for Gradle sync
4. Connect Android device or start emulator
5. Click <img src="https://api.iconify.design/material-symbols:play-arrow.svg?color=%2310b981" width="16" height="16"> **Run** button

### Option B: Command Line

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or combine both
./gradlew clean assembleDebug installDebug
```

### Network Configuration

**Android Emulator**:
```properties
BACKEND_API_URL=http://10.0.2.2:8080
```
(Emulator automatically maps `10.0.2.2` to host machine's `localhost`)

**Physical Device**:
1. Find your computer's local IP:
   - **Mac/Linux**: `ifconfig | grep inet`
   - **Windows**: `ipconfig`
2. Update `local.properties`:
   ```properties
   BACKEND_API_URL=http://192.168.x.x:8080
   ```
3. Ensure device and computer on same WiFi network

<br>

## <img src="https://api.iconify.design/material-symbols:rocket.svg?color=%239333ea" width="26" height="26" style="vertical-align: middle;"> Step 7: Deploy Backend (Production)

> **Note**: This step requires access to the backend repository. If you don't have access yet, email **bansalmanav39@gmail.com**.

### Deploy to Railway

1. Sign up at [railway.app](https://railway.app)
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your StudySage repository
4. Select `studysage-backend` directory
5. Add **Environment Variables**:

```bash
GEMINI_API_KEY=your_gemini_api_key
FIREBASE_CREDENTIALS=<paste entire firebase-credentials.json as single line>
PORT=8080
```

6. **Deploy!** Railway provides URL: `https://your-app.railway.app`

7. Update `local.properties` in Android app:
```properties
BACKEND_API_URL=https://your-app.railway.app
```

8. Rebuild and reinstall app

### Alternative: Google Cloud Run

See backend's `DEPLOYMENT.md` for Cloud Run instructions.

<br>

## <img src="https://api.iconify.design/material-symbols:check-circle.svg?color=%2310b981" width="26" height="26" style="vertical-align: middle;"> Step 8: Verify Installation

Test each feature:

1. <img src="https://api.iconify.design/material-symbols:person-add.svg?color=%239333ea" width="16" height="16"> **Sign Up**: Create account with email/password
2. <img src="https://api.iconify.design/material-symbols:school.svg?color=%239333ea" width="16" height="16"> **Create Course**: Add "CMPT 276 - Software Engineering"
3. <img src="https://api.iconify.design/material-symbols:upload-file.svg?color=%239333ea" width="16" height="16"> **Upload Note**: Upload PDF → See AI summary
4. <img src="https://api.iconify.design/material-symbols:groups.svg?color=%23fbbf24" width="16" height="16"> **Create Group**: Invite friend via email
5. <img src="https://api.iconify.design/material-symbols:sports-esports.svg?color=%239333ea" width="16" height="16"> **Start Game**: Create Quiz Race → Share code
6. <img src="https://api.iconify.design/material-symbols:nfc.svg?color=%239333ea" width="16" height="16"> **Test NFC**: (If available) Share note by tapping

<br>

## <img src="https://api.iconify.design/material-symbols:help.svg?color=%23fbbf24" width="26" height="26" style="vertical-align: middle;"> Troubleshooting

<details>
<summary><b>Build fails with "google-services.json not found"</b></summary>

**Solution**: Ensure `google-services.json` is in `app/` directory, not root.

```bash
# Correct location
StudySage/app/google-services.json

# Verify
ls app/google-services.json
```
</details>

<details>
<summary><b>Backend not connecting from emulator</b></summary>

**Solution**: Use special IP `10.0.2.2` for emulator:

```properties
BACKEND_API_URL=http://10.0.2.2:8080
```
</details>

<details>
<summary><b>Cloudinary upload fails</b></summary>

**Solutions**:
1. Verify cloud name and preset in `local.properties`
2. Ensure upload preset is **unsigned**
3. Check internet connection
4. Verify file size < 10MB
</details>

<details>
<summary><b>WebSocket connection fails</b></summary>

**Solutions**:
1. Ensure backend is running: `curl http://localhost:8080/api/games/health`
2. Check firewall allows port 8080
3. For physical device, verify same WiFi network
4. Check backend logs for errors
</details>

<details>
<summary><b>AI summaries not generating</b></summary>

**Solutions**:
1. Verify Vertex AI is enabled in Firebase Console
2. Check Gemini API key in backend `.env`
3. Ensure PDF text is extractable (not image-based)
4. Check backend logs for AI errors
</details>

<details>
<summary><b>"Duplicate class" errors during build</b></summary>

**Solution**: Clean and rebuild:

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```
</details>

<br>

## <img src="https://api.iconify.design/material-symbols:info.svg?color=%239333ea" width="26" height="26" style="vertical-align: middle;"> Additional Resources

- **Firebase Docs**: [firebase.google.com/docs](https://firebase.google.com/docs)
- **Jetpack Compose**: [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
- **Ktor**: [ktor.io/docs](https://ktor.io/docs)
- **Material Design 3**: [m3.material.io](https://m3.material.io)

<br>

## <img src="https://api.iconify.design/material-symbols:support-agent.svg?color=%23fbbf24" width="26" height="26" style="vertical-align: middle;"> Need Help?

- **Issues**: [github.com/manavbansal1/StudySage/issues](https://github.com/manavbansal1/StudySage/issues)
- **Email**: studysage.team@gmail.com
- **Discussions**: [github.com/manavbansal1/StudySage/discussions](https://github.com/manavbansal1/StudySage/discussions)

---

<div align="center">

**Setup complete!** 🎉 **Start studying smarter with StudySage!**

[← Back to README](README.md)

<img src="https://img.shields.io/badge/Happy_Coding-9333EA?style=for-the-badge&logo=android&logoColor=white" alt="Happy Coding">

</div>
