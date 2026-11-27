package com.hyperlauncher.ai.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HyperLauncherAITheme(ambient: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val manager = remember(context) { WallpaperManager.getInstance(context) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(manager) {
        val listener = WallpaperManager.OnColorsChangedListener { _, _ -> revision++ }
        runCatching { manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { manager.removeOnColorsChangedListener(listener) } }
    }
    // No wallpaper bitmap permission; OEMs may deny colors, in which case use a safe seed.
    val seed by produceState(0xff397a91.toInt(), revision, ambient) {
        if (ambient) value = withContext(Dispatchers.IO) {
            runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }
                .getOrNull() ?: 0xff397a91.toInt()
        }
    }
    val scheme = remember(dark, ambient, seed, revision) {
        when {
            ambient && Build.VERSION.SDK_INT >= 31 ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            ambient -> {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(seed, hsl)
                fun tone(lightness: Float, saturation: Float = 0.22f): Color =
                    Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], saturation, lightness)))
                if (dark) darkColorScheme(primary = tone(.78f, .45f),
                    background = tone(.08f), surface = tone(.11f), surfaceVariant = tone(.2f),
                    onBackground = tone(.94f), onSurface = tone(.94f), onSurfaceVariant = tone(.84f),
                    primaryContainer = tone(.28f), onPrimaryContainer = tone(.95f), onPrimary = tone(.12f))
                else lightColorScheme(primary = tone(.32f, .5f), background = tone(.97f),
                    surface = tone(.95f), surfaceVariant = tone(.89f), onSurface = tone(.12f),
                    onBackground = tone(.12f), onSurfaceVariant = tone(.28f),
                    primaryContainer = tone(.86f), onPrimaryContainer = tone(.14f), onPrimary = Color.White)
            }
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
    }
    MaterialTheme(colorScheme = scheme, typography = HyperTypography, content = content)
}
