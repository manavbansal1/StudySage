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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.group_7.studysage.data.repository.Course
import com.group_7.studysage.data.repository.CourseRepository
import com.group_7.studysage.data.repository.Semester
import com.group_7.studysage.ui.theme.StudySageTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.group_7.studysage.viewmodels.CourseViewModel
import com.group_7.studysage.viewmodels.AddCourseViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    viewModel: CourseViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CourseViewModel(
                    courseRepository = CourseRepository(),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    ),
    authViewModel: com.group_7.studysage.viewmodels.AuthViewModel,
    navCourseId: String? = null,
    navNoteId: String? = null, // NEW optional nav params
    onNavigateToCanvas: () -> Unit = {},
    navController: androidx.navigation.NavController? = null,
    homeViewModel: com.group_7.studysage.viewmodels.HomeViewModel? = null // Add homeViewModel to refresh recently opened
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCourseDialog by rememberSaveable { mutableStateOf(false) }
    
    // Edit/Delete State - now from ViewModel to survive rotation
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    val courseToEdit = uiState.courseToEdit
    val showEditDialog = uiState.showEditDialog
    val showDeleteDialog = uiState.showDeleteDialog
    val selectedCourseForAction = uiState.selectedCourseForAction

    // ViewModel for add/edit course dialog (scoped to this screen)
    val addCourseViewModel: AddCourseViewModel = viewModel()

    // Restore course state on configuration change (rotation)
    LaunchedEffect(Unit) {
        android.util.Log.d("CoursesScreen", "========================================")
        android.util.Log.d("CoursesScreen", "🔄 LaunchedEffect(Unit) triggered")
        android.util.Log.d("CoursesScreen", "   Calling restoreSelectedCourseIfNeeded()...")
        viewModel.restoreSelectedCourseIfNeeded()
        android.util.Log.d("CoursesScreen", "========================================")
    }

    // Restore dialog states after courses are loaded (for rotation persistence)
    LaunchedEffect(uiState.allCourses) {
        if (uiState.allCourses.isNotEmpty()) {
            android.util.Log.d("CoursesScreen", "🔄 Restoring dialog states after courses loaded")
            viewModel.restoreDialogStates()
        }
    }

    // If navigation provided a courseId, load it and set pendingNote
    LaunchedEffect(navCourseId, navNoteId) {
        android.util.Log.d("CoursesScreen", "========================================")
        android.util.Log.d("CoursesScreen", "🧭 Navigation LaunchedEffect triggered")
        android.util.Log.d("CoursesScreen", "   navCourseId: ${navCourseId ?: "null"}")
        android.util.Log.d("CoursesScreen", "   navNoteId: ${navNoteId ?: "null"}")
        if (!navCourseId.isNullOrBlank()) {
            android.util.Log.d("CoursesScreen", "   Setting pending note and loading course...")
            viewModel.setPendingOpenNote(navNoteId)
            viewModel.loadCourseWithNotes(navCourseId)
        }
        android.util.Log.d("CoursesScreen", "========================================")
    }

    // Check if we should show the course detail screen or courses list
    // Show detail screen if:
    // 1. We have a selected course, OR
    // 2. We're in the process of restoring state after rotation
    if (uiState.selectedCourse != null) {
        // Show course detail screen when we have the course data
        val courseId = uiState.selectedCourse?.course?.id
        android.util.Log.d("CoursesScreen", "🎯 Showing CourseDetailScreen for course: $courseId")

        CourseDetailScreen(
            courseWithNotes = uiState.selectedCourse!!,
            onBack = {
                android.util.Log.d("CoursesScreen", "⬅️ Back button explicitly pressed from CourseDetailScreen")
                android.util.Log.d("CoursesScreen", "   shouldPopBackOnClose: ${uiState.shouldPopBackOnClose}")
                android.util.Log.d("CoursesScreen", "   navController: ${navController != null}")

                // Check the flag BEFORE clearing state
                val shouldPopBack = uiState.shouldPopBackOnClose

                // Clear course state first
                viewModel.clearSelectedCourse()
                viewModel.setShouldPopBack(false)

                // If we came from Recently Opened, navigate back to home
                if (shouldPopBack && navController != null) {
                    android.util.Log.d("CoursesScreen", "🔙 Came from Recently Opened - navigating to home")
                    // Pop all the way back to home (inclusive = false keeps home in the stack)
                    navController.navigate("home") {
                        popUpTo("home") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                } else {
                    android.util.Log.d("CoursesScreen", "📋 Normal course flow - showing courses list")
                    // Just clearing selectedCourse above will show the courses list
                }
            },
            courseViewModel = viewModel,
            authViewModel = authViewModel
        )
    } else if (uiState.isRestoringState) {
        // Show loading indicator while restoring state after rotation
        android.util.Log.d("CoursesScreen", "⏳ Restoring course state after rotation (isRestoring=${uiState.isRestoringState})...")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        // Show courses list when no course is selected and not restoring
        android.util.Log.d("CoursesScreen", "📋 Showing courses list (selectedCourse is null, isRestoring=false)")
        Scaffold(
            containerColor = Color.Transparent, // Let the theme background show
        ) { paddingValues ->

            // Pull-to-refresh wrapper for the grid
            val isRefreshing = uiState.isRefreshing
            @Suppress("DEPRECATION")
            val swipeState = rememberSwipeRefreshState(isRefreshing)

            @Suppress("DEPRECATION")
            SwipeRefresh(
                state = swipeState,
                onRefresh = { viewModel.refreshCourses() },
                indicator = { state, trigger ->
                    @Suppress("DEPRECATION")
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp) // Page horizontal padding
                ) {
                    // 1. HEADER
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        CoursesHeader(
                            onAddClick = { showAddCourseDialog = true },
                            onCanvasClick = onNavigateToCanvas,
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
                                onClick = { viewModel.loadCourseWithNotes(course.id) },
                                onLongClick = {
                                    viewModel.setSelectedCourseForAction(course)
                                    showOptionsSheet = true
                                }
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
        CourseDialog(
            isLoading = uiState.isCreatingCourse,
            semester = uiState.selectedSemester,
            year = uiState.selectedYear,
            onDismiss = { 
                showAddCourseDialog = false
                addCourseViewModel.clearState() // Clear state when dialog is dismissed
            },
            onConfirm = { title, code, instructor, description, credits, color ->
                viewModel.createCourse(title, code, instructor, description, credits, color)
                showAddCourseDialog = false
                addCourseViewModel.clearState() // Clear state after successful creation
            },
            addCourseViewModel = addCourseViewModel
        )
    }

    // Edit Course Dialog
    if (showEditDialog && courseToEdit != null) {
        CourseDialog(
            isLoading = uiState.isLoading,
            semester = courseToEdit!!.semester,
            year = courseToEdit!!.year,
            existingCourse = courseToEdit,
            onDismiss = { 
                viewModel.setShowEditDialog(false)
            },
            onConfirm = { title, code, instructor, description, credits, color ->
                val updatedCourse = courseToEdit!!.copy(
                    title = title,
                    code = code,
                    instructor = instructor,
                    description = description,
                    credits = credits,
                    color = color
                )
                viewModel.updateCourse(updatedCourse)
                viewModel.setShowEditDialog(false)
            },
            addCourseViewModel = addCourseViewModel
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedCourseForAction != null) {
        val coroutineScope = rememberCoroutineScope()
        DeleteConfirmationDialog(
            courseName = selectedCourseForAction!!.title,
            onDismiss = { viewModel.setShowDeleteDialog(false) },
            onConfirm = {
                val courseIdToDelete = selectedCourseForAction!!.id
                android.util.Log.d("CourseScreen", "Deleting course: $courseIdToDelete")
                viewModel.deleteCourse(courseIdToDelete)
                viewModel.setShowDeleteDialog(false)
                // Refresh recently opened after a short delay to ensure database cleanup completes
                coroutineScope.launch {
                    kotlinx.coroutines.delay(500) // Wait for backend to complete
                    android.util.Log.d("CourseScreen", "Refreshing recently opened PDFs after course deletion")
                    homeViewModel?.loadRecentlyOpenedPdfs()
                }
            }
        )
    }

    // Options Bottom Sheet
    if (showOptionsSheet && selectedCourseForAction != null) {
        CourseOptionsSheet(
            onDismiss = { showOptionsSheet = false },
            onEdit = {
                showOptionsSheet = false
                viewModel.setCourseToEdit(selectedCourseForAction)
                viewModel.setShowEditDialog(true)
            },
            onDelete = {
                showOptionsSheet = false
                viewModel.setShowDeleteDialog(true)
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
    onCanvasClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Track and organize your classes",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Courses",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Canvas button
                IconButton(
                    onClick = onCanvasClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Connect Canvas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Semester Filter
        var semesterExpanded by remember { mutableStateOf(false) }
        var semesterWidth by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { semesterExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        semesterWidth = with(density) { coordinates.size.width.toDp() }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = selectedSemester.ifEmpty { "Semester" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = semesterExpanded,
                onDismissRequest = { semesterExpanded = false },
                modifier = Modifier.width(semesterWidth),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
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
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Year Filter
        var yearExpanded by remember { mutableStateOf(false) }
        var yearWidth by remember { mutableStateOf(0.dp) }

        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { yearExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        yearWidth = with(density) { coordinates.size.width.toDp() }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = selectedYear.ifEmpty { "Year" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false },
                modifier = Modifier.width(yearWidth),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
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
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseGridCard(
    course: Course,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            .aspectRatio(0.85f), // Slightly taller for better proportions
        onClick = null // We handle clicks manually for combinedClickable
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            // Top Half - Visual Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f) // Give more space to the visual part
                    .background(
                        // Use a light tint of the course color
                        tintColor.copy(alpha = 0.15f)
                    )
            ) {
                // Course icon/image placeholder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Just the icon, larger and cleaner
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Course Icon",
                        tint = tintColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Credits badge in top-right corner
                if (course.credits > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${course.credits} CR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
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
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Main course info
                Column {
                    // Course code
                    Text(
                        text = course.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = tintColor,
                        fontWeight = FontWeight.ExtraBold, // Bolder
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Course title
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium, // Larger title
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    // Instructor (if available)
                    if (course.instructor.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Instructor",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Course", fontWeight = FontWeight.SemiBold)
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
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseOptionsSheet(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp) // Extra padding for bottom nav/gestures
        ) {
            // Header
            Text(
                text = "Course Options",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Edit Option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEdit() },
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Edit Course Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Modify title, code, credits, etc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Delete Option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDelete() },
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Delete Course",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Permanently remove course and notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    courseName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Delete Course?")
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$courseName\"? This action cannot be undone and will delete all notes associated with this course.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}