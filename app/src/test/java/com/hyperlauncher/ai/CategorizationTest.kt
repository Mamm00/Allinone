package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.repository.CategorizationEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CategorizationTest {

    private val engine = CategorizationEngine(RuntimeEnvironment.getApplication())

    @Test
    fun categorize_aiApp() {
        assertEquals("ai_assistants", engine.categorize("com.openai.chatgpt", "ChatGPT"))
    }

    @Test
    fun categorize_socialApp() {
        assertEquals("social", engine.categorize("com.instagram.android", "Instagram"))
    }

    @Test
    fun categorize_unknownIsOthers() {
        assertEquals("others", engine.categorize("com.example.unknown", "Unknown App XYZ"))
    }

    @Test
    fun categorize_keywordMatch() {
        assertEquals("finance", engine.categorize("com.example.mybank", "MyBank"))
    }
}
