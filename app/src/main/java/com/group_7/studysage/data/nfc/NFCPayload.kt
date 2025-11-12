// NFCPayload.kt
package com.group_7.studysage.data.nfc

import kotlinx.serialization.Serializable

@Serializable
data class NFCPayload(
    val noteTitle: String,
    val fileUrl: String,
    val originalFileName: String,
    val fileType: String,
    val courseId: String = "", // Will be set by receiver
    val timestamp: Long = System.currentTimeMillis()
)