package com.hyperlauncher.ai
import com.hyperlauncher.ai.ui.workspace.DisplaySetup
import org.junit.Assert.*
import org.junit.Test
class DisplaySetupTest {
    @Test fun displayOrientationAndPanelsAreIndependent() {
        val portrait=DisplaySetup.key(0,false,400,900,"flat")
        assertNotEquals(portrait,DisplaySetup.key(0,true,900,400,"flat"))
        assertNotEquals(portrait,DisplaySetup.key(1,false,400,900,"flat"))
        assertNotEquals(portrait,DisplaySetup.key(0,false,800,900,"flat"))
        assertNotEquals(portrait,DisplaySetup.key(0,false,400,900,"halfopen"))
        assertEquals(portrait,DisplaySetup.key(0,false,400,900,"flat"))
    }
}
