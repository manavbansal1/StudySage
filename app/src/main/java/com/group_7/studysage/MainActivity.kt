package com.group_7.studysage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.navigation.StudySageNavigation
import com.group_7.studysage.ui.theme.StudySageTheme
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.viewmodels.AuthViewModelFactory
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import com.group_7.studysage.utils.PermissionHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize & request all permissions at startup
        PermissionHandler.init(this)
        PermissionHandler.requestAllPermissions(this)

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

private fun PermissionHandler.init(activity: MainActivity) {}


@Composable
fun StudySageApp() {
    val authRepository = remember { AuthRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    StudySageNavigation(
        authViewModel = authViewModel,
        modifier = Modifier
    )
}