package com.lechenmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lechenmusic.data.api.VideoApiClient
import com.lechenmusic.data.api.DoubanApiClient
import com.lechenmusic.data.model.*
import com.lechenmusic.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)

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
                        _toastMessage.value = "该源暂无播放资源"
                    }
                    _videoDetail.value = detail
                } else {
                    _toastMessage.value = "加载详情失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "加载详情失败: ${e.message}"
            } finally {
                _detailLoading.value = false
            }
        }
    }

    fun clearDetail() {
        _videoDetail.value = null
    }

    /**
     * 先搜索 LunaTV 找到 source+id，再加载详情
     * 优先选择有 episodes 的源
     */
    fun searchAndLoadDetail(title: String, doubanId: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val searchResp = withContext(Dispatchers.IO) { api.search(title) }
                val results = searchResp.body()?.results ?: emptyList()

                if (results.isEmpty()) {
                    _toastMessage.value = "未找到可用播放源"
                    _detailLoading.value = false
                    return@launch
                }

                // 优先选择标题完全匹配且有 episodes 的源
                val matched = results.firstOrNull {
                    it.title.contains(title, ignoreCase = true) && it.episodes.isNotEmpty()
                } ?: results.firstOrNull { it.episodes.isNotEmpty() }
                ?: results.first()

                loadDetail(matched.source, matched.id)
            } catch (e: Exception) {
                _toastMessage.value = "搜索失败: ${e.message}"
                _detailLoading.value = false
            }
        }
    }

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
            } catch (_: Exception) {
                _categoryResults.value = emptyList()
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
