package com.mslx.console.ui.console

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.remote.ConsoleHubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogLine(val text: String, val system: Boolean = false)

sealed interface ConsoleEvent {
    data class Toast(val message: String) : ConsoleEvent
    data object EulaRequired : ConsoleEvent
}

data class ConsoleUiState(
    val instanceName: String = "",
    val status: Int = 0,
    val statusText: String? = null,
    val onlinePlayers: Int = 0,
    val uptime: String? = null,
    val connecting: Boolean = true,
    val connected: Boolean = false,
    val connectionError: String? = null,
    val busy: Boolean = false,
)

class ConsoleViewModel(
    application: Application,
    private val instanceId: Long,
) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(ConsoleUiState(instanceName = "实例 #$instanceId"))
    val state = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<LogLine>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _events = MutableSharedFlow<ConsoleEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private var client: ConsoleHubClient? = null

    init {
        viewModelScope.launch {
            loadInfo()
            connectHub()
        }
        // 周期性刷新状态(运行时长、在线人数、启停状态)
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                loadInfo()
            }
        }
    }

    private suspend fun loadInfo() {
        repository.instanceInfo(instanceId).onSuccess { info ->
            _state.update {
                it.copy(
                    instanceName = info.name ?: it.instanceName,
                    status = info.status,
                    statusText = info.statusText,
                    onlinePlayers = info.onlinePlayers,
                    uptime = info.uptime,
                )
            }
        }
    }

    private suspend fun connectHub() {
        val hubClient = repository.createConsoleClient(
            instanceId = instanceId,
            onLog = { line -> appendLogs(listOf(LogLine(line))) },
            onCommandResult = { result ->
                if (!result.success) {
                    appendLogs(listOf(LogLine(">>> ${result.message ?: "命令发送失败"}", system = true)))
                }
            },
            onEulaRequired = { _events.tryEmit(ConsoleEvent.EulaRequired) },
        )
        client = hubClient
        try {
            withContext(Dispatchers.IO) { hubClient.connect() }
            _state.update { it.copy(connecting = false, connected = true) }
        } catch (e: Exception) {
            _state.update { it.copy(connecting = false, connected = false, connectionError = e.message) }
        }
    }

    fun retryConnect() {
        if (_state.value.connecting || _state.value.connected) return
        _state.update { it.copy(connecting = true, connectionError = null) }
        viewModelScope.launch { connectHub() }
    }

    fun sendCommand(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        appendLogs(listOf(LogLine("> $cmd")))
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { client?.sendCommand(cmd) }
            }
        }
    }

    fun sendAction(action: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = repository.sendAction(instanceId, action)
            if (result.isSuccess) {
                _events.tryEmit(ConsoleEvent.Toast(result.getOrNull() ?: "操作成功"))
                loadInfo()
            } else {
                _events.tryEmit(ConsoleEvent.Toast(result.exceptionOrNull()?.message ?: "操作失败"))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun agreeEulaAndStart() {
        viewModelScope.launch {
            repository.sendAction(instanceId, "agreeEula?true")
            val startResult = repository.sendAction(instanceId, "start")
            if (startResult.isSuccess) {
                _events.tryEmit(ConsoleEvent.Toast(startResult.getOrNull() ?: "启动成功"))
            } else {
                _events.tryEmit(ConsoleEvent.Toast(startResult.exceptionOrNull()?.message ?: "启动失败"))
            }
            loadInfo()
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun appendLogs(new: List<LogLine>) {
        // 用原子 update 保证 SignalR 回调线程并发追加时不丢日志
        _logs.update { current ->
            val merged = current + new
            if (merged.size > 3000) merged.takeLast(3000) else merged
        }
    }

    override fun onCleared() {
        val hub = client
        client = null
        if (hub != null) {
            viewModelScope.launch(Dispatchers.IO) { hub.disconnect() }
        }
    }
}
