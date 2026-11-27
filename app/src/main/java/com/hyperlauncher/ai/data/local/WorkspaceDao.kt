package com.hyperlauncher.ai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workspace_layouts")
data class WorkspaceEntity(@PrimaryKey val setupId: String, val layoutJson: String)

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_layouts WHERE setupId = :setupId")
    fun observe(setupId: String): Flow<WorkspaceEntity?>
    @Query("SELECT * FROM workspace_layouts")
    suspend fun all(): List<WorkspaceEntity>
    @Upsert suspend fun save(layout: WorkspaceEntity)
    @Upsert suspend fun saveAll(layouts: List<WorkspaceEntity>)
}
