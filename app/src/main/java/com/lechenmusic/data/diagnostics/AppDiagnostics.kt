package com.lechenmusic.data.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Debug
import android.os.SystemClock
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用内内存与加载诊断（设置 → 诊断）。
 *
 * 为什么要有这个入口：APP 曾两次在 OkHttp 解析响应时抛 OOM 闪退
 * （crash_log1：5224B 分配失败、剩余 1.87MB；crash_log2：32B 分配失败、剩余 1.86MB），
 * 但崩溃日志只能说明「堆被打满」，说不清是被谁吃掉的：
 * 曲库全量列表、歌词 SharedPreferences、图片内存/磁盘缓存、播放器解码缓冲都可能。
 * 现场又拿不到 adb，于是把取证手段内置到 APP 里：
 *
 *  1. 运行时快照：堆上限/已用/占比、largeHeap 是否真的生效（memoryClass vs largeMemoryClass）、
 *     进程 PSS、native 堆、系统可用内存与 lowMemory 标志；
 *  2. 规模统计：曲库/专辑/缓存歌曲条数、曲库缓存文件大小、歌词条数与体积、
 *     图片缓存目录体积、filesDir 总体积——用来判断哪个缓存无上限地长；
 *  3. 加载耗时：登录后「拉全曲库」与「解析本地曲库缓存」各花多久，定位「打开 APP 等很久」的瓶颈；
 *  4. 堆转储：点一下 dump 出 .hprof 并直接分享出去，用 MAT/Android Studio 看谁占着堆。
 *
 * 所有采集都是只读操作；唯一的写动作是 dump hprof 与导出报告文本，
 * 都落在 getExternalFilesDir(null)/diagnostics 下（FileProvider 已配置根目录，可直接分享）。
 */
object AppDiagnostics {

    const val DIR_NAME = "diagnostics"

    private const val MB = 1024L * 1024L
    private const val LYRICS_PREFS = "lyrics_cache"

    /** 最近一次「从服务端拉全曲库」的耗时与条数（-1 表示本次进程内还没拉过） */
    @Volatile private var libraryLoadMs: Long = -1L
    @Volatile private var libraryLoadCount: Int = 0

    /** 最近一次「解析本地曲库缓存文件」的耗时与条数 */
    @Volatile private var cacheParseMs: Long = -1L
    @Volatile private var cacheParseCount: Int = 0

    @Volatile private var lastLoadAt: Long = 0L

    fun recordLibraryLoad(durationMs: Long, count: Int) {
        libraryLoadMs = durationMs
        libraryLoadCount = count
        lastLoadAt = System.currentTimeMillis()
    }

    fun recordCacheParse(durationMs: Long, count: Int) {
        cacheParseMs = durationMs
        cacheParseCount = count
    }

    /** 一次采集的完整快照，字段全部是展示用原始值 */
    data class Snapshot(
        val maxHeapMb: Long,
        val usedHeapMb: Long,
        val heapPercent: Int,
        val memoryClassMb: Int,
        val largeMemoryClassMb: Int,
        val nativeHeapMb: Long,
        val pssMb: Long,
        val systemTotalMb: Long,
        val systemAvailMb: Long,
        val lowMemory: Boolean,
        val songCount: Int,
        val albumCount: Int,
        val cachedSongCount: Int,
        val allSongsCacheBytes: Long,
        val lyricsCount: Int,
        val lyricsBytes: Long,
        val imageCacheBytes: Long,
        val filesBytes: Long,
        val libraryLoadMs: Long,
        val libraryLoadCount: Int,
        val cacheParseMs: Long,
        val cacheParseCount: Int,
        val lastLoadAt: Long,
        val uptimeMinutes: Long
    )

    /**
     * 采集当前内存与缓存规模。曲库/专辑等条数由调用方（ViewModel 的 StateFlow）传入，
     * 这里不持有 ViewModel，避免把诊断代码绑死在 UI 层。
     */
    fun capture(
        context: Context,
        songCount: Int,
        albumCount: Int,
        cachedSongCount: Int,
        lyricsCount: Int
    ): Snapshot {
        val runtime = Runtime.getRuntime()
        val maxBytes = runtime.maxMemory()
        val totalBytes = runtime.totalMemory()
        val freeBytes = runtime.freeMemory()
        val usedBytes = totalBytes - freeBytes

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryClass = am?.memoryClass ?: 0
        val largeMemoryClass = am?.largeMemoryClass ?: 0

        var systemTotalMb = 0L
        var systemAvailMb = 0L
        var lowMemory = false
        try {
            if (am != null) {
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                systemTotalMb = info.totalMem / MB
                systemAvailMb = info.availMem / MB
                lowMemory = info.lowMemory
            }
        } catch (_: Exception) {
        }

        var nativeMb = 0L
        try {
            nativeMb = Debug.getNativeHeapAllocatedSize() / MB
        } catch (_: Exception) {
        }

        var pssMb = 0L
        try {
            val memInfo = Debug.MemoryInfo()
            if (Debug.getMemoryInfo(memInfo)) pssMb = memInfo.totalPss.toLong() / 1024L
        } catch (_: Exception) {
        }

        val allSongsFile = File(context.filesDir, "all_songs_cache.json")
        val imageCacheDir = File(context.cacheDir, "lechen_image_cache")
        val lyricsPrefsFile = File(context.dataDir, "shared_prefs/$LYRICS_PREFS.xml")

        return Snapshot(
            maxHeapMb = maxBytes / MB,
            usedHeapMb = usedBytes / MB,
            heapPercent = if (maxBytes > 0) ((usedBytes * 100) / maxBytes).toInt() else 0,
            memoryClassMb = memoryClass,
            largeMemoryClassMb = largeMemoryClass,
            nativeHeapMb = nativeMb,
            pssMb = pssMb,
            systemTotalMb = systemTotalMb,
            systemAvailMb = systemAvailMb,
            lowMemory = lowMemory,
            songCount = songCount,
            albumCount = albumCount,
            cachedSongCount = cachedSongCount,
            allSongsCacheBytes = if (allSongsFile.exists()) allSongsFile.length() else 0L,
            lyricsCount = lyricsCount,
            lyricsBytes = if (lyricsPrefsFile.exists()) lyricsPrefsFile.length() else 0L,
            imageCacheBytes = dirSize(imageCacheDir),
            filesBytes = dirSize(context.filesDir),
            libraryLoadMs = libraryLoadMs,
            libraryLoadCount = libraryLoadCount,
            cacheParseMs = cacheParseMs,
            cacheParseCount = cacheParseCount,
            lastLoadAt = lastLoadAt,
            uptimeMinutes = SystemClock.elapsedRealtime() / 60000L
        )
    }

