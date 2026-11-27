package com.hyperlauncher.ai.data.network

import android.content.Context
import com.hyperlauncher.ai.data.local.KeyStoreManager

/**
 * Offline flavor: no network stack. The OpenRouter service always returns
 * [AiResult.Offline]. This build declares no INTERNET permission, so even
 * if network code were present it could not reach the network.
 */
class NetworkModule private constructor(val openRouterService: OpenRouterService) {

    fun enforceLockdown() {
        // No-op: offline build has no INTERNET permission and no network client.
    }

    companion object {
        fun create(context: Context, keyStoreManager: KeyStoreManager): NetworkModule {
            return NetworkModule(OfflineOpenRouterService())
        }
    }
}

class OfflineOpenRouterService : OpenRouterService {
    override suspend fun complete(
        prompt: String,
        model: String,
        apiKey: String?,
        enabled: Boolean
    ): AiResult {
        return AiResult.Offline
    }
}
