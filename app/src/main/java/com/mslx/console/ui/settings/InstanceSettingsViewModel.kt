package com.mslx.console.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.ServerSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstanceSettingsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val saving: Boolean = false,
    val settings: ServerSettings? = null,
)

class InstanceSettingsViewModel(
    application: Application,
    private val instanceId: Long,
) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(InstanceSettingsUiState())
    val state = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message = _message.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.getSettings(instanceId).fold(
                onSuccess = { settings ->
                    _state.update { it.copy(loading = false, settings = settings) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "加载失败")
                    }
                },
            )
        }
    }

    /** 修改单个字段（基于当前表单 copy）。 */
    fun update(transform: (ServerSettings) -> ServerSettings) {
        _state.update { s ->
            val cur = s.settings ?: return@update s
            s.copy(settings = transform(cur))
        }
    }

    fun save() {
        val settings = _state.value.settings ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.updateSettings(instanceId, settings).fold(
                onSuccess = { msg ->
                    _message.tryEmit(msg)
                    _state.update { it.copy(saving = false) }
                },
                onFailure = { e ->
                    _message.tryEmit("保存失败：${e.message ?: "未知错误"}")
                    _state.update { it.copy(saving = false) }
                },
            )
        }
    }
}
