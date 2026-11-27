package com.hyperlauncher.ai.data.network

data class ChatMessage(val role: String, val content: String)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024
)

data class ChatCompletionResponse(
    val content: String,
    val model: String,
    val finishReason: String?
)

sealed class AiResult {
    data class Success(val response: ChatCompletionResponse) : AiResult()
    data class Error(val message: String, val cause: Throwable? = null) : AiResult()
    object Offline : AiResult()
    object NoKey : AiResult()
    object Disabled : AiResult()
}
