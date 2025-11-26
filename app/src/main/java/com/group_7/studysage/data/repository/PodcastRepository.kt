package com.group_7.studysage.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.group_7.studysage.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PodcastRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "PodcastRepository"
        private const val MAX_CONTENT_LENGTH = 15000
        private const val GOOGLE_TTS_CHAR_LIMIT = 5000 // Google Cloud TTS limit per request
    }

    // Gemini model for podcast script generation
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f // Balanced creativity for podcast scripts
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192 // Increased for longer podcasts (15 min = ~2250 words = ~3000 tokens)
        },
    )

    /**
     * Generate a podcast script from note content using Gemini AI
     * Auto-calculates appropriate length based on content size
     */
    suspend fun generatePodcastScript(
        content: String,
        noteTitle: String = ""
    ): String {
        return try {
            // Auto-calculate duration based on content length (keep it simple and short)
            // Aim for 150 words per minute of speech
            // Cap at 700 words max to stay well under 5000 char limit and avoid chunking issues
            val maxWords = 700
            val estimatedMinutes = (maxWords / 150).coerceAtMost(5)

            val titlePart = if (noteTitle.isNotBlank()) {
                "Title: $noteTitle\n\n"
            } else ""

            val prompt = """
                Create a friendly, conversational podcast about the following content. Imagine you're explaining this to a friend - casual, engaging, and easy to understand.

                ${titlePart}CRITICAL REQUIREMENTS - READ CAREFULLY:
                - Write ONLY words that should be spoken out loud
                - Do NOT use ANY special characters: no asterisks (*), no brackets [], no parentheses for actions
                - Do NOT include: "*pause*", "[music]", "(laughs)", "*intro music*" or ANY similar formatting
                - Do NOT include speaker labels like "Host:", "Narrator:", etc.
                - Do NOT use markdown formatting (no **, no ##, no _)
                - Write as if you're having a friendly conversation - just plain sentences
                - Use periods, commas, and question marks ONLY

                TONE AND STYLE:
                - Be friendly, warm, and casual like talking to a friend
                - Use simple, everyday language - avoid being overly formal or academic
                - Use "you" and "we" to make it personal and engaging
                - Include conversational phrases like "you know", "let me tell you", "here's the thing", "pretty cool right"
                - Ask rhetorical questions to keep it engaging
                - Use analogies and relatable examples when explaining complex ideas
                - Sound enthusiastic and genuinely interested in the topic
                - Break down complex ideas into bite-sized, easy-to-digest pieces

                GOOD EXAMPLE: "Hey there. So today we're going to talk about something really interesting. You know how sometimes you wonder about this topic? Well, let me tell you, it's actually pretty fascinating when you break it down."
                BAD EXAMPLE: "*Intro music fades* Hello! [applause] Welcome to today's episode. Today we will discuss the fundamental principles of..."

                IMPORTANT: Keep it short and sweet - maximum $maxWords words (about $estimatedMinutes minutes).
                Focus on the key points only. Be concise and engaging.

                Content:
                $content
            """.trimIndent()

            Log.d(TAG, "Generating podcast script for content: ${content.length} chars")

            val response = generativeModel.generateContent(prompt)
            val rawScript = response.text ?: throw Exception("Empty response from AI model")

            // Clean up any formatting that slipped through
            val cleanScript = rawScript
                .replace(Regex("\\*[^*]*\\*"), "") // Remove anything between asterisks
                .replace(Regex("\\[[^\\]]*\\]"), "") // Remove anything in brackets
                .replace(Regex("\\([^)]*\\)"), "") // Remove anything in parentheses (stage directions)
                .replace("**", "") // Remove markdown bold
                .replace("__", "") // Remove markdown underline
                .replace("##", "") // Remove markdown headers
                .replace("***", "") // Remove emphasis
                .replace(Regex("\\s+"), " ") // Normalize whitespace
                .trim()

            Log.d(TAG, "Script generated and cleaned: ${cleanScript.length} characters")
            cleanScript

        } catch (e: Exception) {
            Log.e(TAG, "Podcast script generation failed: ${e.message}", e)
            throw Exception("Failed to generate podcast script: ${e.message}")
        }
    }

    /**
     * Split text into chunks that fit Google Cloud TTS limit
     */
    private fun splitTextIntoChunks(text: String, maxChars: Int = GOOGLE_TTS_CHAR_LIMIT): List<String> {
        if (text.length <= maxChars) return listOf(text)

        val chunks = mutableListOf<String>()
        val sentences = text.split(". ", "! ", "? ")
        var currentChunk = ""

        for (sentence in sentences) {
            if ((currentChunk + sentence).length <= maxChars) {
                currentChunk += "$sentence. "
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.trim())
                }
                currentChunk = "$sentence. "
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.trim())
        }

        Log.d(TAG, "Split text into ${chunks.size} chunks")
        return chunks
    }

    /**
     * Convert single text chunk to speech using Google Cloud TTS
     */
    private suspend fun convertChunkToSpeech(
        text: String,
        speakingRate: Double = 1.0
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GOOGLE_CLOUD_TTS_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_GOOGLE_CLOUD_TTS_API_KEY_HERE") {
                throw Exception("Google Cloud TTS API key not configured")
            }

            val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"

            val requestBody = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", "en-US")
                    put("name", "en-US-Neural2-J")
                    put("ssmlGender", "MALE")
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "LINEAR16") // WAV format for better quality
                    put("speakingRate", speakingRate)
                    put("pitch", 0.0)
                    put("sampleRateHertz", 24000) // High quality audio
                })
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "TTS API error: ${response.code} - $errorBody")
                throw Exception("TTS API Error (${response.code})")
            }

            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody ?: "")
            val audioContentBase64 = jsonResponse.getString("audioContent")

            Base64.decode(audioContentBase64, Base64.DEFAULT)
        }
    }

    /**
     * Concatenate multiple WAV files into one
     * WAV files can be concatenated by combining their data chunks
     */
    private fun concatenateWavFiles(inputFiles: List<File>, outputFile: File) {
        if (inputFiles.isEmpty()) return

        // Read first file to get header
        val firstFile = inputFiles[0]
        val firstBytes = firstFile.readBytes()

        // WAV header is 44 bytes
        val header = firstBytes.take(44).toByteArray().toMutableList()

        // Calculate total audio data size
        var totalDataSize = 0
        inputFiles.forEach { file ->
            val bytes = file.readBytes()
            if (bytes.size > 44) {
                totalDataSize += bytes.size - 44
            }
        }

        // Update WAV header with correct file size
        // Bytes 4-7: file size minus 8 bytes
        val fileSize = totalDataSize + 36
        header[4] = (fileSize and 0xFF).toByte()
        header[5] = ((fileSize shr 8) and 0xFF).toByte()
        header[6] = ((fileSize shr 16) and 0xFF).toByte()
        header[7] = ((fileSize shr 24) and 0xFF).toByte()

        // Bytes 40-43: data chunk size
        header[40] = (totalDataSize and 0xFF).toByte()
        header[41] = ((totalDataSize shr 8) and 0xFF).toByte()
        header[42] = ((totalDataSize shr 16) and 0xFF).toByte()
        header[43] = ((totalDataSize shr 24) and 0xFF).toByte()

        FileOutputStream(outputFile).use { output ->
            // Write corrected header
            output.write(header.toByteArray())

            // Write audio data from all files (skip 44-byte header for each)
            inputFiles.forEach { file ->
                val bytes = file.readBytes()
                if (bytes.size > 44) {
                    output.write(bytes.copyOfRange(44, bytes.size))
                }
            }
        }

        Log.d(TAG, "Concatenated ${inputFiles.size} WAV files, total data: $totalDataSize bytes")
    }

    /**
     * Convert text to speech using Google Cloud TTS, handling long text by splitting
     */
    suspend fun convertTextToSpeech(
        text: String,
        noteId: String,
        speakingRate: Double = 1.0
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Converting ${text.length} characters to speech")

                val chunks = splitTextIntoChunks(text)
                Log.d(TAG, "Split into ${chunks.size} chunks for TTS")

                val tempFiles = mutableListOf<File>()

                // Convert each chunk to audio and save as separate files
                chunks.forEachIndexed { index, chunk ->
                    Log.d(TAG, "Converting chunk ${index + 1}/${chunks.size} (${chunk.length} chars)")
                    val audioBytes = convertChunkToSpeech(chunk, speakingRate)

                    val tempFile = File(context.cacheDir, "podcast_${noteId}_part${index}.wav")
                    tempFile.writeBytes(audioBytes)
                    tempFiles.add(tempFile)
                }

                // Combine all WAV files into one and save to app files directory (persistent storage)
                val podcastsDir = File(context.filesDir, "podcasts")
                if (!podcastsDir.exists()) {
                    podcastsDir.mkdirs()
                }
                val finalAudioFile = File(podcastsDir, "podcast_${noteId}.wav")
                if (finalAudioFile.exists()) {
                    finalAudioFile.delete()
                }

                if (tempFiles.size == 1) {
                    // If only one chunk, just copy it
                    tempFiles[0].copyTo(finalAudioFile, overwrite = true)
                } else {
                    // Concatenate WAV files properly
                    concatenateWavFiles(tempFiles, finalAudioFile)
                }

                // Clean up temp files
                tempFiles.forEach { it.delete() }

                Log.d(TAG, "Podcast audio saved to: ${finalAudioFile.absolutePath} (${finalAudioFile.length()} bytes)")
                Result.success(finalAudioFile.absolutePath)

            } catch (e: Exception) {
                Log.e(TAG, "Text-to-speech conversion failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Generate complete podcast from note content
     * Combines script generation and text-to-speech conversion
     * Auto-calculates duration based on content
     */
    suspend fun generatePodcast(
        noteId: String,
        content: String,
        noteTitle: String = "",
        speakingRate: Double = 1.0,
        onProgress: (String) -> Unit,
        onScriptGenerated: (String) -> Unit = {}
    ): Result<String> {
        return try {
            Log.d(TAG, "Starting podcast generation for note: $noteId")
            onProgress("Generating podcast script...")

            val script = generatePodcastScript(content, noteTitle)
            Log.d(TAG, "Podcast script generated: ${script.length} characters")

            // Callback with the generated script
            onScriptGenerated(script)

            onProgress("Converting to audio...")
            Log.d(TAG, "Starting text-to-speech conversion...")

            val audioResult = convertTextToSpeech(script, noteId, speakingRate)

            audioResult.onSuccess {
                Log.d(TAG, "Podcast generation completed successfully")
                onProgress("Podcast generated successfully!")
            }.onFailure { error ->
                Log.e(TAG, "Podcast generation failed: ${error.message}", error)
                onProgress("Failed to generate podcast")
            }

            audioResult
        } catch (e: Exception) {
            Log.e(TAG, "Podcast generation exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete podcast audio file from persistent storage
     */
    fun deletePodcast(noteId: String): Boolean {
        return try {
            val podcastsDir = File(context.filesDir, "podcasts")
            val audioFile = File(podcastsDir, "podcast_${noteId}.wav")
            if (audioFile.exists()) {
                val deleted = audioFile.delete()
                Log.d(TAG, "Podcast deleted: $deleted for note $noteId")
                deleted
            } else {
                Log.d(TAG, "No podcast file found for note $noteId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting podcast: ${e.message}", e)
            false
        }
    }

    /**
     * Check if a podcast already exists for a note
     */
    fun podcastExists(noteId: String): Boolean {
        val podcastsDir = File(context.filesDir, "podcasts")
        val audioFile = File(podcastsDir, "podcast_${noteId}.wav")
        return audioFile.exists()
    }

    /**
     * Get podcast file path if it exists
     */
    fun getPodcastPath(noteId: String): String? {
        val podcastsDir = File(context.filesDir, "podcasts")
        val audioFile = File(podcastsDir, "podcast_${noteId}.wav")
        return if (audioFile.exists()) audioFile.absolutePath else null
    }
}
