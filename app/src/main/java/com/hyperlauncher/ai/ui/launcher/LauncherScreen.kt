package com.hyperlauncher.ai.ui.launcher

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.R
import com.hyperlauncher.ai.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onOpenVault: () -> Unit,
    aiPromptBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var editingApp by remember { mutableStateOf<AppInfo?>(null) }
    var options by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { viewModel.load() }
        }
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        val suspension = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGES_SUSPENDED); addAction(Intent.ACTION_PACKAGES_UNSUSPENDED)
        }
        ContextCompat.registerReceiver(context, receiver, packages, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(context, receiver, suspension, ContextCompat.RECEIVER_EXPORTED)
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            DrawerContent(state, viewModel::onSearchQueryChange, viewModel::selectCategory,
                onLaunch = { app ->
                    if (app.isSuspended || app.isArchived) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${app.packageName}"))) }
                    } else viewModel.onAppLaunched(app.packageName)
                }, onEdit = { editingApp = it }, onOptions = { options = true },
                onSettings = onOpenSettings, onVault = onOpenVault, aiPromptBar = aiPromptBar)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    editingApp?.let { app ->
        ChangeCategoryDialog(app, state, onDismiss = { editingApp = null }) { category, similar ->
            viewModel.changeCategory(app.packageName, category, similar)
            editingApp = null
        }
    }
    if (options) DrawerOptionsDialog(state, viewModel::setOption, viewModel::saveCategory,
        onRefresh = viewModel::load, onDismiss = { options = false })
}

@Composable
fun DrawerContent(
    state: LauncherUiState,
    onQuery: (String) -> Unit,
    onCategory: (String) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onEdit: (AppInfo) -> Unit,
    onOptions: () -> Unit,
    onSettings: () -> Unit,
    onVault: () -> Unit,
    aiPromptBar: @Composable () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.drawer_title), style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        OutlinedTextField(value = state.searchQuery, onValueChange = onQuery,
            placeholder = { Text(stringResource(R.string.search_hint)) }, singleLine = true,
            shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().testTag("drawer_search"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onOptions, Modifier.weight(1f)) { Text(stringResource(R.string.drawer_options)) }
            TextButton(onSettings, Modifier.weight(1f)) { Text(stringResource(R.string.settings_title)) }
            TextButton(onVault, Modifier.weight(1f)) { Text(stringResource(R.string.vault_short)) }
        }
        if (!state.preferences.alphabetical) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(state.selectedCategoryId == "all", { onCategory("all") },
                    label = { Text(stringResource(R.string.all_apps)) }) }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(state.selectedCategoryId == category.id, { onCategory(category.id) },
                        label = { Text(category.name) })
                }
            }
        } else Text(stringResource(R.string.drawer_alphabetical), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.apps.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.apps.isEmpty() -> Text(
                    if (state.searchQuery.isBlank()) stringResource(R.string.no_apps_found)
                    else stringResource(R.string.search_no_results, state.searchQuery),
                    modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize().testTag("drawer_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppRow(app, state.badgeCounts[app.packageName] ?: 0, state.preferences.showSystemState,
                            onClick = { onLaunch(app) }, onLongClick = { onEdit(app) })
                    }
                }
            }
        }
        // Keep the existing opt-in AI workflow without consuming the list's entire height.
        Box(Modifier.heightIn(max = 160.dp)) { aiPromptBar() }
    }
}

@Composable
fun AppRow(app: AppInfo, count: Int = 0, showState: Boolean = true,
           onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.iconPng) {
        value = withContext(Dispatchers.IO) {
            app.iconPng?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }
        }
    }
    val inactive = app.isSuspended || app.isArchived
    ListItem(
        headlineContent = { Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (showState && inactive) Text(stringResource(
                if (app.isArchived) R.string.app_archived else R.string.app_paused))
        },
        leadingContent = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                val bitmap = icon
                if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(44.dp),
                    colorFilter = if (app.isSuspended) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                else Text(app.label.take(1), style = MaterialTheme.typography.titleLarge)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showState && app.isArchived) Text("☁", modifier = Modifier.testTag("archived_cloud"))
                if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().alpha(if (inactive) .62f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).testTag("app_${app.packageName}")
    )
}
