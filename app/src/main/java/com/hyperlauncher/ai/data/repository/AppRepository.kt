package com.hyperlauncher.ai.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.hyperlauncher.ai.data.local.dao.AppDao
import com.hyperlauncher.ai.data.local.dao.CategoryDao
import com.hyperlauncher.ai.data.local.entity.AppEntity
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val categorizationEngine: CategorizationEngine
) {

    fun observeApps(): Flow<List<AppInfo>> = flow {
        emit(loadAppsFromDb())
    }.flowOn(Dispatchers.IO)

    suspend fun getApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        loadAppsFromDb()
    }

    suspend fun getAppsByCategory(categoryId: String): List<AppInfo> = withContext(Dispatchers.IO) {
        if (categoryId == "all") {
            loadAppsFromDb()
        } else {
            appDao.getByCategory(categoryId).map { it.toAppInfo() }
        }
    }

    suspend fun searchApps(query: String): List<AppInfo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext loadAppsFromDb()
        appDao.search(query.trim()).map { it.toAppInfo() }
    }

    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        val entities = categoryDao.getAll()
        if (entities.isEmpty()) {
            seedCategories()
            categoryDao.getAll().map { it.toCategory() }
        } else {
            entities.map { it.toCategory() }
        }
    }

    suspend fun refreshInstalledApps(): Int = withContext(Dispatchers.IO) {
        val installed = scanInstalledApps()
        appDao.upsertAll(installed)
        installed.size
    }

    suspend fun recordLaunch(packageName: String) = withContext(Dispatchers.IO) {
        appDao.recordLaunch(packageName, System.currentTimeMillis())
    }

    suspend fun getMostUsed(limit: Int = 8): List<AppInfo> = withContext(Dispatchers.IO) {
        appDao.getMostUsed(limit).map { it.toAppInfo() }
    }

    fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun loadAppsFromDb(): List<AppInfo> {
        val entities = appDao.getAll()
        if (entities.isEmpty()) {
            refreshInstalledApps()
            return appDao.getAll().map { it.toAppInfo() }
        }
        return entities.map { it.toAppInfo() }
    }

    private suspend fun seedCategories() {
        val rules = categorizationEngine.getRules()
        val entities = rules.mapIndexed { index, rule ->
            CategoryEntity(id = rule.id, name = rule.name, sortOrder = index)
        }
        categoryDao.upsertAll(entities)
    }

    private fun scanInstalledApps(): List<AppEntity> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }
        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppEntity>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!seen.add(pkg)) continue
            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }
            val categoryId = categorizationEngine.categorize(pkg, label)
            val isSystem = (info.activityInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            result.add(
                AppEntity(
                    packageName = pkg,
                    label = label,
                    categoryId = categoryId,
                    isSystem = isSystem
                )
            )
        }
        return result.sortedBy { it.label.lowercase() }
    }

    private fun AppEntity.toAppInfo() = AppInfo(
        packageName = packageName,
        label = label,
        categoryId = categoryId,
        isSystem = isSystem,
        launchCount = launchCount,
        lastUsedAt = lastUsedAt
    )

    private fun CategoryEntity.toCategory() = Category(
        id = id,
        name = name,
        sortOrder = sortOrder
    )
}
