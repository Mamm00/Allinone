package com.hyperlauncher.ai

import androidx.test.platform.app.InstrumentationRegistry
import com.hyperlauncher.ai.data.local.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PreferencesPersistenceTest {
    @Test fun appearanceAndHiddenPackagesPersistAcrossRepositoryRecreation()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val first=SettingsRepository(context)
        val original=first.appearance.first()
        try {
            first.setAppearance("font","serif")
            assertEquals("serif",SettingsRepository(context).appearance.first()["font"])
            if(!first.hiddenPinConfigured.first()) first.createHiddenPin("67382910")
            first.setAppHidden("com.test.hidden",true)
            assertTrue("com.test.hidden" in SettingsRepository(context).hiddenPackages.first())
            first.setAppHidden("com.test.hidden",false)
            assertFalse("com.test.hidden" in first.hiddenPackages.first())
        } finally {first.setAppearance("font",original["font"] ?:"system")}
    }
}
