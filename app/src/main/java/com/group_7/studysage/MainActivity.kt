package com.group_7.studysage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.navigation.StudySageNavigation
import com.group_7.studysage.ui.theme.StudySageTheme
import com.group_7.studysage.ui.screens.auth.AuthViewModel
import com.group_7.studysage.ui.screens.auth.AuthViewModelFactory
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            StudySageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudySageApp()
                }
            }
        }
    }
}

@Composable
fun StudySageApp() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    StudySageNavigation(
        navController = navController,
        authViewModel = authViewModel,
        authRepository = authRepository,
        modifier = Modifier.padding(top = 26.dp)
    )
}