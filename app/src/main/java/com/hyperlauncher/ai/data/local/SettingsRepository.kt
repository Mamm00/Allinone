package com.hyperlauncher.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hyperlauncher_settings")

data class DrawerPreferences(
    val alphabetical: Boolean = false,
    val badges: Boolean = false,
    val showSystemState: Boolean = true,
    val ambient: Boolean = true,
    val autoCategorize: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private val pinMutex=Mutex()
    val appearance: Flow<Map<String,String>> = context.dataStore.data.map {prefs ->runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(prefs[KEY_APPEARANCE] ?: "{}").let {element ->
            (element as kotlinx.serialization.json.JsonObject).mapValues {(it.value as kotlinx.serialization.json.JsonPrimitive).content}
        }
    }.getOrDefault(emptyMap())}
    suspend fun setAppearance(key: String,value: String) {
        require(key in setOf("font","clockFont","fontVersion","clockFontVersion","fontWeight","iconShape","iconPack","immersive"))
        context.dataStore.edit {prefs ->
            val old=runCatching {kotlinx.serialization.json.Json.parseToJsonElement(prefs[KEY_APPEARANCE] ?:"{}") as kotlinx.serialization.json.JsonObject}.getOrDefault(kotlinx.serialization.json.JsonObject(emptyMap()))
            prefs[KEY_APPEARANCE]=kotlinx.serialization.json.JsonObject(old+(key to kotlinx.serialization.json.JsonPrimitive(value.take(200)))).toString()
        }
    }

    val hiddenPackages: Flow<Set<String>> = context.dataStore.data.map {it[KEY_HIDDEN] ?: emptySet()}
    val hiddenPinConfigured: Flow<Boolean> = context.dataStore.data.map {it[KEY_HIDDEN_PIN]!=null}
    suspend fun createHiddenPin(pin: String) = withContext(Dispatchers.IO) {
        pinMutex.withLock {
            require(context.dataStore.data.first()[KEY_HIDDEN_PIN]==null) {"A hidden-folder PIN already exists"}
            val encoded=HiddenPin.create(pin)
            context.dataStore.edit {it[KEY_HIDDEN_PIN]=encoded}
        }
    }
    suspend fun setAppHidden(pkg: String,hidden: Boolean) {
        require(context.dataStore.data.first()[KEY_HIDDEN_PIN]!=null) {"Create a PIN first"}
        context.dataStore.edit {
            val current=it[KEY_HIDDEN] ?:emptySet()
            it[KEY_HIDDEN]=if(hidden) current+pkg else current-pkg
        }
    }
    suspend fun unlockHidden(pin: String): Boolean = withContext(Dispatchers.IO) {
        pinMutex.withLock {
            val prefs=context.dataStore.data.first()
            require((prefs[KEY_PIN_LOCKED_UNTIL] ?:0L)<=System.currentTimeMillis()) {"Too many attempts. Wait one minute."}
            val valid=HiddenPin.verify(pin,prefs[KEY_HIDDEN_PIN].orEmpty())
            context.dataStore.edit {
                if(valid) {it[KEY_PIN_FAILURES]=0;it[KEY_PIN_LOCKED_UNTIL]=0L}
                else {
                    val failures=(it[KEY_PIN_FAILURES] ?:0)+1
                    it[KEY_PIN_FAILURES]=if(failures>=5) 0 else failures
                    if(failures>=5) it[KEY_PIN_LOCKED_UNTIL]=System.currentTimeMillis()+60000L
                }
            }
            valid
        }
    }

    val drawerPreferences: Flow<DrawerPreferences> = context.dataStore.data.map {
        DrawerPreferences(it[KEY_ALPHABETICAL] ?: false, it[KEY_BADGES] ?: false,
            it[KEY_SYSTEM_STATE] ?: true, it[KEY_AMBIENT] ?: true, it[KEY_AUTO_CATEGORIZE] ?: true)
    }

    suspend fun setDrawerOption(key: String, enabled: Boolean) {
        val preference = when (key) {
            "alphabetical" -> KEY_ALPHABETICAL
            "badges" -> KEY_BADGES
            "systemState" -> KEY_SYSTEM_STATE
            "ambient" -> KEY_AMBIENT
            "autoCategorize" -> KEY_AUTO_CATEGORIZE
            else -> return
        }
        context.dataStore.edit { it[preference] = enabled }
    }


    val openRouterEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_OPENROUTER_ENABLED] ?: false }
    val openRouterModel: Flow<String> = context.dataStore.data.map { it[KEY_OPENROUTER_MODEL] ?: DEFAULT_MODEL }
    val accessibilityCaptureEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ACCESSIBILITY_ENABLED] ?: false }
    val whitelistedPackages: Flow<Set<String>> = context.dataStore.data.map { it[KEY_WHITELIST] ?: emptySet() }
    val floatingPillEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_FLOATING_PILL] ?: false }
    val selectedCategory: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_CATEGORY] ?: "all" }

    suspend fun setOpenRouterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_OPENROUTER_ENABLED] = enabled }
    }

    suspend fun setOpenRouterModel(model: String) {
        context.dataStore.edit { it[KEY_OPENROUTER_MODEL] = model }
    }

    suspend fun setAccessibilityEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ACCESSIBILITY_ENABLED] = enabled }
    }

    suspend fun setWhitelistedPackages(packages: Set<String>) {
        context.dataStore.edit { it[KEY_WHITELIST] = packages }
    }

    suspend fun addWhitelistedPackage(pkg: String) {
        context.dataStore.edit { it[KEY_WHITELIST] = (it[KEY_WHITELIST] ?: emptySet()) + pkg }
    }

    suspend fun removeWhitelistedPackage(pkg: String) {
        context.dataStore.edit { it[KEY_WHITELIST] = (it[KEY_WHITELIST] ?: emptySet()) - pkg }
    }

    suspend fun setFloatingPillEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_PILL] = enabled }
    }

    suspend fun setSelectedCategory(categoryId: String) {
        context.dataStore.edit { it[KEY_SELECTED_CATEGORY] = categoryId }
    }

    companion object {
        private val KEY_APPEARANCE=stringPreferencesKey("appearance")
        private val KEY_HIDDEN=stringSetPreferencesKey("hidden_packages")
        private val KEY_HIDDEN_PIN=stringPreferencesKey("hidden_pin_hash")
        private val KEY_PIN_FAILURES=intPreferencesKey("hidden_pin_failures")
        private val KEY_PIN_LOCKED_UNTIL=longPreferencesKey("hidden_pin_locked_until")
        private val KEY_ALPHABETICAL = booleanPreferencesKey("drawer_alphabetical")
        private val KEY_BADGES = booleanPreferencesKey("drawer_badges")
        private val KEY_SYSTEM_STATE = booleanPreferencesKey("drawer_system_state")
        private val KEY_AMBIENT = booleanPreferencesKey("drawer_ambient")
        private val KEY_AUTO_CATEGORIZE = booleanPreferencesKey("drawer_auto_categorize")
        val KEY_OPENROUTER_ENABLED = booleanPreferencesKey("openrouter_enabled")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val KEY_ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
        val KEY_WHITELIST = stringSetPreferencesKey("whitelist_packages")
        val KEY_FLOATING_PILL = booleanPreferencesKey("floating_pill")
        val KEY_SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
    }
}
