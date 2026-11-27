package com.hyperlauncher.ai.ui.launcher

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.hyperlauncher.ai.R
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.ai.notifications.LauncherNotificationListener

@Composable
fun ChangeCategoryDialog(app: AppInfo, state: LauncherUiState, onDismiss: () -> Unit,
                         onSave: (String?, Boolean) -> Unit) {
    var selected by remember(app.packageName) { mutableStateOf<String?>(if (app.hasManualCategory) app.categoryId else null) }
    var similar by remember { mutableStateOf(false) }
    val similarCount = state.allApps.count { it.packageName != app.packageName &&
        it.inferredCategoryId == app.inferredCategoryId && !it.hasManualCategory }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_category_for, app.label)) },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    item { CategoryChoice(stringResource(R.string.category_automatic), selected == null) { selected = null } }
                    items(state.categories, key = { it.id }) { category ->
                        CategoryChoice(category.name, selected == category.id) { selected = category.id }
                    }
                }
                if (app.inferredCategoryId != "others" && similarCount > 0) {
                    Row(Modifier.fillMaxWidth().clickable { similar = !similar }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(similar, { similar = it })
                        Text(stringResource(R.string.apply_similar, similarCount))
                    }
                    Text(stringResource(R.string.similar_explanation), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton({ onSave(selected, similar) }) { Text(stringResource(R.string.vault_save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun CategoryChoice(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(name)
    }
}

@Composable
fun DrawerOptionsDialog(state: LauncherUiState, onOption: (String, Boolean) -> Unit,
                        onSaveCategory: (String?, String) -> Unit,
                        onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) }
    var editing by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var showDisclosure by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drawer_options)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                DrawerToggle(stringResource(R.string.drawer_alphabetical), state.preferences.alphabetical) { onOption("alphabetical", it) }
                DrawerToggle(stringResource(R.string.auto_categorize), state.preferences.autoCategorize) { onOption("autoCategorize", it) }
                DrawerToggle(stringResource(R.string.ambient_colors), state.preferences.ambient) { onOption("ambient", it) }
                DrawerToggle(stringResource(R.string.show_system_state), state.preferences.showSystemState) { onOption("systemState", it) }
                DrawerToggle(stringResource(R.string.notification_badges), state.preferences.badges) { enabled ->
                    if (enabled) showDisclosure = true else onOption("badges", false)
                }
                if (state.preferences.badges) {
                    Text(stringResource(if (access) R.string.notification_access_granted else R.string.notification_access_needed))
                    TextButton({ showDisclosure = true }) { Text(stringResource(R.string.manage_notification_access)) }
                }
                TextButton(onRefresh) { Text(stringResource(R.string.refresh)) }
                HorizontalDivider()
                Text(stringResource(R.string.manage_categories), style = MaterialTheme.typography.titleSmall)
                TextButton({ editId = null; name = ""; editing = true }) { Text(stringResource(R.string.add_category)) }
                state.categories.forEach { category ->
                    TextButton({ editId = category.id; name = category.name; editing = true }) { Text(category.name) }
                }
            }
        }, confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.done)) } })
    if (editing) AlertDialog(onDismissRequest = { editing = false },
        title = { Text(stringResource(if (editId == null) R.string.add_category else R.string.rename_category)) },
        text = { OutlinedTextField(name, { name = it.take(40) }, singleLine = true,
            label = { Text(stringResource(R.string.category_name)) }) },
        confirmButton = { TextButton({ onSaveCategory(editId, name); editing = false }, enabled = name.isNotBlank()) {
            Text(stringResource(R.string.vault_save)) } },
        dismissButton = { TextButton({ editing = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDisclosure) AlertDialog(onDismissRequest = { showDisclosure = false },
        title = { Text(stringResource(R.string.notification_badges)) },
        text = { Text(stringResource(R.string.notification_disclosure)) },
        confirmButton = { TextButton({
            onOption("badges", true)
            NotificationListenerService.requestRebind(ComponentName(context, LauncherNotificationListener::class.java))
            runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            showDisclosure = false
            onDismiss()
        }) { Text(stringResource(R.string.open_notification_settings)) } },
        dismissButton = { TextButton({ showDisclosure = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun DrawerToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked, onChecked)
    }
}
