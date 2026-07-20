package com.lechenmusic

import android.app.Application
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.lechenmusic.data.repository.LyricsCache
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
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

        // 配置 Coil 图片加载器，为豆瓣图片添加 Referer
        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(this)
                .okHttpClient {
                    okhttp3.OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request = chain.request()
                            val url = request.url.toString()
                            val newRequest = if (url.contains("douban.com") || url.contains("doubanio.com")) {
                                request.newBuilder()
                                    .header("Referer", "https://movie.douban.com/")
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                                    .build()
                            } else request
                            chain.proceed(newRequest)
                        }
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
                ErrorReporter.init(this, serverUrl, username, "")
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

                // Report crash to server
                Companion.sendErrorToServer("crash", throwable.message ?: "Unknown", throwable.stackTrace.take(20).joinToString("\n") { "at $it" }, "crash_${thread.name}")
            } catch (_: Exception) {}
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        lateinit var instance: LeChenApp
            private set
        val appContext get() = instance.applicationContext

        /**
         * Send error log to WEB admin server
         * @param level error/warn/crash
         * @param message error message
         * @param stack stack trace
         * @param screen screen name where error occurred
         */
        fun sendErrorToServer(level: String, message: String, stack: String = "", screen: String = "") {
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
