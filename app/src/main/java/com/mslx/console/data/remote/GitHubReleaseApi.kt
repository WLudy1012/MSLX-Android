package com.mslx.console.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

/** GitHub Releases API 的 release 元素(部分字段)。 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset> = emptyList(),
)

/** release 附件(APK)。 */
data class GitHubReleaseAsset(
    @SerializedName("name") val name: String? = null,
    @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
    @SerializedName("size") val size: Long = 0,
)

/** GitHub Releases 接口(公开仓库，无需认证)。 */
interface GitHubReleaseApi {

    /** 获取最新正式 release。 */
    @GET("repos/WLudy1012/MSLX-Android/releases/latest")
    suspend fun latestRelease(): GitHubRelease
}
