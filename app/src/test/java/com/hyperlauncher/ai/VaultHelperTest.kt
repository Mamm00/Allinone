package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.local.VaultHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VaultHelperTest {

    @Test
    fun hashPassphrase_isDeterministic() {
        assertEquals(VaultHelper.hashPassphrase("secret123"), VaultHelper.hashPassphrase("secret123"))
    }

    @Test
    fun hashPassphrase_differsForDifferentInputs() {
        assertFalse(VaultHelper.hashPassphrase("secret123") == VaultHelper.hashPassphrase("secret456"))
    }

    @Test
    fun unlock_rejectsShortPassphrase() {
        val helper = VaultHelper(RuntimeEnvironment.getApplication())
        assertFalse(helper.unlock("short"))
    }
}
