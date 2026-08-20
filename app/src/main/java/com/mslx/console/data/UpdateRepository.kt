package com.mslx.console.data

import com.mslx.console.data.remote.ApiClient
import com.mslx.console.data.remote.GitHubRelease

/**
 * 应用更新信息（由 GitHub 最新 Release 解析而来）。
 */
data class AppUpdateInfo(
    /** 最新版本号（去掉 v 前缀），如 "1.2.6"。 */
    val version: String,
    /** Release 介绍（更新内容）。 */
    val notes: String,
    /** APK 下载直链。 */
    val downloadUrl: String,
    /** APK 文件名。 */
    val apkName: String,
    /** APK 大小（字节）。 */
    val apkSize: Long,
)

/**
 * 检查应用更新：查询 GitHub 仓库最新正式 Release，
 * 与当前版本比较，返回更新信息（无更新时返回 null）。
 */
class UpdateRepository {

    /** 检查是否有新版本。currentVersion 形如 "1.2.6"。 */
    suspend fun checkLatest(currentVersion: String): Result<AppUpdateInfo?> = runCatching {
        val release = ApiClient.buildGitHubReleaseApi().latestRelease()
        parseUpdate(release, currentVersion)
    }

    /** 从 Release 解析更新信息；版本不高于当前版本时返回 null。 */
    private fun parseUpdate(release: GitHubRelease, currentVersion: String): AppUpdateInfo? {
        val tag = release.tagName ?: return null
        val version = tag.removePrefix("v").trim()
        if (version.isBlank()) return null
        if (compareVersions(version, currentVersion) <= 0) return null

        val apk = release.assets.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
        val downloadUrl = apk?.browserDownloadUrl
        if (downloadUrl.isNullOrBlank()) return null

        return AppUpdateInfo(
            version = version,
            notes = release.body.orEmpty(),
            downloadUrl = downloadUrl,
            apkName = apk?.name ?: "app-release.apk",
            apkSize = apk?.size ?: 0,
        )
    }

    /** 语义化版本比较："1.10.0" > "1.9.9"。返回正数表示 a 更新。 */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").mapNotNull { it.toIntOrNull() }
        val pb = b.split(".").mapNotNull { it.toIntOrNull() }
        val max = maxOf(pa.size, pb.size)
        for (i in 0 until max) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
