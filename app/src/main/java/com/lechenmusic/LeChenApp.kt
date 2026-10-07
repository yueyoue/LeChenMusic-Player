package com.lechenmusic

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.lechenmusic.data.repository.LyricsCache
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.api.NavidromeAuth
import com.lechenmusic.player.MusicPlayerManager

class LeChenApp : Application() {
    lateinit var repository: MusicRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var playerManager: MusicPlayerManager
    lateinit var lyricsCache: LyricsCache

    override fun onCreate() {
        super.onCreate()

        instance = this
        repository = MusicRepository()
        settingsRepository = SettingsRepository(this)
        lyricsCache = LyricsCache(this)
        playerManager = MusicPlayerManager(this)
        playerManager.init(repository)

        // 配置 Coil 图片加载器：豆瓣图片加 Referer，封面/头像两级缓存落手机。
        //
        // 缓存是「打开 APP 秒出图」的关键：内存层（LRU Bitmap）让同一屏图片第二次出现零耗时，
        // 磁盘层（LRU 文件）让杀掉进程重开也能直接读本地，不再重新下载。
        // 行业通行做法（Glide/Coil/Picasso 同一套路）：URL 作 key的两级 LRU，配合服务端
        // 长期 Cache-Control——所以本仓服务端封面是 max-age=315360000（与 Subsonic getCoverArt
        // 一致），APP 侧才能一直命中本地副本。
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request = chain.request()
                            val url = request.url.toString()
                            val newRequest = if (url.contains("douban.com") || url.contains("doubanio.com") || url.contains("cmliussss.com")) {
                                request.newBuilder()
                                    .header("Referer", "https://movie.douban.com/")
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                                    .build()
                            } else request
                            chain.proceed(newRequest)
                        }
                        .build()
                }
                .memoryCache {
                    // 25% 可用内存：首页/列表一滑几十张封面，太小会反复解码
                    MemoryCache.Builder(applicationContext).maxSizePercent(0.25).build()
                }
                // 磁盘缓存显式给到 256MB（封面缩略图约 15–50KB 一张，能放下几千张）。
                // 不设的话 Coil 也会用默认的 SingletonDiskCache（10–250MB），
                // 这里显式声明是为了把配额和目录写死，也避免以后有人再 new ImageLoader绕开它。
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("lechen_image_cache"))
                        .maxSizeBytes(256L * 1024 * 1024)
                        .build()
                }
                .build()
        )

        // #19: Initialize global error reporter
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val serverUrl = prefs.getString("serverUrl", "") ?: ""
            val username = prefs.getString("username", "") ?: ""
            if (serverUrl.isNotBlank()) {
                ErrorReporter.init(this, serverUrl, username) { NavidromeAuth.token }
            }
        } catch (_: Exception) {}

        // Crash handler - log crashes and report to server
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashLog = java.io.File(getExternalFilesDir(null), "crash_log.txt")
                val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                crashLog.appendText("\n[$ts] CRASH on ${thread.name}: ${throwable.message}\n")
                throwable.stackTrace.take(20).forEach { crashLog.appendText("  at $it\n") }
                android.util.Log.e("LeChenMusic", "CRASH: ${throwable.message}", throwable)

                // Report crash to server (同步阻塞上报：马上要 killProcess，异步会丢)
                Companion.sendErrorToServerBlocking("crash", throwable.message ?: "Unknown", throwable.stackTrace.take(20).joinToString("\n") { "at $it" }, "crash_${thread.name}")
            } catch (_: Exception) {}
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        lateinit var instance: LeChenApp
            private set
        val appContext get() = instance.applicationContext

        /**
         * Send error log to WEB admin server (fire-and-forget on a background thread).
         *
         * @param level error/warn/crash
         * @param message error message
         * @param stack stack trace
         * @param screen screen name where error occurred
         *
         * 绝不能在主线程同步发网络请求：旧实现用 OkHttp .execute() 在调用线程阻塞，
         * 播放出错时 onPlayerError（主线程）→ 卡死 UI 数秒甚至触发 ANR，
         * 用户再点几下按钮就闪退。
         */
        fun sendErrorToServer(level: String, message: String, stack: String = "", screen: String = "") {
            Thread {
                sendErrorToServerBlocking(level, message, stack, screen)
            }.start()
        }

        /** 阻塞版本：仅供崩溃处理器在进程退出前调用（否则上报会丢失）。 */
        fun sendErrorToServerBlocking(level: String, message: String, stack: String = "", screen: String = "") {
            try {
                val context = appContext
                val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                val serverUrl = prefs.getString("serverUrl", "") ?: ""
                if (serverUrl.isBlank()) return

                val url = "${serverUrl.trimEnd('/')}/api/error-log"
                val jsonBody = org.json.JSONObject().apply {
                    put("level", level)
                    put("message", message)
                    put("stack", stack)
                    put("screen", screen)
                    put("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    put("appVersion", try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (_: Exception) { "unknown" })
                }

                val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                // Fire and forget - don't block
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {
                // Silently fail - don't crash while reporting a crash
            }
        }
    }
}
