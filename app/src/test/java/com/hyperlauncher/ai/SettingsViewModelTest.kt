package com.hyperlauncher.ai

import android.content.Context
import androidx.lifecycle.ViewModelStore
import com.hyperlauncher.ai.data.local.KeyStoreManager
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.ui.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before fun setup() {Dispatchers.setMain(StandardTestDispatcher())}
    @After fun teardown() {Dispatchers.resetMain()}
    @Test fun rapidMutationsAreOrderedAndFailuresDoNotKillSettings()=runTest {
        val repo=mock(SettingsRepository::class.java);val keys=mock(KeyStoreManager::class.java)
        `when`(repo.openRouterEnabled).thenReturn(MutableStateFlow(false))
        `when`(repo.openRouterModel).thenReturn(MutableStateFlow("original"))
        `when`(repo.accessibilityCaptureEnabled).thenReturn(MutableStateFlow(false))
        `when`(repo.floatingPillEnabled).thenReturn(MutableStateFlow(false))
        val whitelist=MutableStateFlow<Set<String>>(emptySet())
        `when`(repo.whitelistedPackages).thenReturn(whitelist)
        `when`(repo.setWhitelistedPackages(anySet())).thenAnswer {call -> whitelist.value=call.getArgument(0);Unit}
        val vm=SettingsViewModel(mock(Context::class.java),repo,keys,StandardTestDispatcher(testScheduler),{emptyList()})
        val store=ViewModelStore().apply {put("settings",vm)}
        try {
            vm.setOpenRouterEnabled(true);vm.setOpenRouterModel("new");vm.toggleWhitelist("one");vm.toggleWhitelist("two")
            vm.saveApiKey("test-only");vm.clearApiKey();advanceUntilIdle()
            assertTrue(vm.uiState.value.openRouterEnabled);assertEquals("new",vm.uiState.value.openRouterModel)
            assertEquals(setOf("one","two"),vm.uiState.value.whitelistedPackages);assertFalse(vm.uiState.value.hasApiKey)
            val order=inOrder(keys);order.verify(keys).hasOpenRouterKey();order.verify(keys).saveOpenRouterKey("test-only");order.verify(keys).clearOpenRouterKey()
            `when`(repo.setOpenRouterEnabled(false)).thenThrow(IllegalStateException("private detail"))
            vm.setOpenRouterEnabled(false);advanceUntilIdle()
            assertNotNull(vm.uiState.value.errorMessage);assertFalse(vm.uiState.value.errorMessage!!.contains("private detail"))
            vm.setOpenRouterModel("recovered");advanceUntilIdle()
            assertEquals("recovered",vm.uiState.value.openRouterModel);assertNull(vm.uiState.value.errorMessage)
        } finally {store.clear()}
    }
}
