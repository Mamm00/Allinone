package com.hyperlauncher.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException

/**
 * Stores the OpenRouter API key and vault passphrase hash in EncryptedSharedPreferences
 * (AES-256-GCM via Android Keystore). The key is never logged or committed.
 */
class KeyStoreManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "hyperlauncher_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveOpenRouterKey(key: String) {
        prefs.edit().putString(KEY_OPENROUTER, key.trim()).apply()
    }

    fun getOpenRouterKey(): String? {
        return try {
            prefs.getString(KEY_OPENROUTER, null)?.takeIf { it.isNotBlank() }
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun clearOpenRouterKey() {
        prefs.edit().remove(KEY_OPENROUTER).apply()
    }

    fun hasOpenRouterKey(): Boolean = !getOpenRouterKey().isNullOrBlank()

    fun saveVaultPassphraseHash(hash: String) {
        prefs.edit().putString(KEY_VAULT_HASH, hash).apply()
    }

    fun getVaultPassphraseHash(): String? {
        return try {
            prefs.getString(KEY_VAULT_HASH, null)
        } catch (e: Exception) {
            null
        }
    }

    fun hasVault(): Boolean = !getVaultPassphraseHash().isNullOrBlank()

    fun clearVault() {
        prefs.edit().remove(KEY_VAULT_HASH).apply()
    }

    companion object {
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_VAULT_HASH = "vault_passphrase_hash"
    }
}
