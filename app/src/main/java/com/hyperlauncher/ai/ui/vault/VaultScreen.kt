package com.hyperlauncher.ai.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.R

@Composable
fun VaultScreen(viewModel: VaultViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = stringResource(R.string.vault_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (!state.hasVault) {
            Text(stringResource(R.string.vault_passphrase_hint))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(stringResource(R.string.vault_passphrase_hint)) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(stringResource(R.string.vault_passphrase_confirm)) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.createVault(passphrase, confirm) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.vault_create))
            }
        } else if (!state.isUnlocked) {
            Text(stringResource(R.string.vault_locked_message))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(stringResource(R.string.vault_passphrase_hint)) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.unlock(passphrase) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                Text(stringResource(R.string.vault_unlock))
            }
        } else {
            OutlinedButton(onClick = { viewModel.lock() }) {
                Text("Lock vault")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noteBody,
                onValueChange = { noteBody = it },
                label = { Text(stringResource(R.string.vault_note_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.addNote(noteTitle, noteBody)
                    noteTitle = ""
                    noteBody = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.vault_add))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (state.notes.isEmpty()) {
                Text(stringResource(R.string.vault_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.notes, key = { it.id }) { note ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(note.title, style = MaterialTheme.typography.titleLarge)
                                Text(note.body)
                                if (note.sourcePackage != null) {
                                    Text(
                                        note.sourcePackage,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                OutlinedButton(onClick = { viewModel.deleteNote(note.id) }) {
                                    Text(stringResource(R.string.vault_delete))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
