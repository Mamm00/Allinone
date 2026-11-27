package com.hyperlauncher.ai

import com.hyperlauncher.ai.ui.workspace.*
import org.junit.Assert.*
import org.junit.Test

class WorkspaceTest {
    @Test fun pagesWrapWithoutDuplicatingLayoutData() {
        assertEquals(0, CircularPages.wrap(3,3))
        assertEquals(2, CircularPages.wrap(-1,3))
        assertEquals(1, CircularPages.shortestDelta(2,0,3))
        assertEquals(-1, CircularPages.shortestDelta(0,2,3))
        assertEquals(0, CircularPages.logical(CircularPages.initial(3),3))
        for (count in 1..9) for (from in 0 until count) for (to in 0 until count) {
            val delta = CircularPages.shortestDelta(from,to,count)
            assertEquals(to, CircularPages.wrap(from+delta,count))
            assertTrue(kotlin.math.abs(delta) <= (count+1)/2)
        }
    }
    @Test fun placementBoundsAndRoundTrip() {
        val item=Placement(id="test", x=-1f, y=4f,width=.4f,height=.3f,widgets=listOf(2,2,-1,4)).bounded()
        assertEquals(0f,item.x,.001f); assertEquals(.7f,item.y,.001f)
        assertEquals(listOf(2,4),item.widgets)
        val layout=WorkspaceLayout(items=listOf(item))
        assertEquals(layout,WorkspaceCodec.decode(WorkspaceCodec.encode(layout)))
    }
    @Test fun malformedCoordinatesAreClamped() {
        val p=Placement(x=Float.NaN,width=Float.POSITIVE_INFINITY).bounded()
        assertTrue(p.x.isFinite()); assertTrue(p.width in .12f..1f)
    }
}
