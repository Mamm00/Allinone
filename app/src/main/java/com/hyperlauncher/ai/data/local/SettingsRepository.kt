package com.hyperlauncher.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hyperlauncher_settings")

class SettingsRepository(private val context: Context) {

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
        val KEY_OPENROUTER_ENABLED = booleanPreferencesKey("openrouter_enabled")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val KEY_ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
        val KEY_WHITELIST = stringSetPreferencesKey("whitelist_packages")
        val KEY_FLOATING_PILL = booleanPreferencesKey("floating_pill")
        val KEY_SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
    }
}
