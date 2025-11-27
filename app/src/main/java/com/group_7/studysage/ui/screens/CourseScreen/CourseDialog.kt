package com.group_7.studysage.ui.screens.CourseScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group_7.studysage.data.repository.Course
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDialog(
    isLoading: Boolean,
    semester: String,
    year: String,
    existingCourse: Course? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, code: String, instructor: String, description: String, credits: Int, color: String) -> Unit
) {
    var title by remember { mutableStateOf(existingCourse?.title ?: "") }
    var code by remember { mutableStateOf(existingCourse?.code ?: "") }
    var instructor by remember { mutableStateOf(existingCourse?.instructor ?: "") }
    var description by remember { mutableStateOf(existingCourse?.description ?: "") }
    var credits by remember { mutableStateOf(existingCourse?.credits?.toString() ?: "") }
    var selectedColor by remember { mutableStateOf(existingCourse?.color ?: courseColors.first()) }

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

    // Define glassy text field colors
    val glassTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        errorTextColor = MaterialTheme.colorScheme.onErrorContainer,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorSupportingTextColor = MaterialTheme.colorScheme.error
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            shadowElevation = 8.dp,
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
                    text = if (existingCourse != null) "Edit Course" else "Add Course",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Course Title (Required)
                TextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Course Title *") },
                    placeholder = { Text("e.g., Introduction to Computer Science") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Course title is required") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Course Code (Required)
                TextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        codeError = false
                    },
                    label = { Text("Course Code *") },
                    placeholder = { Text("e.g., CS101") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = codeError,
                    supportingText = if (codeError) {
                        { Text("Course code is required") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instructor (Required)
                TextField(
                    value = instructor,
                    onValueChange = {
                        instructor = it
                        instructorError = false
                    },
                    label = { Text("Instructor *") },
                    placeholder = { Text("e.g., Dr. Smith") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = instructorError,
                    supportingText = if (instructorError) {
                        { Text("Instructor name is required") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Credits (Required)
                TextField(
                    value = credits,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.toIntOrNull() != null && newValue.toInt() >= 0)) {
                            credits = newValue
                            creditsError = false
                        }
                    },
                    label = { Text("Credits *") },
                    placeholder = { Text("e.g., 3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = creditsError,
                    supportingText = if (creditsError) {
                        { Text("Valid credit amount is required") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description (Optional)
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Brief description of the course") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Semester and Year (display only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = semester,
                        onValueChange = { },
                        label = { Text("Semester") },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors
                    )

                    TextField(
                        value = year,
                        onValueChange = { },
                        label = { Text("Year") },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors
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
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !isLoading,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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
                            Text(if (existingCourse != null) "Update" else "Add")
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
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) // Use a visible border
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) // Subtle border
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .border(border, CircleShape) // Apply border
            .padding(4.dp) // Padding inside the border
            .clip(CircleShape)
            .background(Color(color.toColorInt()))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White, // White should be visible on all these strong colors
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
