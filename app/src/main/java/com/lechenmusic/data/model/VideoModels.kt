package com.lechenmusic.data.model

import com.google.gson.annotations.SerializedName

// ==================== 登录 ====================

data class LoginResponse(
    val ok: Boolean = false
)

// ==================== 搜索 ====================

data class SearchResponse(
    val results: List<VideoInfo> = emptyList()
)

// ==================== 影视信息 ====================

data class VideoInfo(
    val id: String = "",
    val source: String = "",        // 来源标识（搜索结果可能为空）
    val title: String = "",
    val sourceName: String = "",    // 来源名称
    val year: String = "",
    val cover: String = "",
    @SerializedName("poster") val poster: String = "",  // LunaTV 用 poster
    val index: Int = 0,             // 当前集数
    val totalEpisodes: Int = 0,     // 总集数
    @SerializedName("total_episodes") val totalEpisodesAlt: Int = 0,
    val playTime: Int = 0,          // 已播放秒数
    @SerializedName("play_time") val playTimeAlt: Int = 0,
    val totalTime: Int = 0,         // 总时长秒数
    @SerializedName("total_time") val totalTimeAlt: Int = 0,
    val searchTitle: String = "",
    val rate: String? = null,       // 评分
    val type: String = "",          // 类型：movie/tv/show/anime
    val desc: String = "",
    val episodes: List<String> = emptyList(),                // LunaTV 搜索结果包含 episodes
    @SerializedName("episodes_titles") val episodesTitles: List<String> = emptyList(),
    @SerializedName("quality_tag") val qualityTag: String = "",
    val remarks: String = "",
    val director: String = "",
    val actor: String = "",
    val area: String = "",
    val lang: String = "",
    val category: String = "",
    val updateTime: String = "",
    val status: String = "",
    @SerializedName("source_name") val sourceNameAlt: String = ""
) {
    // 统一访问器，兼容 LunaTV 和豆瓣两种格式
    val displayCover: String get() = cover.ifBlank { poster }
    val displaySourceName: String get() = sourceName.ifBlank { sourceNameAlt }
    val displayTotalEpisodes: Int get() = if (totalEpisodes > 0) totalEpisodes else totalEpisodesAlt
    val displayPlayTime: Int get() = if (playTime > 0) playTime else playTimeAlt
    val displayTotalTime: Int get() = if (totalTime > 0) totalTime else totalTimeAlt

    val progressPercent: Float
        get() = if (displayTotalTime > 0) (displayPlayTime.toFloat() / displayTotalTime).coerceIn(0f, 1f) else 0f

    val formattedPlayTime: String
        get() {
            val t = displayPlayTime
            val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }

    val formattedTotalTime: String
        get() {
            val t = displayTotalTime
            val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
}

// ==================== 影视详情 ====================

data class VideoDetail(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val cover: String = "",
    @SerializedName("poster") val poster: String = "",
    val desc: String = "",
    val type: String = "",
    @SerializedName("type_name") val typeName: String = "",
    val rate: String? = null,
    val director: String = "",
    val actor: String = "",
    val area: String = "",
    val lang: String = "",
    val category: String = "",
    val updateTime: String = "",
    val source: String = "",
    val sourceName: String = "",
    @SerializedName("source_name") val sourceNameAlt: String = "",
    @SerializedName("douban_id") val doubanId: String = "",
    val episodes: List<String> = emptyList(),           // LunaTV: 直接是 URL 列表
    @SerializedName("episodes_titles") val episodesTitles: List<String> = emptyList(),
    @SerializedName("class") val className: String = "",
    val sources: List<VideoSource> = emptyList(),       // 兼容旧格式
    val related: List<VideoInfo> = emptyList()
) {
    val displayCover: String get() = cover.ifBlank { poster }
    val displaySourceName: String get() = sourceName.ifBlank { sourceNameAlt }
    val displayType: String get() = type.ifBlank { typeName }

    /** 将 LunaTV 的 episodes URL 列表转换为 VideoSource/VideoEpisode 结构 */
    fun toSources(): List<VideoSource> {
        if (sources.isNotEmpty()) return sources
        if (episodes.isEmpty()) return emptyList()
        val eps = episodes.mapIndexed { index, url ->
            val title = episodesTitles.getOrNull(index) ?: "第${index + 1}集"
            VideoEpisode(index = index, title = title, url = url)
        }
        return listOf(VideoSource(
            sourceName = displaySourceName.ifBlank { "默认" },
            source = source,
            episodes = eps
        ))
    }
}

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

// ==================== 收藏 ====================

data class FavoriteRequest(
    val source: String,
    val id: String,
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    val type: String = ""
)

// ==================== 播放记录 ====================

/** LunaTV playrecords API 返回的单条记录（dict 的 value 部分） */
data class VideoPlayRecord(
    @SerializedName("videoId") val videoIdRaw: String = "",
    val source: String = "",
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    val index: Int = 0,
    @SerializedName("episode_index") val episodeIndexAlt: Int = 0,
    val totalEpisodes: Int = 0,
    @SerializedName("total_episodes") val totalEpisodesAlt: Int = 0,
    val playTime: Int = 0,
    @SerializedName("play_time") val playTimeAlt: Int = 0,
    val totalTime: Int = 0,
    @SerializedName("total_time") val totalTimeAlt: Int = 0,
    @SerializedName("save_time") val saveTime: Long = System.currentTimeMillis(),
    val type: String = "",
    @SerializedName("source_name") val sourceName: String = "",
    @SerializedName("search_title") val searchTitle: String = ""
) {
    val displayTotalEpisodes: Int get() = if (totalEpisodes > 0) totalEpisodes else totalEpisodesAlt
    val displayPlayTime: Int get() = if (playTime > 0) playTime else playTimeAlt
    val displayTotalTime: Int get() = if (totalTime > 0) totalTime else totalTimeAlt
    val displayEpisodeIndex: Int get() = if (index > 0) index else episodeIndexAlt

    val progressPercent: Float
        get() = if (displayTotalTime > 0) (displayPlayTime.toFloat() / displayTotalTime).coerceIn(0f, 1f) else 0f
}

data class PlayRecordRequest(
    val source: String,
    val id: String,
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    @SerializedName("source_name") val sourceName: String = "",
    val index: Int = 1,
    val total_episodes: Int = 0,
    val play_time: Int = 0,
    val total_time: Int = 0,
    val type: String = ""
)

/** 保存播放记录的请求包装（参考 Selene-Source: key + record） */
data class PlayRecordSaveRequest(
    val key: String,
    val record: PlayRecordRequest
)

// ==================== 直播 ====================

data class LiveSourcesResponse(
    val success: Boolean = false,
    val code: Int = 0,
    val msg: String = "",
    val data: List<LiveSource> = emptyList()
)

data class LiveSource(
    val key: String = "",
    val name: String = "",
    val url: String = "",
    val ua: String = "",
    val epg: String = "",
    val from: String = "",
    val disabled: Boolean = false,
    val source: String = ""  // 兼容旧格式
)

data class LiveChannelsResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: List<LiveChannelGroup> = emptyList()
)

