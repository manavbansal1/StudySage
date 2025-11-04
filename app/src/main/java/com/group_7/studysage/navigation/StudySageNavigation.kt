package com.group_7.studysage.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.ui.screens.CourseScreen.CoursesScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupChatScreen
import com.group_7.studysage.ui.screens.GroupsScreen.GroupScreen
import com.group_7.studysage.ui.screens.HomeScreen.HomeScreen
import com.group_7.studysage.ui.screens.ProfileScreen.NotificationsScreen
import com.group_7.studysage.ui.screens.ProfileScreen.PrivacyScreen
import com.group_7.studysage.ui.screens.ProfileScreen.ProfileScreen
import com.group_7.studysage.ui.screens.auth.SignInScreen
import com.group_7.studysage.ui.screens.auth.SignUpScreen
import com.group_7.studysage.ui.screens.auth.AuthViewModel
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
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudySageNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val isUserSignedIn by authViewModel.isSignedIn

    if (isUserSignedIn) {
        // Keep only 4 tabs in bottom nav (NO Profile)
        val screens = listOf(Screen.Home, Screen.Course, Screen.Groups, Screen.Games)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Hide bottom nav for GroupChat, Profile, Privacy, and Notifications screens
        val shouldHideBottomNav = currentDestination?.route?.startsWith("group_chat/") == true ||
                                  currentDestination?.route == "profile" ||
                                  currentDestination?.route == "privacy_settings" ||
                                  currentDestination?.route == "notification_settings"

        Scaffold(
            // This now correctly gets the new background from your theme
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Show bottom nav everywhere except GroupChat, Profile, Privacy, and Notifications
                if (!shouldHideBottomNav) {

                    // Find the currently selected index from the 4-tab list
                    val selectedIndex = screens.indexOfFirst { screen ->
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }

                    // --- APPLY THE NEW THEME ---
                    // Check if the app is in dark mode to pick the right nav colors
                    val isDark = isSystemInDarkTheme()
                    val navContainerColor = if (isDark) DarkNavContainer else LightNavContainer
                    val navIndicatorColor = if (isDark) DarkNavIndicator else LightNavIndicator
                    val navSelectedColor = if (isDark) DarkNavSelected else LightNavSelected
                    val navUnselectedColor = if (isDark) DarkNavUnselected else LightNavUnselected

                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        TabRow(
                            selectedTabIndex = selectedIndex.takeIf { it >= 0 } ?: 0,
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(36.dp)),
                            containerColor = navContainerColor, // Use themed color
                            contentColor = navSelectedColor,  // Use themed color
                            divider = {},
                            indicator = { tabPositions ->
                                // Only show indicator if one of the 4 tabs is selected
                                if (selectedIndex >= 0 && selectedIndex < tabPositions.size) {
                                    Box(
                                        modifier = Modifier
                                            .tabIndicatorOffset(tabPositions[selectedIndex])
                                            .fillMaxHeight()
                                            .padding(vertical = 6.dp, horizontal = 5.dp)
                                            .background(
                                                color = navIndicatorColor, // Use themed color
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
                                        // Always navigate without checking current route
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
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
                                    selectedContentColor = navSelectedColor,   // Use themed color
                                    unselectedContentColor = navUnselectedColor, // Use themed color
                                    interactionSource = interactionSource,
                                    modifier = Modifier.indication(interactionSource, null)
                                )
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
                modifier = Modifier.padding(innerPadding)  // Add padding for bottom nav
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController)
                }

                composable(Screen.Course.route) {
                    CoursesScreen()
                }

                composable(Screen.Groups.route) {
                    GroupScreen(
                        onGroupClick = { groupId ->
                            navController.navigate(Screen.GroupChat.createRoute(groupId))
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        authRepository = authRepository,
                        navController = navController
                    )
                }

                // Privacy Settings Screen
                composable("privacy_settings") {
                    PrivacyScreen(navController = navController)
                }

                // Notification Settings Screen
                composable("notification_settings") {
                    NotificationsScreen(navController = navController)
                }

                composable(Screen.Games.route) {

                }

                // Group Chat Screen with groupId parameter
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
        // Auth screens (Sign In / Sign Up)
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