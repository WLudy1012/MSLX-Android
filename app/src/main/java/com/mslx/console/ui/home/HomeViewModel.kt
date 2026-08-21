package com.mslx.console.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.InstanceSummary
import com.mslx.console.data.model.SystemInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 开服/关服通知条目。 */
data class ServerNotification(
    val id: Long,
    val instanceName: String,
    val isOpened: Boolean, // true=开服(变为运行中), false=关服(离开运行中)
    val time: Long,
)

data class HomeUiState(
    // 连接
    val connecting: Boolean = true,
    val connected: Boolean = false,
    val daemonName: String = "",
    val baseUrl: String = "",
    val protocol: String = "",
    val systemInfo: SystemInfo? = null,
    val daemonVersion: String = "",
    // 实例
    val instances: List<InstanceSummary> = emptyList(),
    val error: String? = null,
    // 通知
    val notifications: List<ServerNotification> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = getApplication<MSLXApplication>().container
    private val repository = container.instanceRepository
    private val store = container.settingsStore

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    /** 上次轮询到的实例状态快照（id -> status），用于检测开服/关服变化。 */
    private val lastStatus = mutableMapOf<Long, Int>()

    init {
        autoConnect()
        // 周期性刷新负载与实例状态
        viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                if (_state.value.connected) {
                    refreshMetrics()
                    refreshInstances()
                }
            }
        }
    }

    /** 自动连接已保存的激活 Daemon；没有配置则直接进入未连接状态。 */
    fun autoConnect() {
        viewModelScope.launch {
            val settings = store.settingsFlow.first()
            val daemon = settings.activeDaemon
            if (daemon == null) {
                _state.update { it.copy(connecting = false, connected = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    connecting = true,
                    daemonName = daemon.name.ifBlank { daemon.baseUrl },
                    baseUrl = daemon.baseUrl,
                    protocol = protocolLabel(daemon.baseUrl),
                )
            }
            val result = runCatching {
                repository.configure(daemon.baseUrl, daemon.apiKey)
                repository.verify()
            }
            if (result.isSuccess) {
                _state.update { it.copy(connecting = false, connected = true, error = null) }
                refreshMetrics()
                refreshInstances()
            } else {
                _state.update {
                    it.copy(
                        connecting = false,
                        connected = false,
                        error = result.exceptionOrNull()?.message ?: "连接失败",
                    )
                }
            }
        }
    }

    /** 重新连接（主页"重试"按钮）。 */
    fun retryConnect() = autoConnect()

    /** 刷新 Daemon 基础状态(版本/系统/负载)。 */
    fun refreshMetrics() {
        viewModelScope.launch {
            repository.getStatus().onSuccess { status ->
                _state.update {
                    it.copy(
                        systemInfo = status.systemInfo ?: SystemInfo(
                            cpuUsage = status.cpuUsage,
                            memoryUsage = status.memoryUsage,
                            memoryUsed = status.memoryUsed,
                            memoryTotal = status.memoryTotal,
                        ),
                        daemonVersion = status.version.orEmpty(),
                    )
                }
            }
        }
    }

    /** 刷新实例列表并检测开服/关服变化。 */
    fun refreshInstances() {
        viewModelScope.launch {
            repository.listInstances().fold(
                onSuccess = { list ->
                    detectStatusChanges(list)
                    _state.update {
                        it.copy(
                            instances = list,
                            error = null,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(error = e.message ?: "加载失败")
                    }
                },
            )
        }
    }

    /** 对比上次状态快照，生成开服/关服通知（仅保留最近 50 条）。 */
    private fun detectStatusChanges(current: List<InstanceSummary>) {
        val now = System.currentTimeMillis()
        val notifications = mutableListOf<ServerNotification>()
        for (instance in current) {
            val previous = lastStatus[instance.id]
            if (previous != null && previous != instance.status) {
                val becameRunning = instance.status == 2 && previous != 2
                val leftRunning = previous == 2 && instance.status != 2
                if (becameRunning || leftRunning) {
                    notifications.add(
                        ServerNotification(
                            id = instance.id,
                            instanceName = instance.name ?: "实例 #${instance.id}",
                            isOpened = becameRunning,
                            time = now,
                        ),
                    )
                }
            }
            lastStatus[instance.id] = instance.status
        }
        if (notifications.isNotEmpty()) {
            _state.update {
                it.copy(notifications = (notifications + it.notifications).take(50))
            }
        }
    }

    fun clearNotifications() {
        _state.update { it.copy(notifications = emptyList()) }
    }

    private fun protocolLabel(baseUrl: String): String = when {
        baseUrl.startsWith("https://", ignoreCase = true) -> "HTTPS / WSS"
        baseUrl.startsWith("http://", ignoreCase = true) -> "HTTP / WS"
        else -> "未知"
    }
}
