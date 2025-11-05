package com.group_7.studysage.data.repository

import java.util.List

/**
 * Data class representing a Course entity.
 * It includes details such as title, code, semester, year, instructor, description, color, credits,
 * timestamps for creation and updates, associated user ID, and archival status.
 * It also includes an enum for semesters and a composite data class for courses with their associated notes.
 *
 */
data class Course(
    val id: String = "",
    val title: String = "",
    val code: String = "",
    val semester: String = "",
    val year: String = "",
    val instructor: String = "",
    val description: String = "",
    val color: String = "#6200EE", // Default purple color
    val credits: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val isArchived: Boolean = false
)

enum class Semester(val displayName: String) {
    SPRING("Spring"),
    SUMMER("Summer"),
    FALL("Fall"),
    WINTER("Winter")
}

data class CourseWithNotes(
    val course: Course,
    val notes: List<Note>
)