    /** 生成人类可读的文本报告（同一条也会写进导出的 .txt） */
    fun format(context: Context, s: Snapshot): String {
        val sb = StringBuilder()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("悦音 APP 诊断报告")
        sb.appendLine("时间：$stamp")
        sb.appendLine("版本：${versionName(context)}")
        sb.appendLine("机型：${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("系统：Android ${android.os.Build.VERSION.RELEASE}（API ${android.os.Build.VERSION.SDK_INT}）")
        sb.appendLine("启动：已运行 ${s.uptimeMinutes} 分钟")
        sb.appendLine()

        sb.appendLine("【内存】")
        sb.appendLine("堆上限：${s.maxHeapMb} MB（堆已用 ${s.usedHeapMb} MB，占 ${s.heapPercent}%）")
        sb.appendLine("heap 配额：memoryClass ${s.memoryClassMb} MB / largeMemoryClass ${s.largeMemoryClassMb} MB")
        sb.appendLine("largeHeap 生效：${if (s.maxHeapMb > s.memoryClassMb) "是" else "否（仍受 memoryClass 限制）"}")
        sb.appendLine("native 堆：${s.nativeHeapMb} MB")
        sb.appendLine("进程 PSS：${s.pssMb} MB")
        sb.appendLine("系统内存：可用 ${s.systemAvailMb} MB / 共 ${s.systemTotalMb} MB${if (s.lowMemory) "（lowMemory 已置位）" else ""}")
        sb.appendLine()

        sb.appendLine("【缓存规模】")
        sb.appendLine("曲库条数：${s.songCount}")
        sb.appendLine("专辑条数：${s.albumCount}")
        sb.appendLine("缓存歌曲条数：${s.cachedSongCount}")
        sb.appendLine("曲库缓存文件：${mb(s.allSongsCacheBytes)}（all_songs_cache.json）")
        sb.appendLine("歌词缓存：${s.lyricsCount} 条 / ${mb(s.lyricsBytes)}（SharedPreferences lyrics_cache）")
        sb.appendLine("图片缓存目录：${mb(s.imageCacheBytes)}（lechen_image_cache）")
        sb.appendLine("filesDir 合计：${mb(s.filesBytes)}")
        sb.appendLine()

        sb.appendLine("【加载耗时】")
        sb.appendLine("解析本地曲库缓存：${ms(s.cacheParseMs, s.cacheParseCount)}")
        sb.appendLine("服务端拉全曲库：${ms(s.libraryLoadMs, s.libraryLoadCount)}")
        if (s.lastLoadAt > 0L) {
            val at = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(s.lastLoadAt))
            sb.appendLine("最近一次全库同步：$at")
        } else {
            sb.appendLine("最近一次全库同步：本次进程内还没有拉取")
        }
        return sb.toString()
    }

    /** 把报告写成 txt 落到 diagnostics 目录，返回文件（供分享） */
    fun saveReport(context: Context, text: String): File {
        val dir = diagnosticsDir(context)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "diagnostic-$stamp.txt")
        file.writeText(text)
        return file
    }

    /**
     * 转储当前进程堆。必须在后台线程调用：dump 期间应用线程会被短暂挂起，
     * 大堆（几百 MB）会持续数秒，放主线程必定 ANR。
     */
    fun dumpHeap(context: Context): File {
        val dir = diagnosticsDir(context)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "heap-$stamp.hprof")
        Debug.dumpHprofData(file.absolutePath)
        return file
    }

    /** 用系统分享面板把诊断文件发出去（微信/邮件/文件管理器都可以） */
    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "hprof") "application/octet-stream" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "导出诊断文件").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /** 诊断文件目录：getExternalFilesDir 下，FileProvider 的 external-files-path 根已覆盖 */
    fun diagnosticsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var total = 0L
        val children = dir.listFiles() ?: return total
        for (child in children) {
            total += if (child.isDirectory) dirSize(child) else child.length()
        }
        return total
    }

    private fun mb(bytes: Long): String = String.format(Locale.US, "%.2f MB", bytes / 1048576.0)

    private fun ms(durationMs: Long, count: Int): String =
        if (durationMs < 0L) "本次进程内还没有执行" else "${durationMs} ms（$count 条）"

    private fun versionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}