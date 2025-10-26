package com.group_7.studysage.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.graphics.Brush
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
import com.group_7.studysage.ui.viewmodels.HomeViewModel

// Data class as specified
data class DailyTask(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val icon: ImageVector,
    val isCompleted: Boolean = false
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Fetch user name from ViewModel (now comes from Firebase)
    val userFullName by homeViewModel.userFullName
    val userProfile by homeViewModel.userProfile
    val isLoadingProfile by homeViewModel.isLoadingProfile

    // Local state for tasks (start empty, load in LaunchedEffect)
    val tasksState = remember { mutableStateListOf<DailyTask>() }

    // Load sample daily tasks once
    LaunchedEffect(Unit) {
        val sample = listOf(
            DailyTask(
                id = "1",
                title = "Complete a Quiz",
                description = "Test your knowledge",
                xpReward = 25,
                icon = Icons.Default.MenuBook
            ),
            DailyTask(
                id = "2",
                title = "Study for 30 mins",
                description = "Focus time",
                xpReward = 50,
                icon = Icons.Default.MenuBook
            ),
            DailyTask(
                id = "3",
                title = "Review Flashcards",
                description = "Memorize concepts",
                xpReward = 30,
                icon = Icons.Default.MenuBook
            ),
            DailyTask(
                id = "4",
                title = "Join Group Study",
                description = "Collaborate with peers",
                xpReward = 40,
                icon = Icons.Default.MenuBook
            )
        )
        tasksState.clear()
        tasksState.addAll(sample)
    }

    // Calculate stats
    val completedTasksCount = tasksState.count { it.isCompleted }
    val totalTasks = tasksState.size
    val currentLevel = 5 // Mock data - replace with actual from ViewModel
    val levelProgress = 0.65f // Mock data - 65% to next level

    val pageCount = tasksState.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Dark gradient background matching website theme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D1B4E),  // Lighter starting point
                            Color(0xFF3D2B5E),  // Lighter ending point
                            Color(0xFF2D1B4E)   // Back to starting for subtle variation
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // HEADER with improved typography
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - User greeting
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Show loading indicator or actual name with better typography
                        if (isLoadingProfile) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF9333EA)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Loading...",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            // Professional "Welcome back" style
                            Text(
                                text = "Welcome back",
                                fontSize = 16.sp,
                                color = Color(0xFFB0B0C0),
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = userFullName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right side - Clean Avatar (matching ProfileScreen)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable { navController.navigate("profile") }
                    ) {
                        val profileImage = userProfile?.get("profileImageUrl") as? String
                        val firstInitial = userFullName.firstOrNull()?.uppercaseChar() ?: 'U'

                        if (!profileImage.isNullOrBlank()) {
                            // Profile image with purple border
                            Image(
                                painter = rememberAsyncImagePainter(profileImage),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color(0xFF9333EA), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Letter avatar with purple border (matching ProfileScreen)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(4.dp, Color(0xFF9333EA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstInitial.toString(),
                                    color = Color(0xFF9333EA),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // DAILY TASKS SECTION HEADER with updated colors
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Tasks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Complete tasks to earn XP",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B0C0)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Circular completion indicator with purple theme
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
                            color = Color(0xFF9333EA),
                            strokeWidth = 4.dp,
                            trackColor = Color(0xFF4A3A5E).copy(alpha = 0.3f),
                        )
                        Text(
                            text = "$completedTasksCount/$totalTasks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9333EA)
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
                                .fillMaxWidth()
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
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Enhanced Pager Indicators with purple theme
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
                                            if (isSelected) Color(0xFF9333EA)
                                            else Color(0xFF4A3A5E).copy(alpha = 0.5f)
                                        )
                                        .animateContentSize()
                                )
                            }
                        }
                    }
                } else {
                    // Enhanced Empty State with dark theme
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D1B4E).copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF9333EA).copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All Done!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Great job completing today's tasks",
                                fontSize = 14.sp,
                                color = Color(0xFFB0B0C0),
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
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid of quick actions with website colors
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Book,
                            label = "Take Quiz",
                            backgroundColor = Color(0xFF9333EA),
                            onClick = { /* Navigate to quiz */ }
                        )
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Style,
                            label = "Flashcards",
                            backgroundColor = Color(0xFF7C3AED),
                            onClick = { /* Navigate to flashcards */ }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Groups,
                            label = "Study Groups",
                            backgroundColor = Color(0xFFB794F6),
                            onClick = { navController.navigate("groups") }
                        )
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.SportsEsports,
                            label = "Games",
                            backgroundColor = Color(0xFFA855F7),
                            onClick = { /* Navigate to games */ }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // RECENTLY OPENED PDFs SECTION
                val recentPdfs by homeViewModel.recentlyOpenedPdfs

                if (recentPdfs.isNotEmpty()) {
                    // Recently Opened Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Opened",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        TextButton(onClick = { /* TODO: Navigate to all PDFs */ }) {
                            Text(
                                text = "See All",
                                fontSize = 14.sp,
                                color = Color(0xFF9333EA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal scrolling PDF cards
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(recentPdfs.size) { index ->
                            val pdf = recentPdfs[index]
                            RecentPdfCard(
                                pdfName = pdf["pdfName"] as? String ?: "Unknown",
                                subject = pdf["subject"] as? String ?: "",
                                progress = (pdf["progress"] as? Number)?.toFloat() ?: 0f,
                                pageCount = (pdf["pageCount"] as? Number)?.toInt() ?: 0,
                                lastPage = (pdf["lastPage"] as? Number)?.toInt() ?: 0,
                                onClick = {
                                    homeViewModel.openPdf(pdf["pdfUrl"] as? String ?: "")
                                }
                            )
                        }
                    }
                } else {
                    // Empty state when no PDFs
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3D2564).copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF6B4BA6).copy(alpha = 0.3f)
                        )
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
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start reading to see your recent files",
                                fontSize = 13.sp,
                                color = Color(0xFFB0B0C0),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Helper composable for stat items in the overview card
