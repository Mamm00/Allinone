package com.hyperlauncher.ai

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hyperlauncher.ai.data.local.*
import com.hyperlauncher.ai.ui.workspace.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
class WorkspacePersistenceTest {
    @Test fun orientationLayoutsSurviveDatabaseReopen()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="workspace-persistence-test.db";context.deleteDatabase(name)
        val portrait=DisplaySetup.key(0,false,400,900,"flat")
        val landscape=DisplaySetup.key(0,true,900,400,"flat")
        val clock=WorkspaceLayout(items=listOf(Placement(id="clock",kind="clock")))
        var db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            db.workspaceDao().save(WorkspaceEntity(portrait,WorkspaceCodec.encode(clock)))
            db.workspaceDao().save(WorkspaceEntity(landscape,WorkspaceCodec.encode(WorkspaceLayout(columns=7))))
            db.close();db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
            assertEquals("clock",WorkspaceCodec.decode(db.workspaceDao().observe(portrait).first()!!.layoutJson).items.single().id)
            assertEquals(7,WorkspaceCodec.decode(db.workspaceDao().observe(landscape).first()!!.layoutJson).columns)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
