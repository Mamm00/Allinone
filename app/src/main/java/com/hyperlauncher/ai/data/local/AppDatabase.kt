package com.hyperlauncher.ai.data.local

import android.content.Context
import com.hyperlauncher.ai.feed.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hyperlauncher.ai.data.local.dao.AiPromptLogDao
import com.hyperlauncher.ai.data.local.dao.AppDao
import com.hyperlauncher.ai.data.local.dao.CategoryDao
import com.hyperlauncher.ai.data.local.dao.LlmCacheDao
import com.hyperlauncher.ai.data.local.entity.AiPromptLogEntity
import com.hyperlauncher.ai.data.local.entity.AppEntity
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.data.local.entity.LlmCacheEntity

@Database(
    entities = [AppEntity::class, CategoryEntity::class, AiPromptLogEntity::class, LlmCacheEntity::class, WorkspaceEntity::class, FeedSource::class, FeedArticle::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun aiPromptLogDao(): AiPromptLogDao
    abstract fun llmCacheDao(): LlmCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN manualCategoryId TEXT")
                db.execSQL("ALTER TABLE apps ADD COLUMN isSuspended INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE apps ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE apps ADD COLUMN iconPng BLOB")
                db.execSQL("ALTER TABLE apps ADD COLUMN packageUpdatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workspace_layouts (setupId TEXT NOT NULL PRIMARY KEY, layoutJson TEXT NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS feed_sources (url TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS feed_articles (url TEXT NOT NULL PRIMARY KEY, source TEXT NOT NULL, title TEXT NOT NULL, published TEXT NOT NULL, isRead INTEGER NOT NULL, fetchedAt INTEGER NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun build(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hyperlauncher.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
