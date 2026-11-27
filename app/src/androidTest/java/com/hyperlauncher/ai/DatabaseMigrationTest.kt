package com.hyperlauncher.ai

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hyperlauncher.ai.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun v1UpgradePreservesUsageAndSupportsOverrides() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-phase-one.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE apps (packageName TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, categoryId TEXT NOT NULL, isSystem INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL, launchCount INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE ai_prompt_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, prompt TEXT NOT NULL, response TEXT NOT NULL, model TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE llm_response_cache (cacheKey TEXT NOT NULL PRIMARY KEY, response TEXT NOT NULL, model TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("INSERT INTO apps VALUES ('test.pkg', 'Original', 'tools', 0, 1234, 7)")
            db.execSQL("INSERT INTO categories VALUES ('tools', 'My tools', 0)")
            db.version = 1
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            val original = migrated.appDao().getByPackage("test.pkg")!!
            assertEquals(7, original.launchCount)
            assertEquals(1234L, original.lastUsedAt)
            assertNull(original.manualCategoryId)
            assertFalse(original.isSuspended)
            assertEquals("My tools", migrated.categoryDao().getAll().single().name)
            migrated.appDao().setManualCategory(listOf("test.pkg"), "custom")
            assertEquals("custom", migrated.appDao().getByPackage("test.pkg")!!.manualCategoryId)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
