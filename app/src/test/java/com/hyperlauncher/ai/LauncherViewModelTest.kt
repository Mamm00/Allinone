package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.local.DrawerPreferences
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import com.hyperlauncher.ai.data.repository.AppRepository
import com.hyperlauncher.ai.ui.launcher.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun cachedAppsAndSearchSurviveCategoryChanges() = runTest {
        val repository = mock(AppRepository::class.java)
        val settings = mock(SettingsRepository::class.java)
        val apps = MutableStateFlow(listOf(AppInfo("a", "Mail", "messaging"), AppInfo("b", "Maps", "travel")))
        `when`(repository.observeApps()).thenReturn(apps)
        `when`(repository.observeCategories()).thenReturn(MutableStateFlow(emptyList<Category>()))
        `when`(settings.drawerPreferences).thenReturn(MutableStateFlow(DrawerPreferences()))
        `when`(settings.hiddenPackages).thenReturn(MutableStateFlow(emptySet()))
        `when`(settings.hiddenPinConfigured).thenReturn(MutableStateFlow(false))
        `when`(repository.getCategories()).thenReturn(emptyList())
        `when`(repository.refreshInstalledApps()).thenReturn(2)
        val vm = LauncherViewModel(repository, settings, StandardTestDispatcher(testScheduler))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        advanceUntilIdle()
        vm.onSearchQueryChange("Ma")
        vm.selectCategory("travel")
        advanceUntilIdle()
        assertEquals(listOf("b"), vm.uiState.value.apps.map { it.packageName })
        assertEquals("Ma", vm.uiState.value.searchQuery)
        vm.selectCategory("all")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.apps.size)
        assertFalse(vm.uiState.value.isLoading)
        verify(repository, never()).searchApps(anyString())
    }
}
