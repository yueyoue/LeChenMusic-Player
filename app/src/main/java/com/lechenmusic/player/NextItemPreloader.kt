package com.lechenmusic.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 下一曲 / 下一章 预缓存（方案 A）。
 *
 * 把「下一个要播的网盘资源」的开头若干字节提前写进 SimpleCache，
 * 切歌/切章时直接命中本地缓存，消除网盘链路（DNS+TLS+直链申请+首字节）的等待。
 *
 * 职责边界（见开发步骤文档 §3.2）：
 *  - 「下一目标解析」在调用方（MusicPlayerManager 管音乐、MainViewModel 管有声书），
 *    本类不反向依赖 ViewModel；
 *  - 本类只做资源侧闸门（开关 / 网络策略 / 已缓存命中 / 去重）与执行（CacheWriter）。
 *
 * 执行模型：
 *  - 单工作线程串行队列，低线程优先级，不与当前播放抢资源；
 *  - 新任务顶掉旧任务（cancel 旧 CacheWriter，CacheWriter 支持断点续跑）；
 *  - 播放器 BUFFERING 时暂停预载（进度回调里协作式阻塞），READY 后恢复；
 *  - 失败只打日志：播放时 ExoPlayer 自己还会请求，不阻塞任何流程。
 */
class NextItemPreloader(
    private val context: Context,
    private val cacheProvider: () -> Cache?,
    private val dataSourceFactoryProvider: () -> CacheDataSource.Factory?
) {

    enum class Kind {
        MUSIC, AUDIOBOOK;

        /** 单次预载字节上限（WiFi / 有线网络） */
        val preloadBytes: Long
            get() = when (this) {
                MUSIC -> NextItemPreloader.MUSIC_PRELOAD_BYTES
                AUDIOBOOK -> NextItemPreloader.AUDIOBOOK_PRELOAD_BYTES
            }
    }

    private class Task(
        val uri: Uri,
        val cacheKey: String,
        val bytes: Long,
        val kind: Kind
    ) {
        val canceled = AtomicBoolean(false)
    }

    companion object {
        private const val TAG = "LeChenPreload"
        const val MUSIC_PRELOAD_BYTES = 1_572_864L // 1.5MB ≈ 音乐前 60s
        const val AUDIOBOOK_PRELOAD_BYTES = 3_145_728L // 3MB ≈ 有声书前 2 分钟
        const val CELLULAR_PRELOAD_BYTES = 524_288L // 移动网络 512KB 上限
        const val CACHE_HIT_BYTES = 262_144L // 256KB：已缓存这么多就不再预载
        private const val MAX_ATTEMPTS_PER_TARGET = 3 // STATE_READY 触发 + 切项触发 + 50% 兜底
    }

    /** 设置项：预加载总开关（默认开）。关闭时不入队，打开即时生效。 */
    @Volatile
    var enabled: Boolean = true

    /** 设置项：是否允许移动网络预载（"仅 WiFi"=false，"WiFi + 移动网络"=true）。 */
    @Volatile
    var allowCellular: Boolean = false

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "LeChenPreload").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    @Volatile
    private var pendingTask: Task? = null

    @Volatile
    private var runningTask: Task? = null

    @Volatile
    private var runningWriter: CacheWriter? = null

    // 同一目标的去重（多个触发点会重复请求同一目标）
    private var lastAttemptKey: String? = null
    private var attemptCount: Int = 0

    // 播放器 BUFFERING 时暂停预载
    private val pauseLock = Object()

    @Volatile
    private var playbackBuffering: Boolean = false

    /**
     * 入队预载一个目标。调用方已完成「下一目标解析」与语义闸门
     * （电台/B 本地资源/已完整播放 等），这里做资源侧闸门。
     */
    fun enqueue(url: String, kind: Kind) {
        if (!enabled) return
        val uri = Uri.parse(url)
        // 只预载能规范化缓存 key 的目标（音乐 / 有声书章节）
        val cacheKey = StableCacheKeyFactory.fromUri(uri) ?: return

        // 去重：同一目标最多尝试 MAX_ATTEMPTS_PER_TARGET 次（触发点会重复来）
        if (cacheKey == lastAttemptKey) {
            if (attemptCount >= MAX_ATTEMPTS_PER_TARGET) return
            attemptCount++
        } else {
            lastAttemptKey = cacheKey
            attemptCount = 1
        }

        // 网络策略
        val tier = networkTier()
        when (tier) {
            NetworkTier.NONE -> return
            NetworkTier.CELLULAR -> if (!allowCellular) return
            NetworkTier.UNMETERED -> {}
        }
        val bytes = if (tier == NetworkTier.CELLULAR) {
            minOf(kind.preloadBytes, CELLULAR_PRELOAD_BYTES)
        } else {
            kind.preloadBytes
        }

        // 新任务顶掉旧任务；同目标正在跑就不打断（CacheWriter 本身支持断点续跑）
        val running = runningTask
        if (running != null && running.cacheKey == cacheKey && runningWriter != null) return
        val task = Task(uri, cacheKey, bytes, kind)
        pendingTask = task
        running?.canceled?.set(true)
        runningWriter?.cancel()
        executor.execute { drain() }
    }

    /** 播放状态联动：BUFFERING 时暂停预载，恢复后继续。 */
    fun onPlaybackBufferingChanged(buffering: Boolean) {
        playbackBuffering = buffering
        if (!buffering) {
            synchronized(pauseLock) { pauseLock.notifyAll() }
        }
    }

    private fun drain() {
        val task = pendingTask ?: return
        pendingTask = null
        runTask(task)
    }

    private fun runTask(task: Task) {
        if (task.canceled.get()) return
        val factory = dataSourceFactoryProvider() ?: return
        val cache = cacheProvider() ?: return
        // 排队期间可能已被播放器写进缓存
        try {
            if (cache.getCachedBytes(task.cacheKey, 0, Long.MAX_VALUE) >= CACHE_HIT_BYTES) return
        } catch (_: Exception) {
        }

        val writer = CacheWriter(
            factory.createDataSource(),
            DataSpec(task.uri, 0, task.bytes),
            /* temporaryBuffer= */ null,
            object : CacheWriter.ProgressListener {
                override fun onProgress(requestLength: Long, bytesCached: Long, newBytesCached: Long) {
                    // 播放器 BUFFERING 时暂停预载（协作式阻塞），保证当前播放优先；
                    // 被新任务顶掉时退出等待，让队列继续。
                    awaitPlaybackReady(task)
                }
            }
        )
        runningTask = task
        runningWriter = writer
        val startMs = System.currentTimeMillis()
        try {
            writer.cache()
            val tookMs = System.currentTimeMillis() - startMs
            Log.d(TAG, "预载完成 ${task.kind} ${task.cacheKey} 耗时 ${tookMs}ms")
        } catch (e: Exception) {
            // 失败/取消静默放弃：播放时走完整链路（与现状一致，不会更差）
            Log.d(TAG, "预载放弃 ${task.kind} ${task.cacheKey}: ${e.javaClass.simpleName}")
        } finally {
            if (runningWriter === writer) {
                runningWriter = null
                runningTask = null
            }
        }
    }

    /** 等待播放器离开 BUFFERING（或任务被顶掉 / 线程被中断）。 */
    private fun awaitPlaybackReady(task: Task) {
        synchronized(pauseLock) {
            while (playbackBuffering && !task.canceled.get() && !Thread.currentThread().isInterrupted) {
                try {
                    pauseLock.wait(1000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    private enum class NetworkTier { UNMETERED, CELLULAR, NONE }

    /** WiFi / 以太网视为不限量；其余活动网络按移动网络处理；无活动网络返回 NONE。 */
    private fun networkTier(): NetworkTier {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            var hasCellular = false
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                ) {
                    return NetworkTier.UNMETERED
                }
                hasCellular = true
            }
            if (hasCellular) NetworkTier.CELLULAR else NetworkTier.NONE
        } catch (_: Exception) {
            NetworkTier.NONE
        }
    }
}
