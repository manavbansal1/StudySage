package com.group_7.studysage.ui.screens.CourseScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.Semester
import com.group_7.studysage.ui.theme.StudySageTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.group_7.studysage.viewmodels.CourseViewModel

/**
 * A consistent "glass" card style for the entire app.
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        // The base "glass" color
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    )
    val border = BorderStroke(
        1.dp,
        // A subtle border to catch the light
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )

    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No shadow
            onClick = onClick,
            content = { content() }
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No shadow
            content = { content() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: CourseViewModel = viewModel(),
    authViewModel: com.group_7.studysage.viewmodels.AuthViewModel,
    navCourseId: String? = null,
    navNoteId: String? = null // NEW optional nav params
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCourseDialog by rememberSaveable { mutableStateOf(false) }

    // If navigation provided a courseId, load it and set pendingNote
    LaunchedEffect(navCourseId, navNoteId) {
        if (!navCourseId.isNullOrBlank()) {
            viewModel.setPendingOpenNote(navNoteId)
            viewModel.loadCourseWithNotes(navCourseId)
        }
    }

    if (uiState.selectedCourse != null) {
        CourseDetailScreen(
            courseWithNotes = uiState.selectedCourse!!,
            onBack = { viewModel.clearSelectedCourse() },
            courseViewModel = viewModel,
            authViewModel = authViewModel
        )
    } else {
        Scaffold(
            containerColor = Color.Transparent, // Let the theme background show
        ) { paddingValues ->

            // Pull-to-refresh wrapper for the grid
            val isRefreshing = uiState.isRefreshing
            val swipeState = rememberSwipeRefreshState(isRefreshing)

            SwipeRefresh(
                state = swipeState,
                onRefresh = { viewModel.refreshCourses() },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = 6.dp
                    )
                }
            ) {
                // The entire screen is a scrolling grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = paddingValues, // Apply insets
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp) // Page horizontal padding
                ) {
                    // 1. HEADER
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        CoursesHeader(
                            onAddClick = { showAddCourseDialog = true },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // 2. FILTER SECTION
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        FilterSection(
                            selectedSemester = uiState.selectedSemester,
                            selectedYear = uiState.selectedYear,
                            availableYears = uiState.availableYears,
                            onSemesterChange = { viewModel.setSemesterFilter(it) },
                            onYearChange = { viewModel.setYearFilter(it) }
                        )
                    }

                    // 3. COURSES HEADER
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Courses",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.courses.size} courses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4. LOADING / EMPTY / CONTENT
                    if (uiState.isLoading) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (uiState.courses.isEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            EmptyCoursesState(
                                semester = uiState.selectedSemester,
                                year = uiState.selectedYear,
                                onAddCourse = { showAddCourseDialog = true }
                            )
                        }
                    } else {
                        // 5. COURSES GRID
                        items(uiState.courses) { course ->
                            CourseGridCard(
                                course = course,
                                onClick = { viewModel.loadCourseWithNotes(course.id) }
                            )
                        }
                    }

                    // 6. SPACER FOR NAV BAR
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AddCourseDialog(
            isLoading = uiState.isCreatingCourse,
            semester = uiState.selectedSemester,
            year = uiState.selectedYear,
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { title, code, instructor, description, credits, color ->
                viewModel.createCourse(title, code, instructor, description, credits, color)
                showAddCourseDialog = false
            }
        )
    }

    // --- NEW: Confirmation Dialog ---
    // Show error message if it exists
    uiState.error?.let { errorMessage ->
        ConfirmationOverlay(
            message = errorMessage,
            isError = true,
            onDismiss = { viewModel.clearMessage() }
        )
    }

    // Show success message if it exists
    uiState.message?.let { successMessage ->
        ConfirmationOverlay(
            message = successMessage,
            isError = false,
            onDismiss = { viewModel.clearMessage() }
        )
    }
}

@Composable
fun CoursesHeader(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Track and organize your classes",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Courses",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Add button
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Course",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    selectedSemester: String,
    selectedYear: String,
    availableYears: List<String>,
    onSemesterChange: (String) -> Unit,
    onYearChange: (String) -> Unit
) {
    // Use the GlassCard style
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter Courses",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Re-styled "glassy" text field
                val glassTextFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Semester Filter
                var semesterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = !semesterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedSemester,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Semester") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors
                    )

                    ExposedDropdownMenu(
                        expanded = semesterExpanded,
                        onDismissRequest = { semesterExpanded = false }
                    ) {
                        Semester.values().forEach { semester ->
                            DropdownMenuItem(
                                text = { Text(semester.displayName) },
                                onClick = {
                                    onSemesterChange(semester.displayName)
                                    semesterExpanded = false
                                },
                                leadingIcon = {
                                    if (selectedSemester == semester.displayName) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Year Filter
                var yearExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedYear,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors
                    )

                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        availableYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    onYearChange(year)
                                    yearExpanded = false
                                },
                                leadingIcon = {
                                    if (selectedYear == year) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseGridCard(
    course: Course,
    onClick: () -> Unit
) {
    // Attempt to parse the user's color, fall back to primary if invalid
    val tintColor = try {
        Color(course.color.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Perfect square cards
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Half - Visual Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up half the height
                    .background(
                        // Consistent, theme-aware background
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // Course icon/image placeholder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        // Use the user's color with alpha
                        color = tintColor.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = "Course Icon",
                                tint = tintColor, // Use the user's color
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Credits badge in top-right corner
                if (course.credits > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${course.credits} CR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Half - Course Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up the other half
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Main course info
                Column {
                    // Course code
                    Text(
                        text = course.code,
                        style = MaterialTheme.typography.labelLarge,
                        color = tintColor, // Use the user's color
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Course title
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.titleSmall.lineHeight
                    )

                    // Instructor (if available)
                    if (course.instructor.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Instructor",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = course.instructor,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action indicator at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Course",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCoursesState(
    semester: String,
    year: String,
    onAddCourse: () -> Unit
) {
    // This state can be simpler and inside a GlassCard for consistency
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Courses Found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No courses for $semester $year. Add your first course to get started!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddCourse,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(48.dp) // Ensure consistent height
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add") // <-- CHANGED
            }
        }
    }
}

// --- NEW COMPOSABLE ---
@Composable
fun ConfirmationOverlay(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        // Re-using the GlassCard from this file
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Icon(
                    imageVector = if (isError) Icons.Default.Warning
                    else Icons.Default.CheckCircle,
                    contentDescription = "Status Icon",
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = if (isError) "An Error Occurred" else "Success",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}