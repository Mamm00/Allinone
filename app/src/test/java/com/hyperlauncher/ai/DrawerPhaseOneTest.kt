package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.repository.CategorizationEngine
import com.hyperlauncher.ai.notifications.BadgeEntry
import com.hyperlauncher.ai.notifications.countBadges
import com.hyperlauncher.ai.ui.launcher.filterDrawerApps
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DrawerPhaseOneTest {
    private val rules = CategorizationEngine.defaultRules()
    private fun category(pkg: String, label: String, platform: Int = -1) =
        CategorizationEngine.categorizeWithRules(rules, pkg, label, platform)

    @Test fun platformCategoryAndExplicitMapping() {
        assertEquals("games", category("org.new.vendor", "A new title", 0))
        assertEquals("ai_assistants", category("com.openai.chatgpt", "ChatGPT", 0))
        assertEquals("messaging", category("com.whatsapp", "WhatsApp"))
        assertEquals("internet", category("org.mozilla.firefox", "Firefox"))
        assertEquals("settings", category("com.android.settings", "Settings"))
    }
    @Test fun shortKeywordsDoNotMatchUnrelatedSubstrings() {
        assertEquals("others", category("com.example.unknown", "Fair Explorer"))
        assertEquals("others", category("org.vendor.daily", "Daily"))
        assertEquals("others", category("org.vendor.xylophone", "Xylophone"))
    }
    @Test fun classificationIsIndependentOfDeviceLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("finance", category("org.vendor.app", "FINANCE"))
        } finally { Locale.setDefault(old) }
    }
    @Test fun searchAndCategoryIntersectAndAlphabeticalIgnoresCategory() {
        val apps = listOf(AppInfo("z", "Zoom", "messaging"), AppInfo("m", "Mail", "messaging"),
            AppInfo("g", "Maps", "travel"))
        assertEquals(listOf("m"), filterDrawerApps(apps, " ma ", "messaging", false).map { it.packageName })
        assertEquals(listOf("m", "g"), filterDrawerApps(apps, "ma", "messaging", true).map { it.packageName })
        assertTrue(filterDrawerApps(apps, "nothing", "all", false).isEmpty())
    }
    @Test fun badgeCountsExcludeGroupSummariesAndClampInvalidCounts() {
        assertEquals(mapOf("chat" to 5, "mail" to 999), countBadges(listOf(
            BadgeEntry("chat", 200, true), BadgeEntry("chat", 4, false),
            BadgeEntry("chat", 0, false), BadgeEntry("mail", Int.MAX_VALUE, false))))
        assertTrue(countBadges(emptyList()).isEmpty())
    }
}
