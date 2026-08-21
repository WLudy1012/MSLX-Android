package com.mslx.console.data.remote

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.mslx.console.data.model.NodeStatsPayload
import com.mslx.console.data.model.SystemStatsEnvelope

/**
 * 订阅 /api/hubs/system 的系统负载实时推送。
 *
 * 服务端约定：
 *  - 客户端调用：JoinMonitor()、LeaveMonitor()
 *  - 服务端推送：ReceiveSystemStats(单参数对象 { local: NodeStatsPayload, slaves: {...} })
 *
 * 注意：连接、断开均为阻塞网络操作，务必在 IO 线程调用。
 *
 * 坑（已字节码级确认）：服务端推的是对象而非字符串，若用 String::class.java 接收，
 * SignalR 8.0.8 的 GsonHubProtocol 会在 bindArguments 时抛 IllegalStateException 并
 * 把该次调用静默丢弃（不回调、不报错、logcat 仅一条 slf4j error）。必须注册
 * SystemStatsEnvelope::class.java 类型化接收。
 */
class SystemMonitorClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val onStats: (NodeStatsPayload) -> Unit,
) {
    private var connection: HubConnection? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun connect() {
        if (connection != null) return
        val hub = HubConnectionBuilder
            .create("${baseUrl.trimEnd('/')}/api/hubs/system")
            .withHeader("x-api-key", apiKey)
            .setHttpClientBuilderCallback { builder -> ApiClient.configureDaemonHttpClient(builder) }
            .build()
        hub.on(
            "ReceiveSystemStats",
            { envelope: SystemStatsEnvelope -> envelope.local?.let(onStats) },
            SystemStatsEnvelope::class.java,
        )
        hub.onClosed { _: Exception? ->
            isConnected = false
        }
        hub.start().blockingAwait()
        connection = hub
        isConnected = true
        hub.send("JoinMonitor")
    }

    fun disconnect() {
        val hub = connection ?: return
        connection = null
        isConnected = false
        try {
            hub.send("LeaveMonitor")
            hub.stop().blockingAwait()
        } catch (_: Exception) {
            // 断开阶段忽略异常
        }
    }
}
