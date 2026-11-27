package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.repository.CategorizationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationTest {

    private val rules = CategorizationEngine.defaultRules()

    @Test
    fun categorize_aiApp() {
        assertEquals("ai_assistants", CategorizationEngine.categorizeWithRules(rules, "com.openai.chatgpt", "ChatGPT"))
    }

    @Test
    fun categorize_socialApp() {
        assertEquals("social", CategorizationEngine.categorizeWithRules(rules, "com.instagram.android", "Instagram"))
    }

    @Test
    fun categorize_unknownIsOthers() {
        assertEquals("others", CategorizationEngine.categorizeWithRules(rules, "com.example.unknown", "Unknown App XYZ"))
    }

    @Test
    fun categorize_keywordMatch() {
        assertEquals("finance", CategorizationEngine.categorizeWithRules(rules, "com.example.mybank", "MyBank"))
    }
}
