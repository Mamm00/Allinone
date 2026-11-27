package com.hyperlauncher.ai.ui.gestures

import kotlin.math.*
enum class HomeGesture { UP, DOWN, DOUBLE_TAP, PINCH_IN, PINCH_OUT, ROTATE_LEFT, ROTATE_RIGHT }
object GestureMath {
    fun rotationDegrees(x1: Float,y1: Float,x2: Float,y2: Float): Float {
        var delta = Math.toDegrees((atan2(y2,x2)-atan2(y1,x1)).toDouble()).toFloat()
        while(delta>180f) delta-=360f
        while(delta< -180f) delta+=360f
        return delta
    }
    fun direction(x: Float,y: Float): String = if(abs(x)>abs(y)) { if(x>0) "RIGHT" else "LEFT" } else if(y>0) "DOWN" else "UP"
    fun defaultAction(gesture: HomeGesture): String = when(gesture) {
        HomeGesture.UP -> "drawer"; HomeGesture.DOWN -> "notifications"
        HomeGesture.DOUBLE_TAP -> "none"; HomeGesture.PINCH_IN -> "edit"
        HomeGesture.PINCH_OUT -> "widgets"; else -> "none"
    }
    fun iconAction(kind: String,direction: String,configured: String?): String =
        if(kind=="folder" && direction=="UP") "folder" else configured ?: "none"
}
