package com.hyperlauncher.ai.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenRouter Creative Layer (network flavor only).
 * All calls go through [LockedDownOkHttpClient]. The API key is sent only
 * as an Authorization header and is never logged.
 */
class NetworkOpenRouterService : OpenRouterService {

    private val client = LockedDownOkHttpClient.create()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(
        prompt: String,
        model: String,
        apiKey: String?,
        enabled: Boolean
    ): AiResult {
        if (!enabled) return AiResult.Disabled
        if (apiKey.isNullOrBlank()) return AiResult.NoKey
        return try {
            val body = buildRequestBody(model, prompt)
            val request = Request.Builder()
                .url("https://api.openrouter.ai/api/v1/chat/completions")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return AiResult.Error("Request failed (${response.code})")
                }
                val responseBody = response.body?.string() ?: return AiResult.Error("Empty response")
                val content = parseContent(responseBody) ?: return AiResult.Error("Could not parse response")
                AiResult.Success(
                    ChatCompletionResponse(
                        content = content,
                        model = model,
                        finishReason = null
                    )
                )
            }
        } catch (e: Exception) {
            AiResult.Error("AI request failed. Check your connection and try again.", e)
        }
    }

    private fun buildRequestBody(model: String, prompt: String): String {
        val messages = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "role" to kotlinx.serialization.json.JsonPrimitive("user"),
                        "content" to kotlinx.serialization.json.JsonPrimitive(prompt)
                    )
                )
            )
        )
        val obj = JsonObject(
            mapOf(
                "model" to kotlinx.serialization.json.JsonPrimitive(model),
                "messages" to messages,
                "temperature" to kotlinx.serialization.json.JsonPrimitive(0.7),
                "max_tokens" to kotlinx.serialization.json.JsonPrimitive(1024)
            )
        )
        return obj.toString()
    }

    private fun parseContent(responseBody: String): String? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val choices = root["choices"]?.jsonArray ?: return null
            if (choices.isEmpty()) return null
            val message = choices[0].jsonObject["message"]?.jsonObject ?: return null
            message["content"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
