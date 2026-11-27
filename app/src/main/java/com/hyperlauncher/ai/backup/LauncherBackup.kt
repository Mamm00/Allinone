package com.hyperlauncher.ai.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import com.hyperlauncher.ai.HyperLauncherApp
import com.hyperlauncher.ai.data.local.*
import com.hyperlauncher.ai.data.local.entity.CategoryEntity
import com.hyperlauncher.ai.feed.FeedSource
import com.hyperlauncher.ai.ui.workspace.*
import com.hyperlauncher.importer.NovaXmlParser
import java.io.*
import java.util.zip.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Portable configuration snapshot. Never exports keys, vault contents, notification data or PINs. */
class LauncherBackup(private val context: Context) {
    private val db get()=(context.applicationContext as HyperLauncherApp).container.database
    suspend fun export(output: OutputStream)=withContext(Dispatchers.IO) {
        val snapshot=db.withTransaction {
            Triple(db.workspaceDao().all(),db.categoryDao().getAll(),db.appDao().getAll().filter {it.manualCategoryId!=null})
        }
        val feedSources=db.feedDao().sources()
        val json=buildJsonObject {
                put("format","HyperLauncher");put("version",1)
                putJsonArray("layouts") {snapshot.first.forEach {row -> add(buildJsonObject {put("setup",row.setupId);put("data",Json.parseToJsonElement(row.layoutJson))})}}
                putJsonArray("categories") {snapshot.second.forEach {row ->add(buildJsonObject {put("id",row.id);put("name",row.name);put("order",row.sortOrder)})}}
                putJsonArray("assignments") {snapshot.third.forEach {row -> add(buildJsonObject {put("package",row.packageName);put("category",row.manualCategoryId)})}}
                putJsonArray("feedSources") {feedSources.forEach {row ->add(buildJsonObject {put("url",row.url);put("name",row.name)})}}
            }.toString()
        ZipOutputStream(output).use {zip ->
            zip.putNextEntry(ZipEntry("launcher.json"));zip.write(json.toByteArray());zip.closeEntry()
            val uris=snapshot.first.flatMap {row -> runCatching {
                WorkspaceCodec.decode(row.layoutJson).options["kwgtExports"]?.let {Json.parseToJsonElement(it).jsonArray.map {uri->uri.jsonPrimitive.content}}.orEmpty()
            }.getOrDefault(emptyList())}.distinct().take(10)
            var total=0
            uris.forEachIndexed {index,uri ->
                val bytes=context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use {bounded(it,32*1024*1024)} ?: return@forEachIndexed
                total+=bytes.size;require(total<=32*1024*1024) {"Selected KWGT exports exceed the 32 MiB backup safety budget"}
                zip.putNextEntry(ZipEntry("kwgt/export-$index.kwgt"));zip.write(bytes);zip.closeEntry()
            }
        }
    }
    data class Preview(val layouts: List<WorkspaceEntity>,val categories: List<CategoryEntity> = emptyList(),
        val assignments: Map<String,String> = emptyMap(),val sources: List<FeedSource> = emptyList(),val note: String)
    suspend fun preview(input: InputStream,currentSetup: String): Preview=withContext(Dispatchers.IO) {
        val bytes=input.use {bounded(it,64*1024*1024)}
        val content=if(bytes.size>=2 && bytes[0]==0x50.toByte() && bytes[1]==0x4b.toByte()) {
            ZipInputStream(ByteArrayInputStream(bytes)).use {zip ->
                var found: String?=null;var count=0;var total=0
                while(true) {
                    val entry=zip.nextEntry ?:break
                    require(++count<=100) {"Too many archive entries"}
                    val entryData=bounded(zip,32*1024*1024);total+=entryData.size
                    require(total<=64*1024*1024) {"Expanded backup exceeds safety budget"}
                    if(entry.name in listOf("launcher.json","favorites.xml","workspace.xml","nova.xml")) {
                        require(found==null) {"Ambiguous backup archive"};require(entryData.size<=4*1024*1024);found=entryData.toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                }
                found ?: error("This Nova archive is not a supported XML export. Binary/SQLite exports require a version-specific adapter. No data was changed.")
            }
        } else bytes.toString(Charsets.UTF_8)
        if(content.trimStart().startsWith("<")) {
            val nova=NovaXmlParser.parse(content)
            val top=nova.items.filter {it.container=="-100" || it.container=="desktop"}
            val items=top.map {entry ->
                val pkg=if(entry.kind=="folder") nova.items.filter {it.container==entry.id}.map {it.packageName}.filter {it.isNotBlank()}.joinToString("|") else entry.packageName
                Placement(page=entry.page,x=entry.x,y=entry.y,width=entry.width,height=entry.height,kind=entry.kind,packageName=pkg).bounded()
            }
            val options=nova.gestures.mapKeys {"gesture_"+it.key}+mapOf("dock" to nova.dock.joinToString("|"))
            val layout=WorkspaceLayout(pages=((items.maxOfOrNull {it.page} ?:0)+1).coerceIn(1,9),items=items,options=options)
            Preview(listOf(WorkspaceEntity(currentSetup,WorkspaceCodec.encode(layout))),note="Nova XML: ${items.size} items. Widgets require Android rebinding. Only supported XML fields are mapped; review before applying.")
        } else {
            val root=Json.parseToJsonElement(content).jsonObject
            require(root["format"]?.jsonPrimitive?.content=="HyperLauncher" && root["version"]?.jsonPrimitive?.int==1) {"Unsupported native backup version"}
            val layouts=root["layouts"]?.jsonArray.orEmpty().take(100).map {raw ->
                val row=raw.jsonObject;val setup=row["setup"]!!.jsonPrimitive.content
                require(setup.length in 1..200)
                val layout=WorkspaceCodec.decode(row["data"].toString())
                WorkspaceEntity(setup,WorkspaceCodec.encode(layout.copy(items=layout.items.map {it.copy(widgets=emptyList())})))
            }
            val categories=root["categories"]?.jsonArray.orEmpty().take(100).map {raw ->val row=raw.jsonObject;CategoryEntity(row["id"]!!.jsonPrimitive.content.take(100),row["name"]!!.jsonPrimitive.content.take(40),row["order"]!!.jsonPrimitive.int)}
            val assignments=root["assignments"]?.jsonArray.orEmpty().take(3000).associate {raw ->val row=raw.jsonObject;row["package"]!!.jsonPrimitive.content to row["category"]!!.jsonPrimitive.content}
            val sources=root["feedSources"]?.jsonArray.orEmpty().take(30).map {raw ->val row=raw.jsonObject;FeedSource(row["url"]!!.jsonPrimitive.content,row["name"]!!.jsonPrimitive.content)}
                .filter {com.hyperlauncher.feed.FeedParser.safeUrl(it.url)}
            Preview(layouts,categories,assignments,sources,"${layouts.size} display/orientation layouts. Matching layouts will be replaced; other setups remain. Widget IDs are never transferred: rebind widgets after restore.")
        }
    }
    suspend fun restore(preview: Preview)=withContext(Dispatchers.IO) {
        db.withTransaction {
            db.workspaceDao().saveAll(preview.layouts)
            db.categoryDao().upsertAll(preview.categories)
            val valid=db.categoryDao().getAll().map {it.id}.toSet()
            preview.assignments.filterValues {it in valid}.forEach {(pkg,category)->db.appDao().setManualCategory(listOf(pkg),category)}
            preview.sources.forEach {db.feedDao().addSource(it)}
        }
    }
    private fun bounded(input: InputStream,max: Int): ByteArray {
        val result=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) {val n=input.read(buffer);if(n<0) break;require(result.size()+n<=max) {"Backup input exceeds safe import limit"};result.write(buffer,0,n)}
        return result.toByteArray()
    }
}

class DailyBackupWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result=withContext(Dispatchers.IO) {
        try {
            val dir=File(applicationContext.filesDir,"launcher-backups").apply {mkdirs()}
            val destination=File(dir,"launcher-${java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(java.util.Date())}.zip")
            val temporary=File(dir,"pending-backup.tmp")
            LauncherBackup(applicationContext).export(temporary.outputStream())
            check(temporary.renameTo(destination))
            dir.listFiles()?.filter {it.extension=="zip"}?.sortedByDescending {it.name}?.drop(7)?.forEach {it.delete()}
            Result.success()
        } catch(e: Exception) {if(e is kotlinx.coroutines.CancellationException) throw e;Result.failure(workDataOf("error" to "Local backup failed"))}
    }
}
