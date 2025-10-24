package com.group_7.studysage.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    object Notes : Screen("notes", "Notes", Icons.Filled.Description)
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
        val screens = listOf(Screen.Home, Screen.Notes, Screen.Groups, Screen.Profile)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Check if we're on a detail screen (like GroupChat)
        val isDetailScreen = currentDestination?.route?.startsWith("group_chat/") == true

        Scaffold(
            bottomBar = {
                // Only show bottom navigation if we're not on a detail screen
                if (!isDetailScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { screen.icon?.let { Icon(it, screen.title) } },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                    HomeScreen()
                }

                composable(Screen.Notes.route) {
                    NotesScreen()
                }

                composable(Screen.Groups.route) {
                    GroupScreen(
                        onGroupClick = { groupId ->
                            navController.navigate(Screen.GroupChat.createRoute(groupId))
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(authRepository = authRepository)
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