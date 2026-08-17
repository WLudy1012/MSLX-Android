package com.mslx.console.data.remote

import com.mslx.console.data.model.ActionRequest
import com.mslx.console.data.model.ApiResponse
import com.mslx.console.data.model.InstanceInfo
import com.mslx.console.data.model.InstanceSummary
import com.mslx.console.data.model.StatusData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * MSLX Daemon 的 REST API。
 * 认证通过 OkHttp 拦截器统一附加 `x-api-key` 请求头完成。
 */
interface MslxApi {

    @GET("api/status")
    suspend fun status(): ApiResponse<StatusData>

    @GET("api/instance/list")
    suspend fun instanceList(): ApiResponse<List<InstanceSummary>>

    @GET("api/instance/info")
    suspend fun instanceInfo(@Query("id") id: Long): ApiResponse<InstanceInfo>

    @POST("api/instance/action")
    suspend fun action(@Body body: ActionRequest): ApiResponse<Any?>
}
