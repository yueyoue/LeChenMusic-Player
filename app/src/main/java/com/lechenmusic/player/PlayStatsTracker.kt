package com.lechenmusic.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.lechenmusic.data.api.SubsonicApi
import com.lechenmusic.data.model.DeviceRequest
import com.lechenmusic.data.model.PlayLogRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 播放时长统计追踪器
 *
 * 使用方式：
 * 1. 在 Player.Listener 的 onIsPlayingChanged 中调用 onPlayStateChanged
 * 2. 在 onMediaItemTransition 中调用 onMediaItemChanged
 * 3. APP 启动时调用 reportDeviceVersion
 */
class PlayStatsTracker(
    private val context: Context,
    private val apiProvider: () -> SubsonicApi?,
    private val authHeaderProvider: () -> String?
) {
    companion object {
        private const val TAG = "PlayStatsTracker"
        private const val PREFS_NAME = "play_stats"
        private const val KEY_TOTAL_SECONDS = "total_play_seconds"
        private const val REPORT_THRESHOLD_SECONDS = 300 // 5分钟上报一次
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    // 当前播放状态
    private var playStartTime: Long = 0L
    private var accumulatedSeconds: Int = 0
    private var currentItemType: String = "music"
    private var currentItemId: String = ""
    private var currentItemTitle: String = ""
    private var currentItemArtist: String = ""

    /**
     * 播放/暂停状态变化时调用
     */
    fun onPlayStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            playStartTime = System.currentTimeMillis()
            Log.d(TAG, "Play started, startTime=$playStartTime")
        } else if (playStartTime > 0) {
            val durationMs = System.currentTimeMillis() - playStartTime
            val durationSec = (durationMs / 1000).toInt()
            playStartTime = 0

            if (durationSec > 0) {
                accumulatedSeconds += durationSec
                addTotalSeconds(durationSec)
                Log.d(TAG, "Play paused, +${durationSec}s, accumulated=${accumulatedSeconds}s, total=${getTotalPlaySeconds()}s")

                // 超过阈值则上报
                if (accumulatedSeconds >= REPORT_THRESHOLD_SECONDS) {
                    flushToServer()
                }
            }
        }
    }

    /**
     * 切换歌曲时调用（上报之前累积的时长，然后更新当前曲目信息）
     */
    fun onMediaItemChanged(itemType: String, itemId: String, itemTitle: String, itemArtist: String) {
        // 先结算上一首
        if (playStartTime > 0) {
            val durationMs = System.currentTimeMillis() - playStartTime
            val durationSec = (durationMs / 1000).toInt()
            playStartTime = System.currentTimeMillis() // 重置开始时间
            if (durationSec > 0) {
                accumulatedSeconds += durationSec
                addTotalSeconds(durationSec)
            }
        }

        // 更新当前曲目
        currentItemType = itemType
        currentItemId = itemId
        currentItemTitle = itemTitle
        currentItemArtist = itemArtist

        // 如果累积够了就上报
        if (accumulatedSeconds >= REPORT_THRESHOLD_SECONDS) {
            flushToServer()
        }
    }

    /**
     * 上报累积的播放时长到服务端
     */
    private fun flushToServer() {
        if (accumulatedSeconds <= 0 || currentItemId.isEmpty()) return

        val duration = accumulatedSeconds
        accumulatedSeconds = 0

        val api = apiProvider() ?: return
        val authHeader = authHeaderProvider() ?: return

        scope.launch {
            try {
                val request = PlayLogRequest(
                    itemType = currentItemType,
                    itemId = currentItemId,
                    itemTitle = currentItemTitle,
                    itemArtist = currentItemArtist,
                    duration = duration
                )
                val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
                val response = api.reportPlayLog(body, authHeader)
                if (response.isSuccessful) {
                    Log.d(TAG, "Play log reported: ${duration}s for $currentItemTitle")
                } else {
                    Log.w(TAG, "Play log report failed: ${response.code()}")
                    // 失败时把时长加回去
                    accumulatedSeconds += duration
                }
            } catch (e: Exception) {
                Log.e(TAG, "Play log report error", e)
                accumulatedSeconds += duration
            }
        }
    }

    /**
     * 上报设备版本信息（APP启动时调用一次）
     */
    fun reportDeviceVersion() {
        val api = apiProvider() ?: return
        val authHeader = authHeaderProvider() ?: return

        scope.launch {
            try {
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
                val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
                val appVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
                } catch (e: Exception) { "unknown" }

                val request = DeviceRequest(
                    deviceId = deviceId,
                    deviceInfo = deviceInfo,
                    appVersion = appVersion
                )
                val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
                val response = api.reportDevice(body, authHeader)
                if (response.isSuccessful) {
                    Log.d(TAG, "Device info reported: v$appVersion on $deviceInfo")
                } else {
                    Log.w(TAG, "Device report failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Device report error", e)
            }
        }
    }

    // ─── 本地累计时长 ────────────────────────────────────

    private fun addTotalSeconds(seconds: Int) {
        val current = prefs.getLong(KEY_TOTAL_SECONDS, 0)
        prefs.edit().putLong(KEY_TOTAL_SECONDS, current + seconds).apply()
    }

    /** 获取累计播放总秒数 */
    fun getTotalPlaySeconds(): Long {
        return prefs.getLong(KEY_TOTAL_SECONDS, 0)
    }

    /** 格式化累计时长显示 */
    fun getFormattedTotalDuration(): String {
        val totalSec = getTotalPlaySeconds()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "${totalSec}秒"
        }
    }

    /** APP退出时调用，结算当前播放 */
    fun release() {
        if (playStartTime > 0) {
            val durationMs = System.currentTimeMillis() - playStartTime
            val durationSec = (durationMs / 1000).toInt()
            playStartTime = 0
            if (durationSec > 0) {
                accumulatedSeconds += durationSec
                addTotalSeconds(durationSec)
            }
        }
        // 最后尝试上报
        if (accumulatedSeconds > 0) {
            flushToServer()
        }
    }
}
