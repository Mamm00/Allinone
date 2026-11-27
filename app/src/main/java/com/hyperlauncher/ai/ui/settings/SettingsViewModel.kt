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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openRouterEnabled: Boolean = false,
    val openRouterModel: String = SettingsRepository.DEFAULT_MODEL,
    val hasApiKey: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val whitelistedPackages: Set<String> = emptySet(),
    val floatingPillEnabled: Boolean = false,
    val installedApps: List<com.hyperlauncher.ai.util.LauncherAppInfo> = emptyList(),
    val errorMessage: String? = null
)

class SettingsViewModel(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val keyStoreManager: KeyStoreManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scanApps: (Context)->List<com.hyperlauncher.ai.util.LauncherAppInfo> = AppScanner::getLauncherApps
) : ViewModel() {

    private val appContext=context.applicationContext ?:context
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // One ordered worker: rapid toggles/key save/remove must not race each other
    // or overwrite a newer UI state with the startup snapshot.
    private val actions = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        viewModelScope.launch(ioDispatcher) {
            for (action in actions) {
                try { action(); _uiState.update {it.copy(errorMessage=null)} }
                catch (cancelled: CancellationException) {throw cancelled}
                catch (_: Exception) {_uiState.update {it.copy(errorMessage="Settings could not be saved or loaded. Try again.")}}
            }
        }
        load()
    }
    private fun enqueue(action: suspend ()->Unit) { actions.trySend(action) }
    override fun onCleared() { actions.close(); super.onCleared() }

    fun load() = enqueue {
        val enabled = settingsRepository.openRouterEnabled.first()
        val model = settingsRepository.openRouterModel.first()
        val a11y = settingsRepository.accessibilityCaptureEnabled.first()
        val whitelist = settingsRepository.whitelistedPackages.first()
        val pill = settingsRepository.floatingPillEnabled.first()
        val apps = scanApps(appContext)
        _uiState.value = SettingsUiState(enabled,model,keyStoreManager.hasOpenRouterKey(),a11y,whitelist,pill,apps)
    }
    fun setOpenRouterEnabled(enabled: Boolean) = enqueue {
        settingsRepository.setOpenRouterEnabled(enabled)
        _uiState.update {it.copy(openRouterEnabled=enabled)}
    }
    fun setOpenRouterModel(model: String) = enqueue {
        settingsRepository.setOpenRouterModel(model)
        _uiState.update {it.copy(openRouterModel=model)}
    }
    fun saveApiKey(key: String) = enqueue {
        keyStoreManager.saveOpenRouterKey(key)
        _uiState.update {it.copy(hasApiKey=true)}
    }
    fun clearApiKey() = enqueue {
        keyStoreManager.clearOpenRouterKey()
        _uiState.update {it.copy(hasApiKey=false)}
    }
    fun setAccessibilityEnabled(enabled: Boolean) = enqueue {
        settingsRepository.setAccessibilityEnabled(enabled)
        _uiState.update {it.copy(accessibilityEnabled=enabled)}
    }
    fun toggleWhitelist(pkg: String) = enqueue {
        val current=settingsRepository.whitelistedPackages.first()
        val updated=if(pkg in current) current-pkg else current+pkg
        settingsRepository.setWhitelistedPackages(updated)
        _uiState.update {it.copy(whitelistedPackages=updated)}
    }
    fun setFloatingPillEnabled(enabled: Boolean) = enqueue {
        settingsRepository.setFloatingPillEnabled(enabled)
        _uiState.update {it.copy(floatingPillEnabled=enabled)}
    }
}
