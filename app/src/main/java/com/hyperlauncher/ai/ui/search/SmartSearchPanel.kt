package com.hyperlauncher.ai.ui.search

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hyperlauncher.ai.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SearchResult(val title: String, val group: String, val action: () -> Unit, val packageName: String? = null)

@Composable
fun SmartSearchPanel(apps: List<AppInfo>, provider: String, onProvider: (String) -> Unit,
                     onLaunch: (String) -> Unit, onClose: () -> Unit) {
    val context=LocalContext.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }
    var contacts by remember { mutableStateOf(false) }
    var contactResults by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var custom by remember { mutableStateOf(false) }
    var template by remember { mutableStateOf(provider) }
    var error by remember { mutableStateOf<String?>(null) }
    fun launch(intent: Intent) { runCatching { context.startActivity(intent) }.onFailure { error="No app can handle this action" } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { contacts=it }
    LaunchedEffect(query,contacts) {
        contactResults=if (!contacts || query.trim().length<2) emptyList() else withContext(Dispatchers.IO) {
            runCatching {
                val uri=Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,Uri.encode(query.trim()))
                context.contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null)?.use { cursor ->
                    buildList { while(cursor.moveToNext() && size<20) add(cursor.getString(0).orEmpty() to cursor.getString(1).orEmpty()) }
                }.orEmpty()
            }.getOrDefault(emptyList())
        }
        selected=0
    }
    val results=buildList {
        LocalAnswers.answer(query)?.let { add(SearchResult(it,"Answer",{})) }
        if (query.isNotBlank()) apps.filter { it.label.contains(query,true) || it.packageName.contains(query,true) }.take(40).forEach { app ->
            add(SearchResult(app.label,"Apps",{ onLaunch(app.packageName); onClose() },app.packageName))
        }
        contactResults.forEach { (name,number) -> add(SearchResult(name,"Contacts",{ launch(Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",number,null))) })) }
        val settings=listOf("Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,"Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "Display" to Settings.ACTION_DISPLAY_SETTINGS,"Sound" to Settings.ACTION_SOUND_SETTINGS,
            "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,"Apps" to Settings.ACTION_APPLICATION_SETTINGS)
        if (query.isNotBlank()) settings.filter { it.first.contains(query,true) }.forEach { (name,action) ->
            add(SearchResult(name,"Settings",{ launch(Intent(action)) }))
        }
        if (query.isNotBlank()) add(SearchResult("Search the web for “$query”","Web",{
            launch(Intent(Intent.ACTION_VIEW,Uri.parse(SearchTemplates.resolve(provider,query))))
        }))
    }
    Surface(tonalElevation=8.dp,modifier=Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp).imePadding()) {
            Row {
                Text("Smart Search",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                TextButton(onClose) { Text("Close") }
            }
            OutlinedTextField(query,{ query=it; selected=0 },singleLine=true,modifier=Modifier.fillMaxWidth().onPreviewKeyEvent { key ->
                if(key.type!=KeyEventType.KeyDown || results.isEmpty()) false else when(key.key) {
                    Key.DirectionDown -> { selected=(selected+1).coerceAtMost(results.lastIndex); true }
                    Key.DirectionUp -> { selected=(selected-1).coerceAtLeast(0); true }
                    Key.Enter -> { results.getOrNull(selected)?.action?.invoke(); true }
                    else -> false
                }
            },label={ Text("Apps, settings, contacts, calculations…") })
            Row {
                TextButton({ custom=true }) { Text("Web provider") }
                TextButton({
                    if (contacts) contacts=false
                    else if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED) contacts=true
                    else permission.launch(Manifest.permission.READ_CONTACTS)
                }) { Text(if(contacts) "Contacts: on" else "Enable contacts") }
            }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            Text("Drag an app result to the home drop area below, or tap to open.",style=MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(results) { index,result ->
                    val drag=result.packageName?.let { pkg -> Modifier.dragAndDropSource(transferData = {
                        DragAndDropTransferData(ClipData.newPlainText("hyperlauncher/app",pkg))
                    }) } ?: Modifier
                    ListItem(headlineContent={ Text(result.title) },overlineContent={ Text(result.group) },
                        modifier=drag.background(if(index==selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface))
                    TextButton(result.action) { Text(if(result.group=="Answer") "Local result" else "Open ${result.title}") }
                }
            }
        }
    }
    if(custom) AlertDialog(onDismissRequest={custom=false},title={Text("Search provider")},
        text={ Column {
            listOf("Google" to "https://www.google.com/search?q={query}","DuckDuckGo" to "https://duckduckgo.com/?q={query}",
                "Bing" to "https://www.bing.com/search?q={query}").forEach { (name,url) -> TextButton({onProvider(url);custom=false}) { Text(name) } }
            OutlinedTextField(template,{template=it},label={Text("HTTPS URL containing {query}")})
            Text("Only explicit web-result taps open your browser. No query is sent while typing.")
        } },confirmButton={TextButton({onProvider(template);custom=false},enabled=SearchTemplates.valid(template)) {Text("Save custom")}},
        dismissButton={TextButton({custom=false}) {Text("Cancel")}})
}
