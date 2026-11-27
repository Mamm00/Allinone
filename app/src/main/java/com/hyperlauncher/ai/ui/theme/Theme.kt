package com.hyperlauncher.ai.ui.theme

import com.hyperlauncher.ai.ui.appearance.*
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HyperLauncherAITheme(ambient: Boolean = true, appearance: Map<String,String> = emptyMap(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { WallpaperManager.getInstance(context) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(manager, ambient) {
        val listener = WallpaperManager.OnColorsChangedListener { _, _ -> revision++ }
        if (ambient) runCatching { manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
        onDispose { if (ambient) runCatching { manager.removeOnColorsChangedListener(listener) } }
    }
    // No wallpaper bitmap permission; OEMs may deny colors, in which case use a safe seed.
    val seed by produceState(DEFAULT_ACCENT_SEED, revision, ambient) {
        value = if (ambient) withContext(Dispatchers.IO) {
            runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }
                .getOrNull() ?: DEFAULT_ACCENT_SEED
        } else DEFAULT_ACCENT_SEED
    }
    val scheme = remember(ambient, seed) { whiteColorScheme(if (ambient) seed else null) }
    val uiFont=appearanceFont(appearance)
    val clockFont=appearanceFont(appearance,true)
    val typography=HyperTypography.copy(
        displayLarge=HyperTypography.displayLarge.withAppearance(uiFont,appearance),
        displayMedium=HyperTypography.displayMedium.withAppearance(uiFont,appearance),
        displaySmall=HyperTypography.displaySmall.withAppearance(uiFont,appearance),
        headlineLarge=HyperTypography.headlineLarge.withAppearance(uiFont,appearance),
        headlineMedium=HyperTypography.headlineMedium.withAppearance(uiFont,appearance),
        headlineSmall=HyperTypography.headlineSmall.withAppearance(uiFont,appearance),
        titleLarge=HyperTypography.titleLarge.withAppearance(uiFont,appearance),
        titleMedium=HyperTypography.titleMedium.withAppearance(uiFont,appearance),
        titleSmall=HyperTypography.titleSmall.withAppearance(uiFont,appearance),
        bodyLarge=HyperTypography.bodyLarge.withAppearance(uiFont,appearance),
        bodyMedium=HyperTypography.bodyMedium.withAppearance(uiFont,appearance),
        bodySmall=HyperTypography.bodySmall.withAppearance(uiFont,appearance),
        labelLarge=HyperTypography.labelLarge.withAppearance(uiFont,appearance),
        labelMedium=HyperTypography.labelMedium.withAppearance(uiFont,appearance),
        labelSmall=HyperTypography.labelSmall.withAppearance(uiFont,appearance)
    )
    CompositionLocalProvider(LocalAppearance provides appearance,LocalClockFont provides clockFont) {
        MaterialTheme(colorScheme=scheme,typography=typography) {
            // MaterialTheme supplies colors, not pixels. Without this, Settings/Vault
            // inherited the platform's black window behind their transparent Columns.
            Surface(Modifier.fillMaxSize().testTag("app_surface"),color=scheme.background,
                contentColor=scheme.onBackground,content=content)
        }
    }
}
