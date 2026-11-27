package com.hyperlauncher.ai

import androidx.lifecycle.ViewModelStore
import com.hyperlauncher.ai.data.local.WorkspaceDao
import com.hyperlauncher.ai.data.local.WorkspaceEntity
import com.hyperlauncher.ai.ui.workspace.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    @Before fun setup() {Dispatchers.setMain(StandardTestDispatcher())}
    @After fun teardown() {Dispatchers.resetMain()}
    private class FakeDao: WorkspaceDao {
        val rows=mutableMapOf<String,MutableSharedFlow<WorkspaceEntity?>>()
        val saved=mutableListOf<WorkspaceEntity>()
        override fun observe(setupId: String)=rows.getOrPut(setupId) {MutableSharedFlow(replay=1)}
        override suspend fun all()=saved.toList()
        override suspend fun save(layout: WorkspaceEntity) {saved.add(layout)}
        override suspend fun saveAll(layouts: List<WorkspaceEntity>) {saved.addAll(layouts)}
        suspend fun emit(id: String,layout: WorkspaceLayout) {observe(id).emit(WorkspaceEntity(id,WorkspaceCodec.encode(layout)))}
    }
    @Test fun olderDatabaseEmissionsCannotRollbackNewerOptimisticEdits()=runTest {
        val dao=FakeDao();val model=WorkspaceViewModel(dao,StandardTestDispatcher(testScheduler))
        val store=ViewModelStore().apply {put("workspace",model)}
        try {
            model.selectSetup("portrait");runCurrent();dao.emit("portrait",WorkspaceLayout());runCurrent()
            val first=WorkspaceLayout(columns=5);val second=first.copy(rows=8)
            model.save(first);model.save(second);runCurrent()
            dao.emit("portrait",first);runCurrent()
            assertEquals(second,model.layout.value)
            dao.emit("portrait",second);runCurrent()
            assertEquals(second,model.layout.value)
            assertEquals(2,dao.saved.size)
            model.selectSetup("landscape")
            model.save(second.copy(columns=7));runCurrent()
            assertEquals("",model.loadedSetup.value)
            assertEquals(2,dao.saved.size)
            dao.emit("landscape",WorkspaceLayout(columns=6));runCurrent()
            assertEquals(6,model.layout.value.columns)
        } finally {store.clear()}
    }
    @Test fun corruptLayoutCannotBeOverwrittenWithDefault()=runTest {
        val dao=FakeDao();val model=WorkspaceViewModel(dao,StandardTestDispatcher(testScheduler))
        val store=ViewModelStore().apply {put("workspace",model)}
        try {
            model.selectSetup("broken");runCurrent()
            dao.observe("broken").emit(WorkspaceEntity("broken","{malformed"));runCurrent()
            model.save(WorkspaceLayout());runCurrent()
            assertTrue(dao.saved.isEmpty());assertEquals("",model.loadedSetup.value);assertNotNull(model.error.value)
        } finally {store.clear()}
    }
}
