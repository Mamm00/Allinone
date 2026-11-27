package com.hyperlauncher.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperlauncher.ai.data.local.DrawerPreferences
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import com.hyperlauncher.ai.ui.launcher.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerUiTest {
    @get:Rule val compose = createComposeRule()
    private val apps = listOf(AppInfo("mail", "Mail", "messaging"), AppInfo("maps", "Maps", "travel"))
    private fun content(dark: Boolean) {
        compose.setContent {
            var query by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("all") }
            var selected by remember { mutableStateOf<AppInfo?>(null) }
            val state = LauncherUiState(apps = filterDrawerApps(apps, query, category, false), allApps = apps,
                categories = listOf(Category("messaging", "Communication", 0), Category("travel", "Travel", 1)),
                selectedCategoryId = category, searchQuery = query, isLoading = false,
                preferences = DrawerPreferences())
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                DrawerContent(state, { query = it }, { category = it }, {}, { selected = it }, {}, {}, {})
                selected?.let { ChangeCategoryDialog(it, state, { selected = null }) { _, _ -> selected = null } }
            }
        }
    }
    @Test fun drawerSearchAndCategoryInLightMode() {
        content(false)
        compose.onNodeWithTag("drawer_search").performTextInput("Ma")
        compose.onNodeWithText("Communication").performClick()
        compose.onNodeWithTag("app_mail").assertIsDisplayed()
        compose.onNodeWithTag("app_maps").assertDoesNotExist()
        compose.onNodeWithText("All Apps").performClick()
        compose.onNodeWithTag("app_maps").assertIsDisplayed()
    }
    @Test fun darkModeLongPressAndEmptySearch() {
        content(true)
        compose.onNodeWithTag("app_mail").performTouchInput { longClick() }
        compose.onNodeWithText("Change category: Mail").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("drawer_search").performTextInput("zzzzz")
        compose.onNodeWithTag("app_mail").assertDoesNotExist()
        compose.onNodeWithText("No results for “zzzzz”").assertIsDisplayed()
    }
}
