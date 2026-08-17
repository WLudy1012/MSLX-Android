package com.mslx.console.data.model

import com.google.gson.annotations.SerializedName

/**
 * MSLX Daemon 统一响应结构。
 * 后端约定：{ "code": 200, "message": "...", "data": ... }
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
)

/** GET /api/status 的 data 字段(部分字段)。 */
data class StatusData(
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("user") val user: String? = null,
    @SerializedName("username") val username: String? = null,
)

/** 实例列表项中的额外信息。 */
data class InstanceExtra(
    @SerializedName("onlinePlayers") val onlinePlayers: Int = 0,
)

/** GET /api/instance/list 的 data 数组元素。 */
data class InstanceSummary(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("basePath") val basePath: String? = null,
    @SerializedName("java") val java: String? = null,
    @SerializedName("core") val core: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("status") val status: Int = 0,
    @SerializedName("statusText") val statusText: String? = null,
    @SerializedName("expireTime") val expireTime: String? = null,
    @SerializedName("extra") val extra: InstanceExtra? = null,
)

/** GET /api/instance/info 的 data 字段(部分字段)。 */
data class InstanceInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: Int = 0,
    @SerializedName("statusText") val statusText: String? = null,
    @SerializedName("uptime") val uptime: String? = null,
    @SerializedName("onlinePlayers") val onlinePlayers: Int = 0,
    @SerializedName("java") val java: String? = null,
    @SerializedName("core") val core: String? = null,
)

/** POST /api/instance/action 的请求体。 */
data class ActionRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("action") val action: String,
)

/** SignalR CommandResult 事件负载：{ "success": bool, "message": "..." } */
data class CommandResultPayload(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
)
