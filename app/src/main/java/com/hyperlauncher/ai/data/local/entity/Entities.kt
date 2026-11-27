package com.hyperlauncher.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val categoryId: String,
    val isSystem: Boolean = false,
    val lastUsedAt: Long = 0L,
    val launchCount: Int = 0
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int
)

@Entity(tableName = "ai_prompt_logs")
data class AiPromptLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val response: String,
    val model: String,
    val createdAt: Long
)

@Entity(tableName = "llm_response_cache")
data class LlmCacheEntity(
    @PrimaryKey val cacheKey: String,
    val response: String,
    val model: String,
    val createdAt: Long
)
