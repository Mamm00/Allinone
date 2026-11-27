package com.hyperlauncher.ai.ui.workspace

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.background
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
                    onSettings: () -> Unit, onLaunch: (String) -> Unit) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val displayId = (context as? android.app.Activity)?.windowManager?.defaultDisplay?.displayId ?: 0
    val setup = "display_${displayId}_${if (config.screenWidthDp > config.screenHeightDp) "landscape" else "portrait"}"
    LaunchedEffect(setup) { model.selectSetup(setup) }
    val stored by model.layout.collectAsState()
    val loaded by model.loadedSetup.collectAsState()
    var layout by remember(setup) { mutableStateOf(WorkspaceLayout()) }
    LaunchedEffect(stored, setup, loaded) { layout = if (loaded == setup) stored else WorkspaceLayout() }
    fun save(value: WorkspaceLayout) { layout = value; model.save(value) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(false) }
    var widgetTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var pin by remember { mutableStateOf(false) }
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
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Home", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton({ edit = !edit }) { Text(if (edit) "Done" else "Edit") }
                TextButton({ preferences = true }) { Text("Layout") }
            }
            if (edit) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton({ widgetTarget = null; picking = true }) { Text("+ Widget") }
                TextButton({ save(layout.copy(items = layout.items + Placement(page = logical, kind = "clock"))) }) { Text("+ Clock") }
                TextButton({ pin = true }) { Text("+ App") }
            }
            HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = !edit) { virtual ->
                val page = CircularPages.logical(virtual, layout.pages)
                // The recycler addresses one logical page; layouts are never cloned at the wrap.
                // Only the settled/current page owns live RemoteViews (one host view per widget ID).
                if (virtual == pager.currentPage) WorkspacePage(layout, page, edit, host, apps,
                    onChange = { changed -> save(layout.copy(items = layout.items.map { if (it.id == changed.id) changed else it })) },
                    onRemove = { item ->
                        item.widgets.forEach { host.deleteAppWidgetId(it) }
                        save(layout.copy(items = layout.items.filterNot { it.id == item.id }))
                    }, onStack = { widgetTarget = it.id; picking = true }, onLaunch = onLaunch)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Page ${page + 1}") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(layout.pages) { page -> TextButton({ scope.launch {
                    pager.animateScrollToPage(pager.currentPage + CircularPages.shortestDelta(logical, page, layout.pages))
                } }) { Text(if (logical == page) "●" else "○") } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalButton(onDrawer) { Text("All apps") }
                TextButton(onSettings) { Text("Settings") }
            }
        }
    }
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
                save(layout.copy(items = layout.items + Placement(page = logical, kind = "app", packageName = app.packageName,
                    width = 1f/layout.columns, height = 1f/layout.rows)))
                pin = false
            }) { Text(app.label) } }
        } }, confirmButton = { TextButton({ pin = false }) { Text("Cancel") } })
    if (preferences) AlertDialog(onDismissRequest = { preferences = false }, title = { Text("Layout for this display/orientation") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Gridless placement (default). Disable to align moves to the configured grid.")
            Switch(layout.gridless, { save(layout.copy(gridless = it)) })
            Text("Columns: ${layout.columns}")
            Slider(layout.columns.toFloat(), { save(layout.copy(columns = it.roundToInt())) }, valueRange = 2f..8f, steps = 5)
            Text("Rows: ${layout.rows}")
            Slider(layout.rows.toFloat(), { save(layout.copy(rows = it.roundToInt())) }, valueRange = 2f..12f, steps = 9)
            Text("Pages: ${layout.pages}")
            Slider(layout.pages.toFloat(), { save(layout.copy(pages = it.roundToInt())) }, valueRange = 1f..7f, steps = 5)
            Text("Reducing page count hides extra pages; it does not delete their contents. Swipe from the last page to wrap to the first.")
        } }, confirmButton = { TextButton({ preferences = false }) { Text("Done") } })
}

@Composable
fun WorkspacePage(layout: WorkspaceLayout, page: Int, edit: Boolean, host: AppWidgetHost,
                  apps: List<AppInfo>, onChange: (Placement) -> Unit, onRemove: (Placement) -> Unit,
                  onStack: (Placement) -> Unit, onLaunch: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("workspace_page")) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        if (layout.items.none { it.page == page }) Text("Your space. Tap Edit to add apps or widgets.",
            Modifier.align(Alignment.Center).padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        layout.items.filter { it.page == page }.forEach { item -> key(item.id) {
            var position by remember(item) { mutableStateOf(item) }
            fun commit() {
                val value = if (layout.gridless) position else position.copy(
                    x = (position.x * layout.columns).roundToInt().toFloat()/layout.columns,
                    y = (position.y * layout.rows).roundToInt().toFloat()/layout.rows)
                onChange(value.bounded())
            }
            Box(Modifier.offset { IntOffset((position.x*widthPx).roundToInt(), (position.y*heightPx).roundToInt()) }
                .size(maxWidth*position.width, maxHeight*position.height)
                .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("placement_${item.id}")) {
                when (item.kind) {
                    "clock" -> BuiltInClock(Modifier.fillMaxSize())
                    "app" -> TextButton({ if (!edit) onLaunch(item.packageName) }, Modifier.fillMaxSize()) {
                        Text(apps.firstOrNull { it.packageName == item.packageName }?.label ?: item.packageName)
                    }
                    else -> if (item.widgets.isNotEmpty()) {
                        val id = item.widgets[Math.floorMod(item.activeWidget, item.widgets.size)]
                        key(id) { HostedWidget(host, id, (maxWidth*position.width).value.toInt(), (maxHeight*position.height).value.toInt()) }
                    }
                }
                if (edit) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha=.08f))
                    .pointerInput(item.id, widthPx, heightPx) { detectDragGestures(onDragEnd = { commit() }) { change, amount ->
                        change.consume(); position = position.copy(x=position.x+amount.x/widthPx.coerceAtLeast(1f),
                            y=position.y+amount.y/heightPx.coerceAtLeast(1f)).bounded()
                    } })
                if (edit) {
                    Row(Modifier.align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface.copy(alpha=.9f))) {
                        TextButton({ onRemove(item) }, contentPadding = PaddingValues(2.dp)) { Text("×") }
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
