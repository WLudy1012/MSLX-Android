package com.mslx.console.data.remote

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder

/**
 * 监听实例创建进度 (/api/hubs/creationProgressHub)。
 * 服务端约定：
 *  - 客户端调用：TrackServer(serverId)、UnTrackServer(serverId)
 *  - 服务端推送：StatusUpdate(serverId, message, progress)
 */
class CreationProgressClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val serverId: String,
    private val onStatus: (id: String, message: String, progress: Double) -> Unit,
) {
    private var connection: HubConnection? = null

    fun connect() {
        if (connection != null) return
        val hub = HubConnectionBuilder
            .create("${baseUrl.trimEnd('/')}/api/hubs/creationProgressHub")
            .withHeader("x-api-key", apiKey)
            .setHttpClientBuilderCallback { builder -> ApiClient.configureDaemonHttpClient(builder) }
            .build()
        hub.on(
            "StatusUpdate",
            { id: String, message: String, progress: Double ->
                onStatus(id, message, progress)
            },
            String::class.java,
            String::class.java,
            Double::class.java,
        )
        hub.start().blockingAwait()
        connection = hub
        hub.send("TrackServer", serverId)
    }

    fun disconnect() {
        val hub = connection ?: return
        connection = null
        try {
            hub.send("UnTrackServer", serverId)
            hub.stop().blockingAwait()
        } catch (_: Exception) {
            // 退出阶段忽略异常
        }
    }
}
