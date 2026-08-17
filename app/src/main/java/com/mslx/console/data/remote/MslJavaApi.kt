package com.mslx.console.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** MSLX 官方在线 API(MSLAPI)，无需认证。 */
interface MslJavaApi {

    @GET("query/jdk")
    suspend fun jdkVersions(
        @Query("os") os: String,
        @Query("arch") arch: String,
    ): List<String>
}
