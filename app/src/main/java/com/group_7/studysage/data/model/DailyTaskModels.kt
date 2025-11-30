package com.group_7.studysage.data.model

import com.google.firebase.Timestamp

/**
 * Represents a single daily task item
 */
data class DailyTaskItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val taskType: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
