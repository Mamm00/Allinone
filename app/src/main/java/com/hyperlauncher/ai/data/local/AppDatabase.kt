package com.hyperlauncher.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hyperlauncher.ai.data.local.dao.AiPromptLogDao
import com.hyperlauncher.ai.data.local.dao.AppDao
import com.hyperlauncher.ai.data.local.dao.CategoryDao
import com.hyperlauncher.ai.data.local.dao.LlmCacheDao
import com.hyperlauncher.ai.data.local.entity.AiPromptLogEntity
import com.hyperlauncher.ai.data.local.entity.AppEntity
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.data.local.entity.LlmCacheEntity

@Database(
    entities = [AppEntity::class, CategoryEntity::class, AiPromptLogEntity::class, LlmCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun aiPromptLogDao(): AiPromptLogDao
    abstract fun llmCacheDao(): LlmCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun build(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hyperlauncher.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
