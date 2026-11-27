package com.hyperlauncher.ai.ui.workspace

import com.hyperlauncher.ai.ui.appearance.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.ai.HyperLauncherApp
import com.hyperlauncher.ai.backup.BackupDialog
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import com.hyperlauncher.ai.feed.FeedScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.hyperlauncher.ai.ui.gestures.*
import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.widget.Toast
import com.hyperlauncher.ai.service.GestureAccessibilityService
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import com.hyperlauncher.ai.ui.search.SmartSearchPanel
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hyperlauncher.ai.data.model.AppInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WorkspaceScreen(model: WorkspaceViewModel, apps: List<AppInfo>, onDrawer: () -> Unit,
                    onSettings: () -> Unit, onLaunch: (String) -> Unit,preferencesRequest: Int=0) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val adaptive=rememberAdaptiveSetup()
    val setup=adaptive.key
    LaunchedEffect(setup) { model.selectSetup(setup) }
    val stored by model.layout.collectAsState()
    val loaded by model.loadedSetup.collectAsState()
    var layout by remember(setup) { mutableStateOf(WorkspaceLayout()) }
    LaunchedEffect(stored, setup, loaded) { layout = if (loaded == setup) stored else WorkspaceLayout() }
    fun save(value: WorkspaceLayout) { layout = value; model.save(value) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(false) }
    LaunchedEffect(preferencesRequest) {if(preferencesRequest>0) preferences=true}
    var widgetTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var pin by remember { mutableStateOf(false) }
    var pinDock by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var gesturePrefs by remember { mutableStateOf(false) }
    var iconPrefs by remember { mutableStateOf<Placement?>(null) }
    var folder by remember { mutableStateOf<Placement?>(null) }
    var builtins by remember {mutableStateOf(false)}
    var feeding by rememberSaveable {mutableStateOf(false)}
    var backupDialog by remember {mutableStateOf(false)}
    var appearanceDialog by remember {mutableStateOf(false)}
    val appearance=LocalAppearance.current
    DisposableEffect(appearance["immersive"]) {
        val activity=context as? android.app.Activity
        val controller=activity?.window?.let {WindowCompat.getInsetsController(it,it.decorView)}
        if(appearance["immersive"]=="true") {
            controller?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {controller?.show(WindowInsetsCompat.Type.systemBars())}
    }

    val host = remember { AppWidgetHost(context.applicationContext, 0x48594c) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(host, lifecycle) {
        fun start() { runCatching { host.startListening() } }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) start()
            if (event == Lifecycle.Event.ON_STOP) host.stopListening()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { lifecycle.removeObserver(observer); host.stopListening() }
    }
    val pager = rememberPagerState(initialPage = CircularPages.initial(layout.pages), pageCount = { Int.MAX_VALUE })
    val logical = CircularPages.logical(pager.currentPage, layout.pages)
    LaunchedEffect(pager,layout.options["feedEnabled"]) {
        snapshotFlow {pager.currentPage to pager.currentPageOffsetFraction}.collect {(page,offset)->
            if(BuildConfig.ENABLE_FEED && layout.options["feedEnabled"]=="true" && CircularPages.logical(page,layout.pages)==0 && offset<-.22f) {
                feeding=true
                pager.scrollToPage(page)
            }
        }
    }
    val scope = rememberCoroutineScope()
    val wallpaperPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) scope.launch {
            runCatching {
                val wallpaperId=withContext(Dispatchers.IO) {
                    val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                    context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
                    require(bounds.outWidth>0 && bounds.outHeight>0)
                    var sample=1
                    while(bounds.outWidth/sample>1600 || bounds.outHeight/sample>1600) sample*=2
                    val bitmap=context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})}
                        ?: error("Unreadable image")
                    java.io.File(context.filesDir,"workspace-wallpaper.jpg").outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)}
                    WallpaperManager.getInstance(context).setBitmap(bitmap)
                    WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                }
                save(layout.option("wallpaperVersion",wallpaperId.toString()))
            }.onFailure {Toast.makeText(context,"Wallpaper could not be applied",Toast.LENGTH_LONG).show()}
        }
    }

    fun perform(action: String, origin: Offset = Offset.Zero) {
        fun unavailable() { Toast.makeText(context,"Enable the optional read-free gesture service in Android Accessibility Settings",Toast.LENGTH_LONG).show() }
        when {
            action=="drawer" || action=="drawerSearch" -> onDrawer()
            action=="search" -> searching=true
            action=="widgets" -> {widgetTarget=null;picking=true}
            action=="edit" -> edit=!edit
            action=="notifications" -> if(!GestureAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) unavailable()
            action=="screenOff" -> if(!GestureAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) unavailable()
            action=="recents" -> if(!GestureAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_RECENTS)) unavailable()
            action.startsWith("app:") -> {
                val pkg=action.removePrefix("app:")
                val intent=if(apps.any {it.packageName==pkg}) context.packageManager.getLaunchIntentForPackage(pkg) else null
                val view=(context as? android.app.Activity)?.window?.decorView
                if(intent!=null) runCatching {
                    val options=view?.let { ActivityOptions.makeClipRevealAnimation(it,origin.x.toInt(),origin.y.toInt(),1,1).toBundle() }
                    context.startActivity(intent,options)
                }
            }
            action.startsWith("intent:") -> runCatching {
                val intent=Intent.parseUri(action.removePrefix("intent:"),Intent.URI_INTENT_SCHEME)
                intent.flags=intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION).inv()
                context.startActivity(intent)
            }.onFailure { Toast.makeText(context,"Custom intent could not be opened",Toast.LENGTH_SHORT).show() }
        }
    }
    BackHandler(searching || feeding || edit) {searching=false;feeding=false;edit=false}
    Surface(Modifier.fillMaxSize().padding(adaptive.safePane), color = MaterialTheme.colorScheme.background) {
        if(feeding && BuildConfig.ENABLE_FEED) FeedScreen((context.applicationContext as HyperLauncherApp).container.database.feedDao(),onClose={feeding=false})
        else if (searching) Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.weight(1f)) { SmartSearchPanel(apps,
                layout.options["provider"] ?: "https://duckduckgo.com/?q={query}",
                onProvider={save(layout.option("provider",it))},onLaunch=onLaunch,onClose={searching=false}) }
            val dropTarget = remember(layout, logical, apps) { object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val pkg = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString() ?: return false
                    if (apps.none { it.packageName == pkg }) return false
                    save(layout.copy(items=layout.items+Placement(page=logical,kind="app",packageName=pkg,
                        width=1f/layout.columns,height=1f/layout.rows)))
                    searching=false; return true
                }
            } }
            Surface(Modifier.fillMaxWidth().height(80.dp).dragAndDropTarget(shouldStartDragAndDrop={true},target=dropTarget),
                color=MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment=Alignment.Center) { Text("Drop shortcut on home page ${logical+1}") }
            }
        } else Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Home", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton({ edit = !edit }) { Text(if (edit) "Done" else "Edit") }
                TextButton({ preferences = true }) { Text("Layout") }
            }
            if (edit) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton({ widgetTarget = null; picking = true }) { Text("+ Widget") }
                TextButton({ builtins=true }) { Text("+ Built-in") }
                TextButton({ pinDock=false; pin = true }) { Text("+ App") }
            }
            HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth().homeGestures(!edit && layout.options["gesturesEnabled"]!="false") { gesture,origin ->
                perform(layout.options["gesture_"+gesture.name] ?: GestureMath.defaultAction(gesture),origin)
            }, userScrollEnabled = !edit) { virtual ->
                val page = CircularPages.logical(virtual, layout.pages)
                // The recycler addresses one logical page; layouts are never cloned at the wrap.
                // Only the settled/current page owns live RemoteViews (one host view per widget ID).
                if (virtual == pager.currentPage) WorkspacePage(layout, page, edit, host, apps,
                    onChange = { changed -> save(layout.copy(items = layout.items.map { if (it.id == changed.id) changed else it })) },
                    onRemove = { item ->
                        item.widgets.forEach { host.deleteAppWidgetId(it) }
                        save(layout.copy(items = layout.items.filterNot { it.id == item.id }))
                    }, onStack = { widgetTarget = it.id; picking = true }, onLaunch = onLaunch,
                    onWidgetOption={key,value -> save(layout.option(key,value))},onFeed={if(BuildConfig.ENABLE_FEED) feeding=true},
                    onIconPreferences={iconPrefs=it},onIconGesture={item,direction,origin ->
                        if(item.kind=="folder" && (direction=="UP" || direction=="OPEN")) folder=item
                        else perform(GestureMath.iconAction(item.kind,direction,layout.options["icon_${item.id}_$direction"]),origin)
                    })
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Page ${page + 1}") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(layout.pages) { page -> TextButton({ scope.launch {
                    pager.animateScrollToPage(pager.currentPage + CircularPages.shortestDelta(logical, page, layout.pages))
                } }) { Text(if (logical == page) "●" else "○") } }
            }
            if (layout.options["dockSearch"] != "false") FilledTonalButton({searching=true},Modifier.fillMaxWidth().padding(horizontal=16.dp)) { Text("Search apps, contacts and web") }
            val slots=(layout.options["dockSlots"]?.toIntOrNull() ?: 5).coerceIn(4,6)
            val dock=layout.dockApps.filter {pkg->apps.any {it.packageName==pkg}}
            val dockPager=rememberPagerState(pageCount={ ((dock.size+slots-1)/slots).coerceAtLeast(1) })
            HorizontalPager(dockPager,modifier=Modifier.fillMaxWidth().height(64.dp)) { dockPage ->
                Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    repeat(slots) { slot ->
                        val pkg=dock.getOrNull(dockPage*slots+slot)
                        TextButton({ if (pkg==null || edit) {pinDock=true;pin=true} else onLaunch(pkg) },Modifier.weight(1f)) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                apps.firstOrNull {it.packageName==pkg}?.let {AppIcon(it,26.dp)}
                                Text(pkg?.let {name->apps.firstOrNull {it.packageName==name}?.label} ?: "+",maxLines=1)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalButton(onDrawer) { Text("All apps") }
                TextButton({searching=true}) {Text("Search")}
                TextButton(onSettings) { Text("Settings") }
            }
        }
    }
    if(appearanceDialog) AppearanceDialog(onDismiss={appearanceDialog=false})
    if(backupDialog && BuildConfig.ENABLE_IMPORT) BackupDialog(setup,
        onKwgt={uris ->save(layout.option("kwgtExports",JsonArray(uris.map {JsonPrimitive(it)}).toString()))},onDismiss={backupDialog=false})
    if(builtins) AlertDialog(onDismissRequest={builtins=false},title={Text("Built-in widget")},text={Column {
        listOf("clock","calendar","weather","battery","music","notes","photos","rss").forEach {kind ->
            TextButton({save(layout.copy(items=layout.items+Placement(page=logical,kind=kind)));builtins=false}) {Text(kind)}
        }
    }},confirmButton={TextButton({builtins=false}){Text("Cancel")}})
    if(gesturePrefs) GesturePreferences(layout.options,apps,onSet={key,value -> save(layout.option(key,value))},onDismiss={gesturePrefs=false})
    iconPrefs?.let { item -> GesturePreferences(layout.options,apps,prefix="icon_${item.id}_",onSet={key,value -> save(layout.option(key,value))},onDismiss={iconPrefs=null}) }
    folder?.let { item -> AlertDialog(onDismissRequest={folder=null},title={Text("Folder")},text={
        Column(Modifier.heightIn(max=350.dp).verticalScroll(rememberScrollState())) {
            item.packageName.split('|').filter {pkg->apps.any {it.packageName==pkg}}.forEach {pkg -> TextButton({onLaunch(pkg);folder=null}) {Text(apps.firstOrNull {it.packageName==pkg}?.label ?: pkg)} }
        }
    },confirmButton={TextButton({folder=null}){Text("Close")}}) }
    val message by model.error.collectAsState()
    message?.let { AlertDialog(onDismissRequest = { model.error.value = null }, text = { Text(it) },
        confirmButton = { TextButton({ model.error.value = null }) { Text("OK") } }) }
    if (picking) WidgetPicker(host, onAdded = { id ->
        val target = widgetTarget
        save(if (target == null) layout.copy(items = layout.items + Placement(page = logical, widgets = listOf(id)))
            else layout.copy(items = layout.items.map { if (it.id == target) it.copy(widgets = (it.widgets + id).take(8)) else it }))
        picking = false
    }, onDismiss = { picking = false })
    if (pin) AlertDialog(onDismissRequest = { pin = false }, title = { Text("Pin an app") },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            apps.forEach { app -> TextButton({
                if(pinDock) save(layout.option("dock",(layout.dockApps+app.packageName).distinct().take(30).joinToString("|")))
                else save(layout.copy(items = layout.items + Placement(page = logical, kind = "app", packageName = app.packageName,
                    width = 1f/layout.columns, height = 1f/layout.rows)))
                pin = false
            }) { Text(app.label) } }
        } }, confirmButton = { TextButton({ pin = false }) { Text("Cancel") } })
    if(preferences) WorkspacePreferencesDialog(layout,onSave=::save,
        onBackup={backupDialog=true},onFeed={feeding=true;preferences=false},
        onWallpaper={wallpaperPicker.launch(arrayOf("image/*"))},onGestures={gesturePrefs=true},
        onFolder={save(layout.copy(items=layout.items+Placement(page=logical,kind="folder",packageName=layout.dockApps.joinToString("|"),width=.3f,height=.15f)))},
        onAddDock={pinDock=true;pin=true;preferences=false},onAppearance={appearanceDialog=true},onDismiss={preferences=false})

}

