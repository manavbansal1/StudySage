package com.group_7.studysage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.Semester
import com.group_7.studysage.ui.theme.StudySageTheme
import com.group_7.studysage.ui.screens.viewmodels.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: CourseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCourseDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessage()
        }
    }

    if (uiState.selectedCourse != null) {
        CourseDetailScreen(
            courseWithNotes = uiState.selectedCourse!!,
            onBack = { viewModel.clearSelectedCourse() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Courses") },
                    actions = {
                        IconButton(onClick = { showAddCourseDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Course")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Filter Section
                FilterSection(
                    selectedSemester = uiState.selectedSemester,
                    selectedYear = uiState.selectedYear,
                    availableYears = uiState.availableYears,
                    onSemesterChange = { viewModel.setSemesterFilter(it) },
                    onYearChange = { viewModel.setYearFilter(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Courses Header with count
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Loading state
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.courses.isEmpty()) {
                    EmptyCoursesState(
                        semester = uiState.selectedSemester,
                        year = uiState.selectedYear,
                        onAddCourse = { showAddCourseDialog = true }
                    )
                } else {
                    // Courses Grid (2 columns)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.courses) { course ->
                            CourseGridCard(
                                course = course,
                                onClick = { viewModel.loadCourseWithNotes(course.id) }
                            )
                        }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter Courses",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Semester Filter
                var semesterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = !semesterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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
                    OutlinedTextField(
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Perfect square cards
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Half - Image/Visual Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up half the height
                    .background(
                        Color(android.graphics.Color.parseColor(course.color)).copy(alpha = 0.15f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                // Background pattern or gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor(course.color)).copy(alpha = 0.3f),
                                    Color(android.graphics.Color.parseColor(course.color)).copy(alpha = 0.1f)
                                ),
                                radius = 200f
                            )
                        )
                )

                // Course icon/image placeholder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        color = Color(android.graphics.Color.parseColor(course.color)).copy(alpha = 0.2f),
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
                                tint = Color(android.graphics.Color.parseColor(course.color)),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Credits badge in top-right corner
                if (course.credits > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${course.credits} CR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        color = Color(android.graphics.Color.parseColor(course.color)),
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
                        Icons.Default.ArrowForward,
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
    Column(
        modifier = Modifier.fillMaxSize(),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No courses for $semester $year. Add your first course to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddCourse,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Course")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoursesScreenPreview() {
    StudySageTheme {
        CoursesScreen()
    }
}