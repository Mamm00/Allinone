package com.hyperlauncher.ai.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlin.math.*
import androidx.compose.ui.unit.dp

/** Observe the final pass: widget scrolling and icon gestures retain first refusal. */
fun Modifier.homeGestures(enabled: Boolean,onGesture: (HomeGesture,Offset)->Unit): Modifier =
    if(!enabled) this else pointerInput(onGesture) {
        var previousTap=0L
        var previousPosition=Offset.Zero
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            val start=down.position
            var last=start; var consumed=false; var multi=false
            var firstVector: Offset?=null; var lastVector: Offset?=null
            do {
                val event=awaitPointerEvent(PointerEventPass.Final)
                if(event.changes.any {it.isConsumed}) consumed=true
                val pressed=event.changes.filter {it.pressed}
                if(pressed.size>=2) {
                    multi=true
                    val vector=pressed[1].position-pressed[0].position
                    if(firstVector==null && vector.getDistance()>20f) firstVector=vector
                    lastVector=vector
                }
                event.changes.firstOrNull {it.id==down.id}?.let {last=it.position}
            } while(event.changes.any {it.pressed})
            if(multi) {
                val a=firstVector; val b=lastVector
                if(a!=null && b!=null && a.getDistance()>20f) {
                    val zoom=b.getDistance()/a.getDistance()
                    val angle=GestureMath.rotationDegrees(a.x,a.y,b.x,b.y)
                    when {
                        abs(angle)>28f -> onGesture(if(angle>0) HomeGesture.ROTATE_RIGHT else HomeGesture.ROTATE_LEFT,start)
                        zoom<.78f -> onGesture(HomeGesture.PINCH_IN,start)
                        zoom>1.28f -> onGesture(HomeGesture.PINCH_OUT,start)
                    }
                }
            } else if(!consumed) {
                val delta=last-start
                if(abs(delta.y)>70.dp.toPx() && abs(delta.y)>abs(delta.x)*1.4f)
                    onGesture(if(delta.y>0) HomeGesture.DOWN else HomeGesture.UP,start)
                else if(delta.getDistance()<18.dp.toPx()) {
                    if(down.uptimeMillis-previousTap in 1..300 && (start-previousPosition).getDistance()<50.dp.toPx()) {
                        onGesture(HomeGesture.DOUBLE_TAP,start); previousTap=0L
                    } else {previousTap=down.uptimeMillis;previousPosition=start}
                }
            }
        }
    }
