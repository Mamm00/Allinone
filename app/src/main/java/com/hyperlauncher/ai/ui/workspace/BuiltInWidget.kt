package com.hyperlauncher.ai.ui.workspace

import com.hyperlauncher.ai.ui.appearance.LocalClockFont
import android.Manifest
import android.app.AlarmManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hyperlauncher.ai.notifications.LauncherNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BuiltInWidget(item: Placement,options: Map<String,String>,onOption: (String,String)->Unit,
                  onFeed: ()->Unit) {
    val context=LocalContext.current
    var editing by remember {mutableStateOf(false)}
    var text by remember(options,item.id) {mutableStateOf(options["note_${item.id}"].orEmpty())}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    var battery by remember {mutableIntStateOf(-1)}
    DisposableEffect(context) {
        val receiver=object : BroadcastReceiver() {
            override fun onReceive(c: Context?,intent: Intent?) {
                now=System.currentTimeMillis()
                if(intent?.action==Intent.ACTION_BATTERY_CHANGED) {
                    val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
                    val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100)
                    battery=if(scale>0) level*100/scale else -1
                }
            }
        }
        val filter=IntentFilter().apply {addAction(Intent.ACTION_TIME_TICK);addAction(Intent.ACTION_TIME_CHANGED);addAction(Intent.ACTION_TIMEZONE_CHANGED);addAction(Intent.ACTION_BATTERY_CHANGED)}
        ContextCompat.registerReceiver(context,receiver,filter,ContextCompat.RECEIVER_EXPORTED)
        onDispose {context.unregisterReceiver(receiver)}
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) {
            runCatching {context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            onOption("photo_${item.id}",uri.toString())
        }
    }
    var calendarAllowed by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED)}
    val calendarPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {calendarAllowed=it}
    var calendarRevision by remember {mutableIntStateOf(0)}
    DisposableEffect(calendarAllowed) {
        val observer=object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {calendarRevision++}
        }
        if(calendarAllowed) runCatching {context.contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,true,observer)}
        onDispose {runCatching {context.contentResolver.unregisterContentObserver(observer)}}
    }
    val nextEvent by produceState<String?>(null,calendarAllowed,calendarRevision) {
        value=if(!calendarAllowed) null else withContext(Dispatchers.IO) {runCatching {
            val builder=CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder,System.currentTimeMillis());ContentUris.appendId(builder,System.currentTimeMillis()+7*86400000L)
            context.contentResolver.query(builder.build(),arrayOf(CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN),
                null,null,CalendarContract.Instances.BEGIN+" ASC")?.use {cursor ->
                if(cursor.moveToFirst()) cursor.getString(0)+" · "+DateFormat.getTimeFormat(context).format(java.util.Date(cursor.getLong(1))) else "No events in the next 7 days"
            }
        }.getOrNull()}
    }
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        when(item.kind) {
            "clock" -> {
                Text(DateFormat.getTimeFormat(context).format(java.util.Date(now)),style=MaterialTheme.typography.headlineLarge.copy(fontFamily=LocalClockFont.current))
                Text(DateFormat.getMediumDateFormat(context).format(java.util.Date(now)),style=MaterialTheme.typography.bodySmall)
                val alarm=(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock
                alarm?.let {Text("Alarm: "+DateFormat.getTimeFormat(context).format(java.util.Date(it.triggerTime)),style=MaterialTheme.typography.bodySmall)}
                nextEvent?.let {Text(it,maxLines=2,style=MaterialTheme.typography.bodySmall)}
            }
            "calendar" -> {
                Text("Calendar",style=MaterialTheme.typography.titleMedium)
                if(!calendarAllowed) TextButton({calendarPermission.launch(Manifest.permission.READ_CALENDAR)}) {Text("Allow calendar access")}
                else Text(nextEvent ?: "No readable event")
            }
            "battery" -> {Text(if(battery>=0) "$battery%" else "Battery unavailable",style=MaterialTheme.typography.headlineMedium);Text("Battery")}
            "notes" -> TextButton({editing=true}) {Text(text.ifBlank {"Tap to write a home note (not encrypted)"},maxLines=6)}
            "photos" -> {
                val uri=options["photo_${item.id}"]
                val bitmap by produceState<android.graphics.Bitmap?>(null,uri) {value=withContext(Dispatchers.IO) {
                    uri?.let {runCatching {context.contentResolver.openInputStream(android.net.Uri.parse(it))?.use {stream ->
                        BitmapFactory.decodeStream(stream,null,BitmapFactory.Options().apply {inSampleSize=16})
                    }}.getOrNull()}
                }}
                bitmap?.let {Image(it.asImageBitmap(),null,Modifier.weight(1f).fillMaxWidth())}
                TextButton({photo.launch(arrayOf("image/*"))}) {Text("Choose photo")}
            }
            "weather" -> com.hyperlauncher.ai.weather.WeatherWidget(options["weatherCoordinate"].orEmpty()) {onOption("weatherCoordinate",it)}
            "music" -> MusicWidget()
            "rss" -> TextButton(onFeed) {Text("Your Feed · read saved articles")}
        }
    }
    if(editing) AlertDialog(onDismissRequest={editing=false},title={Text("Home note (not vault)")},
        text={OutlinedTextField(text,{text=it.take(4000)},label={Text("Stored locally, unencrypted")})},
        confirmButton={TextButton({onOption("note_${item.id}",text);editing=false}){Text("Save")}},
        dismissButton={TextButton({editing=false}){Text("Cancel")}})
}

@Composable
private fun MusicWidget() {
    val context=LocalContext.current
    val manager=remember {context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager}
    var controller by remember {mutableStateOf<MediaController?>(null)}
    var title by remember {mutableStateOf("No active music session")}
    DisposableEffect(manager) {
        var current: MediaController?=null
        val callback=object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {title=metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Music"}
        }
        val listener=MediaSessionManager.OnActiveSessionsChangedListener {sessions ->
            current?.unregisterCallback(callback);current=sessions?.firstOrNull();controller=current
            title=current?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "No active music session"
            current?.registerCallback(callback)
        }
        val component=ComponentName(context,LauncherNotificationListener::class.java)
        runCatching {
            manager.addOnActiveSessionsChangedListener(listener,component,Handler(Looper.getMainLooper()))
            listener.onActiveSessionsChanged(manager.getActiveSessions(component))
        }.onFailure {title="Enable notification access to control an active media session"}
        onDispose {runCatching {manager.removeOnActiveSessionsChangedListener(listener)};current?.unregisterCallback(callback)}
    }
    Text(title,maxLines=2)
    Row {
        TextButton({controller?.transportControls?.play()}) {Text("Play")}
        TextButton({controller?.transportControls?.pause()}) {Text("Pause")}
    }
    if(controller==null) TextButton({runCatching {context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}}) {Text("Notification access")}
}
