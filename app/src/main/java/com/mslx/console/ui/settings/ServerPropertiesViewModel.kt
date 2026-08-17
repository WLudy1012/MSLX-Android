package com.mslx.console.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.SERVER_PROPERTIES_SCHEMA
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerPropertiesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val missing: Boolean = false,
    val path: String = "server.properties",
    val values: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
)

class ServerPropertiesViewModel(
    application: Application,
    private val instanceId: Long,
) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(ServerPropertiesUiState())
    val state = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message = _message.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // 1. 从实例设置拿 server.properties 路径
            val path = repository.getSettings(instanceId).getOrNull()
                ?.serverPropertiesPath
                ?.takeIf { it.isNotBlank() }
                ?: "server.properties"

            // 2. 读文件内容
            val contentResult = repository.fileContent(instanceId, path)
            contentResult.fold(
                onSuccess = { content ->
                    _state.update {
                        it.copy(loading = false, path = path, values = parse(content), missing = false)
                    }
                },
                onFailure = {
                    // 文件不存在等：路径已知，可编辑（空表单），保存时会创建
                    _state.update {
                        it.copy(loading = false, path = path, values = emptyMap(), missing = true)
                    }
                },
            )
        }
    }

    fun setValue(key: String, value: String) {
        _state.update { s -> s.copy(values = s.values + (key to value)) }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveFileContent(instanceId, s.path, stringify(s.values)).fold(
                onSuccess = { msg ->
                    _message.tryEmit(msg)
                    _state.update { it.copy(saving = false, missing = false) }
                },
                onFailure = { e ->
                    _message.tryEmit("保存失败：${e.message ?: "未知错误"}")
                    _state.update { it.copy(saving = false) }
                },
            )
        }
    }

    private fun parse(content: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        content.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) return@forEach
            val idx = t.indexOf('=')
            if (idx != -1) {
                map[t.substring(0, idx).trim()] = t.substring(idx + 1).trim()
            }
        }
        return map
    }

    private fun stringify(map: Map<String, String>): String {
        val definedKeys = SERVER_PROPERTIES_SCHEMA.map { it.key }
        val sb = StringBuilder()
        sb.append("#Minecraft server properties\n")
        definedKeys.forEach { k -> if (map.containsKey(k)) sb.append("$k=${map[k]}\n") }
        map.keys.forEach { k -> if (k !in definedKeys) sb.append("$k=${map[k]}\n") }
        return sb.toString()
    }
}
