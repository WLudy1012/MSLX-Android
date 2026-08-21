package com.mslx.console.data

import com.mslx.console.data.remote.ApiClient
import com.mslx.console.data.remote.GitHubRelease

/**
 * 应用更新信息（由 GitHub Release 解析而来）。
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
    /**
     * 是否强制更新：任一历史 Release 的说明含"强制更新"标记且其版本高于当前版本时为 true。
     * 防绕过：即使最新版本本身非强制，只要存在"高于当前版本"的强制版本（如 1.2.10 强制、
     * 用户停留在 1.2.9 而最新是 1.2.11），也必须强制升级到最新版。
     */
    val forceUpdate: Boolean = false,
)

/**
 * 检查应用更新：查询 GitHub 仓库 Release 列表，
 * 与当前版本比较，返回更新信息（无更新时返回 null）。
 */
class UpdateRepository {

    /** 检查是否有新版本。currentVersion 形如 "1.2.6"。 */
    suspend fun checkLatest(currentVersion: String): Result<AppUpdateInfo?> = runCatching {
        val releases = ApiClient.buildGitHubReleaseApi().releases()
        parseUpdate(releases, currentVersion)
    }

    /** 从 Release 列表解析更新信息；无更新时返回 null。 */
    private fun parseUpdate(releases: List<GitHubRelease>, currentVersion: String): AppUpdateInfo? {
        // 过滤出正式(非预发布)且带 APK 资产的版本
        val valid = releases
            .filter { !it.prerelease }
            .mapNotNull { release ->
                val version = release.tagName?.removePrefix("v")?.trim()
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val apk = release.assets.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apk?.browserDownloadUrl
                if (downloadUrl.isNullOrBlank()) return@mapNotNull null
                Triple(version, release, apk)
            }
            // 语义化版本从高到低（字符串字典序会把 1.2.9 排在 1.2.10 前面，必须用数值比较）
            .sortedWith { a, b -> compareVersions(b.first, a.first) }

        val newest = valid.firstOrNull() ?: return null
        val newestVersion = newest.first
        // 最新版本不高于当前版本 → 无更新
        if (compareVersions(newestVersion, currentVersion) <= 0) return null

        // 强制判定：任一版本高于当前版本且说明含"强制更新"标记
        val forceUpdate = valid.any { (version, release, _) ->
            compareVersions(version, currentVersion) > 0 &&
                release.body.orEmpty().contains("强制更新", ignoreCase = true)
        }

        val (_, newestRelease, newestApk) = newest
        return AppUpdateInfo(
            version = newestVersion,
            notes = newestRelease.body.orEmpty(),
            downloadUrl = newestApk.browserDownloadUrl.orEmpty(),
            apkName = newestApk.name ?: "app-release.apk",
            apkSize = newestApk.size ?: 0,
            forceUpdate = forceUpdate,
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
