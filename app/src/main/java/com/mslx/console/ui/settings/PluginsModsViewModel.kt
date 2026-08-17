package com.mslx.console.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.PmListData
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
        _state.update { it.copy(loading = true, error = null) }
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

    fun delete(fileName: String) = runAction("delete", listOf(fileName))

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
