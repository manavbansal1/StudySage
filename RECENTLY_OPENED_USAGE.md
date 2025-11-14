# Recently Opened PDFs - Usage Guide

## Overview
The "Recently Opened PDFs" feature tracks when users open/view notes and displays them on the home screen with timestamps and open counts.

## Implementation Summary

### Backend (AuthRepository.kt)
Three new methods were added:

1. **`addToRecentlyOpened()`** - Tracks when a note is opened
2. **`getRecentlyOpened(limit: Int = 10)`** - Retrieves recently opened notes
3. **`clearRecentlyOpened()`** - Clears the history

### ViewModel (HomeViewModel.kt)
New functionality:

1. **`markNoteAsOpened()`** - Call this when user opens a note
2. **`openPdf(context, pdfUrl)`** - Opens PDF in external viewer
3. **`loadRecentlyOpenedPdfs()`** - Loads data from Firebase

### UI (HomeScreen.kt)
Simplified card display:
- Shows PDF icon, title, subject, and relative time
- No progress tracking (removed)
- Displays "2h ago", "3d ago" format

## Firebase Schema

Data is stored in `users/{userId}/recentlyOpened` array:

```json
{
  "recentlyOpened": [
    {
      "noteId": "note123",
      "title": "Introduction to Biology",
      "fileName": "bio_chapter1.pdf",
      "fileUrl": "https://res.cloudinary.com/...",
      "courseId": "BIO101",
      "lastOpenedAt": 1699900000000,
      "openCount": 5
    }
  ]
}
```

## How to Use

### 1. When User Opens a Note from Course Screen

In your note viewing screen or when navigating to a note:

```kotlin
// Example in CoursesScreen.kt or wherever notes are opened
fun openNote(note: Note, homeViewModel: HomeViewModel) {
    // Mark the note as opened (tracks in recently opened)
    homeViewModel.markNoteAsOpened(
        noteId = note.id,
        title = note.title,
        fileName = note.originalFileName,
        fileUrl = note.fileUrl,
        courseId = note.courseId
    )
    
    // Then navigate to note detail or open the PDF
    // navController.navigate("note_detail/${note.id}")
}
```

### 2. When User Clicks on Recently Opened Card

This is already implemented in HomeScreen.kt:

```kotlin
RecentPdfCard(
    pdfName = pdf["title"] as? String ?: "Unknown",
    subject = pdf["courseId"] as? String ?: "",
    lastOpenedAt = (pdf["lastOpenedAt"] as? Number)?.toLong() ?: 0L,
    openCount = (pdf["openCount"] as? Number)?.toInt() ?: 0,
    onClick = {
        val pdfUrl = pdf["fileUrl"] as? String
        if (!pdfUrl.isNullOrBlank()) {
            homeViewModel.openPdf(context, pdfUrl)
        }
    }
)
```

### 3. Integration Example with NotesViewModel

If you have a note detail screen, integrate like this:

```kotlin
// In your NoteDetailScreen.kt or similar
@Composable
fun NoteDetailScreen(
    noteId: String,
    navController: NavController,
    notesViewModel: NotesViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val note by notesViewModel.currentNote.collectAsState()
    
    LaunchedEffect(note) {
        note?.let {
            // Mark as opened when screen is viewed
            homeViewModel.markNoteAsOpened(
                noteId = it.id,
                title = it.title,
                fileName = it.originalFileName,
                fileUrl = it.fileUrl,
                courseId = it.courseId
            )
        }
    }
    
    // ... rest of your screen UI
}
```

### 4. From CoursesScreen - When Clicking Note Card

```kotlin
// In CoursesScreen.kt
NoteCard(
    note = note,
    onClick = {
        // Track the open
        homeViewModel.markNoteAsOpened(
            noteId = note.id,
            title = note.title,
            fileName = note.originalFileName,
            fileUrl = note.fileUrl,
            courseId = note.courseId
        )
        
        // Open the PDF
        homeViewModel.openPdf(context, note.fileUrl)
        
        // OR navigate to detail screen
        // navController.navigate("note_detail/${note.id}")
    }
)
```

### 5. After Uploading a New Note

The upload flow already adds to userLibrary. To also add to recently opened:

```kotlin
// In HomeViewModel.kt uploadAndProcessNote() success block
result.onSuccess { note ->
    _processedNote.value = note
    _uploadStatus.value = "Document processed successfully!"
    
    // Automatically mark as opened since user just uploaded it
    markNoteAsOpened(
        noteId = note.id,
        title = note.title,
        fileName = note.originalFileName,
        fileUrl = note.fileUrl,
        courseId = note.courseId
    )
    
    loadRecentNotes()
}
```

## Key Features

### Automatic Deduplication
- If a note is opened multiple times, it updates the timestamp and increments open count
- Most recent opens appear first

### Limit Management
- Keeps only the 20 most recent items
- Automatically prunes old entries

### Relative Time Display
- Just now
- 5m ago
- 2h ago
- 3d ago
- 2w ago
- 3mo ago

## Testing

### Manual Test
1. Open a note from courses
2. Check home screen - should appear in "Recently Opened"
3. Open the same note again - timestamp should update
4. Open different notes - they should appear in order

### Clear Recently Opened (for testing)
```kotlin
// In your ViewModel or repository
viewModelScope.launch {
    authRepository.clearRecentlyOpened()
    loadRecentlyOpenedPdfs()
}
```

## Troubleshooting

### Cards Show "Unknown" Title
- Check that `title` field is being passed correctly in `markNoteAsOpened()`
- Verify Firebase data has `title` field

### Cards Don't Update
- Ensure `loadRecentlyOpenedPdfs()` is called after `markNoteAsOpened()`
- Check Firebase console for `recentlyOpened` array

### PDF Won't Open
- Verify `fileUrl` is valid and accessible
- Check if device has a PDF viewer app installed
- Test URL in browser first

## Future Enhancements

### Progress Tracking (Not Implemented Yet)
To add progress tracking in the future:

1. Add fields to Firebase:
```json
{
  "progress": 0.45,
  "pageCount": 50,
  "lastPage": 23
}
```

2. Update `markNoteAsOpened()` to accept progress:
```kotlin
fun markNoteAsOpened(
    noteId: String,
    title: String,
    fileName: String,
    fileUrl: String,
    courseId: String,
    currentPage: Int = 0,
    totalPages: Int = 0
)
```

3. Update UI to show progress bar in `RecentPdfCard`

### "See All" Navigation
Implement a full recently opened screen:

```kotlin
TextButton(onClick = { 
    navController.navigate("recently_opened") 
}) {
    Text("See All")
}
```

## Summary

The Recently Opened feature is now fully functional with:
- ✅ Backend tracking in Firebase
- ✅ ViewModel integration
- ✅ UI display with relative time
- ✅ Automatic deduplication
- ✅ Open count tracking
- ✅ External PDF opening
- ✅ No sample data initialization

Just call `homeViewModel.markNoteAsOpened()` whenever a user views/opens a note!

