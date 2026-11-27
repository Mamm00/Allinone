package com.hyperlauncher.ai

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Executed in both system light and system dark modes by the device script. */
class WhiteThemeUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun assertWhiteWindow() {
        compose.waitForIdle()
        val pixels=compose.onNodeWithTag("app_surface").captureToImage().toPixelMap()
        assertEquals(Color.White,pixels[2,2])
        compose.runOnIdle {
            val controller=androidx.core.view.WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView)
            org.junit.Assert.assertTrue(controller.isAppearanceLightStatusBars)
            org.junit.Assert.assertTrue(controller.isAppearanceLightNavigationBars)
        }
    }
    @Test fun homeSettingsDrawerVaultAndRecreationRemainWhite() {
        compose.waitUntil(15000) {compose.onAllNodesWithText("All apps").fetchSemanticsNodes().isNotEmpty()}
        assertWhiteWindow()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Search preferences").assertExists()
        assertWhiteWindow()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Search preferences").assertExists()
        assertWhiteWindow()
        // A singleTop launcher HOME intent must leave Settings and return to home.
        compose.runOnIdle {
            compose.activity.startActivity(Intent(compose.activity,MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        compose.waitUntil(15000) {compose.onAllNodesWithText("All apps").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("All apps").performClick()
        compose.onNodeWithTag("drawer_search").assertExists()
        assertWhiteWindow()
        compose.onNodeWithText("Vault").performClick()
        compose.onNodeWithText("AI Notes Vault").assertExists()
        assertWhiteWindow()
        val passwords=compose.onAllNodes(isEditable() and SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Password)).fetchSemanticsNodes()
        org.junit.Assert.assertTrue("Vault passphrases must be masked",passwords.isNotEmpty())
    }
}
