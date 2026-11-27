package com.hyperlauncher.ai

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperlauncher.glass.GlassView
import org.junit.Rule
import org.junit.Test

class GlassRenderingTest {
    @get:Rule val compose=createComposeRule()
    @Test fun liquidShaderCompilesAndRendersWithoutFrameLoop() {
        val bitmap=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.CYAN)}
        compose.setContent {AndroidView(factory={GlassView(it)},modifier=Modifier.size(180.dp),update={it.configure(bitmap,16f,.5f,.2f)})}
        compose.waitForIdle()
    }
}
