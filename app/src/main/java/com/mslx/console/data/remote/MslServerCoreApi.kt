package com.mslx.console.data.remote

import com.google.gson.JsonElement
import com.mslx.console.data.model.ServerCoreDownloadInfo
import com.mslx.console.data.model.ServerCoreGameVersion
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MSLAPI v4 服务端核心接口(无需认证)。
 * 与网页版 api/mslapi/serverCore.ts 保持一致。
 */
interface MslServerCoreApi {

    /** 服务端核心分类。接口可能返回对象，也可能返回单元素数组，故用 JsonElement 手动解析。 */
    @GET("mirrors")
    suspend fun classify(): JsonElement

    @GET("mirrors/{name}")
    suspend fun gameVersion(@Path("name") name: String): ServerCoreGameVersion

    @GET("mirrors/{name}/{version}")
    suspend fun builds(
        @Path("name") name: String,
        @Path("version") version: String,
    ): List<String>

    @GET("download/server/{name}/{version}")
    suspend fun downloadInfo(
        @Path("name") name: String,
        @Path("version") version: String,
        @Query("build") build: String = "latest",
    ): ServerCoreDownloadInfo
}
