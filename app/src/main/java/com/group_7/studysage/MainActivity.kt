package com.group_7.studysage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.group_7.studysage.ui.theme.StudySageTheme
import com.group_7.studysage.navigation.StudySageNavigation
import com.group_7.studysage.ui.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val authViewModel: AuthViewModel = viewModel()

    StudySageNavigation(
        navController = navController,
        authViewModel = authViewModel
    )
}

@Preview(showBackground = true)
@Composable
fun StudySageAppPreview() {
    StudySageTheme {
        StudySageApp()
    }
}
