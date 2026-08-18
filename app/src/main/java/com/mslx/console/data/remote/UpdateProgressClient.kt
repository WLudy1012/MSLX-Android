package com.mslx.console.data.remote

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder

/** 监听实例设置更新中的 Java/Core 下载进度。 */
class UpdateProgressClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val instanceId: Long,
    private val onStatus: (message: String, progress: Double, isError: Boolean) -> Unit,
) {
    private var connection: HubConnection? = null

    fun connect() {
        if (connection != null) return
        val hub = HubConnectionBuilder
            .create("${baseUrl.trimEnd('/')}/api/hubs/updateProgressHub")
            .withHeader("x-api-key", apiKey)
            .build()
        hub.on(
            "UpdateStatus",
            { message: String, progress: Double, isError: Boolean ->
                onStatus(message, progress, isError)
            },
            String::class.java,
            Double::class.java,
            Boolean::class.java,
        )
        hub.start().blockingAwait()
        connection = hub
        hub.send("JoinGroup", instanceId.toString())
    }

    fun disconnect() {
        val hub = connection ?: return
        connection = null
        try {
            hub.send("LeaveGroup", instanceId.toString())
            hub.stop().blockingAwait()
        } catch (_: Exception) {
            // 退出时忽略断开异常
        }
    }
}
