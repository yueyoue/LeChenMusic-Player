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
 */
interface LunaTvApi {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/search")
    suspend fun search(@Query("keyword") keyword: String): Response<SearchResponse>

    @GET("api/detail")
    suspend fun getDetail(
        @Query("source") source: String,
        @Query("id") id: String
    ): Response<VideoDetailResponse>

    @GET("api/favorites")
    suspend fun getFavorites(): Response<FavoritesResponse>

    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<LunaApiResponse<Unit>>

    @HTTP(method = "DELETE", path = "api/favorites", hasBody = true)
    suspend fun removeFavorite(@Body request: FavoriteRequest): Response<LunaApiResponse<Unit>>

    @GET("api/playrecords")
    suspend fun getPlayRecords(): Response<PlayRecordsResponse>

    @POST("api/playrecords")
    suspend fun savePlayRecord(@Body request: PlayRecordRequest): Response<LunaApiResponse<Unit>>

    @HTTP(method = "DELETE", path = "api/playrecords", hasBody = true)
    suspend fun deletePlayRecord(@Body request: Map<String, String>): Response<LunaApiResponse<Unit>>

    @GET("api/live/sources")
    suspend fun getLiveSources(): Response<LiveSourcesResponse>

    @GET("api/live/channels")
    suspend fun getLiveChannels(@Query("source") source: String): Response<LiveChannelsResponse>

    @GET("api/home")
    suspend fun getHomeRecommend(): Response<HomeRecommendResponse>

    @GET("api/category")
    suspend fun getCategory(
        @Query("type") type: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<CategoryResponse>
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

    fun hasCookies(host: String): Boolean {
        return cookieStore[host]?.isNotEmpty() == true
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
