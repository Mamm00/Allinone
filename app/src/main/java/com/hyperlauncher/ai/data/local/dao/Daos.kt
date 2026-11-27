package com.hyperlauncher.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.hyperlauncher.ai.data.local.entity.AppEntity
import com.hyperlauncher.ai.data.local.entity.AiPromptLogEntity
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.data.local.entity.LlmCacheEntity

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppEntity>>

    @Query("UPDATE apps SET manualCategoryId = :categoryId WHERE packageName IN (:packages)")
    suspend fun setManualCategory(packages: List<String>, categoryId: String?)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:packages)")
    suspend fun removeMissing(packages: List<String>)

    @Transaction
    suspend fun replaceScan(apps: List<AppEntity>) {
        upsertAll(apps)
        removeMissing(apps.map { it.packageName })
    }

    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE ASC")
    suspend fun getAll(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE COALESCE(manualCategoryId, categoryId) = :categoryId ORDER BY label COLLATE NOCASE ASC")
    suspend fun getByCategory(categoryId: String): List<AppEntity>

    @Query("SELECT * FROM apps WHERE label LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY label COLLATE NOCASE ASC")
    suspend fun search(query: String): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AppEntity)

    @Query("DELETE FROM apps")
    suspend fun clearAll()

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastUsedAt = :now WHERE packageName = :packageName")
    suspend fun recordLaunch(packageName: String, now: Long)

    @Query("SELECT * FROM apps ORDER BY launchCount DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int): List<AppEntity>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedMissing(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface AiPromptLogDao {
    @Query("SELECT * FROM ai_prompt_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AiPromptLogEntity>

    @Insert
    suspend fun insert(log: AiPromptLogEntity)

    @Query("DELETE FROM ai_prompt_logs WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface LlmCacheDao {
    @Query("SELECT * FROM llm_response_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): LlmCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: LlmCacheEntity)

    @Query("DELETE FROM llm_response_cache WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM llm_response_cache")
    suspend fun clearAll()
}
