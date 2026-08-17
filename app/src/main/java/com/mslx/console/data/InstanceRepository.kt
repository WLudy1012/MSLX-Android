package com.mslx.console.data

import com.mslx.console.data.model.ActionRequest
import com.mslx.console.data.model.CommandResultPayload
import com.mslx.console.data.model.InstanceInfo
import com.mslx.console.data.model.InstanceSummary
import com.mslx.console.data.model.LocalJava
import com.mslx.console.data.model.PmListData
import com.mslx.console.data.model.PmSetRequest
import com.mslx.console.data.model.SaveFileRequest
import com.mslx.console.data.model.ServerSettings
import com.mslx.console.data.model.StatusData
import com.mslx.console.data.remote.ApiClient
import com.mslx.console.data.remote.ConsoleHubClient
import com.mslx.console.data.remote.MslxApi

/** 实例相关的数据入口，封装 REST 调用与 SignalR 控制台客户端创建。 */
class InstanceRepository {

    @Volatile
    private var api: MslxApi? = null

    @Volatile
    var baseUrl: String = ""
        private set

    @Volatile
    var apiKey: String = ""
        private set

    val isConfigured: Boolean get() = api != null && baseUrl.isNotBlank()

    /** 配置连接地址并重建 API 客户端(每次连接/切换连接时调用)。 */
    fun configure(baseUrl: String, apiKey: String) {
        val normalized = baseUrl.trim().trimEnd('/')
        this.baseUrl = normalized
        this.apiKey = apiKey
        this.api = ApiClient.build(normalized, apiKey)
    }

    private fun requireApi(): MslxApi =
        api ?: throw IllegalStateException("尚未配置连接信息")

    suspend fun verify(): Result<Unit> = runCatching {
        val resp = requireApi().status()
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "API Key 无效")
    }

    suspend fun getStatus(): Result<StatusData> = runCatching {
        val resp = requireApi().status()
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取状态失败")
        resp.data ?: throw IllegalStateException("返回数据为空")
    }

    suspend fun javaList(refresh: Boolean = false): Result<List<LocalJava>> = runCatching {
        val resp = requireApi().javaList(refresh)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取 Java 列表失败")
        resp.data ?: emptyList()
    }

    /** 从 MSLAPI 在线获取 Java 版本号列表。 */
    suspend fun onlineJavaVersions(os: String, arch: String): Result<List<String>> = runCatching {
        ApiClient.buildMslJavaApi().jdkVersions(os, arch)
    }

    suspend fun listInstances(): Result<List<InstanceSummary>> = runCatching {
        val resp = requireApi().instanceList()
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取实例列表失败")
        resp.data ?: emptyList()
    }

    suspend fun instanceInfo(id: Long): Result<InstanceInfo> = runCatching {
        val resp = requireApi().instanceInfo(id)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取实例信息失败")
        resp.data ?: throw IllegalStateException("返回数据为空")
    }

    suspend fun sendAction(id: Long, action: String): Result<String> = runCatching {
        val resp = requireApi().action(ActionRequest(id, action))
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "操作失败")
        resp.message ?: "操作成功"
    }

    suspend fun getSettings(id: Long): Result<ServerSettings> = runCatching {
        val resp = requireApi().instanceSettings(id)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取设置失败")
        resp.data ?: throw IllegalStateException("返回数据为空")
    }

    suspend fun updateSettings(id: Long, settings: ServerSettings): Result<String> = runCatching {
        val resp = requireApi().updateInstanceSettings(id, settings)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "保存失败")
        resp.message ?: "保存成功"
    }

    suspend fun pmList(id: Long, mode: String): Result<PmListData> = runCatching {
        val resp = requireApi().pmList(id, mode)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "获取列表失败")
        resp.data ?: throw IllegalStateException("返回数据为空")
    }

    suspend fun pmSet(id: Long, mode: String, action: String, targets: List<String>): Result<String> = runCatching {
        val resp = requireApi().pmSet(id, PmSetRequest(mode, action, targets))
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "操作失败")
        resp.message ?: "操作成功"
    }

    suspend fun fileContent(id: Long, path: String): Result<String> = runCatching {
        val resp = requireApi().fileContent(id, path)
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "读取失败")
        resp.data ?: throw IllegalStateException("返回数据为空")
    }

    suspend fun saveFileContent(id: Long, path: String, content: String): Result<String> = runCatching {
        val resp = requireApi().saveFileContent(id, SaveFileRequest(path, content))
        if (resp.code != 200) throw IllegalStateException(resp.message ?: "保存失败")
        resp.message ?: "保存成功"
    }

    fun createConsoleClient(
        instanceId: Long,
        onLog: (String) -> Unit,
        onCommandResult: (CommandResultPayload) -> Unit,
        onEulaRequired: () -> Unit,
    ): ConsoleHubClient =
        ConsoleHubClient(baseUrl, apiKey, instanceId, onLog, onCommandResult, onEulaRequired)
}
