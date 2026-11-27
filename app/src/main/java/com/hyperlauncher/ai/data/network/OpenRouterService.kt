package com.hyperlauncher.ai.data.network

/**
 * Creative Layer interface. Implemented by the network flavor (real OpenRouter
 * calls behind the locked-down client) and stubbed in the offline flavor.
 */
interface OpenRouterService {
    suspend fun complete(
        prompt: String,
        model: String,
        apiKey: String?,
        enabled: Boolean
    ): AiResult
}
