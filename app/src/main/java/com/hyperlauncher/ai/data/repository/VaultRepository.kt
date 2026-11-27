package com.hyperlauncher.ai.data.repository

import com.hyperlauncher.ai.data.local.VaultHelper
import com.hyperlauncher.ai.data.local.VaultNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultRepository(private val vaultHelper: VaultHelper) {

    suspend fun unlock(passphrase: String): Boolean = withContext(Dispatchers.IO) {
        vaultHelper.unlock(passphrase)
    }

    suspend fun lock() = withContext(Dispatchers.IO) {
        vaultHelper.lock()
    }

    fun isUnlocked(): Boolean = vaultHelper.isUnlocked()

    suspend fun addNote(title: String, body: String, sourcePackage: String? = null): Long =
        withContext(Dispatchers.IO) {
            vaultHelper.insertNote(title, body, sourcePackage)
        }

    suspend fun getNotes(): List<VaultNote> = withContext(Dispatchers.IO) {
        vaultHelper.getAllNotes()
    }

    suspend fun deleteNote(id: Long): Int = withContext(Dispatchers.IO) {
        vaultHelper.deleteNote(id)
    }
}
