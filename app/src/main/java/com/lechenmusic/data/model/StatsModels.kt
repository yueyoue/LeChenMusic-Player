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
