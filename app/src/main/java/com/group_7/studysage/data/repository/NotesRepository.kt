package com.group_7.studysage.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.group_7.studysage.BuildConfig
import kotlinx.coroutines.tasks.await
import java.io.InputStream

data class Note(
    val id: String = "",
    val title: String = "",
    val originalFileName: String = "",
    val content: String = "",
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val fileUrl: String = "",
    val fileType: String = ""
)

class NotesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Primary model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Fallback model
    private val fallbackModel = GenerativeModel(
        modelName = "gemini-1.0-pro",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

//    // Test Firebase connectivity (Firestore only)
//    suspend fun testFirebaseConnection(): Result<String> {
//        return try {
//            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
//
//            // Test Firestore only
//            val testDoc = firestore.collection("test").document("connectivity")
//            testDoc.set(mapOf("timestamp" to System.currentTimeMillis())).await()
//
//            // Clean up
//            testDoc.delete().await()
//
//            Result.success("Firebase Firestore connection successful")
//        } catch (e: Exception) {
//            Log.e("NotesRepository", "Firebase connection test failed: ${e.message}", e)
//            Result.failure(e)
//        }
//    }
//
//    // Test Gemini AI connectivity
//    suspend fun testGeminiConnection(): Result<String> {
//        return try {
//            Log.d("NotesRepository", "Testing Gemini API connection...")
//
//            val testPrompt = "Say 'Hello' if you can read this message."
//
//            try {
//                val response = generativeModel.generateContent(testPrompt)
//                val result = response.text
//
//                if (!result.isNullOrBlank()) {
//                    Log.d("NotesRepository", "Primary Gemini model working: $result")
//                    Result.success("✅ Gemini AI (primary model) connected successfully")
//                } else {
//                    throw Exception("Empty response from primary model")
//                }
//            } catch (e: Exception) {
//                Log.w("NotesRepository", "Primary model failed, testing fallback: ${e.message}")
//
//                try {
//                    val fallbackResponse = fallbackModel.generateContent(testPrompt)
//                    val fallbackResult = fallbackResponse.text
//
//                    if (!fallbackResult.isNullOrBlank()) {
//                        Log.d("NotesRepository", "Fallback Gemini model working: $fallbackResult")
//                        Result.success("⚠️ Gemini AI (fallback model) connected successfully")
//                    } else {
//                        throw Exception("Both models returned empty responses")
//                    }
//                } catch (fallbackException: Exception) {
//                    throw Exception("Both primary and fallback models failed: ${fallbackException.message}")
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("NotesRepository", "Gemini connection test failed: ${e.message}", e)
//            Result.failure(Exception("❌ Gemini AI connection failed: ${e.message}"))
//        }
//    }

    suspend fun uploadNotesAndProcess(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (String) -> Unit
    ): Result<Note> {

        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")
            Log.d("NotesRepository", "Starting processing for user: $userId, file: $fileName")
            onProgress("Reading file...")

            val input: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Could not read file")

            // Read file content
            val fileBytes = input.readBytes()
            Log.d("NotesRepository", "File size: ${fileBytes.size} bytes")

            val fileContent = when {
                fileName.endsWith(".txt", ignoreCase = true) -> String(fileBytes)
                fileName.endsWith(".pdf", ignoreCase = true) -> {
                    onProgress("Processing PDF...")
                    // For now, treat PDF as text (you can implement proper PDF parsing later)
                    String(fileBytes)
                }
                fileName.endsWith(".doc", ignoreCase = true) ||
                        fileName.endsWith(".docx", ignoreCase = true) -> {
                    onProgress("Processing Word document...")
                    String(fileBytes)
                }
                else -> String(fileBytes)
            }

            onProgress("Generating AI summary...")

            // Generate AI-powered content
            val summary = generateAISummary(fileContent)
            val keyPoints = extractKeyPoints(fileContent)
            val tags = generateTags(fileContent)
            val title = generateTitle(fileContent, fileName)

            onProgress("Saving to database...")

            // Create note object - no file storage needed
            val noteId = firestore.collection("notes").document().id
            val note = Note(
                id = noteId,
                title = title,
                originalFileName = fileName,
                content = fileContent,
                summary = summary,
                keyPoints = keyPoints,
                tags = tags,
                userId = userId,
                fileUrl = "", // No file URL needed
                fileType = getFileType(fileName)
            )

            // Save to Firestore only
            firestore.collection("notes")
                .document(noteId)
                .set(note)
                .await()

            Log.d("NotesRepository", "Note saved successfully with ID: $noteId")
            onProgress("Note processed successfully!")
            Result.success(note)

        } catch (exception: Exception) {
            Log.e("NotesRepository", "uploadNotesAndProcess error: ${exception.message}", exception)

            // Provide more specific error messages
            val errorMessage = when {
                exception.message?.contains("Permission denied") == true ->
                    "Permission denied. Please check your authentication."
                exception.message?.contains("Network") == true ->
                    "Network error. Please check your internet connection."
                exception.message?.contains("Firestore") == true ->
                    "Database error. Please try again."
                else -> "Error processing file: ${exception.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun getUserNotes(): List<Note> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Note::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("NotesRepository", "Error parsing note: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching user notes: ${e.message}")
            emptyList()
        }
    }

    suspend fun getNoteById(noteId: String): Note? {
        return try {
            val document = firestore.collection("notes").document(noteId).get().await()
            document.toObject(Note::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching note: ${e.message}")
            null
        }
    }

    private suspend fun generateAISummary(content: String): String {
        return try {
            // Limit content size for API call (Gemini has token limits)
            val limitedContent = if (content.length > 8000) {
                content.take(8000) + "..."
            } else content

            val prompt = """
                Please provide a concise summary of the following document content in 2-3 paragraphs. 
                Focus on the main concepts, key information, and important details that would be helpful for studying:
                
                $limitedContent
            """.trimIndent()

            Log.d("NotesRepository", "Attempting to generate summary with primary model...")

            try {
                val response = generativeModel.generateContent(prompt)
                response.text ?: throw Exception("Empty response from primary model")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed, trying fallback: ${e.message}")

                // Try fallback model
                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text ?: throw Exception("Empty response from fallback model")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Both models failed for summary generation: ${e.message}")

            // Generate a basic summary from the content
            val words = content.split("\\s+".toRegex())
            val summary = when {
                words.size > 100 -> {
                    val firstPart = words.take(50).joinToString(" ")
                    val lastPart = words.takeLast(30).joinToString(" ")
                    "This document discusses: $firstPart... $lastPart"
                }
                words.size > 20 -> {
                    val mainContent = words.take(50).joinToString(" ")
                    "Document summary: $mainContent"
                }
                else -> "This is a short document containing: ${content.take(200)}"
            }

            summary
        }
    }

    private suspend fun extractKeyPoints(content: String): List<String> {
        return try {
            val limitedContent = if (content.length > 6000) {
                content.take(6000) + "..."
            } else content

            val prompt = """
                Extract 5-7 key points or main concepts from this document. 
                Return them as a bullet-pointed list, each point should be concise and informative:
                
                $limitedContent
            """.trimIndent()

            try {
                val response = generativeModel.generateContent(prompt)
                response.text?.split("\n")
                    ?.filter { it.trim().isNotEmpty() && (it.contains("•") || it.contains("-") || it.contains("*")) }
                    ?.map { it.replace(Regex("^[•\\-*]\\s*"), "").trim() }
                    ?.take(7) ?: throw Exception("Could not parse key points")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for key points, trying fallback")

                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text?.split("\n")
                    ?.filter { it.trim().isNotEmpty() && (it.contains("•") || it.contains("-") || it.contains("*")) }
                    ?.map { it.replace(Regex("^[•\\-*]\\s*"), "").trim() }
                    ?.take(7) ?: throw Exception("Fallback also failed")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "extractKeyPoints failed: ${e.message}")

            // Generate basic key points from content structure
            val sentences = content.split("[.!?]+".toRegex()).filter { it.trim().isNotEmpty() }
            when {
                sentences.size >= 5 -> sentences.take(5).map { it.trim().take(100) }
                sentences.size >= 2 -> sentences.map { it.trim().take(100) }
                else -> listOf("Main content: ${content.take(200)}")
            }
        }
    }

    private suspend fun generateTags(content: String): List<String> {
        return try {
            val limitedContent = if (content.length > 4000) {
                content.take(4000) + "..."
            } else content

            val prompt = """
                Based on this document content, generate 3-5 relevant tags or keywords that describe the main topics.
                Return only the tags separated by commas, no explanation needed:
                
                $limitedContent
            """.trimIndent()

            try {
                val response = generativeModel.generateContent(prompt)
                response.text?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(5) ?: throw Exception("Could not generate tags")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for tags, trying fallback")

                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(5) ?: throw Exception("Fallback failed for tags")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "generateTags failed: ${e.message}")

            // Generate basic tags from content
            val words = content.lowercase().split("\\s+".toRegex())
            val commonWords = setOf("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "a", "an")
            val significantWords = words.filter { it.length > 3 && !commonWords.contains(it) }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first.capitalize() }

            if (significantWords.isNotEmpty()) {
                significantWords + listOf("Document")
            } else {
                listOf("Document", "Notes", "Text")
            }
        }
    }

    private suspend fun generateTitle(content: String, originalFileName: String): String {
        return try {
            val limitedContent = if (content.length > 2000) {
                content.take(2000) + "..."
            } else content

            val prompt = """
                Based on this document content, generate a clear and descriptive title (maximum 60 characters).
                The title should capture the main topic or subject of the document:
                
                $limitedContent
            """.trimIndent()

            try {
                val response = generativeModel.generateContent(prompt)
                val generatedTitle = response.text?.trim()?.take(60)

                if (!generatedTitle.isNullOrBlank() && generatedTitle.length >= 3) {
                    generatedTitle
                } else {
                    throw Exception("Generated title too short or empty")
                }
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for title, trying fallback")

                val fallbackResponse = fallbackModel.generateContent(prompt)
                val fallbackTitle = fallbackResponse.text?.trim()?.take(60)

                if (!fallbackTitle.isNullOrBlank() && fallbackTitle.length >= 3) {
                    fallbackTitle
                } else {
                    throw Exception("Fallback title generation failed")
                }
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "generateTitle failed: ${e.message}")

            // Generate title from content or filename
            val words = content.split("\\s+".toRegex()).take(10)
            when {
                words.size >= 5 -> {
                    val title = words.take(8).joinToString(" ")
                    if (title.length > 60) title.take(60) + "..." else title
                }
                content.isNotEmpty() -> {
                    val title = content.take(60).trim()
                    if (title.contains('\n')) title.substringBefore('\n') else title
                }
                else -> originalFileName.substringBeforeLast(".")
            }
        }
    }

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            fileName.endsWith(".txt", ignoreCase = true) -> "Text"
            fileName.endsWith(".doc", ignoreCase = true) -> "Word"
            fileName.endsWith(".docx", ignoreCase = true) -> "Word"
            fileName.endsWith(".md", ignoreCase = true) -> "Markdown"
            fileName.endsWith(".rtf", ignoreCase = true) -> "RTF"
            else -> "Document"
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")

            // Delete from Firestore only
            firestore.collection("notes").document(noteId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting note: ${e.message}")
            Result.failure(e)
        }
    }
}