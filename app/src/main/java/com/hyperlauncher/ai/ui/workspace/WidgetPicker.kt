package com.hyperlauncher.ai.ui.workspace

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Binding and provider configuration are system consent flows, never bypassed. */
@Composable
fun WidgetPicker(host: AppWidgetHost, onAdded: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    var pendingId by rememberSaveable { mutableIntStateOf(-1) }
    var selecting by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    fun cancel() { if (pendingId > 0) host.deleteAppWidgetId(pendingId); pendingId = -1; onDismiss() }
    fun complete() { val id = pendingId; pendingId = -1; onAdded(id) }
    val configure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) complete() else cancel()
    }
    fun configureOrAdd() {
        val info = manager.getAppWidgetInfo(pendingId)
        if (info == null) { cancel(); return }
        if (info.configure != null) {
            runCatching { configure.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(info.configure).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)) }
                .onFailure { error = "The widget provider could not open configuration"; selecting = true }
        } else complete()
    }
    val bind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) configureOrAdd() else cancel()
    }
    if (selecting) AlertDialog(onDismissRequest = { cancel() }, title = { Text("Add a widget") },
        text = {
            val providers = remember { manager.installedProviders.sortedBy { it.loadLabel(context.packageManager).toString() } }
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                if (providers.isEmpty()) item { Text("No widget providers installed. You can add the built-in clock instead.") }
                error?.let { item { Text(it) } }
                items(providers, key = { it.provider.flattenToString() }) { provider ->
                    TextButton({
                        if (pendingId > 0) host.deleteAppWidgetId(pendingId)
                        pendingId = host.allocateAppWidgetId()
                        selecting = false
                        runCatching {
                            if (manager.bindAppWidgetIdIfAllowed(pendingId, provider.provider)) configureOrAdd()
                            else bind.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider))
                        }.onFailure { error = "Android did not allow this widget. Try another provider."; selecting = true }
                    }) { Text(provider.loadLabel(context.packageManager).toString()) }
                }
            }
        }, confirmButton = { TextButton({ cancel() }) { Text("Cancel") } })
}
