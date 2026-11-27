package com.hyperlauncher.ai.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.WorkspaceDao
import com.hyperlauncher.ai.data.local.WorkspaceEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(private val dao: WorkspaceDao) : ViewModel() {
    private val setup = MutableStateFlow("")
    private val writes = Channel<Pair<String, WorkspaceLayout>>(Channel.UNLIMITED)
    val error = MutableStateFlow<String?>(null)
    val loadedSetup = MutableStateFlow("")
    val layout = MutableStateFlow(WorkspaceLayout())
    init {
        viewModelScope.launch(Dispatchers.IO) {
            setup.filter { it.isNotEmpty() }.collectLatest { id ->
                dao.observe(id).collect { row ->
                    val decoded = runCatching { row?.let { WorkspaceCodec.decode(it.layoutJson) } ?: WorkspaceLayout() }
                        .getOrElse { error.value = "Layout data could not be read; the saved data has not been deleted"; WorkspaceLayout() }
                    layout.value = decoded
                    loadedSetup.value = id
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            for ((id, value) in writes) {
                try { dao.save(WorkspaceEntity(id, WorkspaceCodec.encode(value))) }
                catch (e: Exception) { if (e is CancellationException) throw e; error.value = "Layout could not be saved" }
            }
        }
    }
    fun selectSetup(id: String) { setup.value = id }
    fun save(value: WorkspaceLayout) {
        val id = setup.value
        if (id.isEmpty() || loadedSetup.value != id) return
        layout.value=value
        writes.trySend(id to value)
    }
}
