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

    /** 影视模块错误上报到 WEB 管理端 */
    private fun reportVideoError(screen: String, message: String, throwable: Throwable? = null) {
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

    // ===== 分类搜索 =====
    private val _categoryResults = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryResults: StateFlow<List<VideoInfo>> = _categoryResults.asStateFlow()

    private val _categoryLoading = MutableStateFlow(false)
    val categoryLoading: StateFlow<Boolean> = _categoryLoading.asStateFlow()

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
                val moviesResp = withContext(Dispatchers.IO) { doubanApi.getRecentHot("movie", limit = 12) }
                val tvResp = withContext(Dispatchers.IO) { doubanApi.getRecentHot("tv", limit = 12) }
                val animeResp = withContext(Dispatchers.IO) { doubanApi.getRecentHot("anime", limit = 12) }
                val showResp = withContext(Dispatchers.IO) { doubanApi.getRecentHot("show", limit = 12) }

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
        viewModelScope.launch {
            _detailLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getDetail(source, id) }
                if (response.isSuccessful && response.body() != null) {
                    val detail = response.body()!!
                    // 检查是否有可播放资源
                    if (detail.episodes.isEmpty() && detail.sources.isEmpty()) {
                        _toastMessage.value = "该源暂无播放资源，正在尝试其他源..."
                        // 尝试用标题搜索其他源
                        if (detail.title.isNotBlank()) {
                            searchAndPlay(detail.title, detail.doubanId)
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
    fun searchAndPlay(title: String, doubanId: String) {
        viewModelScope.launch {
            _searchSourceLoading.value = true
            _searchSourceMessage.value = "正在搜索播放源：$title"
            _detailLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val searchResp = withContext(Dispatchers.IO) { api.search(title) }
                val results = searchResp.body()?.results ?: emptyList()

                if (results.isEmpty()) {
                    _toastMessage.value = "未找到「$title」的播放源"
                    _searchSourceLoading.value = false
                    _detailLoading.value = false
                    return@launch
                }

                _searchSourceMessage.value = "已找到 ${results.size} 个源，正在选择最佳..."

                // 优先选择标题完全匹配 + 有 episodes 的结果
                val matched = results.firstOrNull {
                    it.title.contains(title, ignoreCase = true) && it.episodes.isNotEmpty()
                }
                    ?: results.firstOrNull { it.episodes.isNotEmpty() }
                    ?: results.first()

                // 检查 episodes 是否为空
                if (matched.episodes.isEmpty()) {
                    _toastMessage.value = "「${matched.title}」暂无可播放资源"
                    _searchSourceLoading.value = false
                    _detailLoading.value = false
                    return@launch
                }

                _searchSourceMessage.value = "已找到播放源：${matched.displaySourceName}，共 ${matched.episodes.size} 集"

                // 直接用搜索结果构造 VideoDetail（搜索结果已包含 episodes）
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
                _videoDetail.value = detail
                _needNavigateToPlayer.value = true
            } catch (e: Exception) {
                _toastMessage.value = "搜索播放源失败: ${e.message}"
                reportVideoError("searchAndPlay", "搜索播放源失败: $title", e)
            } finally {
                _searchSourceLoading.value = false
                _searchSourceMessage.value = ""
                _detailLoading.value = false
            }
        }
    }

    // 导航触发器
    private val _needNavigateToPlayer = MutableStateFlow(false)
    val needNavigateToPlayer: StateFlow<Boolean> = _needNavigateToPlayer.asStateFlow()
    fun consumeNavigateToPlayer() { _needNavigateToPlayer.value = false }

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

    // ==================== 分类搜索 ====================

    fun searchCategory(keyword: String) {
        viewModelScope.launch {
            _categoryLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.search(keyword) }
                if (response.isSuccessful) {
                    _categoryResults.value = response.body()?.results ?: emptyList()
                } else {
                    _categoryResults.value = emptyList()
                }
            } catch (e: Exception) {
                _categoryResults.value = emptyList()
                reportVideoError("searchCategory", "分类搜索失败: $keyword", e)
            }
            _categoryLoading.value = false
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
