package com.mslx.console.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.FileItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileManagerUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    // 编辑态
    val editing: Boolean = false,
    val editingPath: String = "",
    val editingContent: String = "",
    val loadingContent: Boolean = false,
    val saving: Boolean = false,
)

class FileManagerViewModel(
    application: Application,
    private val instanceId: Long,
) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(FileManagerUiState())
    val state = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message = _message.asSharedFlow()

    init {
        loadDirectory("")
    }

    fun loadDirectory(path: String) {
        _state.update { it.copy(loading = true, error = null, currentPath = path) }
        viewModelScope.launch {
            repository.fileList(instanceId, path).fold(
                onSuccess = { items ->
                    _state.update { it.copy(loading = false, items = items) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "加载失败") }
                },
            )
        }
    }

    fun enterDir(item: FileItem) {
        if (!item.isFolder) return
        val base = _state.value.currentPath
        val next = if (base.isBlank()) item.name else "$base/${item.name}"
        loadDirectory(next)
    }

    fun goUp() {
        val base = _state.value.currentPath
        if (base.isBlank()) return
        val parent = base.substringBeforeLast('/', "")
        loadDirectory(parent)
    }

    fun openFile(item: FileItem) {
        if (item.isFolder) return
        val base = _state.value.currentPath
        val path = if (base.isBlank()) item.name else "$base/${item.name}"
        openEditor(path)
    }

    fun openEditor(path: String) {
        _state.update { it.copy(editing = true, editingPath = path, loadingContent = true, error = null) }
        viewModelScope.launch {
            repository.fileContent(instanceId, path).fold(
                onSuccess = { content ->
                    _state.update { it.copy(loadingContent = false, editingContent = content) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            loadingContent = false,
                            editing = false,
                            error = "读取文件失败：${e.message ?: "未知错误"}",
                        )
                    }
                },
            )
        }
    }

    fun onContentChange(value: String) = _state.update { it.copy(editingContent = value) }

    fun save() {
        val s = _state.value
        if (s.saving || s.editingPath.isBlank()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveFileContent(instanceId, s.editingPath, s.editingContent).fold(
                onSuccess = { msg ->
                    _message.tryEmit("已保存 ${s.editingPath}")
                    _state.update { it.copy(saving = false, editing = false) }
                    loadDirectory(s.currentPath)
                },
                onFailure = { e ->
                    _message.tryEmit("保存失败：${e.message ?: "未知错误"}")
                    _state.update { it.copy(saving = false) }
                },
            )
        }
    }

    fun closeEditor() {
        _state.update { it.copy(editing = false) }
    }

    fun refresh() = loadDirectory(_state.value.currentPath)
}
