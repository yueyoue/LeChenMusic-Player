package com.lechenmusic.data.api

import com.lechenmusic.data.model.*
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * LunaTV API 接口定义
 * 注意：LunaTV 返回的是裸 JSON，没有 code/msg/data 包装
 */
interface LunaTvApi {

    @POST("api/login")
    suspend fun login(@Body request: Map<String, String>): Response<LoginResponse>

    @GET("api/search")
    suspend fun search(@Query("q") keyword: String): Response<SearchResponse>

    @GET("api/detail")
    suspend fun getDetail(
        @Query("source") source: String,
        @Query("id") id: String
    ): Response<VideoDetail>

    @GET("api/favorites")
    suspend fun getFavorites(): Response<List<VideoInfo>>

    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<Map<String, Any>>

    @HTTP(method = "DELETE", path = "api/favorites", hasBody = true)
    suspend fun removeFavorite(@Body request: FavoriteRequest): Response<Map<String, Any>>

    /** 返回 dict: {"source+id": {record...}} */
    @GET("api/playrecords")
    suspend fun getPlayRecords(): Response<Map<String, VideoPlayRecord>>

    @POST("api/playrecords")
    suspend fun savePlayRecord(@Body request: PlayRecordRequest): Response<Map<String, Any>>

    @HTTP(method = "DELETE", path = "api/playrecords", hasBody = true)
    suspend fun deletePlayRecord(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("api/live/sources")
    suspend fun getLiveSources(): Response<List<LiveSource>>

    @GET("api/live/channels")
    suspend fun getLiveChannels(@Query("source") source: String): Response<List<LiveChannelGroup>>

    @GET("api/search/resources")
    suspend fun getSearchResources(): Response<List<SearchResourceResponse>>
}

/**
 * 豆瓣 API 接口
 */
interface DoubanApi {
    @GET("v2/subject/recent_hot/{kind}")
    suspend fun getRecentHot(
        @Path("kind") kind: String,
        @Query("start") start: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String = "\u70ED\u95E8",
        @Query("type") type: String = "\u5168\u90E8"
    ): Response<DoubanHotResponse>
}

/**
 * 基于 Cookie 的认证管理
 */
object VideoCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    fun clear() {
        cookieStore.clear()
    }
}

/**
 * LunaTV API 客户端
 */
object VideoApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var api: LunaTvApi? = null

    fun getApi(baseUrl: String): LunaTvApi {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .cookieJar(VideoCookieJar)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val gson = com.google.gson.GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(SafeJsonConverterFactory(GsonConverterFactory.create(gson)))
                .build()
            currentBaseUrl = normalizedUrl
            api = retrofit!!.create(LunaTvApi::class.java)
        }
        return api!!
    }

    fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }

    fun clearSession() {
        VideoCookieJar.clear()
    }
}

/**
 * 豆瓣 API 客户端
 */
object DoubanApiClient {
    private val _api: DoubanApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Referer", "https://movie.douban.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://m.douban.com/rexxar/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DoubanApi::class.java)
    }

    fun getApi(): DoubanApi = _api
}
