package com.hyperlauncher.ai.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import com.hyperlauncher.ai.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LauncherUiState(
    val apps: List<AppInfo> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String = "all",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class LauncherViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val categories = repository.getCategories()
                val apps = repository.getApps()
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    categories = categories,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load apps"
                )
            }
        }
    }

    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, isLoading = true)
            try {
                val apps = repository.getAppsByCategory(categoryId)
                _uiState.value = _uiState.value.copy(apps = apps, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        // Debounce <16ms target: 12ms debounce keeps typing responsive.
        searchJob = viewModelScope.launch {
            delay(12)
            if (query.isBlank()) {
                val apps = repository.getAppsByCategory(_uiState.value.selectedCategoryId)
                _uiState.value = _uiState.value.copy(apps = apps)
            } else {
                val results = repository.searchApps(query)
                _uiState.value = _uiState.value.copy(apps = results)
            }
        }
    }

    fun onAppLaunched(packageName: String) {
        viewModelScope.launch {
            repository.recordLaunch(packageName)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
