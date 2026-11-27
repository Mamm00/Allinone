package com.hyperlauncher.ai.ui.workspace

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator

/** Use a safe single pane when a separating hinge bisects the window. */
data class AdaptiveSetup(val key: String,val safePane: PaddingValues)
@Composable
fun rememberAdaptiveSetup(): AdaptiveSetup {
    val context=LocalContext.current
    val activity=context as? Activity
    val config=LocalConfiguration.current
    val density=LocalDensity.current.density
    val fold by produceState<FoldingFeature?>(null,activity) {
        if(activity!=null) WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect {info ->
            value=info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
        }
    }
    val display=if(Build.VERSION.SDK_INT>=30) activity?.display?.displayId ?: 0 else activity?.windowManager?.defaultDisplay?.displayId ?: 0
    val maximum=activity?.let {WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(it).bounds}
    val width=(maximum?.width()?.div(density))?.toInt() ?: config.screenWidthDp
    val height=(maximum?.height()?.div(density))?.toInt() ?: config.screenHeightDp
    val posture=when {fold?.state==FoldingFeature.State.HALF_OPENED -> "halfopen";else -> "flat"}
    val key=DisplaySetup.key(display,config.screenWidthDp>config.screenHeightDp,width,height,posture)
    val bounds=fold?.bounds
    val padding=if(fold?.isSeparating==true && bounds!=null) {
        if(fold?.orientation==FoldingFeature.Orientation.VERTICAL) {
            val end=(config.screenWidthDp-bounds.left/density).coerceIn(0f,config.screenWidthDp*.7f)
            PaddingValues(end=end.dp)
        } else {
            val bottom=(config.screenHeightDp-bounds.top/density).coerceIn(0f,config.screenHeightDp*.7f)
            PaddingValues(bottom=bottom.dp)
        }
    } else PaddingValues(0.dp)
    return AdaptiveSetup(key,padding)
}
