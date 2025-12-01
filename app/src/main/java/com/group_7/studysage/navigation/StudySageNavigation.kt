package com.group_7.studysage.navigation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.group_7.studysage.ui.screens.CourseScreen.CoursesScreen
import com.group_7.studysage.ui.screens.CanvasIntegrationScreen
import com.group_7.studysage.ui.screens.GameLobbyScreen
import com.group_7.studysage.ui.screens.GamePlayScreen
import com.group_7.studysage.ui.screens.GameScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupChatScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupScreen
import com.group_7.studysage.ui.screens.HomeScreen.HomeScreen
import com.group_7.studysage.ui.screens.ProfileScreen.NotificationsScreen
import com.group_7.studysage.ui.screens.ProfileScreen.ProfileScreen
import com.group_7.studysage.ui.screens.RecentlyOpened.RecentlyOpenedScreen
import com.group_7.studysage.ui.screens.TempQuiz.TempQuizGenerationScreen
import com.group_7.studysage.ui.screens.TempFlashcards.TempFlashcardsGenerationScreen
import com.group_7.studysage.ui.screens.auth.SignInScreen
import com.group_7.studysage.ui.screens.auth.SignUpScreen
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.viewmodels.CourseViewModel
import com.group_7.studysage.viewmodels.HomeViewModel
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.group_7.studysage.data.models.GameType
import com.group_7.studysage.data.repository.CourseRepository
import com.group_7.studysage.ui.theme.*
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object SignIn : Screen("sign_in", "Sign In")
    object SignUp : Screen("sign_up", "Sign Up")
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Course : Screen("course", "Course", Icons.Filled.Book)
    object Groups : Screen("groups", "Groups", Icons.Filled.Groups)
    object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    object Games : Screen("games", "Games", Icons.Filled.Gamepad)
    object GroupChat : Screen("group_chat/{groupId}", "Group Chat") {
        fun createRoute(groupId: String) = "group_chat/$groupId"
    }
    object GameLobby : Screen("game_lobby/{gameType}", "Game Lobby") {
        fun createRoute(gameType: GameType) = "game_lobby/$gameType"
    }
    object GamePlay : Screen("game_play/{gameCode}", "Play") {
        fun createRoute(gameCode: String) = "game_play/$gameCode"
    }
    object Leaderboard : Screen("leaderboard", "Leaderboard")
    object CanvasIntegration : Screen("canvas_integration", "Canvas Integration")
}
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    )
    val border = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(36.dp),
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = onClick,
            content = { content() }
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(36.dp),
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = { content() }
        )
    }
}
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StudySageNavigation(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val isUserSignedIn by authViewModel.isSignedIn
    if (isUserSignedIn) {
        // Create a fresh NavController for authenticated users
        val navController = androidx.navigation.compose.rememberNavController()

        // Create courseViewModel scoped to the activity to share across course routes
        // and ensure SavedStateHandle works properly for configuration changes
        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = context as androidx.activity.ComponentActivity

        // Remember the factory to avoid recreating ViewModel on every recomposition
        val courseViewModelFactory = remember {
            android.util.Log.d("StudySageNavigation", "========================================")
            android.util.Log.d("StudySageNavigation", "🏗️ Creating CourseViewModel Factory")
            android.util.Log.d("StudySageNavigation", "   Context: ${context::class.simpleName}")
            viewModelFactory {
                initializer {
                    android.util.Log.d("StudySageNavigation", "   📦 Factory initializer called")
                    android.util.Log.d("StudySageNavigation", "   Creating SavedStateHandle...")
                    val handle = createSavedStateHandle()
                    android.util.Log.d("StudySageNavigation", "   SavedStateHandle created: ${handle.hashCode()}")
                    android.util.Log.d("StudySageNavigation", "   SavedStateHandle keys: ${handle.keys()}")
                    CourseViewModel(
                        courseRepository = CourseRepository(),
                        savedStateHandle = handle
                    )
                }
            }
        }

        val courseViewModel: CourseViewModel = viewModel(
            viewModelStoreOwner = activity,
            factory = courseViewModelFactory
        )
        android.util.Log.d("StudySageNavigation", "✅ CourseViewModel instance: ${courseViewModel.hashCode()}")
        android.util.Log.d("StudySageNavigation", "========================================")

        val homeViewModel: HomeViewModel = viewModel()

        // Track the current user ID to detect when a different user logs in
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        // Track previous user ID to detect changes - use remember with a key to persist across recomposition
        val previousUserId = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

        // Detect when user ID changes (different user logs in) and refresh all screens
        androidx.compose.runtime.LaunchedEffect(currentUserId) {
            android.util.Log.d("StudySageNav", "========================================")
            android.util.Log.d("StudySageNav", "🔍 LaunchedEffect triggered")
            android.util.Log.d("StudySageNav", "   currentUserId: $currentUserId")
            android.util.Log.d("StudySageNav", "   previousUserId: ${previousUserId.value}")

            if (currentUserId != null) {
                // Check if this is a different user or first login
                val isDifferentUser = previousUserId.value != null && previousUserId.value != currentUserId
                val isFirstLogin = previousUserId.value == null

                if (isDifferentUser) {
                    android.util.Log.d("StudySageNav", "👤 DIFFERENT USER DETECTED!")
                    android.util.Log.d("StudySageNav", "   Previous: ${previousUserId.value}")
                    android.util.Log.d("StudySageNav", "   Current:  $currentUserId")
                    android.util.Log.d("StudySageNav", "🔄 Refreshing all screens...")

                    // Refresh Home screen data
                    homeViewModel.refreshHomeData()
                    android.util.Log.d("StudySageNav", "✅ Home screen refreshed")

                    // Refresh Course screen data
                    courseViewModel.refreshCourses()
                    android.util.Log.d("StudySageNav", "✅ Course screen refreshed")

                    android.util.Log.d("StudySageNav", "✅ All screens refreshed")
                } else if (isFirstLogin) {
                    android.util.Log.d("StudySageNav", "🔐 First user login: $currentUserId")
                    android.util.Log.d("StudySageNav", "📂 Loading all screen data...")

                    // Load Home screen data
                    homeViewModel.refreshHomeData()
                    android.util.Log.d("StudySageNav", "✅ Home screen loaded")

                    // Load Course screen data
                    courseViewModel.loadCourses()
                    android.util.Log.d("StudySageNav", "✅ Course screen loaded")

                    android.util.Log.d("StudySageNav", "✅ All screens loaded")
                } else {
                    android.util.Log.d("StudySageNav", "ℹ️ Same user, no reset needed ($currentUserId)")
                }

                // Update previous user ID AFTER the check
                previousUserId.value = currentUserId
                android.util.Log.d("StudySageNav", "📝 Updated previousUserId to: $currentUserId")
            } else {
                android.util.Log.d("StudySageNav", "⚠️ currentUserId is null (user signed out?)")
                // Clear previous user ID so next sign-in is treated as new
                previousUserId.value = null
                android.util.Log.d("StudySageNav", "🧹 Cleared previousUserId")
            }
            android.util.Log.d("StudySageNav", "========================================")
        }

        val screens = listOf(Screen.Home, Screen.Course, Screen.Groups, Screen.Games)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Collect fullscreen overlay state from courseViewModel
        val courseUiState by courseViewModel.uiState.collectAsState()

        // Clear selected course when navigating away from course screen to a different tab
        // BUT not during rotation or when navigating between course routes
        androidx.compose.runtime.LaunchedEffect(currentDestination?.route) {
            val currentRoute = currentDestination?.route
            val isCourseRoute = currentRoute == Screen.Course.route ||
                               currentRoute?.startsWith("${Screen.Course.route}/") == true

            // Only clear if we have a selected course AND we're navigating to a non-course screen
            if (!isCourseRoute && courseUiState.selectedCourse != null && currentRoute != null) {
                android.util.Log.d("StudySageNavigation", "📍 Navigating away from courses to: $currentRoute")
                android.util.Log.d("StudySageNavigation", "🧹 Clearing selected course due to navigation")
                courseViewModel.clearSelectedCourse()
            }
        }

        val shouldHideBottomNav = currentDestination?.route?.startsWith("group_chat/") == true ||
                currentDestination?.route == "profile" ||
                currentDestination?.route == "notification_settings" ||
                currentDestination?.route == "temp_quiz" || // Hide nav on temp quiz screen
                currentDestination?.route == "temp_flashcards" || // Hide nav on temp flashcard screen
                currentDestination?.route == "recently_opened" ||
                currentDestination?.route?.startsWith("game_") == true ||
                courseUiState.isShowingFullscreenOverlay || // Hide nav when quiz/NFC screens are showing
                courseUiState.selectedCourse != null // Hide nav when viewing course details

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!shouldHideBottomNav) {
                    val selectedIndex = screens.indexOfFirst { screen ->
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }
                    val isDark = isSystemInDarkTheme()
                    val navIndicatorColor = if (isDark) DarkNavIndicator else LightNavIndicator
                    val navSelectedColor = if (isDark) DarkNavSelected else LightNavSelected
                    val navUnselectedColor = if (isDark) DarkNavUnselected else LightNavUnselected
                    GlassCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .height(72.dp)
                    ) {
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            TabRow(
                                selectedTabIndex = selectedIndex.takeIf { it >= 0 } ?: 0,
                                modifier = Modifier.clip(RoundedCornerShape(36.dp)),
                                containerColor = Color.Transparent,
                                contentColor = navSelectedColor,
                                divider = {},
                                indicator = { tabPositions ->
                                    if (selectedIndex >= 0 && selectedIndex < tabPositions.size) {
                                        Box(
                                            modifier = Modifier
                                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                                .fillMaxHeight()
                                                .padding(vertical = 6.dp, horizontal = 5.dp)
                                                .background(
                                                    color = navIndicatorColor,
                                                    shape = RoundedCornerShape(30.dp)
                                                )
                                        )
                                    }
                                }
                            ) {
                                screens.forEachIndexed { index, screen ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    Tab(
                                        selected = (selectedIndex == index),
                                        onClick = {
                                            // Only navigate if not already on this tab
                                            if (selectedIndex != index) {
                                                try {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("StudySageNavigation", "Navigation to ${screen.route} failed: ${e.message}", e)
                                                }
                                            }
                                        },
                                        icon = {
                                            screen.icon?.let {
                                                Icon(
                                                    it,
                                                    screen.title,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        },
                                        text = {
                                            Text(
                                                screen.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        selectedContentColor = navSelectedColor,
                                        unselectedContentColor = navUnselectedColor,
                                        interactionSource = interactionSource,
                                        modifier = Modifier.indication(interactionSource, null)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(
                    Screen.Home.route,
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) {
                    HomeScreen(
                        navController = navController,
                        courseViewModel = courseViewModel
                    )
                }
                composable(
                    Screen.Course.route,
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) {
                    // Plain course route (no courseId) — show courses list
                    CoursesScreen(
                        courseViewModel,
                        authViewModel,
                        onNavigateToCanvas = {
                            navController.navigate(Screen.CanvasIntegration.route)
                        },
                        navController = navController
                    )
                }

                composable(
                    route = "${Screen.Course.route}/{courseId}?noteId={noteId}",
                    arguments = listOf(
                        navArgument("courseId") { type = NavType.StringType },
                        navArgument("noteId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    ),
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    // Extract required courseId and optional noteId and forward to CoursesScreen
                    val navCourseId = backStackEntry.arguments?.getString("courseId") ?: ""
                    val navNoteId = backStackEntry.arguments?.getString("noteId")
                    // Pass positional args: viewModel, authViewModel, navCourseId, navNoteId
                    CoursesScreen(
                        courseViewModel,
                        authViewModel,
                        navCourseId,
                        navNoteId,
                        onNavigateToCanvas = {
                            navController.navigate(Screen.CanvasIntegration.route)
                        },
                        navController = navController
                    )
                }
                composable(
                    Screen.Groups.route,
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) {
                    GroupScreen(
                        onGroupClick = { groupId ->
                            navController.navigate(Screen.GroupChat.createRoute(groupId))
                        }
                    )
                }
                composable(
                    Screen.Profile.route,
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        navController = navController
                    )
                }
                
                // Canvas Integration Screen
                composable(Screen.CanvasIntegration.route) {
                    CanvasIntegrationScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                // Sub-pages without transitions
                composable("notification_settings") {
                    NotificationsScreen(navController = navController)
                }
                composable("recently_opened") {
                    RecentlyOpenedScreen(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        courseViewModel = courseViewModel
                    )
                }

                // Temporary Quiz Generation Screen
                composable("temp_quiz") {
                    TempQuizGenerationScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }

                // Temporary Flashcards Generation Screen (for quick action feature)
                composable("temp_flashcards") {
                    TempFlashcardsGenerationScreen(navController = navController)
                }

                composable(
                    Screen.Games.route,
                    enterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) -1 else 1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        val initialIndex = screens.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = screens.indexOfFirst { it.route == targetState.destination.route }
                        val direction = if (targetIndex > initialIndex) 1 else -1
                        slideOutHorizontally(targetOffsetX = { fullWidth -> direction * fullWidth }, animationSpec = tween(300))
                    }
                ) {
                    GameScreen(navController = navController)
                }
                composable(
                    route = "game_lobby/{gameType}/{groupId}",
                    arguments = listOf(
                        navArgument("gameType") { type = NavType.StringType },
                        navArgument("groupId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val gameType = enumValueOf<GameType>(backStackEntry.arguments?.getString("gameType")!!)
                    val groupId = backStackEntry.arguments?.getString("groupId")!!
                    GameLobbyScreen(
                        navController = navController,
                        gameType = gameType,
                        authViewModel = authViewModel,
                        groupId = groupId
                    )
                }
                composable(
                    route = "game_play/{gameCode}",
                    arguments = listOf(
                        navArgument("gameCode") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val gameCode = backStackEntry.arguments?.getString("gameCode")!!
                    GamePlayScreen(
                        navController = navController,
                        gameCode = gameCode,
                        authViewModel = authViewModel
                    )
                }
                composable(
                    route = "leaderboard"
                ) {
                    // TODO: Implement LeaderboardScreen
                    // LeaderboardScreen(navController = navController)
                }
                // GroupChat without transitions
                composable(
            route = Screen.GroupChat.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupChatScreen(
                groupId = groupId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
            }
        }
    } else {
        // Create a fresh NavController for unauthenticated users
        val navController = androidx.navigation.compose.rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Screen.SignIn.route,
            modifier = modifier
        ) {
            composable(Screen.SignIn.route) {
                SignInScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onSignInSuccess = { /* handled by authViewModel */ },
                    viewModel = authViewModel
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToSignIn = { navController.popBackStack() },
                    onSignUpSuccess = { /* handled by authViewModel */ },
                    viewModel = authViewModel
                )
            }
        }
    }
}