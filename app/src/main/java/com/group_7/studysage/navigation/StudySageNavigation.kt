package com.group_7.studysage.navigation

import com.group_7.studysage.ui.screens.CoursesScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.group_7.studysage.ui.screens.*
import com.group_7.studysage.ui.screens.auth.SignInScreen
import com.group_7.studysage.ui.screens.auth.SignUpScreen
import com.group_7.studysage.ui.viewmodels.AuthViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object SignIn : Screen("sign_in", "Sign In")
    object SignUp : Screen("sign_up", "Sign Up")
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Course : Screen("course", "Course", Icons.Filled.Book)
    object Groups : Screen("groups", "Groups", Icons.Filled.Groups)
    object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    object GroupChat : Screen("group_chat/{groupId}", "Group Chat") {
        fun createRoute(groupId: String) = "group_chat/$groupId"
    }
}

@Composable
fun StudySageNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val isUserSignedIn by authViewModel.isSignedIn

    if (isUserSignedIn) {
        val screens = listOf(Screen.Home, Screen.Course, Screen.Groups, Screen.Profile)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Check if we're on a detail screen (like GroupChat)
        val isDetailScreen = currentDestination?.route?.startsWith("group_chat/") == true

        Scaffold(
            bottomBar = {
                // Only show bottom navigation if we're not on a detail screen
                if (!isDetailScreen) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        containerColor = Color(0xFF2D1B4E).copy(alpha = 0.75f),  // Frosted glass effect
                        contentColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(
                                            it,
                                            screen.title,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,  // White icon when selected
                                    selectedTextColor = Color.White,  // White text when selected
                                    unselectedIconColor = Color(0xFFB0B0C0),  // Gray when unselected
                                    unselectedTextColor = Color(0xFFB0B0C0),  // Gray when unselected
                                    indicatorColor = Color(0xFF9333EA).copy(alpha = 0.3f)  // Purple glow background
                                )
                            )
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