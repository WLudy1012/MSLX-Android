package com.mslx.console.data.model

import com.google.gson.annotations.SerializedName

/** 插件/模组列表响应。 */
data class PmListData(
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("activeCount") val activeCount: Int = 0,
    @SerializedName("clientOnlyCount") val clientOnlyCount: Int = 0,
    @SerializedName("disabledCount") val disabledCount: Int = 0,
    @SerializedName("jarFiles") val jarFiles: List<String> = emptyList(),
    @SerializedName("clientJarFiles") val clientJarFiles: List<String> = emptyList(),
    @SerializedName("disableJarFiles") val disableJarFiles: List<String> = emptyList(),
)

/** 插件/模组批量操作请求。 */
data class PmSetRequest(
    @SerializedName("mode") val mode: String,
    @SerializedName("action") val action: String,
    @SerializedName("targets") val targets: List<String>,
)

/** 保存文件内容请求。 */
data class SaveFileRequest(
    @SerializedName("path") val path: String,
    @SerializedName("content") val content: String,
)
