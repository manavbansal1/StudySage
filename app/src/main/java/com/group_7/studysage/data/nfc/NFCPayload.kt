// NFCPayload.kt
package com.group_7.studysage.data.nfc

import kotlinx.serialization.Serializable

@Serializable
data class NFCPayload(
    val noteId: String,
    val noteTitle: String,
    val fileUrl: String,
    val originalFileName: String,
    val fileType: String,
    val content: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val courseId: String = "", // Will be set by receiver
    val timestamp: Long = System.currentTimeMillis()
)