package com.hyperlauncher.ai.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import com.hyperlauncher.ai.data.local.dao.AppDao
import com.hyperlauncher.ai.data.local.dao.CategoryDao
import com.hyperlauncher.ai.data.local.entity.AppEntity
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.data.model.Category
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val categorizationEngine: CategorizationEngine
) {
    private val mutations = Mutex()

    fun observeApps(): Flow<List<AppInfo>> = appDao.observeAll()
        .map { apps -> apps.map { it.toAppInfo() } }.flowOn(Dispatchers.IO)

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()
        .map { rows -> rows.map { Category(it.id, it.name, it.sortOrder) } }.flowOn(Dispatchers.IO)

    suspend fun getApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        appDao.getAll().map { it.toAppInfo() }
    }

    suspend fun getAppsByCategory(categoryId: String): List<AppInfo> = getApps().filter {
        categoryId == "all" || it.categoryId == categoryId
    }

    suspend fun searchApps(query: String): List<AppInfo> = getApps().filter {
        it.label.contains(query.trim(), true) || it.packageName.contains(query.trim(), true)
    }

    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.seedMissing(categorizationEngine.getRules().mapIndexed { index, rule ->
            CategoryEntity(rule.id, rule.name, index)
        })
        categoryDao.getAll().map { Category(it.id, it.name, it.sortOrder) }
    }

    suspend fun renameCategory(id: String, name: String) = withContext(Dispatchers.IO) {
        require(name.trim().length in 1..40)
        categoryDao.rename(id, name.trim())
    }

    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        require(name.trim().length in 1..40)
        categoryDao.upsertAll(listOf(CategoryEntity("custom_" + UUID.randomUUID(), name.trim(),
            (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1)))
    }

    /** Similar means same inferred category, excluding Unknown and any manual overrides. */
    suspend fun changeCategory(packageName: String, categoryId: String?, applySimilar: Boolean) =
        withContext(Dispatchers.IO) {
            mutations.withLock {
                require(categoryId == null || categoryDao.getAll().any { it.id == categoryId })
                val apps = appDao.getAll()
                val source = apps.firstOrNull { it.packageName == packageName } ?: return@withLock
                val targets = apps.filter {
                    it.packageName == packageName || (applySimilar && source.categoryId != "others" &&
                        it.categoryId == source.categoryId && it.manualCategoryId == null)
                }.map { it.packageName }
                appDao.setManualCategory(targets, categoryId)
            }
        }

    suspend fun refreshInstalledApps(): Int = withContext(Dispatchers.IO) {
        mutations.withLock {
            val previous = appDao.getAll().associateBy { it.packageName }
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            // Do not swallow scan failure and erase a working cache.
            val infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val candidates = infos.map { it.activityInfo.applicationInfo }.toMutableList()
            if (Build.VERSION.SDK_INT >= 35) {
                candidates += pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(
                    PackageManager.MATCH_ARCHIVED_PACKAGES))
                    .filter { it.isArchived }
            }
            val scanned = candidates.distinctBy { it.packageName }
                .filter { it.packageName != context.packageName }.map { info ->
                    val pkg = info.packageName
                    val old = previous[pkg]
                    val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                    val updated = runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)
                    val icon = if (old?.iconPng != null && old.packageUpdatedAt == updated) old.iconPng
                    else runCatching {
                        ByteArrayOutputStream().use { stream ->
                            info.loadIcon(pm).toBitmap(96, 96).compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.toByteArray()
                        }
                    }.getOrNull()
                    AppEntity(
                        packageName = pkg, label = label,
                        categoryId = categorizationEngine.categorize(pkg, label, info.category),
                        isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        launchCount = old?.launchCount ?: 0, lastUsedAt = old?.lastUsedAt ?: 0,
                        manualCategoryId = old?.manualCategoryId,
                        isSuspended = info.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
                        isArchived = Build.VERSION.SDK_INT >= 35 && info.isArchived,
                        iconPng = icon, packageUpdatedAt = updated
                    )
                }
            appDao.replaceScan(scanned)
            scanned.size
        }
    }

    suspend fun recordLaunch(packageName: String) = withContext(Dispatchers.IO) {
        mutations.withLock { appDao.recordLaunch(packageName, System.currentTimeMillis()) }
    }

    suspend fun getMostUsed(limit: Int = 8): List<AppInfo> = withContext(Dispatchers.IO) {
        appDao.getMostUsed(limit).map { it.toAppInfo() }
    }

    fun launchApp(packageName: String): Boolean = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    private fun AppEntity.toAppInfo() = AppInfo(
        packageName = packageName, label = label, categoryId = manualCategoryId ?: categoryId,
        isSystem = isSystem, launchCount = launchCount, lastUsedAt = lastUsedAt,
        inferredCategoryId = categoryId, hasManualCategory = manualCategoryId != null,
        isSuspended = isSuspended, isArchived = isArchived, iconPng = iconPng
    )
}
