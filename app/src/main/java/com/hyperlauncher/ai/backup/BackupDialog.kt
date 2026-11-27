package com.hyperlauncher.ai.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Composable
fun BackupDialog(setupId: String,onKwgt: (List<String>)->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val backup=remember {LauncherBackup(context)}
    var preview by remember {mutableStateOf<LauncherBackup.Preview?>(null)}
    var message by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    val manager=remember {WorkManager.getInstance(context)}
    val jobs by manager.getWorkInfosForUniqueWorkFlow("launcher-daily-backup").collectAsState(emptyList())
    val daily=jobs.any {!it.state.isFinished}
    val snapshots by produceState<List<java.io.File>>(emptyList(),jobs) {
        value=withContext(Dispatchers.IO) {
            java.io.File(context.filesDir,"launcher-backups").listFiles()?.filter {it.extension=="zip"}
                ?.sortedByDescending {it.name}?.take(7).orEmpty()
        }
    }
    var exportSnapshot by remember {mutableStateOf<java.io.File?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {uri ->
        if(uri!=null) scope.launch {busy=true;runCatching {withContext(Dispatchers.IO) {
            val destination=context.contentResolver.openOutputStream(uri) ?:error("Cannot open destination")
            val saved=exportSnapshot
            if(saved==null) backup.export(destination) else destination.use {output ->saved.inputStream().use {it.copyTo(output)}}

        }}.onSuccess {message="Backup saved. Keep it private: it includes app layout and unencrypted home notes."}.onFailure {message=it.message};busy=false}
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) scope.launch {busy=true;runCatching {withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.let {backup.preview(it,setupId)} ?:error("Cannot read file")
        }}.onSuccess {preview=it}.onFailure {message=it.message};busy=false}
    }
    val kwgt=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {uris ->
        uris.forEach {uri ->runCatching {context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}}
        onKwgt(uris.take(10).map {it.toString()});message="Selected exported files will be included in future backups. Restore them through KWGT itself."
    }
    AlertDialog(onDismissRequest={if(!busy) onDismiss()},title={Text("Import and backup")},text={
        Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState())) {
            Text("Configuration snapshots never include vault contents, API keys or notification data. Exported home notes and app layout are not encrypted.")
            TextButton({exportSnapshot=null;export.launch("HyperLauncher-backup.zip")},enabled=!busy) {Text("Export native backup")}
            TextButton({restore.launch(arrayOf("*/*"))},enabled=!busy) {Text("Preview native restore or Nova XML import")}
            Text("Nova XML favorites, dock, folders, geometry and gesture fields are supported. Proprietary/binary Nova variants are not. Android widget IDs cannot be transferred: imported widgets need rebinding.")
            TextButton({kwgt.launch(arrayOf("*/*"))},enabled=!busy) {Text("Include user-exported KWGT files")}
            Text("Daily backup keeps the latest 7 snapshots in private app storage. Android schedules timing; uninstall removes these copies. Export a separate backup for safekeeping.")
            Row {
                Text("Daily local backup",Modifier.weight(1f))
                Switch(daily,{enabled ->if(enabled) manager.enqueueUniquePeriodicWork("launcher-daily-backup",ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<DailyBackupWorker>(1,TimeUnit.DAYS).build()) else manager.cancelUniqueWork("launcher-daily-backup")})
            }
            snapshots.forEach {file ->
                Text(file.name,style=MaterialTheme.typography.bodySmall)
                Row {
                    TextButton({scope.launch {busy=true;runCatching {withContext(Dispatchers.IO) {
                        backup.preview(file.inputStream(),setupId)
                    }}.onSuccess {preview=it}.onFailure {message=it.message};busy=false}},enabled=!busy) {Text("Preview restore")}
                    TextButton({exportSnapshot=file;export.launch(file.name)},enabled=!busy) {Text("Export copy")}
                }
            }
            message?.let {Text(it)}
            if(busy) CircularProgressIndicator()
        }
    },confirmButton={TextButton(onDismiss,enabled=!busy){Text("Done")}})
    preview?.let {candidate ->AlertDialog(onDismissRequest={if(!busy) preview=null},title={Text("Confirm restore/import")},
        text={Text(candidate.note+"\nOnly import files you trust. No data changes until you confirm. KWGT exports stay inside the ZIP for manual import in KWGT.")},
        confirmButton={TextButton({scope.launch {busy=true;runCatching {backup.restore(candidate)}.onSuccess {message="Restore applied. Rebind imported widgets using + Widget on each empty slot."}.onFailure {message=it.message};preview=null;busy=false}},enabled=!busy){Text("Apply")}},
        dismissButton={TextButton({preview=null},enabled=!busy){Text("Cancel")}})}
}
