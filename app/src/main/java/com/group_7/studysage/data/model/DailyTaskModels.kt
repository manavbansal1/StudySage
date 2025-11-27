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
    val taskType: String = "", // e.g., "quiz", "study", "flashcards", "reading"
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a set of daily tasks for a specific user and date
 */
data class DailyTaskSet(
    val userId: String = "",
    val date: String = "", // Format: "YYYY-MM-DD"
    val tasks: List<DailyTaskItem> = emptyList(),
    val totalXP: Int = 0,
    val completedCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

