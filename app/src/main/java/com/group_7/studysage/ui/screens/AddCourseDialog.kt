package com.group_7.studysage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    isLoading: Boolean,
    semester: String,
    year: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, code: String, instructor: String, description: String, credits: Int, color: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(courseColors.first()) }

    // Error states for validation
    var titleError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    var instructorError by remember { mutableStateOf(false) }
    var creditsError by remember { mutableStateOf(false) }

    // Validation function
    fun validateFields(): Boolean {
        titleError = title.isBlank()
        codeError = code.isBlank()
        instructorError = instructor.isBlank()
        creditsError = credits.isBlank() || credits.toIntOrNull() == null || credits.toInt() < 0

        return !titleError && !codeError && !instructorError && !creditsError
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add Course",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Course Title (Required)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false // Clear error when user types
                    },
                    label = { Text("Course Title *") },
                    placeholder = { Text("e.g., Introduction to Computer Science") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Course title is required", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Course Code (Required)
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        codeError = false // Clear error when user types
                    },
                    label = { Text("Course Code *") },
                    placeholder = { Text("e.g., CS101") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = codeError,
                    supportingText = if (codeError) {
                        { Text("Course code is required", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instructor (Required)
                OutlinedTextField(
                    value = instructor,
                    onValueChange = {
                        instructor = it
                        instructorError = false // Clear error when user types
                    },
                    label = { Text("Instructor *") },
                    placeholder = { Text("e.g., Dr. Smith") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = instructorError,
                    supportingText = if (instructorError) {
                        { Text("Instructor name is required", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Credits (Required)
                OutlinedTextField(
                    value = credits,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.toIntOrNull() != null && newValue.toInt() >= 0)) {
                            credits = newValue
                            creditsError = false // Clear error when user types valid input
                        }
                    },
                    label = { Text("Credits *") },
                    placeholder = { Text("e.g., 3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = creditsError,
                    supportingText = if (creditsError) {
                        { Text("Valid credit amount is required", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description (Optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Brief description of the course") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Semester and Year (display only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { },
                        label = { Text("Semester") },
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { },
                        label = { Text("Year") },
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Color Selection
                Text(
                    text = "Choose Course Color *",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(courseColors) { color ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }

                // Required fields note
                Text(
                    text = "* Required fields",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (validateFields()) {
                                val creditsInt = credits.toIntOrNull() ?: 0
                                onConfirm(title.trim(), code.trim(), instructor.trim(), description.trim(), creditsInt, selectedColor)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Add Course")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(color)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Predefined course colors
val courseColors = listOf(
    "#6200EE", // Purple
    "#F44336", // Red
    "#4CAF50", // Green
    "#2196F3", // Blue
    "#FF9800", // Orange
    "#9C27B0", // Purple Variant
    "#00BCD4", // Cyan
    "#8BC34A", // Light Green
    "#FFC107", // Amber
    "#E91E63", // Pink
    "#607D8B", // Blue Grey
    "#795548"  // Brown
)