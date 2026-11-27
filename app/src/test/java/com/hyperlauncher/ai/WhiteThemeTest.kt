package com.hyperlauncher.ai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hyperlauncher.ai.ui.theme.whiteColorScheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class WhiteThemeTest {
    @Test fun everyWallpaperHueKeepsSurfacesWhiteAndTextReadable() {
        val seeds=(0..350 step 10).map {ColorUtils.HSLToColor(floatArrayOf(it.toFloat(),1f,.5f))}+listOf(null,0xff000000.toInt(),0xffffffff.toInt())
        for(seed in seeds) {
            val colors=whiteColorScheme(seed)
            assertEquals(Color.White,colors.background)
            assertEquals(Color.White,colors.surface)
            assertEquals(Color.White,colors.surfaceContainerHigh)
            assertEquals(Color.White,colors.surfaceContainer)
            val pairs=listOf(colors.onBackground to colors.background,colors.onSurface to colors.surface,
                colors.onSurfaceVariant to colors.surfaceVariant,colors.onPrimary to colors.primary,
                colors.onSecondary to colors.secondary,colors.onError to colors.error,
                colors.inverseOnSurface to colors.inverseSurface)
            pairs.forEach {(text,background) ->
                assertTrue("Unreadable text with seed $seed: $text on $background",ColorUtils.calculateContrast(text.toArgb(),background.toArgb())>=4.5)
            }
        }
    }
}