@Composable
private fun StatItem(
    icon: ImageVector?,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// Enhanced TaskCard with dark theme and website colors
@Composable
private fun EnhancedTaskCard(
    task: DailyTask,
    onToggleCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate card alpha and elevation based on completion
    val alpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.6f else 1f,
        animationSpec = tween(300),
        label = "alpha"
    )

    val elevation by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 4f,
        animationSpec = tween(300),
        label = "elevation"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = modifier.alpha(alpha),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D1B4E).copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, Color(0xFF9333EA).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon container with dark purple background
                    Card(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3D2B5E)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = task.icon,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title / description / xp
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 14.sp,
                            color = Color(0xFFB0B0C0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // XP Badge with gold theme
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFBBF24).copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${task.xpReward} XP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Animated check icon with purple theme
                    val scale by animateFloatAsState(
                        targetValue = if (task.isCompleted) 1.1f else 1f,
                        animationSpec = tween(200),
                        label = "scale"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (task.isCompleted)
                            Color(0xFF9333EA)
                        else
                            Color(0xFF9333EA).copy(alpha = 0.5f),
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

// Quick Action Button with dark theme and glassmorphism effect
@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, backgroundColor.copy(alpha = 0.5f)),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D1B4E).copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = backgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Recent PDF Card composable
@Composable
fun RecentPdfCard(
    pdfName: String,
    subject: String,
    progress: Float,
    pageCount: Int,
    lastPage: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(220.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2564)
        ),
        border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - PDF icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF9333EA),
                                Color(0xFF7C3AED)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color.White,
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
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subject,
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0C0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom section - Progress and info
                Column {
                    // Progress info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFB0B0C0),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Page $lastPage of $pageCount",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B0C0)
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF9333EA),
                        trackColor = Color(0xFF2D1B4E)
                    )
                }
            }
        }
    }
}
