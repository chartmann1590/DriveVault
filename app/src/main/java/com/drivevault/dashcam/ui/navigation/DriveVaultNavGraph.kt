package com.drivevault.dashcam.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Permissions : Screen("permissions")
    data object Recording : Screen("recording")
    data object ClipLibrary : Screen("clip_library")
    data object ClipDetail : Screen("clip_detail/{clipId}") {
        fun createRoute(clipId: Long) = "clip_detail/$clipId"
    }
    data object Settings : Screen("settings")
}
