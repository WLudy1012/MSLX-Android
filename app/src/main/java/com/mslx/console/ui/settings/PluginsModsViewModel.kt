package com.mslx.console.ui.settings

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.PmListData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PluginsModsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val mode: String = "plugins",
    val data: PmListData? = null,
    val busy: Boolean = false,
)

class PluginsModsViewModel(
    application: Application,
    private val instanceId: Long,
) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(PluginsModsUiState())
    val state = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val message = _message.asSharedFlow()

    init {
        load()
    }

    fun setMode(mode: String) {
        if (_state.value.mode == mode) return
        _state.update { it.copy(mode = mode) }
        load()
    }

    fun load() {
        // 加载前清空 data，避免切换模式时残留上一模式的旧列表
        _state.update { it.copy(loading = true, error = null, data = null) }
        viewModelScope.launch {
            repository.pmList(instanceId, _state.value.mode).fold(
                onSuccess = { data -> _state.update { it.copy(loading = false, data = data) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "加载失败") } },
            )
        }
    }

    /** 启用 ↔ 禁用切换。 */
    fun toggle(fileName: String, currentlyDisabled: Boolean) {
        val action = if (currentlyDisabled) "enable" else "disable"
        runAction(action, listOf(fileName))
    }

    fun batch(action: String) {
        val data = _state.value.data ?: return
        val targets = when (action) {
            "enable" -> data.disableJarFiles
            "disable" -> data.jarFiles + data.clientJarFiles
            else -> emptyList()
        }
        if (targets.isNotEmpty()) runAction(action, targets)
    }

    fun delete(fileName: String) = runAction("delete", listOf(fileName))

    /** 多线程分片上传插件/模组文件。 */
    fun upload(uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val app = getApplication<MSLXApplication>()
            val result = runCatching {
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("无法读取文件")
                val fileName = queryFileName(app, uri) ?: "upload.jar"
                val chunkSize = 5 * 1024 * 1024
                val chunks = bytes.toList().chunked(chunkSize)

                val uploadId = repository.uploadInit().getOrThrow()

                // 多线程并行上传分片
                coroutineScope {
                    chunks.mapIndexed { index, chunk ->
                        async(Dispatchers.IO) {
                            repository.uploadChunk(uploadId, index, chunk.toByteArray()).getOrThrow()
                        }
                    }.awaitAll()
                }

                repository.uploadFinish(uploadId, chunks.size).getOrThrow()
                repository.saveUpload(instanceId, uploadId, fileName, _state.value.mode).getOrThrow()
            }
            result.fold(
                onSuccess = { msg -> _message.tryEmit(msg) },
                onFailure = { e -> _message.tryEmit("上传失败：${e.message ?: "未知错误"}") },
            )
            _state.update { it.copy(busy = false) }
            load()
        }
    }

    private fun queryFileName(app: Application, uri: Uri): String? =
        app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }

    private fun runAction(action: String, targets: List<String>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            repository.pmSet(instanceId, _state.value.mode, action, targets).fold(
                onSuccess = { msg ->
                    _message.tryEmit(msg)
                    load()
                },
                onFailure = { e -> _message.tryEmit("操作失败：${e.message ?: "未知错误"}") },
            )
            _state.update { it.copy(busy = false) }
        }
    }
}
