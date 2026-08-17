package com.mslx.console.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun build(baseUrl: String, apiKey: String): MslxApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-api-key", apiKey)
                    .addHeader("User-Agent", "MSLX-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MslxApi::class.java)
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
