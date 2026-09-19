package com.thermalprinter.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OrangeLightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = TextOnOrange,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = PrimaryOrangeDark,
    secondary = PrimaryOrangeLight,
    onSecondary = TextOnOrange,
    secondaryContainer = OrangeSubtle,
    onSecondaryContainer = PrimaryOrangeDark,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderSubtle,
    error = ErrorRed,
    onError = TextOnOrange,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

@Composable
fun AndroidThermalPrinterTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = OrangeLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = LightBackground.toArgb()
            window.navigationBarColor = LightBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
