package com.group_7.studysage.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.group_7.studysage.BuildConfig
import com.group_7.studysage.data.api.CloudRunApiService
import com.group_7.studysage.ui.screens.Flashcards.Flashcard
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

class FlashcardRepository {

    companion object {
        private const val TAG = "FlashcardRepository"
        private const val COLLECTION_FLASHCARDS = "flashcards"
        private const val COLLECTION_NOTES = "notes"
    }

    private val firestore = FirebaseFirestore.getInstance()

    // Use Cloud Run API service for Gemini AI
    private val cloudRunApi = CloudRunApiService(BuildConfig.CLOUD_RUN_URL)

    /**
     * Generate flashcards using AI from note content.
     * Works universally for any topic.
     */
    suspend fun generateFlashcardsWithAI(
        noteContent: String,
        numberOfCards: Int,
        difficulty: String,
        onProgress: (Int) -> Unit
    ): Result<List<Flashcard>> {
        return try {
            onProgress(20)

            val contentToUse = if (noteContent.length > 6000)
                noteContent.take(6000) + "..."
            else noteContent

            val prompt = """
                You are an AI tutor generating educational flashcards.

                Your goal is to read the content below and create high-quality, meaningful flashcards that help learners recall key facts, understand concepts, and think critically.

                ### Instructions:
                1. Identify the most important ideas, facts, events, or definitions in the text.
                2. For each idea, generate one question and one concise answer.
                3. The question must:
                   - Be natural, grammatically correct, and end with a question mark.
                   - Avoid repetitive phrasing (don’t start every question with "What is").
                   - Encourage understanding (definitions, causes, effects, comparisons, reasoning).
                   - Make sense even without the original text.
                4. The answer must:
                   - Be accurate, self-contained, and under 50 words.
                   - Avoid copying full sentences unless absolutely necessary.
                   - Provide clear, complete information.

                ### Flashcard coverage:
                - Prioritize key entities, people, dates, and core concepts.
                - Include both factual recall and conceptual understanding.
                - Adjust difficulty as follows:
                  • easy → direct recall (facts, definitions)
                  • medium → understanding and short reasoning
                  • hard → cause-effect, application, analysis

                ### Output:
                **STRICTLY RETURN ONLY a JSON array** with exactly $numberOfCards flashcards in this format:
                [
                  {"question": "A concise question derived from the text, ending with a question mark?", "answer": "The accurate, self-contained answer, under 50 words.", "difficulty": "$difficulty"},
                  ...
                ]

                DO NOT include explanations, markdown, code blocks (like ```json), or extra commentary.

                ### Text to analyze:
                $contentToUse
            """.trimIndent()

            onProgress(40)
            Log.d(TAG, "Sending universal flashcard request to Gemini via Cloud Run...")

            val aiContent = cloudRunApi.generateContent(prompt)
            onProgress(75)

            Log.d(TAG, "Received AI response for flashcards")

            onProgress(90)

            val flashcardsData = parseFlashcardsFromAI(aiContent, numberOfCards).map {
                it.copy(question = refineQuestion(it.question))
            }

            if (flashcardsData.isNotEmpty()) {
                onProgress(100)
                Result.success(flashcardsData)
            } else {
                Result.failure(Exception("No flashcards generated"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI flashcard generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generate fallback flashcards if AI fails or note lacks internet.
     */
    suspend fun generateFlashcardsFromNote(
        noteId: String,
        numberOfCards: Int,
        difficulty: String,
        onProgress: (Int) -> Unit
    ): Result<List<Flashcard>> {
        return try {
            onProgress(20)
            val noteDoc = firestore.collection(COLLECTION_NOTES)
                .document(noteId)
                .get()
                .await()

            if (!noteDoc.exists()) {
                return Result.failure(Exception("Note not found"))
            }

            val summary = noteDoc.getString("summary") ?: ""
            @Suppress("UNCHECKED_CAST")
            val keyPoints = noteDoc.get("keyPoints") as? List<String> ?: emptyList()
            val content = noteDoc.getString("content") ?: ""

            val flashcards = mutableListOf<Flashcard>()
            val questionPatterns = listOf(
                "What is the definition of",
                "Explain the importance of",
                "How does",
                "Describe the concept of",
                "What are the effects of",
                "Define",
                "What is the main idea of"
            )

            // --- From key points (best fallback data) ---
            keyPoints.take(numberOfCards).forEachIndexed { i, point ->
                val pattern = questionPatterns[i % questionPatterns.size]
                val cleanPoint = point.trim().replaceFirstChar { it.uppercase() }

                // Use the key point itself as the answer, and create a question based on its start.
                val questionFragment = cleanPoint.split(" ").take(4).joinToString(" ")

                var question = "$pattern $questionFragment...?"
                question = refineQuestion(question)

                flashcards.add(
                    Flashcard(
                        id = "${noteId}_keypoint_$i",
                        question = question,
                        answer = cleanPoint,
                        difficulty = difficulty
                    )
                )
            }

            onProgress(70)

            // --- From summary ---
            if (flashcards.size < numberOfCards && summary.isNotBlank()) {
                val summarySentences = summary.split(". ").filter { it.length > 20 }
                summarySentences.take(numberOfCards - flashcards.size).forEachIndexed { i, s ->
                    val pattern = questionPatterns[(flashcards.size + i) % questionPatterns.size]
                    val cleanSentence = s.trim().replaceFirstChar { it.uppercase() }
                    val questionFragment = cleanSentence.split(" ").take(4).joinToString(" ")

                    var question = "$pattern $questionFragment...?"
                    question = refineQuestion(question)

                    flashcards.add(
                        Flashcard(
                            id = "${noteId}_summary_$i",
                            question = question,
                            answer = cleanSentence,
                            difficulty = difficulty
                        )
                    )
                }
            }

            onProgress(85)

            // --- From full content ---
            if (flashcards.size < numberOfCards && content.isNotBlank()) {
                val sentences = content
                    .split(". ", ".\n", "? ", "! ")
                    .filter { it.length in 20..300 }
                    .take(numberOfCards)

                sentences.forEachIndexed { i, s ->
                    val pattern = questionPatterns[(flashcards.size + i) % questionPatterns.size]
                    val cleanSentence = s.trim().replaceFirstChar { it.uppercase() }
                    val questionFragment = cleanSentence.split(" ").take(4).joinToString(" ")

                    // The key fix: generate a generic question based on the first few words of the sentence
                    var question = "$pattern $questionFragment...?"
                    question = refineQuestion(question)

                    flashcards.add(
                        Flashcard(
                            id = "${noteId}_content_$i",
                            question = question,
                            answer = cleanSentence,
                            difficulty = difficulty
                        )
                    )
                }
            }

            onProgress(100)

            if (flashcards.isNotEmpty()) Result.success(flashcards.take(numberOfCards))
            else Result.failure(Exception("No data to generate flashcards"))
        } catch (e: Exception) {
            Log.e(TAG, "Fallback generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Parse Gemini JSON output, making it more robust against malformed responses.
     */
    private fun parseFlashcardsFromAI(aiContent: String, expectedCount: Int): List<Flashcard> {
        return try {
            // Use regex to extract the content strictly between the first '[' and the last ']'
            val jsonMatch = Regex("\\[[\\s\\S]*\\]").find(aiContent)

            val jsonString = if (jsonMatch != null) {
                // Get the string that is just the array
                jsonMatch.value.trim()
            } else {
                Log.e(TAG, "No JSON array found using regex. Raw content received: $aiContent")
                throw Exception("No valid JSON array found in response")
            }

            // Attempt to parse the cleaned JSON string
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Flashcard>()

            // Iterate up to the expected count or the actual length
            for (i in 0 until minOf(jsonArray.length(), expectedCount)) {
                val obj = jsonArray.optJSONObject(i) // Use optJSONObject for safety

                if (obj != null) {
                    val q = obj.optString("question", "").trim()
                    val a = obj.optString("answer", "").trim()
                    val d = obj.optString("difficulty", "medium").lowercase().trim()

                    if (q.isNotBlank() && a.isNotBlank()) {
                        list.add(Flashcard(id = "", question = q, answer = a, difficulty = d))
                    } else {
                        Log.w(TAG, "Skipping malformed flashcard object at index $i: $obj")
                    }
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI flashcards. Error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Basic text cleanup to ensure readability and proper punctuation.
     */
    private fun refineQuestion(question: String): String {
        return question.trim()
            .replace(Regex("\\s+"), " ")
            .replace("..", ".")
            .replace("??", "?")
            .replace(": ?", "?")
            .replace(" :", ":")
            .replace(" ,", ",")
            .trim()
            .replaceFirstChar { it.uppercase() }
            .let {
                // Trim trailing punctuation marks like '.', ':' before adding '?'
                val cleaned = it.trimEnd('.', ':')
                // Ensure it ends with a question mark and clean up double question marks
                if (!cleaned.endsWith("?")) cleaned + "?" else cleaned
            }
            .replace("? ?", "?") // Fix for double '?' after forced append
            .replace("..", ".")
    }



    suspend fun saveFlashcards(noteId: String, flashcards: List<Flashcard>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val existing = firestore.collection(COLLECTION_FLASHCARDS)
                .whereEqualTo("noteId", noteId)
                .get()
                .await()

            existing.documents.forEach { batch.delete(it.reference) }

            flashcards.forEach {
                val ref = firestore.collection(COLLECTION_FLASHCARDS).document()
                batch.set(ref, hashMapOf(
                    "noteId" to noteId,
                    "question" to it.question,
                    "answer" to it.answer,
                    "difficulty" to it.difficulty,
                    "createdAt" to System.currentTimeMillis()
                ))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save flashcards", e)
            Result.failure(e)
        }
    }

    suspend fun loadFlashcardsForNote(noteId: String): Result<List<Flashcard>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_FLASHCARDS)
                .whereEqualTo("noteId", noteId)
                .get()
                .await()

            val flashcards = snapshot.documents.mapNotNull { doc ->
                try {
                    Flashcard(
                        id = doc.id,
                        question = doc.getString("question") ?: "",
                        answer = doc.getString("answer") ?: "",
                        difficulty = doc.getString("difficulty") ?: "medium"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing flashcard document: ${doc.id}", e)
                    null
                }
            }

            Result.success(flashcards)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load flashcards for note: $noteId", e)
            Result.failure(e)
        }
    }
}