@Composable
fun WorkspacePage(layout: WorkspaceLayout, page: Int, edit: Boolean, host: AppWidgetHost,
                  apps: List<AppInfo>, onChange: (Placement) -> Unit, onRemove: (Placement) -> Unit,
                  onStack: (Placement) -> Unit, onLaunch: (String) -> Unit,
                  onWidgetOption: (String,String)->Unit = {_,_->}, onFeed: ()->Unit = {},
                  onIconPreferences: (Placement)->Unit = {}, onIconGesture: (Placement,String,Offset)->Unit = {_,_,_->}) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("workspace_page")) {
        val pageWidth = maxWidth
        val pageHeight = maxHeight
        val density = LocalDensity.current
        val widthPx = with(density) { pageWidth.toPx() }
        val heightPx = with(density) { pageHeight.toPx() }
        if (layout.items.none { it.page == page }) Text("Your space. Tap Edit to add apps or widgets.",
            Modifier.align(Alignment.Center).padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        layout.items.filter { it.page == page && (it.kind!="app" || apps.any {app->app.packageName==it.packageName}) }.forEach { item -> key(item.id) {
            var position by remember(item) { mutableStateOf(item) }
            fun commit() {
                val value = if (layout.gridless) position else position.copy(
                    x = (position.x * layout.columns).roundToInt().toFloat()/layout.columns,
                    y = (position.y * layout.rows).roundToInt().toFloat()/layout.rows)
                onChange(value.bounded())
            }
            Box(Modifier.offset { IntOffset((position.x*widthPx).roundToInt(), (position.y*heightPx).roundToInt()) }
                .size(pageWidth*position.width, pageHeight*position.height)
                .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("placement_${item.id}")) {
                if(BuildConfig.ENABLE_GLASS && layout.options["theme"] in listOf("frosted","liquid")) GlassBackdrop(item,layout.options)
                when (item.kind) {
                    "clock", "calendar", "weather", "battery", "music", "notes", "photos", "rss" -> BuiltInWidget(item,layout.options,onWidgetOption,onFeed)
                    "app", "folder" -> {
                        var origin by remember {mutableStateOf(Offset.Zero)}
                        var drag by remember {mutableStateOf(Offset.Zero)}
                        Box(Modifier.fillMaxSize().onGloballyPositioned {origin=it.positionInRoot()}
                            .combinedClickable(onClick={if(!edit) {if(item.kind=="folder") onIconGesture(item,"OPEN",origin) else onLaunch(item.packageName)}},
                                onDoubleClick={if(!edit) onIconGesture(item,"DOUBLE_TAP",origin)},onLongClick={onIconPreferences(item)})
                            .pointerInput(item.id,edit) { if(!edit) detectDragGestures(onDragStart={drag=Offset.Zero},onDragEnd={
                                if(drag.getDistance()>30.dp.toPx()) onIconGesture(item,GestureMath.direction(drag.x,drag.y),origin)
                            }) {change,amount -> change.consume();drag+=amount} },contentAlignment=Alignment.Center) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                apps.firstOrNull {it.packageName==item.packageName}?.let {AppIcon(it,36.dp)}
                                Text(if(item.kind=="folder") "▦ Folder" else apps.firstOrNull {it.packageName==item.packageName}?.label ?: item.packageName,maxLines=1)
                            }
                        }
                    }
                    else -> if (item.widgets.isNotEmpty()) {
                        val id = item.widgets[Math.floorMod(item.activeWidget, item.widgets.size)]
                        key(id) { HostedWidget(host, id, (pageWidth*position.width).value.toInt(), (pageHeight*position.height).value.toInt()) }
                    } else TextButton({onStack(item)},Modifier.fillMaxSize()) {Text("Rebind imported widget")}
                }
                if (edit) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha=.08f))
                    .pointerInput(item.id, widthPx, heightPx) { detectDragGestures(onDragEnd = { commit() }) { change, amount ->
                        change.consume(); position = position.copy(x=position.x+amount.x/widthPx.coerceAtLeast(1f),
                            y=position.y+amount.y/heightPx.coerceAtLeast(1f)).bounded()
                    } })
                if (edit) {
                    Row(Modifier.align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface.copy(alpha=.9f))) {
                        TextButton({ onRemove(item) }, contentPadding = PaddingValues(2.dp)) { Text("×") }
                        if(item.kind=="app" || item.kind=="folder") TextButton({onIconPreferences(item)},contentPadding=PaddingValues(2.dp)) {Text("⋮")}
                        if (item.kind == "widget" && item.widgets.size < 8)
                            TextButton({ onStack(item) }, contentPadding = PaddingValues(2.dp)) { Text("+") }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).size(40.dp).background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("resize_${item.id}").pointerInput(item.id, widthPx, heightPx) {
                            detectDragGestures(onDragEnd = { commit() }) { change, amount ->
                                change.consume(); position=position.copy(width=position.width+amount.x/widthPx.coerceAtLeast(1f),
                                    height=position.height+amount.y/heightPx.coerceAtLeast(1f)).bounded()
                            }
                        }, contentAlignment = Alignment.Center) { Text("↘") }
                } else if (item.widgets.size > 1) TextButton({ onChange(item.copy(activeWidget = (item.activeWidget+1)%item.widgets.size)) },
                    Modifier.align(Alignment.BottomEnd)) { Text("${Math.floorMod(item.activeWidget,item.widgets.size)+1}/${item.widgets.size} ›") }
            }
        } }
    }
}

@Composable
private fun HostedWidget(host: AppWidgetHost, id: Int, width: Int, height: Int) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val info = remember(id) { manager.getAppWidgetInfo(id) }
    if (info == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Widget unavailable. Remove and add it again.") }
    else AndroidView(factory = { host.createView(it, id, info) }, modifier = Modifier.fillMaxSize(), update = { view ->
        view.updateAppWidgetOptions(Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
        })
    })
}

@Composable
private fun BuiltInClock(modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { now=System.currentTimeMillis() } }
        val filter = IntentFilter().apply { addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(DateFormat.getTimeFormat(context).format(java.util.Date(now)), style = MaterialTheme.typography.headlineLarge)
        Text(DateFormat.getMediumDateFormat(context).format(java.util.Date(now)), style = MaterialTheme.typography.bodySmall)
    }
}
