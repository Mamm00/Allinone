package com.hyperlauncher.ai.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.DrawerPreferences
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import com.hyperlauncher.ai.data.repository.AppRepository
import com.hyperlauncher.ai.notifications.NotificationBadges
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class DrawerSelection(val category: String = "all", val query: String = "")
data class LauncherUiState(
    val apps: List<AppInfo> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String = "all",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val preferences: DrawerPreferences = DrawerPreferences(),
    val badgeCounts: Map<String, Int> = emptyMap(),
    val allApps: List<AppInfo> = emptyList()
)

class LauncherViewModel(
    private val repository: AppRepository,
    private val settings: SettingsRepository,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val selection = MutableStateFlow(DrawerSelection())
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null
    private val content = combine(repository.observeApps(), repository.observeCategories(),
        settings.drawerPreferences, selection) { apps, categories, preferences, selected ->
        val effective = apps.map {
            if (!preferences.autoCategorize && !it.hasManualCategory) it.copy(categoryId = "others") else it
        }
        LauncherUiState(
            apps = filterDrawerApps(effective, selected.query, selected.category, preferences.alphabetical),
            allApps = effective, categories = categories, selectedCategoryId = selected.category,
            searchQuery = selected.query, preferences = preferences
        )
    }.flowOn(filterDispatcher)

    val uiState = combine(content, loading, error, NotificationBadges.counts) { state, busy, message, counts ->
        state.copy(isLoading = busy, errorMessage = message,
            badgeCounts = if (state.preferences.badges) counts else emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherUiState())

    init { load() }

    fun load() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.getCategories()
                repository.refreshInstalledApps()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) { error.value = exception.message ?: "Unable to refresh apps" }
            finally { loading.value = false }
        }
    }

    fun selectCategory(categoryId: String) { selection.value = selection.value.copy(category = categoryId) }
    fun onSearchQueryChange(query: String) { selection.value = selection.value.copy(query = query) }
    fun setOption(key: String, enabled: Boolean) = action { settings.setDrawerOption(key, enabled) }
    fun changeCategory(packageName: String, categoryId: String?, similar: Boolean) = action {
        repository.changeCategory(packageName, categoryId, similar)
    }
    fun saveCategory(id: String?, name: String) = action {
        if (id == null) repository.addCategory(name) else repository.renameCategory(id, name)
    }
    fun onAppLaunched(packageName: String) = action {
        if (repository.launchApp(packageName)) repository.recordLaunch(packageName)
        else error.value = "Android could not open this app. Check its system app settings."
    }
    fun clearError() { error.value = null }
    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) { error.value = exception.message ?: "Action failed" }
        }
    }
}
