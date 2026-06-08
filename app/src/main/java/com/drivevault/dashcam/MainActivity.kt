package com.drivevault.dashcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.permissions.PermissionManager
import kotlinx.coroutines.launch
import com.drivevault.dashcam.ui.navigation.Screen
import com.drivevault.dashcam.ui.screens.*
import com.drivevault.dashcam.ui.theme.DriveVaultTheme
import com.drivevault.dashcam.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = SettingsRepository(this)

        setContent {
            DriveVaultTheme {
                val navController = rememberNavController()
                val recordingViewModel = remember { RecordingViewModel(application) }
                val clipLibraryViewModel = remember { ClipLibraryViewModel(application) }
                val settingsViewModel = remember { SettingsViewModel(application) }

                var startDestination by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    settings.onboardingComplete.collect { complete ->
                        startDestination = when {
                            !complete -> Screen.Onboarding.route
                            !PermissionManager.areAllRequiredGranted(this@MainActivity) -> Screen.Permissions.route
                            else -> Screen.Recording.route
                        }
                    }
                }

                if (startDestination != null) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onComplete = {
                                    scope.launch {
                                        settings.setOnboardingComplete(true)
                                    }
                                    navController.navigate(Screen.Permissions.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Permissions.route) {
                            PermissionsScreen(
                                onAllGranted = {
                                    navController.navigate(Screen.Recording.route) {
                                        popUpTo(Screen.Permissions.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Recording.route) {
                            RecordingScreen(
                                viewModel = recordingViewModel,
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToLibrary = { navController.navigate(Screen.ClipLibrary.route) }
                            )
                        }

                        composable(Screen.ClipLibrary.route) {
                            ClipLibraryScreen(
                                viewModel = clipLibraryViewModel,
                                onNavigateToClip = { clipId ->
                                    navController.navigate(Screen.ClipDetail.createRoute(clipId))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.ClipDetail.route,
                            arguments = listOf(navArgument("clipId") { type = NavType.LongType })
                        ) { entry ->
                            val clipId = entry.arguments?.getLong("clipId") ?: return@composable
                            val clipDetailViewModel = remember { ClipDetailViewModel(application) }
                            ClipDetailScreen(
                                clipId = clipId,
                                viewModel = clipDetailViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
