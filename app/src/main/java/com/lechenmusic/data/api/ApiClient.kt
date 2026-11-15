package com.lechenmusic.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * A tolerant JSON converter.
 *
 * Some server endpoints commit the status code before setting Content-Type, and
 * proxies may rewrite the header as well, so a perfectly valid JSON body can
 * arrive labelled "text/plain; charset=utf-8". The previous implementation
 * rejected any text/plain or text/html body outright, which surfaced as
 * "服务器返回了非JSON响应" even though the request had actually succeeded — for
 * example an online-download task was created while the app reported an error.
 *
 * The body is now always read as text and parsed as JSON regardless of the
 * declared media type; only a body that is genuinely not JSON raises an error,
 * and its snippet is kept for diagnostics.
 */
class SafeJsonConverterFactory(
    private val delegate: GsonConverterFactory,
    private val gson: com.google.gson.Gson = com.google.gson.GsonBuilder().setLenient().create()
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {

        // ResponseBody 必须原样透传：/api/song 一页 2~3MB，要按流逐条解析（见
        // MusicRepository.parseNativeSongPage）。若交给本工厂，会先 body.string() 读成
        // 整页大字符串再建 JsonElement 树，正是内存峰值与 OOM 的来源。
        // 返回 null 表示本工厂不处理，由 Retrofit 内置的 BuiltInConverters 接管。
        if (type === ResponseBody::class.java) return null

        return Converter<ResponseBody, Any?> { body ->
            // The media type is only a hint: it cannot be trusted to decide
            // whether the payload is JSON, because a server that commits the
            // status code before setting Content-Type (or a proxy rewriting it)
            // delivers valid JSON labelled text/plain.
            val text = try {
                body.string()
            } catch (e: Exception) {
                throw Exception("读取服务器响应失败，请检查网络或服务器状态")
            }
            // Gson rejects a leading BOM; trim it together with stray whitespace.
            val json = text.trim().removePrefix("\uFEFF")
            if (json.isEmpty()) {
                throw Exception("服务器返回了空响应，请检查服务器地址是否正确")
            }
            // 这里必须显式给 fromJson 类型实参 <Any?>：泛型 <T> T fromJson(String, Type)
            // 的 T 在 try 表达式内无法从期望类型推断，Kotlin 1.9 会把它推断成 Nothing，
            // 编译出 checkcast java/lang/Void —— 解析成功反而抛 ClassCastException，
            // 被下面的 catch 包装成"非JSON响应"，登录/所有接口全军覆没（1.7.1/1.7.2 的登录报错根因）。
            try {
                gson.fromJson<Any?>(json, type)
            } catch (e: Exception) {
                // Genuinely not JSON (HTML error page, proxy notice, …).
                throw Exception("服务器返回了非JSON响应: ${json.take(200)}")
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        return delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)!!
    }
}

// Navidrome JWT token for native API authentication
object NavidromeAuth {
    private var _token: String? = null
    val token: String? get() = _token
    fun setToken(t: String?) { _token = t }
    fun clear() { _token = null }
}

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var audiobookRetrofit: Retrofit? = null

    fun getApi(baseUrl: String): SubsonicApi {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
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
        }
        return retrofit!!.create(SubsonicApi::class.java)
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }

    /**
     * @param size >0 时附带 &size= 参数，由服务端直接缩放后返回（原图几百 KB~几 MB 会变成几十 KB）。
     *             默认 640（列表/卡片/详情头图够用）；全屏播放器传 1024；小图标可传 320。
     *             传 0 = 原图，除特殊场景不建议。
     */
    fun getCoverArtUrl(baseUrl: String, username: String, password: String, coverArtId: String?, size: Int = 640): String? {
        if (coverArtId.isNullOrBlank()) return null
        val normalizedUrl = normalizeUrl(baseUrl)
        val encodedPass = if (password.startsWith("enc:")) password else "enc:${password.toByteArray().joinToString("") { "%02x".format(it) }}"
        val sizeParam = if (size > 0) "&size=$size" else ""
        return "${normalizedUrl}rest/getCoverArt?u=$username&p=$encodedPass&id=$coverArtId&v=1.16.1&c=lechenmusic" + sizeParam
    }

    fun getStreamUrl(baseUrl: String, username: String, password: String, songId: String): String {
        val normalizedUrl = normalizeUrl(baseUrl)
        val encodedPass = if (password.startsWith("enc:")) password else "enc:${password.toByteArray().joinToString("") { "%02x".format(it) }}"
        return "${normalizedUrl}rest/stream?u=$username&p=$encodedPass&id=$songId&v=1.16.1&c=lechenmusic"
    }

    /**
     * Get a Retrofit client configured with Navidrome JWT auth for audiobook API.
     * The interceptor adds X-ND-Authorization header to all requests.
     */
    fun getAudiobookApi(baseUrl: String): SubsonicApi {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (audiobookRetrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                // HTTP 缓存：服务端对有声书只读接口返回 Cache-Control/ETag，
                // 配上 Cache 后短时间内重复进入首页/详情直接命中本地缓存（Loading 秒过），
                // 过期后带 If-None-Match 走 304，不再重复传整包 JSON。
                .cache(okhttp3.Cache(java.io.File(com.lechenmusic.LeChenApp.appContext.cacheDir, "http_cache"), 32L * 1024 * 1024))
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val token = NavidromeAuth.token
                    val builder = original.newBuilder()
                    if (token != null) {
                        builder.header("X-ND-Authorization", "Bearer $token")
                    }
                    val response = chain.proceed(builder.build())
                    // Read refreshed JWT token from server response header
                    // The server's JWTRefresher middleware sends a new token in the Authorization header
                    val newToken = response.header("Authorization")
                    if (!newToken.isNullOrBlank()) {
                        val tokenValue = if (newToken.startsWith("Bearer ", ignoreCase = true)) {
                            newToken.substring(7)
                        } else {
                            newToken
                        }
                        if (tokenValue.isNotBlank() && tokenValue != NavidromeAuth.token) {
                            NavidromeAuth.setToken(tokenValue)
                        }
                    }
                    response
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val gson = com.google.gson.GsonBuilder()
                .setLenient()
                .create()

            audiobookRetrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(SafeJsonConverterFactory(GsonConverterFactory.create(gson)))
                .build()
        }
        return audiobookRetrofit!!.create(SubsonicApi::class.java)
    }

    /**
     * Authenticate with Navidrome native API and get JWT token.
     * Called once on login; the token is stored in NavidromeAuth.
     */
    suspend fun authenticateNavidrome(baseUrl: String, username: String, password: String): String? {
        return try {
            val normalizedUrl = normalizeUrl(baseUrl)
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val jsonBody = com.google.gson.Gson().toJson(mapOf(
                "username" to username,
                "password" to password
            ))
            val body = okhttp3.RequestBody.create(
                "application/json; charset=utf-8".toMediaType(),
                jsonBody
            )
            val request = okhttp3.Request.Builder()
                .url("${normalizedUrl}auth/login")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = com.google.gson.JsonParser.parseString(response.body?.string()).asJsonObject
                val token = json.get("token")?.asString
                if (token != null) {
                    NavidromeAuth.setToken(token)
                }
                token
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
