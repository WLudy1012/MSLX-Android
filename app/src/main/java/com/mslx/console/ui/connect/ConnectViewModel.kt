package com.mslx.console.ui.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.DaemonConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ConnectUiState(
    val editingId: String? = null,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val loading: Boolean = false,
    val autoChecking: Boolean = false,
    val error: String? = null,
)

class ConnectViewModel(
    application: Application,
    private val autoConnect: Boolean = true,
    private val editingDaemonId: String? = null,
) : AndroidViewModel(application) {

    private val container = getApplication<MSLXApplication>().container
    private val repository = container.instanceRepository
    private val store = container.settingsStore

    private val _state = MutableStateFlow(ConnectUiState())
    val state = _state.asStateFlow()

    private val _connected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connected = _connected.asSharedFlow()

    /** 启动自动连接失败事件（携带错误信息），由界面回退主页并弹窗提示。 */
    private val _autoConnectFailed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val autoConnectFailed = _autoConnectFailed.asSharedFlow()

    init {
        if (autoConnect || editingDaemonId != null) {
            viewModelScope.launch {
                val settings = store.settingsFlow.first()
                val target = settings.daemons.firstOrNull { it.id == editingDaemonId }
                    ?: settings.activeDaemon
                if (target != null) {
                    _state.update {
                        it.copy(
                            editingId = target.id,
                            name = target.name,
                            baseUrl = target.baseUrl,
                            apiKey = target.apiKey,
                        )
                    }
                    // 已有激活的 Daemon → 自动连接
                    if (autoConnect) doConnect(target, auto = true)
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onBaseUrlChange(value: String) = _state.update { it.copy(baseUrl = value, error = null) }
    fun onApiKeyChange(value: String) = _state.update { it.copy(apiKey = value, error = null) }

    fun connect() {
        val s = _state.value
        val baseUrl = normalizeBaseUrl(s.baseUrl)
        val apiKey = s.apiKey.trim()
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            _state.update { it.copy(error = "请填写完整的 Daemon 地址和 API Key。") }
            return
        }
        val name = s.name.trim().ifBlank { baseUrl }
        val config = DaemonConfig(
            id = s.editingId ?: UUID.randomUUID().toString(),
            name = name,
            baseUrl = baseUrl,
            apiKey = apiKey,
        )
        doConnect(config)
    }

    /**
     * 规范化 Daemon 地址：去除首尾空白/斜杠。
     * 已带 http:// 或 https:// 前缀（大小写不敏感）则原样保留；
     * 完全未写协议时才补默认 http://。
     */
    private fun normalizeBaseUrl(input: String): String {
        var url = input.trim().trimEnd('/')
        val lower = url.lowercase()
        val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
        if (url.isNotBlank() && !hasScheme) {
            url = "http://$url"
        }
        return url
    }

    private fun doConnect(config: DaemonConfig, auto: Boolean = false) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, autoChecking = auto, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                repository.configure(config.baseUrl, config.apiKey)
                repository.verify()
            }
            if (result.isSuccess) {
                store.upsertDaemon(config)
                _state.update { it.copy(loading = false, autoChecking = false) }
                _connected.tryEmit(Unit)
            } else {
                val message = "连接失败：${result.exceptionOrNull()?.message ?: "未知错误"}"
                _state.update {
                    it.copy(loading = false, autoChecking = false, error = message)
                }
                // 启动自动连接失败：通知界面回退主页并弹窗，而不是停留在连接页卡住
                if (auto) {
                    _autoConnectFailed.tryEmit(message)
                }
            }
        }
    }
}
