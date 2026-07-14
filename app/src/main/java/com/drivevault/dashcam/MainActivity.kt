package com.drivevault.dashcam

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivevault.dashcam.ads.AdManager
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.permissions.PermissionManager
import kotlinx.coroutines.launch
import com.drivevault.dashcam.ui.navigation.Screen
import com.drivevault.dashcam.ui.screens.*
import com.drivevault.dashcam.ui.theme.DriveVaultTheme
import com.drivevault.dashcam.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private val recordingViewModel: RecordingViewModel by viewModels()
    private val clipLibraryViewModel: ClipLibraryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val feedbackViewModel: FeedbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = SettingsRepository(this)

        setContent {
            DriveVaultTheme {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val adsSuppressed by AdManager.adsSuppressed.collectAsState()
                var interstitialCounter by remember { mutableIntStateOf(0) }

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

                // Ads never appear on the Recording screen itself — only on
                // secondary screens (library, clip detail, settings) — and are
                // fully suppressed while a recording is actively in progress,
                // even if the user navigates away to a screen that would
                // otherwise show one.
                val showBanner = currentRoute != null &&
                    currentRoute != Screen.Recording.route &&
                    currentRoute != Screen.Onboarding.route &&
                    currentRoute != Screen.Permissions.route &&
                    !adsSuppressed

                if (startDestination != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                            modifier = Modifier.weight(1f).fillMaxWidth()
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
                                // Offer an interstitial only when leaving the Recording screen
                                // while nothing is actively being recorded, and only every third
                                // time to avoid nagging the user.
                                DisposableEffect(Unit) {
                                    onDispose {
                                        if (!AdManager.adsSuppressed.value) {
                                            interstitialCounter++
                                            if (interstitialCounter % 3 == 0) {
                                                AdManager.showInterstitial(this@MainActivity)
                                            }
                                        }
                                    }
                                }
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
                                val clipDetailViewModel: ClipDetailViewModel = viewModel()
                                ClipDetailScreen(
                                    clipId = clipId,
                                    viewModel = clipDetailViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    feedbackViewModel = feedbackViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }

                        if (showBanner) {
                            AndroidView(
                                factory = { ctx ->
                                    AdManager.createBannerView(
                                        ctx as Activity,
                                        BuildConfig.ADMOB_BANNER_ID
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AdManager.resume()
    }

    override fun onPause() {
        AdManager.pause()
        super.onPause()
    }

    override fun onDestroy() {
        AdManager.destroy()
        super.onDestroy()
    }
}
