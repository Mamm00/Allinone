package com.hyperlauncher.ai.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.KeyStoreManager
import com.hyperlauncher.ai.data.local.VaultCrypto
import com.hyperlauncher.ai.data.local.VaultNote
import com.hyperlauncher.ai.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val hasVault: Boolean = false,
    val notes: List<VaultNote> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val keyStoreManager: KeyStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        refreshVaultStatus()
    }

    fun refreshVaultStatus() {
        _uiState.value = _uiState.value.copy(
            hasVault = keyStoreManager.hasVault(),
            isUnlocked = vaultRepository.isUnlocked()
        )
    }

    fun createVault(passphrase: String, confirm: String): Boolean {
        if (passphrase.length < VaultCrypto.MIN_PASSPHRASE_LENGTH) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passphrase must be at least 8 characters")
            return false
        }
        if (passphrase != confirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passphrases do not match")
            return false
        }
        viewModelScope.launch {
            val hash = VaultCrypto.hashPassphrase(passphrase)
            keyStoreManager.saveVaultPassphraseHash(hash)
            val ok = vaultRepository.unlock(passphrase)
            if (ok) {
                _uiState.value = _uiState.value.copy(hasVault = true, isUnlocked = true, errorMessage = null)
                loadNotes()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create vault")
            }
        }
        return true
    }

    fun unlock(passphrase: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val expectedHash = keyStoreManager.getVaultPassphraseHash()
            val actualHash = VaultCrypto.hashPassphrase(passphrase)
            if (expectedHash != null && expectedHash != actualHash) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Wrong passphrase")
                return@launch
            }
            val ok = vaultRepository.unlock(passphrase)
            _uiState.value = _uiState.value.copy(isLoading = false, isUnlocked = ok)
            if (ok) {
                loadNotes()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Wrong passphrase")
            }
        }
    }

    fun lock() {
        viewModelScope.launch {
            vaultRepository.lock()
            _uiState.value = _uiState.value.copy(isUnlocked = false, notes = emptyList())
        }
    }

    fun addNote(title: String, body: String) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            vaultRepository.addNote(title.trim(), body.trim(), null)
            loadNotes()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteNote(id)
            loadNotes()
        }
    }

    private suspend fun loadNotes() {
        val notes = vaultRepository.getNotes()
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
