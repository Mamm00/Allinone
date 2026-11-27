package com.hyperlauncher.ai

import com.hyperlauncher.ai.data.repository.AppRepository
import com.hyperlauncher.ai.ui.launcher.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        val repository = mock(AppRepository::class.java)
        `when`(repository.getCategories()).thenReturn(emptyList())
        `when`(repository.getApps()).thenReturn(emptyList())
        val viewModel = LauncherViewModel(repository)
        assertEquals(true, viewModel.uiState.value.isLoading)
    }
}
