package com.hyperlauncher.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.KeyStoreManager
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.util.AppScanner
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openRouterEnabled: Boolean = false,
    val openRouterModel: String = SettingsRepository.DEFAULT_MODEL,
    val hasApiKey: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val whitelistedPackages: Set<String> = emptySet(),
    val floatingPillEnabled: Boolean = false,
    val installedApps: List<com.hyperlauncher.ai.util.LauncherAppInfo> = emptyList()
)

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val keyStoreManager: KeyStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val enabled = settingsRepository.openRouterEnabled.first()
            val model = settingsRepository.openRouterModel.first()
            val a11y = settingsRepository.accessibilityCaptureEnabled.first()
            val whitelist = settingsRepository.whitelistedPackages.first()
            val pill = settingsRepository.floatingPillEnabled.first()
            val apps = AppScanner.getLauncherApps(context)
            _uiState.value = SettingsUiState(
                openRouterEnabled = enabled,
                openRouterModel = model,
                hasApiKey = keyStoreManager.hasOpenRouterKey(),
                accessibilityEnabled = a11y,
                whitelistedPackages = whitelist,
                floatingPillEnabled = pill,
                installedApps = apps
            )
        }
    }

    fun setOpenRouterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOpenRouterEnabled(enabled)
            _uiState.value = _uiState.value.copy(openRouterEnabled = enabled)
        }
    }

    fun setOpenRouterModel(model: String) {
        viewModelScope.launch {
            settingsRepository.setOpenRouterModel(model)
            _uiState.value = _uiState.value.copy(openRouterModel = model)
        }
    }

    fun saveApiKey(key: String) {
        keyStoreManager.saveOpenRouterKey(key)
        _uiState.value = _uiState.value.copy(hasApiKey = true)
    }

    fun clearApiKey() {
        keyStoreManager.clearOpenRouterKey()
        _uiState.value = _uiState.value.copy(hasApiKey = false)
    }

    fun setAccessibilityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAccessibilityEnabled(enabled)
            _uiState.value = _uiState.value.copy(accessibilityEnabled = enabled)
        }
    }

    fun toggleWhitelist(pkg: String) {
        viewModelScope.launch {
            val current = _uiState.value.whitelistedPackages
            val updated = if (current.contains(pkg)) current - pkg else current + pkg
            settingsRepository.setWhitelistedPackages(updated)
            _uiState.value = _uiState.value.copy(whitelistedPackages = updated)
        }
    }

    fun setFloatingPillEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingPillEnabled(enabled)
            _uiState.value = _uiState.value.copy(floatingPillEnabled = enabled)
        }
    }
}
