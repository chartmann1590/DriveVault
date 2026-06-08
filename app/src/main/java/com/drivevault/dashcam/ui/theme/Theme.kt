package com.drivevault.dashcam.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SafetyRed,
    onPrimary = SafetyRedContainer,
    primaryContainer = SafetyRedBright,
    onPrimaryContainer = Color(0xFF5C0002),
    inversePrimary = SafetyRedDeep,
    secondary = SecondaryNeutral,
    onSecondary = Color(0xFF313030),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = ElectricBlue,
    onTertiary = ElectricBlueContainer,
    tertiaryContainer = ElectricBlueBright,
    onTertiaryContainer = Color(0xFF00285C),
    error = SafetyRed,
    onError = SafetyRedContainer,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DeepCharcoal,
    onBackground = OnSurface,
    surface = DeepCharcoal,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceTint = SurfaceTint,
)

@Composable
fun DriveVaultTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepCharcoal.toArgb()
            window.navigationBarColor = DeepCharcoal.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DriveVaultTypography,
        content = content
    )
}
