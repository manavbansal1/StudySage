package com.group_7.studysage.ui.screens.HomeScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.group_7.studysage.viewmodels.HomeViewModel

// Data class as specified
data class DailyTask(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val icon: ImageVector,
    val isCompleted: Boolean = false
)

// Data class for our new Quick Actions
data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

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
        // The base "glass" color. White in light mode, dark purple in dark mode.
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    courseViewModel: com.group_7.studysage.viewmodels.CourseViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userFullName by homeViewModel.userFullName
    val userProfile by homeViewModel.userProfile
    val isLoadingProfile by homeViewModel.isLoadingProfile
    val recentPdfs by homeViewModel.recentlyOpenedPdfs

    // Pull-to-refresh state
    val isRefreshing by homeViewModel.isRefreshing
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    val tasksState = remember { mutableStateListOf<DailyTask>() }

    // Reload profile data when navigating back to Home
    LaunchedEffect(key1 = true) {
        homeViewModel.refreshHomeData()
    }

    LaunchedEffect(Unit) {
        val sample = listOf(
            DailyTask("1", "Complete a Quiz", "Test your knowledge", 25, Icons.AutoMirrored.Filled.MenuBook),
            DailyTask("2", "Study for 30 mins", "Focus time", 50, Icons.AutoMirrored.Filled.MenuBook),
            DailyTask("3", "Review Flashcards", "Memorize concepts", 30, Icons.AutoMirrored.Filled.MenuBook),
        )
        tasksState.clear()
        tasksState.addAll(sample)
    }

    val completedTasksCount = tasksState.count { it.isCompleted }
    val totalTasks = tasksState.size
    val pageCount = tasksState.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // --- Define Quick Actions ---
    // We use theme colors directly here.
    val quickActions = listOf(
        QuickAction(
            title = "Take Quiz",
            icon = Icons.Default.Book,
            color = MaterialTheme.colorScheme.primary,
            onClick = { /* Navigate to quiz */ }
        ),
        QuickAction(
            title = "Flashcards",
            icon = Icons.Default.Style,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { /* Navigate to flashcards */ }
        ),
        QuickAction(
            title = "Study Groups",
            icon = Icons.Default.Groups,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("groups") }
        ),
        QuickAction(
            title = "Games",
            icon = Icons.Default.SportsEsports,
            color = MaterialTheme.colorScheme.primaryContainer, // A different shade
            onClick = { /* Navigate to games */ }
        )
    )

    // Use the theme's background color
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { homeViewModel.refreshHomeData() },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        scale = true,
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        elevation = 6.dp
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp) // Page padding
                ) {
            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isLoadingProfile) {
                        // ... (Loading state is fine)
                    } else {
                        Text(
                            text = "Welcome back",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = userFullName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Right side - Clean Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable { navController.navigate("profile") }
                ) {
                    val profileImage = userProfile?.get("profileImageUrl") as? String
                    val firstInitial = userFullName.firstOrNull()?.uppercaseChar() ?: 'U'

                    if (!profileImage.isNullOrBlank()) {
                        // Disable cache to always show latest profile image
                        val imageRequest = ImageRequest.Builder(context)
                            .data(profileImage)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build()

                        Image(
                            painter = rememberAsyncImagePainter(imageRequest),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Letter avatar (fixed background)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant) // Use soft purple
                                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstInitial.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DAILY TASKS SECTION
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Daily Tasks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Complete tasks to earn XP",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Circular completion indicator
                Box(contentAlignment = Alignment.Center) {
                    val completionProgress = if (totalTasks > 0) completedTasksCount.toFloat() / totalTasks else 0f
                    val animatedCompletion by animateFloatAsState(
                        targetValue = completionProgress,
                        animationSpec = tween(800),
                        label = "completion"
                    )
                    CircularProgressIndicator(
                        progress = { animatedCompletion },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    )
                    Text(
                        text = "$completedTasksCount/$totalTasks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DAILY TASKS PAGER
            if (pageCount > 0) {
                Column {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .height(170.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        val task = tasksState[page]
                        EnhancedTaskCard(
                            task = task,
                            onToggleCompleted = { toggled ->
                                val index = tasksState.indexOfFirst { it.id == task.id }
                                if (index >= 0) {
                                    tasksState[index] = tasksState[index].copy(isCompleted = toggled)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Pager Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pageCount) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateFloatAsState(
                                targetValue = if (isSelected) 24f else 8f,
                                animationSpec = tween(300),
                                label = "indicator_width"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(width.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        // A subtle, but visible grey in both light and dark modes
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) // <-- FIX
                                    )
                                    .animateContentSize()
                            )
                        }
                    }
                }
            } else {
                // Empty State for Tasks
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All Done!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Great job completing today's tasks",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QUICK ACTIONS SECTION
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NEW: Horizontal Scrolling Quick Actions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(quickActions) { action ->
                    QuickActionCard(
                        action = action,
                        onClick = action.onClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            // RECENTLY OPENED PDFs SECTION
            val recentPdfs by homeViewModel.recentlyOpenedPdfs

            if (recentPdfs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Opened",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { /* TODO: Navigate to all notes */ }) {
                        Text(
                            text = "See All",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(recentPdfs.size) { index ->
                        val pdf = recentPdfs[index]
                        val courseId = pdf["courseId"] as? String ?: ""
                        RecentPdfCard(
                            pdfName = pdf["title"] as? String ?: pdf["fileName"] as? String ?: "Unknown",
                            subject = homeViewModel.getCourseName(courseId),
                            lastOpenedAt = (pdf["lastOpenedAt"] as? Number)?.toLong() ?: 0L,
                            openCount = (pdf["openCount"] as? Number)?.toInt() ?: 0,
                            onClick = {
                                val noteId = pdf["noteId"] as? String
                                if (!courseId.isNullOrBlank()) {
                                    // Open course and request opening this specific note
                                    homeViewModel.openCourse(courseId, noteId, courseViewModel, navController)
                                }
                            }
                        )
                    }
                }
            } else {
                // Empty state when no PDFs
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📄", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No PDFs opened yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start reading to see your recent files",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Spacer for content to scroll above nav bar
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

            // Animated "Refreshing..." message overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isRefreshing,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Refreshing your notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


// Enhanced TaskCard - Refactored for consistency
@Composable
fun EnhancedTaskCard(
    task: DailyTask,
    onToggleCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.6f else 1f,
        animationSpec = tween(300),
        label = "alpha"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        GlassCard(modifier = modifier.alpha(alpha)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon container
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = task.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title / description / xp
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // XP Badge - tied to tertiary color (gold)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${task.xpReward} XP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Animated check icon
                    val scale by animateFloatAsState(
                        targetValue = if (task.isCompleted) 1.1f else 1f,
                        animationSpec = tween(200),
                        label = "scale"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (task.isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        animationSpec = tween(300),
                        label = "color"
                    )
                    IconButton(
                        onClick = { onToggleCompleted(!task.isCompleted) },
                        modifier = Modifier.scale(scale)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (task.isCompleted) "Completed" else "Not completed",
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

// NEW Quick Action Card
@Composable
fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .width(110.dp)
            .height(120.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon highlight
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(action.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = action.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Recent PDF Card - Simplified to show title, subject, and relative time
@Composable
fun RecentPdfCard(
    pdfName: String,
    subject: String,
    lastOpenedAt: Long,
    openCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier
            .width(240.dp)
            .height(130.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - PDF icon highlight
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side - Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Title and subject
                Column {
                    Text(
                        text = pdfName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subject.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subject,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom section - Last opened time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getRelativeTimeString(lastOpenedAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Convert timestamp to relative time string (e.g., "2h ago", "3d ago")
 */
fun getRelativeTimeString(timestamp: Long): String {
    if (timestamp == 0L) return "Just now"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3_600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
        diff < 86_400_000 -> "${diff / 3_600_000}h ago" // Less than 1 day
        diff < 604_800_000 -> "${diff / 86_400_000}d ago" // Less than 1 week
        diff < 2_592_000_000 -> "${diff / 604_800_000}w ago" // Less than 30 days
        else -> "${diff / 2_592_000_000}mo ago" // Months
    }
}

/**
 * Empty state composable for when there are no recently opened PDFs
 * Shows a friendly message encouraging users to upload their first note
 */
@Composable
fun EmptyRecentPdfsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "No recent notes",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No recent notes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Upload your first note to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
