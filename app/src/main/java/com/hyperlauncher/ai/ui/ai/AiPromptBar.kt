package com.hyperlauncher.ai.ui.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.ai.R

@Composable
fun AiPromptBar(viewModel: AiPromptViewModel) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isNetworkFlavor = BuildConfig.FLAVOR == "network"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.prompt,
                onValueChange = { viewModel.onPromptChange(it) },
                placeholder = {
                    Text(
                        stringResource(
                            if (isNetworkFlavor) R.string.ai_prompt_hint_network
                            else R.string.ai_prompt_hint
                        )
                    )
                },
                singleLine = false,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Button(
                onClick = { viewModel.submit() },
                enabled = !state.isLoading && state.prompt.isNotBlank()
            ) {
                Text("Ask")
            }
        }
        if (state.isLoading) {
            Row(modifier = Modifier.padding(top = 8.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.ai_thinking))
            }
        }
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (state.response != null) {
            Text(
                text = state.response!!,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(state.response!!))
                }) {
                    Text(stringResource(R.string.ai_copy))
                }
                Spacer(modifier = Modifier.padding(4.dp))
                OutlinedButton(onClick = { viewModel.saveResponseToVault() }) {
                    Text(stringResource(R.string.ai_save_to_vault))
                }
            }
        }
        if (state.savedToVault) {
            Text(
                text = stringResource(R.string.ai_saved_to_vault),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
