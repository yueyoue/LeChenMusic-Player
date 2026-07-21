package com.lechenmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lechenmusic.data.api.VideoApiClient
import com.lechenmusic.data.api.DoubanApiClient
import com.lechenmusic.data.model.*
import com.lechenmusic.ErrorReporter
import com.lechenmusic.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)

    /** 写入本地文件日志（调试用，闪退后可查看） */
    fun logDebug(tag: String, msg: String) {
        try {
            val ctx = getApplication<Application>()
            val logFile = java.io.File(ctx.getExternalFilesDir(null), "video_debug.log")
            val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            logFile.appendText("[$ts] [$tag] $msg\n")
            android.util.Log.d("VideoDebug", "[$tag] $msg")
        } catch (_: Exception) {}
    }

    /** 影视模块错误上报到 WEB 管理端 + 本地 Toast */
    fun reportVideoError(screen: String, message: String, throwable: Throwable? = null) {
        val fullMsg = "[影视] $message${throwable?.let { "\n${it.javaClass.simpleName}: ${it.message}" } ?: ""}"
        // 本地 Toast 显示（调试用）
        _toastMessage.value = fullMsg.take(200)
        // 上报到 WEB 管理端
        ErrorReporter.reportError(
            level = "error",
            message = "[影视] $message",
            throwable = throwable,
            screen = "video_$screen"
        )
    }

    // ===== 登录状态 =====
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // ===== 服务器配置 =====
    val videoServerUrl: StateFlow<String> = settings.videoServerUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val videoUsername: StateFlow<String> = settings.videoUsername
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val videoPassword: StateFlow<String> = settings.videoPassword
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // ===== 首页推荐 =====
    private val _homeData = MutableStateFlow<HomeRecommendData?>(null)
    val homeData: StateFlow<HomeRecommendData?> = _homeData.asStateFlow()

    private val _homeLoading = MutableStateFlow(false)
    val homeLoading: StateFlow<Boolean> = _homeLoading.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError: StateFlow<String?> = _homeError.asStateFlow()

    // ===== 搜索 =====
    private val _searchResults = MutableStateFlow<List<VideoInfo>>(emptyList())
    val searchResults: StateFlow<List<VideoInfo>> = _searchResults.asStateFlow()

    // 所有搜索到的源（用于详情页片源切换）
    private val _allSearchSources = MutableStateFlow<List<VideoInfo>>(emptyList())
    val allSearchSources: StateFlow<List<VideoInfo>> = _allSearchSources.asStateFlow()

    // 源测速结果: source key -> ping ms (-1 = 超时, 0 = 未测试)
    private val _sourceSpeeds = MutableStateFlow<Map<String, Long>>(emptyMap())
    val sourceSpeeds: StateFlow<Map<String, Long>> = _sourceSpeeds.asStateFlow()

    private val _speedTesting = MutableStateFlow(false)
    val speedTesting: StateFlow<Boolean> = _speedTesting.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // ===== 详情 =====
    private val _videoDetail = MutableStateFlow<VideoDetail?>(null)
    val videoDetail: StateFlow<VideoDetail?> = _videoDetail.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    // ===== 收藏 =====
    private val _favorites = MutableStateFlow<List<VideoInfo>>(emptyList())
    val favorites: StateFlow<List<VideoInfo>> = _favorites.asStateFlow()

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading.asStateFlow()

    // ===== 播放记录 =====
    private val _playRecords = MutableStateFlow<List<VideoPlayRecord>>(emptyList())
    val playRecords: StateFlow<List<VideoPlayRecord>> = _playRecords.asStateFlow()

    // ===== 分类搜索(豆瓣API筛选+分页) =====
    // 参考 Selene-Source: 用豆瓣 API 做筛选浏览,点击影片时搜索 LunaTV 播放
    private val _categoryResults = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryResults: StateFlow<List<VideoInfo>> = _categoryResults.asStateFlow()

    private val _categoryLoading = MutableStateFlow(false)
    val categoryLoading: StateFlow<Boolean> = _categoryLoading.asStateFlow()

    private val _categoryHasMore = MutableStateFlow(true)
    val categoryHasMore: StateFlow<Boolean> = _categoryHasMore.asStateFlow()

    private val _categoryTotalCount = MutableStateFlow(0)
    val categoryTotalCount: StateFlow<Int> = _categoryTotalCount.asStateFlow()

    private val PAGE_SIZE = 25  // 参考 Selene-Source: pageLimit=25
    private var categoryPage = 0
    private var categoryCurrentKind = "movie"  // 当前分类: movie/tv/anime/show
    private var categoryIsLoadingMore = false

    // ===== 分类筛选(参考 Selene-Source) =====
    data class CategoryFilters(
        val category: String = "\u70ED\u95E8",   // 热门/最新/豆瓣高分/冷门佳片/喜剧/爱情/...
        val region: String = "\u5168\u90E8",     // 全部/华语/欧美/韩国/日本/中国大陆/美国/...
        val year: String = "\u5168\u90E8",       // 全部/2026/2025/2024/2023/2020年代/2010年代/更早
        val sort: String = "T"           // T(综合)/U(近期热度)/R(首映时间)/S(高分优先)
    )
    private val _categoryFilters = MutableStateFlow(CategoryFilters())
    val categoryFilters: StateFlow<CategoryFilters> = _categoryFilters.asStateFlow()

    // ===== 直播 =====
    private val _liveSources = MutableStateFlow<List<LiveSource>>(emptyList())
    val liveSources: StateFlow<List<LiveSource>> = _liveSources.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<LiveChannelGroup>>(emptyList())
    val liveChannels: StateFlow<List<LiveChannelGroup>> = _liveChannels.asStateFlow()

    private val _liveLoading = MutableStateFlow(false)
    val liveLoading: StateFlow<Boolean> = _liveLoading.asStateFlow()

    // ===== Toast =====
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() { _toastMessage.value = null }

    init {
        viewModelScope.launch {
            combine(videoServerUrl, videoUsername, videoPassword) { url, user, pass ->
                Triple(url, user, pass)
            }.collect { (url, user, pass) ->
                if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                    autoLogin(url, user, pass)
                }
            }
        }
    }

    // ==================== 登录 ====================

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            try {
                val api = VideoApiClient.getApi(serverUrl)
                val response = withContext(Dispatchers.IO) {
                    api.login(mapOf("username" to username, "password" to password))
                }
                if (response.isSuccessful && response.body()?.ok == true) {
                    settings.saveVideoLogin(serverUrl, username, password)
                    _isLoggedIn.value = true
                    _toastMessage.value = "登录成功"
                    loadHomeData()
                    loadPlayRecords()
                } else {
                    _loginError.value = "登录失败，请检查账号密码"
                }
            } catch (e: Exception) {
                _loginError.value = "连接失败: ${e.message}"
                reportVideoError("login", "影视登录失败: $serverUrl", e)
            } finally {
                _loginLoading.value = false
            }
        }
    }

    private suspend fun autoLogin(serverUrl: String, username: String, password: String) {
        try {
            val api = VideoApiClient.getApi(serverUrl)
            val response = withContext(Dispatchers.IO) {
                api.login(mapOf("username" to username, "password" to password))
            }
            if (response.isSuccessful && response.body()?.ok == true) {
                _isLoggedIn.value = true
                loadHomeData()
                loadPlayRecords()
            } else {
                _isLoggedIn.value = false
            }
        } catch (_: Exception) {
            _isLoggedIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.clearVideoLogin()
            VideoApiClient.clearSession()
            _isLoggedIn.value = false
            _homeData.value = null
            _searchResults.value = emptyList()
            _videoDetail.value = null
            _favorites.value = emptyList()
            _playRecords.value = emptyList()
            _liveSources.value = emptyList()
            _liveChannels.value = emptyList()
        }
    }

    fun testConnection(serverUrl: String, username: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(serverUrl)
                val response = withContext(Dispatchers.IO) {
                    api.login(mapOf("username" to username, "password" to password))
                }
                if (response.isSuccessful && response.body()?.ok == true) {
                    onResult(true, "连接成功 ✓")
                } else {
                    onResult(false, "连接失败")
                }
            } catch (e: Exception) {
                onResult(false, "连接失败: ${e.message}")
            }
        }
    }

    // ==================== 首页推荐（豆瓣） ====================

    fun loadHomeData() {
        viewModelScope.launch {
            _homeLoading.value = true
            _homeError.value = null
            try {
                val doubanApi = DoubanApiClient.getApi()

                // 参考 Selene-Source: 用 /recommend 接口 + tags 精确筛选
                val moviesResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecommendations("movie", tags = "热门", sort = "T", count = 15)
                }
                val tvResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecommendations("tv", tags = "热门", sort = "T", count = 15)
                }
                val animeResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecommendations("tv", tags = "日本,动画", sort = "T", count = 15)
                }
                val showResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecommendations("tv", tags = "综艺", sort = "T", count = 15)
                }

                _homeData.value = HomeRecommendData(
                    continueWatch = _playRecords.value,
                    hotMovies = moviesResp.body()?.items?.map { it.toVideoInfo("movie") } ?: emptyList(),
                    hotTvShows = tvResp.body()?.items?.map { it.toVideoInfo("tv") } ?: emptyList(),
                    hotAnime = animeResp.body()?.items?.map { it.toVideoInfo("anime") } ?: emptyList(),
                    hotVariety = showResp.body()?.items?.map { it.toVideoInfo("show") } ?: emptyList()
                )
            } catch (e: Exception) {
                _homeError.value = "加载失败: ${e.message}"
                reportVideoError("loadHomeData", "首页推荐加载失败", e)
            } finally {
                _homeLoading.value = false
            }
        }
    }

    fun refreshHome() {
        loadHomeData()
        loadPlayRecords()
    }

    // ==================== 搜索 ====================

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchLoading.value = true
            try {
                addSearchHistory(keyword)
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.search(keyword) }
                if (response.isSuccessful) {
                    _searchResults.value = response.body()?.results ?: emptyList()
                } else {
                    _searchResults.value = emptyList()
                    _toastMessage.value = "搜索失败"
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _toastMessage.value = "搜索失败: ${e.message}"
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    private fun addSearchHistory(keyword: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(keyword)
        current.add(0, keyword)
        _searchHistory.value = current.take(20)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    // ==================== 详情 ====================

    fun loadDetail(source: String, id: String) {
        // 如果已经有该 source+id 的完整数据（含episodes），不重复加载
        val existing = _videoDetail.value
        if (existing != null && existing.source == source && existing.id == id
            && (existing.episodes.isNotEmpty() || existing.sources.isNotEmpty())) {
            logDebug("loadDetail", "已有数据,跳过加载: source=$source id=$id")
            _detailLoading.value = false
            return
        }
        viewModelScope.launch {
            _detailLoading.value = true
            try {
                logDebug("loadDetail", "加载详情: source=$source, id=$id")
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getDetail(source, id) }
                if (response.isSuccessful && response.body() != null) {
                    val detail = response.body()!!
                    // 检查是否有可播放资源
                    if (detail.episodes.isEmpty() && detail.sources.isEmpty()) {
                        _toastMessage.value = "该源暂无播放资源，正在尝试其他源..."
                        // 尝试用标题搜索其他源
                        if (detail.title.isNotBlank()) {
                            searchAndPlay(detail.title, detail.doubanId, detail.year)
                            return@launch
                        }
                    }
                    _videoDetail.value = detail
                } else {
                    _toastMessage.value = "加载详情失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "加载详情失败: ${e.message}"
                reportVideoError("loadDetail", "加载详情失败: source=$source id=$id", e)
            } finally {
                _detailLoading.value = false
            }
        }
    }

    fun clearDetail() {
        _videoDetail.value = null
    }

    /**
     * 搜索 LunaTV 找到源，直接构造 VideoDetail 用于播放
     * 跳过详情页，搜索结果已包含 episodes
     *
     * Bug修复：
     * 1. 添加加载状态提示用户正在搜索
     * 2. 搜索结果为空或episodes为空时提示而不是闪退
     * 3. 优先选择标题完全匹配+有episodes的结果
     */
    fun searchAndPlay(title: String, doubanId: String, year: String = "") {
        viewModelScope.launch {
            _searchSourceLoading.value = true
            _searchSourceMessage.value = "正在搜索播放源：$title"
            _detailLoading.value = true
            try {
                logDebug("searchAndPlay", "开始搜索: $title, server=${videoServerUrl.value}")
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val searchResp = withContext(Dispatchers.IO) { api.search(title) }
                val results = searchResp.body()?.results ?: emptyList()
                logDebug("searchAndPlay", "搜索返回: ${results.size} 个源")

                // 保存所有有 episodes 的源（用于片源切换）
                // 参考 Selene-Source: 去重+限制
                val validSources = results.filter { it.episodes.isNotEmpty() }
                    .groupBy { it.source }
                    .values.map { group -> group.first() }
                    .take(30)
                _allSearchSources.value = validSources

                if (results.isEmpty()) {
                    _toastMessage.value = "未找到「$title」的播放源"
                    _searchSourceLoading.value = false
                    _detailLoading.value = false
                    return@launch
                }

                _searchSourceMessage.value = "已找到 ${results.size} 个源，正在选择最佳..."

                // 参考 Selene-Source fetchSourcesData: 三级匹配
                // 第一级: 标题精确匹配(去空格+小写) + 年份匹配
                val normalizedTitle = title.replace(" ", "").lowercase()
                var matched = validSources.firstOrNull { src ->
                    val srcTitle = src.title.replace(" ", "").lowercase()
                    val titleMatch = srcTitle == normalizedTitle
                    val yearMatch = year.isBlank() || src.year.lowercase() == year.lowercase()
                    titleMatch && yearMatch
                }

                // 第二级: 标题包含匹配 + 年份必须一致
                if (matched == null) {
                    matched = validSources.firstOrNull { src ->
                        val srcTitle = src.title.replace(" ", "").lowercase()
                        val containsMatch = srcTitle.contains(normalizedTitle) || normalizedTitle.contains(srcTitle)
                        val yearMatch = year.isBlank() || src.year.lowercase() == year.lowercase()
                        containsMatch && yearMatch
                    }
                }

                // 第三级: 标题精确匹配(忽略年份) - 兜底
                if (matched == null) {
                    matched = validSources.firstOrNull { src ->
                        src.title.replace(" ", "").lowercase() == normalizedTitle
                    }
                }

                // 第四级: 取第一个有效源 - 最终兜底
                if (matched == null) {
                    matched = validSources.firstOrNull()
                }

                if (matched == null) {
                    _toastMessage.value = "未找到「$title」的播放源"
                    _searchSourceLoading.value = false
                    _detailLoading.value = false
                    return@launch
                }

                logDebug("searchAndPlay", "匹配: title=${matched.title}, source=${matched.source}, eps=${matched.episodes.size}")

                if (matched.episodes.isEmpty()) {
                    _toastMessage.value = "「$matched.title}」暂无可播放资源"
                    _searchSourceLoading.value = false
                    _detailLoading.value = false
                    return@launch
                }

                _searchSourceMessage.value = "已找到播放源：$matched.displaySourceName}，共 $matched.episodes.size} 集"

                // 构造 VideoDetail
                val detail = VideoDetail(
                    id = matched.id,
                    title = matched.title,
                    year = matched.year,
                    poster = matched.poster,
                    source = matched.source,
                    sourceName = matched.sourceName,
                    sourceNameAlt = matched.sourceNameAlt,
                    desc = matched.desc,
                    typeName = matched.type,
                    episodes = matched.episodes,
                    episodesTitles = matched.episodesTitles
                )
                logDebug("searchAndPlay", "VideoDetail OK, episodes=${detail.episodes.size}, sources=${validSources.size}")
                _videoDetail.value = detail
                // 先设置导航，再关闭加载（保证弹窗显示到导航完成）
                _navigateToDetail.value = true
                logDebug("searchAndPlay", "完成, navigateToDetail=true")
            } catch (e: Exception) {
                logDebug("searchAndPlay", "异常: ${e.javaClass.simpleName}: ${e.message}")
                _toastMessage.value = "搜索播放源失败: $e.message}"
                reportVideoError("searchAndPlay", "搜索播放源失败: $title", e)
                _searchSourceLoading.value = false
                _detailLoading.value = false
            } finally {
                // 注意：不在这里清除加载状态，由导航完成后的 consumeNavigateToDetail 清除
                _searchSourceMessage.value = ""
            }
        }
    }

    // 导航触发器（直接播放 - 保留兼容）
    private val _needNavigateToPlayer = MutableStateFlow(false)
    val needNavigateToPlayer: StateFlow<Boolean> = _needNavigateToPlayer.asStateFlow()
    fun consumeNavigateToPlayer() { _needNavigateToPlayer.value = false }

    // 导航到详情页（searchAndPlay 用这个）
    private val _navigateToDetail = MutableStateFlow(false)
    val navigateToDetail: StateFlow<Boolean> = _navigateToDetail.asStateFlow()
    fun consumeNavigateToDetail() {
        _navigateToDetail.value = false
        _searchSourceLoading.value = false
        _detailLoading.value = false
    }

    /** 切换播放源（详情页片源选择用） */
    // 源切换版本号（每次切换+1，强制 LaunchedEffect 重新触发）
    private val _switchSourceVersion = MutableStateFlow(0)
    val switchSourceVersion: StateFlow<Int> = _switchSourceVersion.asStateFlow()

    fun switchSource(sourceInfo: VideoInfo) {
        val detail = VideoDetail(
            id = sourceInfo.id,
            title = sourceInfo.title,
            year = sourceInfo.year,
            poster = sourceInfo.poster,
            source = sourceInfo.source,
            sourceName = sourceInfo.sourceName,
            sourceNameAlt = sourceInfo.sourceNameAlt,
            desc = sourceInfo.desc,
            typeName = sourceInfo.type,
            episodes = sourceInfo.episodes,
            episodesTitles = sourceInfo.episodesTitles
        )
        _videoDetail.value = detail
        _switchSourceVersion.value++
        logDebug("switchSource", "切换到: ${sourceInfo.displaySourceName}, eps=${sourceInfo.episodes.size}, version=${_switchSourceVersion.value}")
    }

    // 搜索播放源的加载状态（用于 UI 弹窗提示）
    private val _searchSourceLoading = MutableStateFlow(false)
    val searchSourceLoading: StateFlow<Boolean> = _searchSourceLoading.asStateFlow()
    private val _searchSourceMessage = MutableStateFlow("")
    val searchSourceMessage: StateFlow<String> = _searchSourceMessage.asStateFlow()

    // ==================== 收藏 ====================

    fun loadFavorites() {
        viewModelScope.launch {
            _favoritesLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getFavorites() }
                if (response.isSuccessful) {
                    _favorites.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { }
            _favoritesLoading.value = false
        }
    }

    fun addFavorite(video: VideoInfo) {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val request = FavoriteRequest(
                    source = video.source,
                    id = video.id,
                    title = video.title,
                    cover = video.displayCover,
                    year = video.year,
                    type = video.type
                )
                withContext(Dispatchers.IO) { api.addFavorite(request) }
                _toastMessage.value = "已收藏"
                loadFavorites()
            } catch (e: Exception) {
                _toastMessage.value = "收藏失败: ${e.message}"
            }
        }
    }

    fun removeFavorite(video: VideoInfo) {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val request = FavoriteRequest(
                    source = video.source,
                    id = video.id,
                    title = video.title,
                    cover = video.displayCover
                )
                withContext(Dispatchers.IO) { api.removeFavorite(request) }
                _toastMessage.value = "已取消收藏"
                loadFavorites()
            } catch (e: Exception) {
                _toastMessage.value = "取消收藏失败: ${e.message}"
            }
        }
    }

    fun isFavorite(videoId: String): Boolean {
        return _favorites.value.any { it.id == videoId }
    }

    // ==================== 播放记录 ====================

    fun loadPlayRecords() {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getPlayRecords() }
                if (response.isSuccessful && response.body() != null) {
                    // LunaTV 返回 dict: {"source+id": record}
                    val dict = response.body()!!
                    val records = dict.entries.mapNotNull { (key, record) ->
                        // 从 key 中提取 source 和 id (格式: "source+id")
                        val parts = key.split("+", limit = 2)
                        val source = parts.getOrElse(0) { "" }
                        val videoId = parts.getOrElse(1) { key }
                        record.copy(
                            videoIdRaw = videoId,
                            source = source
                        )
                    }.sortedByDescending { it.saveTime }
                    _playRecords.value = records
                }
            } catch (_: Exception) { }
        }
    }

    fun savePlayRecord(record: PlayRecordRequest) {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                withContext(Dispatchers.IO) { api.savePlayRecord(record) }
            } catch (_: Exception) { }
        }
    }

    fun deletePlayRecord(source: String, id: String) {
        viewModelScope.launch {
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                withContext(Dispatchers.IO) {
                    api.deletePlayRecord(mapOf("source" to source, "id" to id))
                }
                loadPlayRecords()
            } catch (_: Exception) { }
        }
    }

    // ==================== 分类搜索(豆瓣 recommend API + recent_hot) ====================

    /**
     * 分类搜索 - 参考 Selene-Source
     * - 无筛选时用 recent_hot API(快，简单)
     * - 有筛选时用 recommend API(支持类型/地区/年份/排序组合)
     *
     * recommend API 参数(参考 Selene-Source fetchDoubanRecommends):
     * - selected_categories: JSON, 如 {"\u7C7B\u578B":"\u559C\u5267"}
     * - tags: 逗号分隔, 如 "\u559C\u5267,\u97E9\u56FD,2025"
     * - sort: T/U/R/S
     */
    fun fetchDoubanCategory(kind: String, isRefresh: Boolean = true) {
        categoryCurrentKind = kind

        viewModelScope.launch {
            if (isRefresh) {
                _categoryLoading.value = true
                categoryPage = 0
                _categoryHasMore.value = true
            }

            val filters = _categoryFilters.value
            val hasFilters = filters.category != "\u70ED\u95E8" || filters.region != "\u5168\u90E8" ||
                    filters.year != "\u5168\u90E8" || filters.sort != "T"

            // 记录请求时的筛选状态
            val requestFilterKey = "${kind}_${filters.category}_${filters.region}_${filters.year}_${filters.sort}_${categoryPage}"

            try {
                val doubanApi = DoubanApiClient.getApi()
                val doubanKind = when (kind) {
                    "anime" -> "movie"
                    "variety" -> "tv"
                    else -> kind
                }

                val response = withContext(Dispatchers.IO) {
                    if (!hasFilters) {
                        // 无筛选: 用 recent_hot(快)
                        val (defCat, defType) = getDefaultParams(kind)
                        doubanApi.getRecentHot(
                            kind = doubanKind,
                            start = categoryPage * PAGE_SIZE,
                            limit = PAGE_SIZE,
                            category = defCat,
                            type = defType
                        )
                    } else {
                        // 有筛选: 用 recommend API(支持组合筛选)
                        val (tags, selectedCategories) = buildRecommendParams(kind, filters)
                        doubanApi.getRecommendations(
                            kind = doubanKind,
                            start = categoryPage * PAGE_SIZE,
                            count = PAGE_SIZE,
                            selectedCategories = selectedCategories,
                            tags = tags,
                            sort = if (filters.sort != "T") filters.sort else ""
                        )
                    }
                }

                // 检查筛选状态是否已变化
                val curFilters = _categoryFilters.value
                val curKey = "${kind}_${curFilters.category}_${curFilters.region}_${curFilters.year}_${curFilters.sort}_${categoryPage}"
                if (requestFilterKey != curKey) {
                    logDebug("fetchDoubanCategory", "筛选状态已变化,忽略过期响应")
                    return@launch
                }

                if (response.isSuccessful) {
                    val items = response.body()?.items ?: emptyList()
                    val mapped = items.map { it.toVideoInfo(doubanKind) }

                    if (isRefresh) {
                        _categoryResults.value = mapped
                    } else {
                        _categoryResults.value = _categoryResults.value + mapped
                    }

                    categoryPage++
                    _categoryHasMore.value = items.size >= PAGE_SIZE
                    _categoryTotalCount.value = response.body()?.total ?: _categoryResults.value.size
                    logDebug("fetchDoubanCategory", "完成: kind=$doubanKind, filters=$hasFilters, page=${categoryPage-1}, got=${items.size}")
                } else {
                    logDebug("fetchDoubanCategory", "请求失败: ${response.code()}")
                    if (isRefresh) _categoryResults.value = emptyList()
                    _categoryHasMore.value = false
                }
            } catch (e: Exception) {
                logDebug("fetchDoubanCategory", "异常: ${e.message}")
                if (isRefresh) _categoryResults.value = emptyList()
                _categoryHasMore.value = false
                reportVideoError("fetchDoubanCategory", "分类加载失败: $kind", e)
            }
            _categoryLoading.value = false
        }
    }

    /** 无筛选时的默认参数 */
    private fun getDefaultParams(kind: String): Pair<String, String> = when (kind) {
        "movie" -> "\u70ED\u95E8" to "\u5168\u90E8"
        "tv" -> "\u6700\u8FD1\u70ED\u95E8" to "tv"
        "anime" -> "\u70ED\u95E8" to "\u65E5\u672C"
        "variety" -> "show" to "show"
        else -> "\u70ED\u95E8" to "\u5168\u90E8"
    }

    /**
     * 构建 recommend API 参数(参考 Selene-Source)
     * @return Pair(tags, selectedCategoriesJson)
     */
    private fun buildRecommendParams(kind: String, filters: CategoryFilters): Pair<String, String> {
        val tags = mutableListOf<String>()
        val categories = mutableMapOf<String, String>()

        when (kind) {
            "movie" -> {
                // 类型(如喜剧/动作)
                if (filters.category != "\u70ED\u95E8") {
                    categories["\u7C7B\u578B"] = filters.category
                    tags.add(filters.category)
                }
                // 地区
                if (filters.region != "\u5168\u90E8") {
                    categories["\u5730\u533A"] = filters.region
                    tags.add(filters.region)
                }
                // 年份
                if (filters.year != "\u5168\u90E8") {
                    tags.add(filters.year)
                }
            }
            "tv" -> {
                // 剧集: tags 包含地区
                if (filters.region != "\u5168\u90E8") {
                    tags.add(filters.region)
                }
                if (filters.year != "\u5168\u90E8") {
                    tags.add(filters.year)
                }
            }
            "anime" -> {
                tags.add("\u52A8\u6F2B")
                if (filters.region != "\u5168\u90E8") {
                    tags.add(filters.region)
                }
                if (filters.year != "\u5168\u90E8") {
                    tags.add(filters.year)
                }
            }
            "variety" -> {
                tags.add("\u7EFC\u827A")
                if (filters.year != "\u5168\u90E8") {
                    tags.add(filters.year)
                }
            }
        }

        val selectedJson = if (categories.isNotEmpty()) {
            categories.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
                .let { "{$it}" }
        } else "{}"

        return tags.joinToString(",") to selectedJson
    }

    /** 加载更多(豆瓣 API 分页) */
    fun loadMoreCategory() {
        if (!_categoryHasMore.value || categoryIsLoadingMore) return
        categoryIsLoadingMore = true
        viewModelScope.launch {
            try {
                val doubanApi = DoubanApiClient.getApi()
                val filters = _categoryFilters.value
                val hasFilters = filters.category != "\u70ED\u95E8" || filters.region != "\u5168\u90E8" ||
                        filters.year != "\u5168\u90E8" || filters.sort != "T"
                val doubanKind = when (categoryCurrentKind) {
                    "anime" -> "movie"
                    "variety" -> "tv"
                    else -> categoryCurrentKind
                }

                val response = withContext(Dispatchers.IO) {
                    if (!hasFilters) {
                        val (defCat, defType) = getDefaultParams(categoryCurrentKind)
                        doubanApi.getRecentHot(
                            kind = doubanKind,
                            start = categoryPage * PAGE_SIZE,
                            limit = PAGE_SIZE,
                            category = defCat,
                            type = defType
                        )
                    } else {
                        val (tags, selectedCategories) = buildRecommendParams(categoryCurrentKind, filters)
                        doubanApi.getRecommendations(
                            kind = doubanKind,
                            start = categoryPage * PAGE_SIZE,
                            count = PAGE_SIZE,
                            selectedCategories = selectedCategories,
                            tags = tags,
                            sort = if (filters.sort != "T") filters.sort else ""
                        )
                    }
                }

                if (response.isSuccessful) {
                    val items = response.body()?.items ?: emptyList()
                    val mapped = items.map { it.toVideoInfo(doubanKind) }
                    _categoryResults.value = _categoryResults.value + mapped
                    categoryPage++
                    _categoryHasMore.value = items.size >= PAGE_SIZE
                    logDebug("loadMoreCategory", "加载更多: page=${categoryPage-1}, got=${items.size}")
                } else {
                    _categoryHasMore.value = false
                }
            } catch (e: Exception) {
                _categoryHasMore.value = false
                logDebug("loadMoreCategory", "异常: ${e.message}")
            }
            categoryIsLoadingMore = false
        }
    }

    /** 更新筛选条件并重新请求(参考 Selene-Source) */
    fun updateCategoryFilters(filters: CategoryFilters) {
        _categoryFilters.value = filters
        fetchDoubanCategory(categoryCurrentKind, isRefresh = true)
    }

    /** 重置筛选 */
    fun resetCategoryFilters() {
        _categoryFilters.value = CategoryFilters()
        fetchDoubanCategory(categoryCurrentKind, isRefresh = true)
    }

    /**
     * 测试所有源的响应速度（ping m3u8 URL）
     * 按速度排序后更新 allSearchSources
     */
    fun testSourceSpeeds() {
        val sources = _allSearchSources.value
        if (sources.isEmpty()) return
        viewModelScope.launch {
            _speedTesting.value = true
            val speeds = mutableMapOf<String, Long>()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            sources.forEach { source ->
                val url = source.episodes.firstOrNull()
                if (url != null && url.isNotBlank()) {
                    val ping = withContext(Dispatchers.IO) {
                        try {
                            val start = System.currentTimeMillis()
                            val request = okhttp3.Request.Builder().url(url).head().build()
                            client.newCall(request).execute().use { _ -> }
                            System.currentTimeMillis() - start
                        } catch (_: Exception) { -1L }
                    }
                    speeds[source.source] = ping
                    _sourceSpeeds.value = speeds.toMap()
                }
            }
            _speedTesting.value = false
            // 按速度排序（快的在前，超时的排最后）
            val sorted = sources.sortedBy { speeds[it.source] ?: Long.MAX_VALUE }
            _allSearchSources.value = sorted
        }
    }

    fun clearCategoryResults() {
        _categoryResults.value = emptyList()
    }

    // ==================== 直播 ====================

    fun loadLiveSources() {
        viewModelScope.launch {
            _liveLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getLiveSources() }
                if (response.isSuccessful) {
                    _liveSources.value = response.body() ?: emptyList()
                    val sources = response.body()
                    if (!sources.isNullOrEmpty()) {
                        loadLiveChannels(sources.first().source)
                    }
                }
            } catch (_: Exception) { }
            _liveLoading.value = false
        }
    }

    fun loadLiveChannels(source: String) {
        viewModelScope.launch {
            _liveLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getLiveChannels(source) }
                if (response.isSuccessful) {
                    _liveChannels.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { }
            _liveLoading.value = false
        }
    }
}
