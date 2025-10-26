package com.group_7.studysage.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.group_7.studysage.ui.screens.CoursesScreen
import com.group_7.studysage.ui.screens.GroupChatScreen
import com.group_7.studysage.ui.screens.GroupScreen
import com.group_7.studysage.ui.screens.HomeScreen
import com.group_7.studysage.ui.screens.ProfileScreen
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
            containerColor = Color.Transparent,
            bottomBar = {
                // Only show bottom navigation if we're not on a detail screen
                if (!isDetailScreen) {

                    // Find the currently selected index
                    val selectedIndex = screens.indexOfFirst { screen ->
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(36.dp)),
                        containerColor = Color(0xFF2D1B4E).copy(alpha = 0.75f),
                        contentColor = Color.White,
                        divider = {},

                        // --- CHANGES ARE HERE ---
                        indicator = { tabPositions ->
                            if (selectedIndex >= 0 && selectedIndex < tabPositions.size) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedIndex])
                                        .fillMaxHeight()
                                        // 1. Reduced padding to make pill bigger
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .background(
                                            color = Color.White.copy(alpha = 0.2f),
                                            // 2. Adjusted radius to match new padding
                                            shape = RoundedCornerShape(30.dp)
                                        )
                                )
                            }
                        }
                        // --- END OF CHANGES ---

                    ) {
                        screens.forEachIndexed { index, screen ->
                            Tab(
                                selected = (selectedIndex == index),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    screen.icon?.let {
                                        Icon(
                                            it,
                                            screen.title,
                                            // 3. Made icon smaller
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        screen.title,
                                        // 4. Made text smaller
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color(0xFFB0B0C0)
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