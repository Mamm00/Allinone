package com.hyperlauncher.ai.data.local

import java.security.MessageDigest

/**
 * Pure-JVM vault crypto helpers (no SQLCipher dependency, so unit-testable
 * without native libraries).
 */
object VaultCrypto {
    const val MIN_PASSPHRASE_LENGTH = 8

    fun hashPassphrase(passphrase: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
