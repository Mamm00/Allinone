package com.hyperlauncher.ai.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperlauncher.ai.data.local.WorkspaceDao
import com.hyperlauncher.ai.data.local.WorkspaceEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(private val dao: WorkspaceDao, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val setup = MutableStateFlow("")
    private val writes = Channel<WorkspaceEntity>(Channel.UNLIMITED)
    // Room may emit the preceding write while a newer optimistic edit is on screen.
    // Retain the latest desired JSON until that exact revision is observed.
    private val stateLock=Any()
    private val desired=mutableMapOf<String,String>()
    val error = MutableStateFlow<String?>(null)
    val loadedSetup = MutableStateFlow("")
    val layout = MutableStateFlow(WorkspaceLayout())
    init {
        viewModelScope.launch(ioDispatcher) {
            setup.filter { it.isNotEmpty() }.collectLatest { id ->
                dao.observe(id).collect { row ->
                    synchronized(stateLock) {
                        if(setup.value==id) {
                            val expected=desired[id]
                            if(expected==null || expected==row?.layoutJson) {
                                try {
                                    val decoded=row?.let {WorkspaceCodec.decode(it.layoutJson)} ?:WorkspaceLayout()
                                    layout.value=decoded
                                    loadedSetup.value=id
                                    desired.remove(id)
                                } catch (_: Exception) {
                                    // Never enable editing of a fabricated default over corrupt saved data.
                                    loadedSetup.value=""
                                    error.value="Layout data could not be read; the saved data has not been deleted"
                                }
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            for (row in writes) {
                try { dao.save(row) }
                catch (e: Exception) {
                    if(e is CancellationException) throw e
                    synchronized(stateLock) {
                        if(desired[row.setupId]==row.layoutJson) desired.remove(row.setupId)
                    }
                    error.value="Layout could not be saved; your latest edit is not persisted"
                }
            }
        }
    }
    fun selectSetup(id: String) = synchronized(stateLock) {
        if(setup.value!=id) {loadedSetup.value="";setup.value=id}
    }
    fun save(value: WorkspaceLayout) = synchronized(stateLock) {
        val id=setup.value
        if(id.isNotEmpty() && loadedSetup.value==id) {
            val json=WorkspaceCodec.encode(value)
            desired[id]=json
            layout.value=value
            writes.trySend(WorkspaceEntity(id,json))
        }
    }
    override fun onCleared() {writes.close();super.onCleared()}
}
