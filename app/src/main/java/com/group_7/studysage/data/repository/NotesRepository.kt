package com.group_7.studysage.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

class NotesRepository{

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun uploadNotesAndProcess(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (String) -> Unit
    ): Result<Note> {

        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("The user is not authenticated/Signed-in.")
            onProgress("Uploading file...")

            val input: InputStream =  context.contentResolver.openInputStream(uri)
                ?: throw Exception("Could not read file")

            // note. need to add pdf parsing later
            val fileBytes = input.readBytes()
            val fileContent = when {
                fileName.endsWith(".txt", ignoreCase = true) -> String(fileBytes)
                fileName.endsWith(".pdf", ignoreCase = true) -> {
                    onProgress("KSS-> PDF parsing not implemented yeT.")
                    String(fileBytes)
                }
                else -> String(fileBytes)
            }

            onProgress("Uploading file to storage...")

            // Upload file to Firebase Storage
            val fileRef = storage.reference
                .child("notes")
                .child(userId)
                .child("${System.currentTimeMillis()}_$fileName")

            val uploadTask = fileRef.putBytes(fileBytes).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()

            onProgress("Generating AI summary...")

            val summary = generateAISummary(fileContent)
            val keyPoints = extractKeyPoints(fileContent)
            val tags = generateTags(fileContent)

            onProgress("Saving to database...")

            // Create note object
            val noteId = firestore.collection("notes").document().id
            val note = Note(
                id = noteId,
                title = generateTitle(fileContent, fileName),
                originalFileName = fileName,
                content = fileContent,
                summary = summary,
                keyPoints = keyPoints,
                tags = tags,
                userId = userId,
                fileUrl = downloadUrl,
                fileType = getFileType(fileName)
            )

            // Save to Firestore
            firestore.collection("notes")
                .document(noteId)
                .set(note)
                .await()

            onProgress("Note processed successfully!")
            Result.success(note)

        }
        catch(exception: Exception){
            Log.d("ERROR NotesRepository", "uploadNotesAndProcess: ${exception.message}")
            Result.failure(exception)

        }
    }

    private suspend fun generateAISummary(content: String): String {
        return try {
            val prompt = """
                Please provide a concise summary of the following text in 2-3 paragraphs. 
                Focus on the main concepts, key information, and important details:
                
                $content
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            response.text ?: "Summary could not be generated."
        } catch (e: Exception) {
            Log.d("ERROR NotesRepository", "generateAISummary: ${e.message}")
            "Error generating summary: ${e.message}"
        }
    }

    private suspend fun extractKeyPoints(content: String): List<String> {
        return "hello hello hello".split("\n")
    }

    private suspend fun generateTags(content: String): List<String> {
        return listOf("Tag1", "Tag2", "Tag3")
    }

    private suspend fun generateTitle(content: String, originalFileName: String): String {
        return "sfu"
    }

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            fileName.endsWith(".txt", ignoreCase = true) -> "Text"
            fileName.endsWith(".doc", ignoreCase = true) -> "Word"
            fileName.endsWith(".docx", ignoreCase = true) -> "Word"
            else -> "Document"
        }
    }
}