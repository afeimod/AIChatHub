package com.aichathub.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aichathub.ui.screens.*
import com.aichathub.ui.theme.AIChatHubTheme
import com.aichathub.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            AIChatHubTheme(darkTheme = settingsState.settings.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AIChatHubApp()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object APIKey : Screen("api_key")
    object Settings : Screen("settings")
    object Terminal : Screen("terminal")
    object Workspace : Screen("workspace")
    object CustomProvider : Screen("custom_provider")
}

@Composable
fun AIChatHubApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Chat.route) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToAPIKeys = { navController.navigate(Screen.APIKey.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) },
                onNavigateToWorkspace = { navController.navigate(Screen.Workspace.route) }
            )
        }
        composable(Screen.APIKey.route) {
            APIKeyScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCustomProviders = { navController.navigate(Screen.CustomProvider.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCustomProviders = { navController.navigate(Screen.CustomProvider.route) }
            )
        }
        composable(Screen.Terminal.route) {
            TerminalScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Workspace.route) {
            WorkspaceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CustomProvider.route) {
            CustomProviderScreen(onBack = { navController.popBackStack() })
        }
    }
}
