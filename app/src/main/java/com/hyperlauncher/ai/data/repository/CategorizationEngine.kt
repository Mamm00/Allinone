package com.hyperlauncher.ai.data.repository

import android.content.Context
import com.hyperlauncher.ai.data.model.CategoryRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * Local, offline categorization: loads categories.json heuristics and classifies
 * apps by package match, then keyword match on label/package, else Others.
 * No network, no ML model download — deterministic and fast (<16ms per batch).
 */
class CategorizationEngine(private val context: Context) {

    private var rules: List<CategoryRule> = emptyList()

    @Synchronized
    fun ensureLoaded() {
        if (rules.isNotEmpty()) return
        rules = loadRules()
    }

    fun categorize(packageName: String, label: String): String {
        ensureLoaded()
        val pkgLower = packageName.lowercase()
        val labelLower = label.lowercase()
        // 1) Exact package match
        for (rule in rules) {
            if (rule.packages.any { it.equals(packageName, ignoreCase = true) }) {
                return rule.id
            }
        }
        // 2) Package substring match
        for (rule in rules) {
            if (rule.packages.any { pkgLower.contains(it.lowercase()) }) {
                return rule.id
            }
        }
        // 3) Keyword match on label or package
        for (rule in rules) {
            if (rule.id == "others") continue
            if (rule.keywords.any { labelLower.contains(it) || pkgLower.contains(it) }) {
                return rule.id
            }
        }
        return "others"
    }

    fun getRules(): List<CategoryRule> {
        ensureLoaded()
        return rules
    }

    private fun defaultRules(): List<CategoryRule> {
        return listOf(
            CategoryRule("ai_assistants", "AI & Assistants", listOf("assistant", "gemini", "chatgpt", "copilot", "claude", "ai"), listOf("com.openai.chatgpt", "com.google.android.apps.bard")),
            CategoryRule("social", "Social", listOf("facebook", "instagram", "twitter", "whatsapp", "telegram", "snapchat", "tiktok"), listOf("com.instagram.android", "com.facebook.katana", "com.twitter.android")),
            CategoryRule("messaging", "Messaging", listOf("message", "sms", "messages"), listOf("com.google.android.apps.messaging")),
            CategoryRule("media", "Media & Photos", listOf("photo", "camera", "gallery", "video", "music", "spotify", "youtube"), listOf("com.google.android.apps.photos", "com.google.android.youtube")),
            CategoryRule("games", "Games", listOf("game", "play games"), listOf("com.google.android.play.games")),
            CategoryRule("shopping", "Shopping", listOf("amazon", "flipkart", "shop"), listOf("com.amazon.mShop.android.shopping")),
            CategoryRule("finance", "Finance & Banking", listOf("bank", "paytm", "phonepe", "upi", "finance", "money"), listOf("com.paytm.recharge")),
            CategoryRule("productivity", "Productivity", listOf("notes", "keep", "docs", "sheets", "office", "notion", "todo", "calendar"), listOf("com.google.android.apps.docs")),
            CategoryRule("travel", "Travel & Maps", listOf("maps", "uber", "ola", "zomato", "swiggy"), listOf("com.google.android.apps.maps")),
            CategoryRule("health", "Health & Fitness", listOf("health", "fitness", "fit", "gym"), listOf("com.google.android.apps.fitness")),
            CategoryRule("education", "Education", listOf("learn", "course", "udemy", "duolingo"), listOf("com.udemy.android")),
            CategoryRule("tools", "Tools & Utilities", listOf("tool", "utility", "file manager", "vpn", "translate"), listOf("com.google.android.apps.translate")),
            CategoryRule("others", "Others", emptyList(), emptyList())
        )
    }

    private fun loadRules(): List<CategoryRule> {
        return try {
            val jsonText = context.assets.open("categories.json").bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(jsonText).jsonObject
            val categories = root["categories"]?.jsonArray ?: return emptyList()
            categories.map { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: "others"
                val name = obj["name"]?.jsonPrimitive?.content ?: id
                val keywords = (obj["keywords"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
                val packages = (obj["packages"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
                CategoryRule(id = id, name = name, keywords = keywords, packages = packages)
            }
        } catch (e: IOException) {
            defaultRules()
        } catch (e: Exception) {
            defaultRules()
        }
    }
}
