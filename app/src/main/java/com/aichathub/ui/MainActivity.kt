package com.aichathub.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aichathub.ui.screens.APIKeyScreen
import com.aichathub.ui.screens.ChatScreen
import com.aichathub.ui.screens.SettingsScreen
import com.aichathub.ui.theme.AIChatHubTheme
import com.aichathub.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            
            AIChatHubTheme(
                darkTheme = settingsState.settings.isDarkMode
            ) {
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
}

@Composable
fun AIChatHubApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToAPIKeys = {
                    navController.navigate(Screen.APIKey.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.APIKey.route) {
            APIKeyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}