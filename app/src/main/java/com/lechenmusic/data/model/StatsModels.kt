package com.lechenmusic.data.model

import com.google.gson.annotations.SerializedName

/**
 * 播放时长上报请求体
 * POST /api/stats/play-log
 */
data class PlayLogRequest(
    @SerializedName("itemType")
    val itemType: String = "music",

    @SerializedName("itemId")
    val itemId: String,

    @SerializedName("itemTitle")
    val itemTitle: String,

    @SerializedName("itemArtist")
    val itemArtist: String,

    @SerializedName("duration")
    val duration: Int // 累计播放秒数
)

/**
 * 设备版本上报请求体
 * POST /api/stats/device
 */
data class DeviceRequest(
    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("deviceInfo")
    val deviceInfo: String,

    @SerializedName("appVersion")
    val appVersion: String
)

/**
 * 通用 API 响应体
 */
data class StatsApiResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("data")
    val data: Any? = null
)

/**
 * 服务端返回的用户播放统计
 * GET /api/stats/me
 */
data class UserPlayStats(
    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("userName")
    val userName: String = "",

    @SerializedName("totalDuration")
    val totalDuration: Int = 0,  // 累计播放秒数

    @SerializedName("totalCount")
    val totalCount: Int = 0,

    @SerializedName("musicDuration")
    val musicDuration: Int = 0,

    @SerializedName("musicCount")
    val musicCount: Int = 0,

    @SerializedName("audiobookDuration")
    val audiobookDuration: Int = 0,

    @SerializedName("audiobookCount")
    val audiobookCount: Int = 0
)

/**
 * 用户听歌等级定义
 */
data class UserLevel(
    val level: Int,
    val title: String,
    val minMinutes: Int
)

/**
 * 等级计算工具
 */
object UserLevelSystem {
    val levels = listOf(
        UserLevel(1, "路人听众", 120),
        UserLevel(2, "偶尔听歌", 600),
        UserLevel(3, "普通乐迷", 1800),
        UserLevel(4, "日常点歌官", 4800),
        UserLevel(5, "歌单达人", 7200),
        UserLevel(6, "听歌老炮", 12000),
        UserLevel(7, "金曲挖宝人", 21600),
        UserLevel(8, "好声音导师", 36000),
        UserLevel(9, "资深乐评人", 60000),
        UserLevel(10, "退休歌唱家", 96000)
    )

    /** 根据累计分钟数计算当前等级 */
    fun getCurrentLevel(totalMinutes: Long): UserLevel {
        var current = levels[0]
        for (level in levels) {
            if (totalMinutes >= level.minMinutes) {
                current = level
            } else {
                break
            }
        }
        return current
    }

    /** 获取下一个等级，如果已满级返回 null */
    fun getNextLevel(totalMinutes: Long): UserLevel? {
        val current = getCurrentLevel(totalMinutes)
        val currentIdx = levels.indexOf(current)
        return if (currentIdx < levels.size - 1) levels[currentIdx + 1] else null
    }

    /** 距下一级还差多少分钟 */
    fun getMinutesToNextLevel(totalMinutes: Long): Long {
        val next = getNextLevel(totalMinutes) ?: return 0
        return (next.minMinutes - totalMinutes).coerceAtLeast(0)
    }

    /** 当前等级内的进度 (0.0 ~ 1.0) */
    fun getLevelProgress(totalMinutes: Long): Float {
        val current = getCurrentLevel(totalMinutes)
        val next = getNextLevel(totalMinutes)
        if (next == null) return 1f // 已满级
        val range = (next.minMinutes - current.minMinutes).toFloat()
        val progress = (totalMinutes - current.minMinutes).toFloat()
        return (progress / range).coerceIn(0f, 1f)
    }
}
