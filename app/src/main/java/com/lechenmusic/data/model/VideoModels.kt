package com.lechenmusic.data.model

import com.google.gson.annotations.SerializedName

// ==================== API 通用响应 ====================

data class LunaApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)

// ==================== 登录 ====================

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: LoginData? = null
)

data class LoginData(
    val username: String = "",
    val role: String = ""
)

// ==================== 搜索 ====================

data class SearchResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: List<VideoInfo> = emptyList()
)

// ==================== 影视信息 ====================

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
    val type: String = "",          // 类型：movie/tv/show/anime
    val desc: String = "",
    val director: String = "",
    val actor: String = "",
    val area: String = "",
    val lang: String = "",
    val category: String = "",
    val updateTime: String = "",
    val status: String = ""
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

// ==================== 影视详情 ====================

data class VideoDetailResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: VideoDetail? = null
)

data class VideoDetail(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val cover: String = "",
    val desc: String = "",
    val type: String = "",
    val rate: String? = null,
    val director: String = "",
    val actor: String = "",
    val area: String = "",
    val lang: String = "",
    val category: String = "",
    val updateTime: String = "",
    val source: String = "",
    val sourceName: String = "",
    val sources: List<VideoSource> = emptyList(),
    val related: List<VideoInfo> = emptyList()
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

// ==================== 收藏 ====================

data class FavoritesResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: List<VideoInfo> = emptyList()
)

data class FavoriteRequest(
    val source: String,
    val id: String,
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    val type: String = ""
)

// ==================== 播放记录 ====================

data class PlayRecordsResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: List<VideoPlayRecord> = emptyList()
)

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
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val type: String = ""
) {
    val progressPercent: Float
        get() = if (totalTime > 0) (playTime.toFloat() / totalTime).coerceIn(0f, 1f) else 0f
}

data class PlayRecordRequest(
    val source: String,
    val id: String,
    val title: String = "",
    val cover: String = "",
    val year: String = "",
    val episodeIndex: Int = 0,
    val totalEpisodes: Int = 0,
    val playTime: Int = 0,
    val totalTime: Int = 0,
    val type: String = ""
)

// ==================== 直播 ====================

data class LiveSourcesResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: List<LiveSource> = emptyList()
)

data class LiveSource(
    val name: String = "",
    val source: String = ""
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

data class HomeRecommendResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: HomeRecommendData? = null
)

data class HomeRecommendData(
    val continueWatch: List<VideoPlayRecord> = emptyList(),
    val comingSoon: List<VideoInfo> = emptyList(),
    val hotMovies: List<VideoInfo> = emptyList(),
    val hotTvShows: List<VideoInfo> = emptyList(),
    val hotVariety: List<VideoInfo> = emptyList(),
    val hotAnime: List<VideoInfo> = emptyList(),
    val hotShortDrama: List<VideoInfo> = emptyList()
)

// ==================== 分类列表 ====================

data class CategoryResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: CategoryData? = null
)

data class CategoryData(
    val list: List<VideoInfo> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)
