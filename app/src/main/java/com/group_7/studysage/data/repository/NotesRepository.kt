package com.group_7.studysage.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.group_7.studysage.BuildConfig
import com.group_7.studysage.utils.CloudinaryUploader
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
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
    val fileType: String = "",
    val courseId: String = "" // New field for course association
)

class NotesRepository(
    private val cloudinaryUploader: CloudinaryUploader = CloudinaryUploader,
    private val authRepository: AuthRepository = AuthRepository()
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Primary model for content generation
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 32
            topP = 1f
            maxOutputTokens = 4096
        },
    )

    // Fallback model
    private val fallbackModel = GenerativeModel(
        modelName = "gemini-1.0-pro",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 32
            topP = 1f
            maxOutputTokens = 2048
        },
    )

    suspend fun uploadNotesAndProcess(
        context: Context,
        uri: Uri,
        fileName: String,
        courseId: String? = null, // Optional course association
        onProgress: (String) -> Unit
    ): Result<Note> {

        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")
            Log.d("NotesRepository", "Starting processing for user: $userId, file: $fileName")
            onProgress("Reading file...")

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Could not read file")

            // Read file content
            val fileBytes = inputStream.readBytes()
            Log.d("NotesRepository", "File size: ${fileBytes.size} bytes")

            val fileContent = when {
                fileName.endsWith(".txt", ignoreCase = true) -> {
                    onProgress("Processing text file...")
                    processTextFile(fileBytes, onProgress)
                }
                fileName.endsWith(".pdf", ignoreCase = true) -> {
                    onProgress("Processing PDF...")
                    processPdfFile(context, fileBytes, fileName, onProgress)
                }
                fileName.endsWith(".doc", ignoreCase = true) ||
                        fileName.endsWith(".docx", ignoreCase = true) -> {
                    onProgress("Processing Word document...")
                    processDocumentFile(fileBytes, fileName, onProgress)
                }
                // image processing = no
                else -> {
                    onProgress("Processing document...")
                    processGenericFile(fileBytes, fileName, onProgress)
                }
            }

            onProgress("Uploading file to Cloudinary...")
            val cloudinaryFileUrl = cloudinaryUploader.uploadFile(
                context = context,
                fileUri = uri,
                fileType = getFileType(fileName),
                folder = "studysage/notes/${userId}", // Organize by user ID
                resourceType = if (getFileType(fileName).contains("Image")) "image" else "raw"
            ) ?: throw Exception("Failed to upload file to Cloudinary.")

            onProgress("Generating AI summary...")

            val summary = generateAISummary(fileContent as String)
            val keyPoints = extractKeyPoints(fileContent)
            val tags = generateTags(fileContent)
            val title = generateTitle(fileContent, fileName)

            onProgress("Saving to database...")

            // Create note object
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
                fileUrl = cloudinaryFileUrl,
                fileType = getFileType(fileName),
                courseId = courseId ?: "" // Associate with course if provided
            )

            // Save to Firestore
            firestore.collection("notes")
                .document(noteId)
                .set(note)
                .await()

            // Add note to user's library
            authRepository.addNoteToUserLibrary(
                noteId = note.id,
                fileName = note.originalFileName,
                fileUrl = note.fileUrl,
                subject = note.tags.firstOrNull() ?: "General",
                courseId = note.courseId
            )

            Log.d("NotesRepository", "Note saved successfully with ID: $noteId")
            onProgress("Note processed successfully!")
            Result.success(note)

        } catch (exception: Exception) {
            Log.e("NotesRepository", "uploadNotesAndProcess error: ${exception.message}", exception)

            val errorMessage = when {
                exception.message?.contains("Permission denied") == true ->
                    "Permission denied. Please check your authentication."
                exception.message?.contains("Network") == true ->
                    "Network error. Please check your internet connection."
                exception.message?.contains("Firestore") == true ->
                    "Database error. Please try again."
                exception.message?.contains("API") == true ->
                    "AI processing error. Please try again."
                else -> "Error processing file: ${exception.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    // Add method to get notes for a specific course
    suspend fun getNotesForCourse(courseId: String): List<Note> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
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
            Log.e("NotesRepository", "Error fetching notes for course: ${e.message}")
            emptyList()
        }
    }

    // Update existing getUserNotes to optionally filter by course
    suspend fun getUserNotes(courseId: String? = null): List<Note> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            var query = firestore.collection("notes")
                .whereEqualTo("userId", userId)

            // Filter by course if provided
            courseId?.let {
                query = query.whereEqualTo("courseId", it)
            }

            val querySnapshot = query
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

    // Rest of the methods remain the same...
    private suspend fun processTextFile(fileBytes: ByteArray, onProgress: (String) -> Unit): String {
        onProgress("Extracting text content...")
        return String(fileBytes, Charsets.UTF_8)
    }

    private suspend fun processPdfFile(
        context: Context,
        fileBytes: ByteArray,
        fileName: String,
        onProgress: (String) -> Unit
    ): String {
        return try {
            onProgress("Preparing PDF parser...")
            PDFBoxResourceLoader.init(context.applicationContext)

            onProgress("Extracting text from PDF...")
            val rawText = withContext(Dispatchers.IO) {
                ByteArrayInputStream(fileBytes).use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper().apply {
                            setSortByPosition(true)
                            setLineSeparator("\n")
                            setParagraphStart("\n")
                            setParagraphEnd("\n")
                        }
                        stripper.getText(document)
                    }
                }
            }

            val cleanedText = rawText
                .replace("\r\n", "\n")
                .replace("\u0000", "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            if (cleanedText.isBlank()) {
                Log.w("NotesRepository", "No readable text extracted from PDF: $fileName")
                throw Exception("No readable text could be extracted. The PDF may be scanned or image-based.")
            }

            val limitedText = if (cleanedText.length > 200_000) {
                Log.w("NotesRepository", "PDF text too long, truncating for processing.")
                cleanedText.take(200_000)
            } else {
                cleanedText
            }

            onProgress("PDF text extraction complete.")
            limitedText
        } catch (e: Exception) {
            Log.e("NotesRepository", "PDF processing failed: ${e.message}", e)
            throw Exception("Failed to extract PDF text: ${e.message ?: "Please try a different file."}")
        }
    }

    private suspend fun processDocumentFile(fileBytes: ByteArray, fileName: String, onProgress: (String) -> Unit): String {
        return try {
            onProgress("Processing document...")

            val extractedText = String(fileBytes, Charsets.UTF_8)

            val readableText = extractedText.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?;:" }

            if (readableText.length > extractedText.length * 0.7) {
                readableText
            }
            else {
                throw Exception("Word document format not supported for direct text extraction")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Document processing failed: ${e.message}")
            throw Exception("Word documents error.")
        }
    }

    private suspend fun processGenericFile(fileBytes: ByteArray, fileName: String, onProgress: (String) -> Unit): String {
        return try {
            onProgress("Processing file...")

            val content = String(fileBytes, Charsets.UTF_8)

            if (content.trim().isNotEmpty() && content.length > 10) {
                content
            } else {
                throw Exception("File format not supported or contains no readable text")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Generic file processing failed: ${e.message}")
            throw Exception("This file format is not supported. Please use text files (.txt), markdown (.md), or images with text.")
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

    suspend fun generateAISummary(content: String): String {
        return try {
            val limitedContent = if (content.length > 8000) {
                content.take(8000) + "..."
            } else content

            val prompt = """
                Please provide a concise summary of the following document content in 2-3 paragraphs. 
                Focus on the main concepts, key information, and important details that would be helpful for studying:
                
                $limitedContent
            """.trimIndent()

            Log.d("NotesRepository", "Generating summary with primary model...")

            try {
                val response = generativeModel.generateContent(prompt)
                response.text ?: throw Exception("Empty response from primary model")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for summary, trying fallback: ${e.message}")
                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text ?: throw Exception("Empty response from fallback model")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Summary generation failed: ${e.message}")
            generateBasicSummary(content)
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
                    ?.filter { it.trim().isNotEmpty() && (it.contains("•") || it.contains("-") || it.contains("*") || it.matches(Regex("^\\d+\\..*"))) }
                    ?.map { it.replace(Regex("^[•\\-*\\d.\\s]*"), "").trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(7) ?: throw Exception("Could not parse key points")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for key points, trying fallback")
                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text?.split("\n")
                    ?.filter { it.trim().isNotEmpty() && (it.contains("•") || it.contains("-") || it.contains("*") || it.matches(Regex("^\\d+\\..*"))) }
                    ?.map { it.replace(Regex("^[•\\-*\\d.\\s]*"), "").trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(7) ?: throw Exception("Fallback also failed")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Key points extraction failed: ${e.message}")
            generateBasicKeyPoints(content)
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
                    ?.filter { it.isNotEmpty() && it.length > 2 }
                    ?.take(5) ?: throw Exception("Could not generate tags")
            } catch (e: Exception) {
                Log.w("NotesRepository", "Primary model failed for tags, trying fallback")
                val fallbackResponse = fallbackModel.generateContent(prompt)
                fallbackResponse.text?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() && it.length > 2 }
                    ?.take(5) ?: throw Exception("Fallback failed for tags")
            }

        } catch (e: Exception) {
            Log.e("NotesRepository", "Tag generation failed: ${e.message}")
            generateBasicTags(content)
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
            Log.e("NotesRepository", "Title generation failed: ${e.message}")
            generateBasicTitle(content, originalFileName)
        }
    }

    // Fallback methods for when AI fails
    private fun generateBasicSummary(content: String): String {
        val words = content.split("\\s+".toRegex())
        return when {
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
    }

    private fun generateBasicKeyPoints(content: String): List<String> {
        val sentences = content.split("[.!?]+".toRegex()).filter { it.trim().isNotEmpty() }
        return when {
            sentences.size >= 5 -> sentences.take(5).map { it.trim().take(100) }
            sentences.size >= 2 -> sentences.map { it.trim().take(100) }
            else -> listOf("Main content: ${content.take(200)}")
        }
    }

    private fun generateBasicTags(content: String): List<String> {
        val words = content.lowercase().split("\\s+".toRegex())
        val commonWords = setOf("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "a", "an")
        val significantWords = words.filter { it.length > 3 && !commonWords.contains(it) }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first.replaceFirstChar { char -> char.uppercaseChar() } }

        return if (significantWords.isNotEmpty()) {
            significantWords + listOf("Document")
        } else {
            listOf("Document", "Notes", "Text")
        }
    }

    private fun generateBasicTitle(content: String, originalFileName: String): String {
        val words = content.split("\\s+".toRegex()).take(10)
        return when {
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

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            fileName.endsWith(".txt", ignoreCase = true) -> "Text"
            fileName.endsWith(".doc", ignoreCase = true) -> "Word"
            fileName.endsWith(".docx", ignoreCase = true) -> "Word"
            fileName.endsWith(".md", ignoreCase = true) -> "Markdown"
            fileName.endsWith(".rtf", ignoreCase = true) -> "RTF"
            fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "JPEG Image"
            fileName.endsWith(".png", ignoreCase = true) -> "PNG Image"
            fileName.endsWith(".gif", ignoreCase = true) -> "GIF Image"
            else -> "Document"
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User is not authenticated.")
            firestore.collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting note: ${e.message}")
            Result.failure(e)
        }
    }
}