package com.mslx.console.data.remote

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.mslx.console.data.model.CommandResultPayload

/**
 * 封装 /api/hubs/instanceControlHub 的 SignalR 连接。
 *
 * 服务端约定：
 *  - 客户端调用：JoinGroup(instanceId)、LeaveGroup(instanceId)、SendCommand(instanceId, command)
 *  - 服务端推送：ReceiveLog(string)、CommandResult({success,message})、RequireEULA()
 *
 * 注意：连接、断开均为阻塞网络操作，务必在 IO 线程调用。
 */
class ConsoleHubClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val instanceId: Long,
    private val onLog: (String) -> Unit,
    private val onCommandResult: (CommandResultPayload) -> Unit,
    private val onEulaRequired: () -> Unit,
) {
    @Volatile
    private var connection: HubConnection? = null

    val isConnected: Boolean get() = connection != null

    fun connect() {
        if (connection != null) return
        val url = "${baseUrl.trimEnd('/')}/api/hubs/instanceControlHub"

        val hub = HubConnectionBuilder.create(url)
            .withHeader("x-api-key", apiKey)
            .setHttpClientBuilderCallback { builder -> ApiClient.configureDaemonHttpClient(builder) }
            .build()

        hub.on("ReceiveLog", { log: String -> onLog(log) }, String::class.java)
        hub.on(
            "CommandResult",
            { result: CommandResultPayload -> onCommandResult(result) },
            CommandResultPayload::class.java,
        )
        hub.on("RequireEULA", { onEulaRequired() })

        hub.start().blockingAwait()
        connection = hub
        hub.send("JoinGroup", instanceId)
    }

    fun sendCommand(command: String) {
        connection?.send("SendCommand", instanceId, command)
    }

    fun disconnect() {
        val hub = connection ?: return
        connection = null
        try {
            hub.send("LeaveGroup", instanceId)
            hub.stop().blockingAwait()
        } catch (_: Exception) {
            // 断开阶段忽略异常，避免影响界面退出
        }
    }
}
