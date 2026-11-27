package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.local.VaultCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VaultHelperTest {

    @Test
    fun hashPassphrase_isDeterministic() {
        assertEquals(VaultCrypto.hashPassphrase("secret123"), VaultCrypto.hashPassphrase("secret123"))
    }

    @Test
    fun hashPassphrase_differsForDifferentInputs() {
        assertFalse(VaultCrypto.hashPassphrase("secret123") == VaultCrypto.hashPassphrase("secret456"))
    }

    @Test
    fun minLength_isEight() {
        assertEquals(8, VaultCrypto.MIN_PASSPHRASE_LENGTH)
    }
}
