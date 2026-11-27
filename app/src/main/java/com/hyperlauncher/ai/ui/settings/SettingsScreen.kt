package com.hyperlauncher.ai.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.ai.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember(state.openRouterModel) { mutableStateOf(state.openRouterModel) }
    val isNetworkFlavor = BuildConfig.FLAVOR == "network"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.settings_network),
            style = MaterialTheme.typography.titleLarge
        )
        if (!isNetworkFlavor) {
            Text(stringResource(R.string.openrouter_offline_note))
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.openrouter_enable), modifier = Modifier.weight(1f))
                Switch(
                    checked = state.openRouterEnabled,
                    onCheckedChange = { viewModel.setOpenRouterEnabled(it) }
                )
            }
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text(stringResource(R.string.openrouter_model)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.setOpenRouterModel(modelInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save model")
            }
            Text(
                text = if (state.hasApiKey) stringResource(R.string.openrouter_key_saved)
                else stringResource(R.string.openrouter_key_empty)
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text(stringResource(R.string.openrouter_key_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (apiKeyInput.isNotBlank()) {
                        viewModel.saveApiKey(apiKeyInput)
                        apiKeyInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.openrouter_key_save))
            }
            OutlinedButton(
                onClick = { viewModel.clearApiKey() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.openrouter_key_clear))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.titleLarge
        )
        Text(stringResource(R.string.accessibility_desc))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.accessibility_toggle), modifier = Modifier.weight(1f))
            Switch(
                checked = state.accessibilityEnabled,
                onCheckedChange = { viewModel.setAccessibilityEnabled(it) }
            )
        }
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.accessibility_enable))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.accessibility_whitelist))
        if (state.whitelistedPackages.isEmpty()) {
            Text(stringResource(R.string.whitelist_empty))
        }
        state.installedApps.take(30).forEach { app ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = state.whitelistedPackages.contains(app.packageName),
                    onCheckedChange = { viewModel.toggleWhitelist(app.packageName) }
                )
                Text(app.label, modifier = Modifier.weight(1f))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = stringResource(R.string.floating_pill_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(stringResource(R.string.floating_pill_desc))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.floating_pill_title), modifier = Modifier.weight(1f))
            Switch(
                checked = state.floatingPillEnabled,
                onCheckedChange = { viewModel.setFloatingPillEnabled(it) }
            )
        }
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.floating_pill_enable))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleLarge
        )
        Text(stringResource(R.string.about_version))
        Text(
            stringResource(
                if (isNetworkFlavor) R.string.about_network else R.string.about_offline
            )
        )
    }
}
