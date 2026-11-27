package com.hyperlauncher.ai.ui.workspace

object DisplaySetup {
    /** Window width is not part of identity: split-screen resizing must not erase a layout. */
    fun key(displayId: Int,landscape: Boolean,physicalWidthDp: Int,physicalHeightDp: Int,posture: String): String {
        val panel=if(minOf(physicalWidthDp,physicalHeightDp)>=600) "large" else "small"
        val orientation=if(landscape) "landscape" else "portrait"
        return "display_${displayId}_${orientation}_${panel}_${posture}"
    }
}
