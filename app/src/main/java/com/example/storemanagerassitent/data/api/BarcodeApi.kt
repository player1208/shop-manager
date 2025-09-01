package com.example.storemanagerassitent.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// SCF 统一返回结构
data class ScfGoods(
    @SerializedName("goodsName") val goodsName: String?,
    @SerializedName("standard") val standard: String?,
    @SerializedName("barcode") val barcode: String?
)

data class ScfUnifiedResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String?,
    @SerializedName("data") val data: T?
)

interface BarcodeApiService {
    // SCF 条码查询（仅需要 barcode 参数）
    @GET("api/barcode/goods/details")
    suspend fun getGoodsDetails(
        @Query("barcode") barcode: String
    ): Response<ScfUnifiedResponse<ScfGoods>>
}

object BarcodeApiClient {
    private fun resolveBaseUrl(): String {
        // Prefer BuildConfig value if available
        return try {
            com.example.storemanagerassitent.BuildConfig.BARCODE_API_BASE_URL
        } catch (_: Throwable) {
            "https://api.example.com/" // fallback; must end with '/'
        }
    }

    val service: BarcodeApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl(resolveBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(BarcodeApiService::class.java)
    }
}


