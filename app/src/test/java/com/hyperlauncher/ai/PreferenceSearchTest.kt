package com.hyperlauncher.ai
import com.hyperlauncher.ai.ui.settings.matchesPreference
import org.junit.Assert.*
import org.junit.Test
class PreferenceSearchTest {
    @Test fun matchesLocalizedUnicodeAndDiacritics() {
        assertTrue(matchesPreference("Préférences d’affichage","preferences"))
        assertTrue(matchesPreference("İLETİŞİM","iletisim"))
        assertTrue(matchesPreference("通知 设置","通知"))
        assertFalse(matchesPreference("Dock slots","weather"))
    }
}
