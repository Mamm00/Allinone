package com.hyperlauncher.ai.data.repository

import android.content.Context
import com.hyperlauncher.ai.data.model.CategoryRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.Locale

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

    fun categorize(packageName: String, label: String, platformCategory: Int = -1): String {
        ensureLoaded()
        return categorizeWithRules(rules, packageName, label, platformCategory)
    }

    companion object {
        /** Explicit package mapping wins; platform/installer category is an offline hint,
         * not a live Play Store lookup. Unknown values fall through to name heuristics. */
        fun categorizeWithRules(
            rules: List<CategoryRule>, packageName: String, label: String, platformCategory: Int = -1
        ): String {
            rules.firstOrNull { rule -> rule.packages.any { it.equals(packageName, true) } }
                ?.let { return it.id }
            val platformId = when (platformCategory) {
                0 -> "games"
                1, 2, 3 -> "media"
                4 -> "social"
                5 -> "internet"
                6 -> "travel"
                7 -> "productivity"
                else -> null
            }
            if (platformId != null && rules.any { it.id == platformId }) return platformId
            // Token/phrase boundaries avoid "phone" matching "xylophone" and "ai"
            // matching "daily". Split camel-case names so MyBank still matches bank.
            fun normalize(value: String) = value.replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
            val text = " " + normalize(label + " " + packageName) + " "
            for (rule in rules) {
                if (rule.id == "others") continue
                if (rule.keywords.any { raw ->
                    val keyword = normalize(raw)
                    keyword.isNotBlank() && text.contains(" $keyword ")
                }) return rule.id
            }
            return "others"
        }

        fun defaultRules(): List<CategoryRule> = listOf(
            CategoryRule("ai_assistants", "AI & Assistants", listOf("assistant", "gemini", "chatgpt", "copilot", "claude", "meta ai", "grok", "perplexity", "bard", "alexa", "siri"), listOf("com.google.android.apps.bard", "com.openai.chatgpt", "com.microsoft.copilot", "com.anthropic.claude", "com.meta.ai", "com.x.ai", "com.perplexity.app", "com.amazon.dee.app")),
            CategoryRule("social", "Social", listOf("facebook", "instagram", "twitter", "snapchat", "tiktok", "linkedin", "discord", "reddit", "threads", "viber", "wechat"), listOf("com.facebook.katana", "com.instagram.android", "com.twitter.android", "com.snapchat.android", "com.zhiliaoapp.musically", "com.linkedin.android", "com.discord", "com.reddit.frontpage", "com.signal")),
            CategoryRule("messaging", "Communication", listOf("message", "sms", "mms", "messages", "hangouts", "duo", "meet", "zoom", "teams", "skype", "phone", "dialer", "contacts", "email", "mail", "whatsapp", "telegram", "signal"), listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.microsoft.teams", "com.skype.raider", "com.google.android.apps.tachyon", "com.whatsapp", "org.telegram.messenger", "org.thoughtcrime.securesms", "com.google.android.dialer", "com.google.android.contacts", "com.google.android.gm", "com.microsoft.office.outlook")),
            CategoryRule("media", "Media & Photos", listOf("photo", "camera", "gallery", "video", "music", "spotify", "youtube", "vlc", "mx player", "photos", "drive photos"), listOf("com.google.android.apps.photos", "com.google.android.youtube", "com.spotify.music", "org.videolan.vlc", "com.mxtech.videoplayer.ad", "com.miui.gallery", "com.android.camera")),
            CategoryRule("games", "Games", listOf("game", "play games", "steam", "pubg", "bgmi", "free fire", "candy", "chess", "ludo"), listOf("com.google.android.play.games", "com.tencent.ig", "com.dts.freefireth", "com.king.candycrushsaga")),
            CategoryRule("shopping", "Shopping", listOf("amazon", "flipkart", "myntra", "ebay", "walmart", "target", "shop", "cart", "meesho", "aliexpress", "shein"), listOf("com.amazon.mShop.android.shopping", "com.flipkart.android", "com.myntra.android", "com.ebay.mobile", "com.alibaba.aliexpress")),
            CategoryRule("finance", "Finance & Banking", listOf("bank", "paytm", "phonepe", "gpay", "upi", "finance", "money", "wallet", "crypto", "trading", "zerodha", "groww"), listOf("com.paytm.recharge", "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "com.zerodha.kite3", "com.groww")),
            CategoryRule("productivity", "Productivity", listOf("notes", "keep", "docs", "sheets", "slides", "office", "notion", "todo", "calendar", "files", "drive"), listOf("com.google.android.apps.docs", "com.google.android.apps.sheets", "com.google.android.apps.slides", "com.google.android.apps.tasks", "com.google.android.keep", "com.microsoft.office.word", "com.microsoft.office.excel", "com.notion")),
            CategoryRule("travel", "Travel & Maps", listOf("maps", "uber", "ola", "rapido", "irctc", "redbus", "makemytrip", "booking", "airbnb", "zomato", "swiggy"), listOf("com.google.android.apps.maps", "com.ubercab", "com.rapido.passenger", "com.makemytrip", "com.zomato", "com.swiggy.android")),
            CategoryRule("health", "Health & Fitness", listOf("health", "fitness", "fit", "gym", "meditation", "curefit", "practo", "1mg", "pharmeasy"), listOf("com.google.android.apps.fitness", "com.curefit.cult", "com.practo")),
            CategoryRule("education", "Education", listOf("learn", "course", "udemy", "coursera", "duolingo", "byjus", "unacademy", "khan", "school"), listOf("com.udemy.android", "com.duolingo", "com.coursera")),
            CategoryRule("tools", "Tools", listOf("tool", "utility", "file manager", "cleaner", "vpn", "password", "scanner", "translate", "recorder", "compass", "torch"), listOf("com.google.android.apps.translate", "com.miui.filemanager", "com.cleanmaster.mguard")),
            CategoryRule("internet", "Internet", listOf("browser", "firefox", "chrome", "brave", "opera", "internet"), listOf("com.android.chrome", "org.mozilla.firefox", "com.brave.browser", "com.opera.browser")),
            CategoryRule("utilities", "Utilities", listOf("calculator", "clock", "flashlight", "weather"), listOf("com.google.android.calculator", "com.google.android.deskclock", "com.android.deskclock")),
            CategoryRule("settings", "Settings", listOf("settings"), listOf("com.android.settings", "com.miui.securitycenter")),
            CategoryRule("others", "Unknown apps", listOf(), listOf())
        )
    }

    fun getRules(): List<CategoryRule> {
        ensureLoaded()
        return rules
    }

    private fun loadRules(): List<CategoryRule> {
        return try {
            val jsonText = context.assets.open("categories.json").bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(jsonText).jsonObject
            val categories = root["categories"]?.jsonArray ?: return defaultRules()
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
