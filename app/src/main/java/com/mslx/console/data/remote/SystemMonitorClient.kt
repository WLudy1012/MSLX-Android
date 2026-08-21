package com.mslx.console.data.remote

import com.google.gson.Gson
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.mslx.console.data.model.NodeStatsPayload
import com.mslx.console.data.model.SystemStatsEnvelope

/**
 * 订阅 /api/hubs/system 的系统负载实时推送。
 *
 * 服务端约定：
 *  - 客户端调用：JoinMonitor()、LeaveMonitor()
 *  - 服务端推送：ReceiveSystemStats({ local: NodeStatsPayload, slaves: {...} })
 *
 * 注意：连接、断开均为阻塞网络操作，务必在 IO 线程调用。
 */
class SystemMonitorClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val onStats: (NodeStatsPayload) -> Unit,
) {
    private val gson = Gson()
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
            { json: String -> handleStats(json) },
            String::class.java,
        )
        hub.start().blockingAwait()
        connection = hub
        isConnected = true
        hub.send("JoinMonitor")
    }

    private fun handleStats(json: String) {
        val envelope = runCatching { gson.fromJson(json, SystemStatsEnvelope::class.java) }
            .getOrNull() ?: return
        envelope.local?.let(onStats)
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
