package com.hyperlauncher.ai

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import com.hyperlauncher.ai.data.local.DrawerPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperlauncher.ai.ui.ai.AiPromptBar
import com.hyperlauncher.ai.ui.ai.AiPromptViewModel
import com.hyperlauncher.ai.ui.workspace.WorkspaceScreen
import com.hyperlauncher.ai.ui.workspace.WorkspaceViewModel
import com.hyperlauncher.ai.ui.launcher.LauncherScreen
import com.hyperlauncher.ai.ui.launcher.LauncherViewModel
import com.hyperlauncher.ai.ui.settings.SettingsScreen
import com.hyperlauncher.ai.ui.settings.SettingsViewModel
import com.hyperlauncher.ai.ui.theme.HyperLauncherAITheme
import com.hyperlauncher.ai.ui.vault.VaultScreen
import com.hyperlauncher.ai.ui.vault.VaultViewModel
import com.hyperlauncher.ai.util.PerformanceMonitor

private enum class Destination { HOME, LAUNCHER, SETTINGS, VAULT }

class MainActivity : ComponentActivity() {

    private val startTimeMs = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HyperLauncherApp
        val container = app.container

        // Cold-start perf gate: log only in debug builds (no telemetry).
        if (BuildConfig.DEBUG) {
            val elapsed = PerformanceMonitor.measureColdStart(startTimeMs)
            android.util.Log.d("HyperLauncher", "Cold start: ${elapsed}ms")
        }

        setContent {
            val drawerPreferences by container.settingsRepository.drawerPreferences.collectAsState(DrawerPreferences())
            HyperLauncherAITheme(ambient = drawerPreferences.ambient) {
                var destination by rememberSaveable { mutableStateOf(Destination.HOME) }

                val launcherViewModel: LauncherViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LauncherViewModel(container.appRepository, container.settingsRepository) as T
                        }
                    }
                )
                val workspaceViewModel: WorkspaceViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return WorkspaceViewModel(container.database.workspaceDao()) as T
                    }
                })
                val launcherState by launcherViewModel.uiState.collectAsState()
                val aiViewModel: AiPromptViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AiPromptViewModel(
                                openRouterService = container.openRouterService,
                                keyStoreManager = container.keyStoreManager,
                                settingsRepository = container.settingsRepository,
                                vaultRepository = container.vaultRepository
                            ) as T
                        }
                    }
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SettingsViewModel(
                                context = this@MainActivity,
                                settingsRepository = container.settingsRepository,
                                keyStoreManager = container.keyStoreManager
                            ) as T
                        }
                    }
                )
                val vaultViewModel: VaultViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return VaultViewModel(
                                vaultRepository = container.vaultRepository,
                                keyStoreManager = container.keyStoreManager
                            ) as T
                        }
                    }
                )

                BackHandler(enabled = destination != Destination.HOME) { destination = Destination.HOME }
                when (destination) {
                    Destination.HOME -> WorkspaceScreen(workspaceViewModel, launcherState.allApps,
                        onDrawer = { launcherViewModel.load(); destination = Destination.LAUNCHER },
                        onSettings = { destination = Destination.SETTINGS }, onLaunch = launcherViewModel::onAppLaunched)
                    Destination.LAUNCHER -> {
                        LauncherScreen(
                            viewModel = launcherViewModel,
                            onOpenSettings = { destination = Destination.SETTINGS },
                            onOpenVault = { destination = Destination.VAULT },
                            aiPromptBar = { AiPromptBar(viewModel = aiViewModel) }
                        )
                    }
                    Destination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { destination = Destination.HOME }
                        )
                    }
                    Destination.VAULT -> {
                        VaultScreen(
                            viewModel = vaultViewModel,
                            onBack = { destination = Destination.HOME }
                        )
                    }
                }
            }
        }
    }
}
