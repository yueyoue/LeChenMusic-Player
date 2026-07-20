package com.lechenmusic.data.model

/**
 * 影视搜索/列表结果
 */
data class VideoInfo(
    val id: String = "",
    val source: String = "",        // 来源标识
    val title: String = "",
    val sourceName: String = "",    // 来源名称
    val year: String = "",
    val cover: String = "",
    val index: Int = 0,             // 当前集数
    val totalEpisodes: Int = 0,     // 总集数
    val playTime: Int = 0,          // 已播放秒数
    val totalTime: Int = 0,         // 总时长秒数
    val searchTitle: String = "",
    val rate: String? = null,       // 评分
    val type: String = ""           // 类型：movie/tv/show/anime
) {
    val progressPercent: Float
        get() = if (totalTime > 0) (playTime.toFloat() / totalTime).coerceIn(0f, 1f) else 0f

    val formattedPlayTime: String
        get() {
            val h = playTime / 3600
            val m = (playTime % 3600) / 60
            val s = playTime % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }

    val formattedTotalTime: String
        get() {
            val h = totalTime / 3600
            val m = (totalTime % 3600) / 60
            val s = totalTime % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
}

/**
 * 影视详情（含播放源和集数）
 */
data class VideoDetail(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val cover: String = "",
    val desc: String = "",
    val type: String = "",
    val rate: String? = null,
    val sources: List<VideoSource> = emptyList()
)

data class VideoSource(
    val sourceName: String = "",
    val source: String = "",
    val episodes: List<VideoEpisode> = emptyList()
)

data class VideoEpisode(
    val index: Int = 0,
    val title: String = "",
    val url: String = ""
)

/**
 * 播放记录
 */
data class VideoPlayRecord(
    val videoId: String = "",
    val source: String = "",
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    val episodeIndex: Int = 0,
    val totalEpisodes: Int = 0,
    val playTime: Int = 0,
    val totalTime: Int = 0,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

/**
 * 直播频道
 */
data class LiveChannel(
    val name: String = "",
    val url: String = "",
    val logo: String = "",
    val group: String = "",
    val epg: String = ""
)
