package com.hyperlauncher.ai.di

import android.content.Context
import com.hyperlauncher.ai.data.local.AppDatabase
import com.hyperlauncher.ai.data.local.KeyStoreManager
import com.hyperlauncher.ai.data.local.SettingsRepository
import com.hyperlauncher.ai.data.local.VaultHelper
import com.hyperlauncher.ai.data.network.NetworkModule
import com.hyperlauncher.ai.data.network.OpenRouterService
import com.hyperlauncher.ai.data.repository.AppRepository
import com.hyperlauncher.ai.data.repository.CategorizationEngine
import com.hyperlauncher.ai.data.repository.VaultRepository

/**
 * Manual dependency container (no Hilt/Dagger — keeps APK small and startup fast).
 * Flavor-specific network wiring is provided by [NetworkModule] in each flavor source set.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.build(appContext)
    }

    val keyStoreManager: KeyStoreManager by lazy {
        KeyStoreManager(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val vaultHelper: VaultHelper by lazy {
        VaultHelper(appContext)
    }

    val categorizationEngine: CategorizationEngine by lazy {
        CategorizationEngine(appContext)
    }

    val appRepository: AppRepository by lazy {
        AppRepository(
            context = appContext,
            appDao = database.appDao(),
            categoryDao = database.categoryDao(),
            categorizationEngine = categorizationEngine
        )
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepository(vaultHelper = vaultHelper)
    }

    val networkModule: NetworkModule by lazy {
        NetworkModule.create(appContext, keyStoreManager)
    }

    val openRouterService: OpenRouterService by lazy {
        networkModule.openRouterService
    }
}