data class LiveChannelGroup(
    val name: String = "",
    val channels: List<LiveChannel> = emptyList()
)

data class LiveChannel(
    val name: String = "",
    val url: String = "",
    val logo: String = "",
    val group: String = "",
    val epg: String = "",
    val catchup: String = "",
    val catchupSource: String = ""
)

// ==================== 首页推荐 ====================

data class HomeRecommendData(
    val continueWatch: List<VideoPlayRecord> = emptyList(),
    val hotMovies: List<VideoInfo> = emptyList(),
    val hotTvShows: List<VideoInfo> = emptyList(),
    val hotVariety: List<VideoInfo> = emptyList(),
    val hotAnime: List<VideoInfo> = emptyList(),
    val hotShortDrama: List<VideoInfo> = emptyList()
)

// ==================== 豆瓣 ====================

data class DoubanHotResponse(
    val category: String = "",
    val type: String = "",
    val total: Int = 0,
    val items: List<DoubanMovie> = emptyList()
)

data class DoubanMovie(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val cover: String = "",
    val cover_url: String = "",
    val pic: DoubanPic? = null,
    val is_new: Boolean = false,
    val release_date: String = "",
    val uri: String = "",
    val rate: String = "",
    val rating: DoubanRating? = null,
    val card_subtitle: String = "",
    val episodes_info: String = "",
    val rect_cover: String = "",
    val cover_y: Int = 0,
    val is_beetle_mailer: Boolean = false,
    val null_rating_reason: String = ""
) {
    val displayCover: String get() {
        val raw = pic?.normal?.ifBlank { pic.large }
            ?: cover_url.ifBlank { cover }
        // 豆瓣图片CDN代理：统一用 img3.doubanio.com（带 Referer 可访问）
        return raw.replace(Regex("img\\d+\\.doubanio\\.com"), "img3.doubanio.com")
    }
    val displayRate: String get() {
        val r = rate.ifBlank { rating?.value?.toString() ?: "" }
        return if (r.contains(".")) r else if (r.isNotBlank()) "$r.0" else ""
    }
    val displayYear: String get() {
        if (year.isNotBlank()) return year
        return card_subtitle.split("/").firstOrNull()?.trim() ?: ""
    }
}

data class DoubanPic(
    val large: String = "",
    val normal: String = ""
)

data class DoubanRating(
    val count: Int = 0,
    val max: Int = 0,
    val start_count: Int = 0,
    val value: Double = 0.0
)

fun DoubanMovie.toVideoInfo(type: String): VideoInfo {
    return VideoInfo(
        id = id,
        title = title,
        year = displayYear,
        cover = displayCover,
        rate = displayRate,
        type = type
    )
}

// ==================== 搜索资源 ====================

data class SearchResourceResponse(
    val key: String = "",
    val name: String = "",
    val disabled: Boolean = false
)
