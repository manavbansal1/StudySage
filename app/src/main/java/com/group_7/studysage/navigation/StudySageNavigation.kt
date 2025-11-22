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
import com.group_7.studysage.ui.screens.GameLobbyScreen
import com.group_7.studysage.ui.screens.GamePlayScreen
import com.group_7.studysage.ui.screens.GameScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupChatScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupScreen
import com.group_7.studysage.ui.screens.HomeScreen.HomeScreen
import com.group_7.studysage.ui.screens.ProfileScreen.NotificationsScreen
import com.group_7.studysage.ui.screens.ProfileScreen.PrivacyScreen
import com.group_7.studysage.ui.screens.ProfileScreen.ProfileScreen
import com.group_7.studysage.ui.screens.auth.SignInScreen
import com.group_7.studysage.ui.screens.auth.SignUpScreen
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.viewmodels.CourseViewModel
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.models.GameType
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
        val courseViewModel: CourseViewModel = viewModel()
        val screens = listOf(Screen.Home, Screen.Course, Screen.Groups, Screen.Games)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val selectedIndex = screens.indexOfFirst { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }

        // Collect fullscreen overlay state from courseViewModel
        val courseUiState by courseViewModel.uiState.collectAsState()

        val shouldHideBottomNav = currentDestination?.route?.startsWith("group_chat/") == true ||
                currentDestination?.route == "profile" ||
                currentDestination?.route == "privacy_settings" ||
                currentDestination?.route == "notification_settings" ||
                currentDestination?.route?.startsWith("game_") == true ||
                courseUiState.isShowingFullscreenOverlay // Hide nav when quiz/NFC screens are showing

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
                                                            inclusive = false
                                                        }
                                                        launchSingleTop = false
                                                        // Don't save or restore state to force refresh
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
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
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
                    CoursesScreen(courseViewModel, authViewModel)
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
                    CoursesScreen(courseViewModel, authViewModel, navCourseId, navNoteId)
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
                // Sub-pages without transitions
                composable("privacy_settings") {
                    PrivacyScreen(navController = navController)
                }
                composable("notification_settings") {
                    NotificationsScreen(navController = navController)
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
                    arguments = listOf(
                        navArgument("groupId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                    GroupChatScreen(
                        groupId = groupId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
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