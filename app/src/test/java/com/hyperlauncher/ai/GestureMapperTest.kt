package com.hyperlauncher.ai
import com.hyperlauncher.ai.ui.gestures.*
import org.junit.Assert.*
import org.junit.Test
class GestureMapperTest {
    @Test fun rotationUsesTwoPointerVectorAndWrapsAt180() {
        assertEquals(90f,GestureMath.rotationDegrees(1f,0f,0f,1f),.01f)
        assertEquals(-90f,GestureMath.rotationDegrees(1f,0f,0f,-1f),.01f)
        assertTrue(kotlin.math.abs(GestureMath.rotationDegrees(-1f,.01f,-1f,-.01f))<2f)
    }
    @Test fun folderUpCannotBeOverridden() {
        assertEquals("folder",GestureMath.iconAction("folder","UP","screenOff"))
        assertEquals("search",GestureMath.iconAction("app","RIGHT","search"))
        assertEquals("none",GestureMath.defaultAction(HomeGesture.DOUBLE_TAP))
        assertEquals("DOWN",GestureMath.direction(1f,30f))
    }
}
