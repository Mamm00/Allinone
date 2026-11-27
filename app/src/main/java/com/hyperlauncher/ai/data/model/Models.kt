package com.hyperlauncher.ai.data.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val categoryId: String,
    val isSystem: Boolean = false,
    val launchCount: Int = 0,
    val lastUsedAt: Long = 0L
)

data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int
)

data class CategoryRule(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val packages: List<String>
)
