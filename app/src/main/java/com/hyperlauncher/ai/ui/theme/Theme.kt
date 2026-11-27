package com.hyperlauncher.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = HyperPrimary,
    onPrimary = HyperOnPrimary,
    background = HyperBackground,
    onBackground = HyperOnBackground,
    surface = HyperSurface,
    onSurface = HyperOnSurface,
    surfaceVariant = HyperSurfaceVariant,
    onSurfaceVariant = HyperOnSurfaceVariant,
    error = HyperError
)

@Composable
fun HyperLauncherAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = HyperTypography,
        content = content
    )
}
