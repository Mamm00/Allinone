package com.hyperlauncher.ai.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.KeyStoreManager
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.data.network.AiResult
import com.hyperlauncher.ai.data.network.OpenRouterService
import com.hyperlauncher.ai.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AiPromptUiState(
    val prompt: String = "",
    val response: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedToVault: Boolean = false
)

class AiPromptViewModel(
    private val openRouterService: OpenRouterService,
    private val keyStoreManager: KeyStoreManager,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiPromptUiState())
    val uiState: StateFlow<AiPromptUiState> = _uiState.asStateFlow()

    fun onPromptChange(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text, savedToVault = false)
    }

    fun submit() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, response = null)
            val enabled = settingsRepository.openRouterEnabled.first()
            val model = settingsRepository.openRouterModel.first()
            val apiKey = keyStoreManager.getOpenRouterKey()
            val result = openRouterService.complete(prompt, model, apiKey, enabled)
            _uiState.value = when (result) {
                is AiResult.Success -> _uiState.value.copy(
                    isLoading = false,
                    response = result.response.content
                )
                is AiResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                AiResult.Offline -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "AI is offline in this build. Switch to the Network build to use OpenRouter."
                )
                AiResult.NoKey -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Add your OpenRouter API key in Settings to enable AI."
                )
                AiResult.Disabled -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "OpenRouter is disabled. Enable it in Settings."
                )
            }
        }
    }

    fun saveResponseToVault() {
        val response = _uiState.value.response ?: return
        val prompt = _uiState.value.prompt.trim()
        viewModelScope.launch {
            if (!vaultRepository.isUnlocked()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Vault is locked. Unlock it in AI Notes first."
                )
                return@launch
            }
            vaultRepository.addNote(
                title = prompt.take(60),
                body = response,
                sourcePackage = null
            )
            _uiState.value = _uiState.value.copy(savedToVault = true)
        }
    }

    fun clearResponse() {
        _uiState.value = _uiState.value.copy(response = null, errorMessage = null)
    }
}
