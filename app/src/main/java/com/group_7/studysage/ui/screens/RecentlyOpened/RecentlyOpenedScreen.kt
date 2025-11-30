package com.group_7.studysage.ui.screens.RecentlyOpened

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.group_7.studysage.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyOpenedScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    courseViewModel: com.group_7.studysage.viewmodels.CourseViewModel
) {
    val recentPdfs by homeViewModel.recentlyOpenedPdfs.collectAsState()
    val isLoading by homeViewModel.isLoading

    // Log screen composition and navigation state
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.d("RecentlyOpenedScreen", "========================================")
        android.util.Log.d("RecentlyOpenedScreen", "🎬 SCREEN COMPOSED")
        android.util.Log.d("RecentlyOpenedScreen", "   Current destination: ${navController.currentDestination?.route}")
        android.util.Log.d("RecentlyOpenedScreen", "   Previous back stack entry: ${navController.previousBackStackEntry?.destination?.route}")
        android.util.Log.d("RecentlyOpenedScreen", "========================================")
    }

    // Handle device back button - pop back stack to return to previous screen (home)
    BackHandler {
        android.util.Log.d("RecentlyOpenedScreen", "========================================")
        android.util.Log.d("RecentlyOpenedScreen", "🔙 DEVICE BACK BUTTON PRESSED")
        android.util.Log.d("RecentlyOpenedScreen", "   Current destination: ${navController.currentDestination?.route}")
        android.util.Log.d("RecentlyOpenedScreen", "   Previous entry: ${navController.previousBackStackEntry?.destination?.route}")
        android.util.Log.d("RecentlyOpenedScreen", "   Calling popBackStack()...")

        val popped = navController.popBackStack()
        android.util.Log.d("RecentlyOpenedScreen", "   Pop result: $popped")
        android.util.Log.d("RecentlyOpenedScreen", "   After pop - destination: ${navController.currentDestination?.route}")
        android.util.Log.d("RecentlyOpenedScreen", "========================================")
    }

    // Load recently opened PDFs when screen opens
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.d("RecentlyOpenedScreen", "LaunchedEffect triggered - Loading recently opened PDFs...")
        homeViewModel.loadRecentlyOpenedPdfs()
    }

    // Log when recentPdfs changes
    androidx.compose.runtime.LaunchedEffect(recentPdfs) {
        android.util.Log.d("RecentlyOpenedScreen", "Recent PDFs updated: ${recentPdfs.size} items")
        recentPdfs.forEachIndexed { index, pdf ->
            android.util.Log.d("RecentlyOpenedScreen", "  [$index] ${pdf["title"]} (${pdf["courseId"]})")
        }
    }

    // Add DisposableEffect to handle configuration changes
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            // Cleanup if needed
        }
    }

    // Show loading indicator while data is loading
    if (isLoading && recentPdfs.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar matching app theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Back button
                IconButton(
                    onClick = {
                        android.util.Log.d("RecentlyOpenedScreen", "========================================")
                        android.util.Log.d("RecentlyOpenedScreen", "⬅️ UI BACK BUTTON CLICKED")
                        android.util.Log.d("RecentlyOpenedScreen", "   Current destination: ${navController.currentDestination?.route}")
                        android.util.Log.d("RecentlyOpenedScreen", "   Attempting to navigate to 'home'...")

                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                        android.util.Log.d("RecentlyOpenedScreen", "   After navigation - destination: ${navController.currentDestination?.route}")
                        android.util.Log.d("RecentlyOpenedScreen", "========================================")
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = "Recently Opened",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Clear All button (only show if there are items)
                if (recentPdfs.isNotEmpty()) {
                    TextButton(
                        onClick = { homeViewModel.clearAllRecentlyOpened() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (recentPdfs.isEmpty()) {
                // Empty state with glass card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "No recent notes",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "No recent notes yet",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Start opening notes to see them here",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with count
                    item {
                        Text(
                            text = "${recentPdfs.size} ${if (recentPdfs.size == 1) "note" else "notes"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }

                    // List of PDFs with glass cards
                    items(recentPdfs.size) { index ->
                        val pdf = recentPdfs[index]
                        val courseId = pdf["courseId"] as? String ?: ""

                        GlassPdfCard(
                            pdfName = pdf["title"] as? String ?: pdf["fileName"] as? String ?: "Unknown",
                            subject = homeViewModel.getCourseName(courseId),
                            lastOpenedAt = (pdf["lastOpenedAt"] as? Number)?.toLong() ?: 0L,
                            openCount = (pdf["openCount"] as? Number)?.toInt() ?: 0,
                            onClick = {
                                android.util.Log.d("RecentlyOpenedScreen", "========================================")
                                android.util.Log.d("RecentlyOpenedScreen", "📄 PDF CARD CLICKED")
                                val noteId = pdf["noteId"] as? String
                                android.util.Log.d("RecentlyOpenedScreen", "   Course ID: $courseId")
                                android.util.Log.d("RecentlyOpenedScreen", "   Note ID: $noteId")

                                if (!courseId.isNullOrBlank()) {
                                    android.util.Log.d("RecentlyOpenedScreen", "   Setting shouldPopBack = true")
                                    courseViewModel.setShouldPopBack(true)
                                    android.util.Log.d("RecentlyOpenedScreen", "   ✅ Flag set! Current value: ${courseViewModel.uiState.value.shouldPopBackOnClose}")

                                    android.util.Log.d("RecentlyOpenedScreen", "   Setting pending note ID: $noteId")
                                    courseViewModel.setPendingOpenNote(noteId)

                                    val route = if (!noteId.isNullOrBlank()) {
                                        "course/$courseId?noteId=$noteId"
                                    } else {
                                        "course/$courseId"
                                    }

                                    android.util.Log.d("RecentlyOpenedScreen", "   Navigating to route: $route")
                                    android.util.Log.d("RecentlyOpenedScreen", "   Current destination before: ${navController.currentDestination?.route}")

                                    navController.navigate(route) {
                                        // Don't pop recently_opened here - let the back button handle it
                                    }

                                    android.util.Log.d("RecentlyOpenedScreen", "   Current destination after: ${navController.currentDestination?.route}")
                                    android.util.Log.d("RecentlyOpenedScreen", "========================================")
                                } else {
                                    android.util.Log.e("RecentlyOpenedScreen", "   ❌ Course ID is blank!")
                                    android.util.Log.d("RecentlyOpenedScreen", "========================================")
                                }
                            }
                        )
                    }

                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPdfCard(
    pdfName: String,
    subject: String,
    lastOpenedAt: Long,
    openCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF icon with primary color background
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

            Spacer(modifier = Modifier.width(16.dp))

            // Content column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = pdfName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
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

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getRelativeTimeString(lastOpenedAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (openCount > 1) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$openCount ${if (openCount == 1) "view" else "views"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convert timestamp to relative time string (e.g., "2h ago", "3d ago")
 */
private fun getRelativeTimeString(timestamp: Long): String {
    if (timestamp == 0L) return "Just now"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
        else -> "${diff / 2_592_000_000}mo ago"
    }
}