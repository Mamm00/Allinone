package com.hyperlauncher.ai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

val HyperBackground = Color.White
val HyperSurface = Color.White
val HyperSurfaceVariant = Color(0xFFF1F3F5)
val HyperPrimary = Color(0xFF205C90)
val HyperOnPrimary = Color.White
val HyperOnBackground = Color(0xFF171B20)
val HyperOnSurface = HyperOnBackground
val HyperOnSurfaceVariant = Color(0xFF454D57)
val HyperError = Color(0xFFBA1A1A)
const val DEFAULT_ACCENT_SEED: Int = -14656368 // #205C90

/** Always white, even with system dark mode or a dark wallpaper. Wallpaper affects accents only. */
fun whiteColorScheme(seed: Int? = null): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed ?: DEFAULT_ACCENT_SEED, hsl)
    // A bounded dark accent keeps white button labels readable for every wallpaper hue.
    val accent = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], .55f, .29f)))
    return lightColorScheme(
        primary = accent, onPrimary = HyperOnPrimary,
        primaryContainer = Color(0xFFE8EFF8), onPrimaryContainer = Color(0xFF15334D),
        secondary = Color(0xFF465C71), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAF0F6), onSecondaryContainer = Color(0xFF253746),
        tertiary = Color(0xFF555486), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEEECFA), onTertiaryContainer = Color(0xFF302F53),
        background = HyperBackground, onBackground = HyperOnBackground,
        surface = HyperSurface, onSurface = HyperOnSurface,
        surfaceVariant = HyperSurfaceVariant, onSurfaceVariant = HyperOnSurfaceVariant,
        surfaceDim = Color(0xFFE8EBEF), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFBFC),
        surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color(0xFFF1F3F5), surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFEDF0F4), inverseOnSurface = HyperOnSurface,
        inversePrimary = accent,
        outline = Color(0xFF6E7781), outlineVariant = Color(0xFFD4DAE1),
        error = HyperError, onError = Color.White,
        errorContainer = Color(0xFFFFEDEA), onErrorContainer = Color(0xFF7D1515)
    )
}
