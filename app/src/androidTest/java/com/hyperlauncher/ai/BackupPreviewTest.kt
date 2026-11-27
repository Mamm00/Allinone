package com.hyperlauncher.ai

import androidx.test.platform.app.InstrumentationRegistry
import com.hyperlauncher.ai.backup.LauncherBackup
import com.hyperlauncher.ai.ui.workspace.WorkspaceCodec
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class BackupPreviewTest {
    @Test fun novaAndNativeUseSamePreviewWithoutTransferringWidgetIds()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val backup=LauncherBackup(context)
        val nova="""<favorites><favorite packageName="com.example"/><appwidget _id="2" spanX="2" spanY="2"/></favorites>"""
        val result=backup.preview(ByteArrayInputStream(nova.toByteArray()),"test-setup")
        assertEquals(2,WorkspaceCodec.decode(result.layouts.single().layoutJson).items.size)
        assertTrue(WorkspaceCodec.decode(result.layouts.single().layoutJson).items.all {it.widgets.isEmpty()})
        val native="""{"format":"HyperLauncher","version":1,"layouts":[{"setup":"test","data":{"items":[{"id":"w","widgets":[42]}]}}]}"""
        val bytes=ByteArrayOutputStream()
        ZipOutputStream(bytes).use {it.putNextEntry(ZipEntry("launcher.json"));it.write(native.toByteArray());it.closeEntry()}
        val preview=backup.preview(ByteArrayInputStream(bytes.toByteArray()),"ignored")
        assertTrue(WorkspaceCodec.decode(preview.layouts.single().layoutJson).items.single().widgets.isEmpty())
    }
}
