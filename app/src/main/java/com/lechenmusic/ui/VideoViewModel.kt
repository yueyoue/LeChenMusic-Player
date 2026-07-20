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

    // ===== 分类 =====
    private val _categoryMovies = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryMovies: StateFlow<List<VideoInfo>> = _categoryMovies.asStateFlow()

    private val _categoryTv = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryTv: StateFlow<List<VideoInfo>> = _categoryTv.asStateFlow()

    private val _categoryAnime = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryAnime: StateFlow<List<VideoInfo>> = _categoryAnime.asStateFlow()

    private val _categoryVariety = MutableStateFlow<List<VideoInfo>>(emptyList())
    val categoryVariety: StateFlow<List<VideoInfo>> = _categoryVariety.asStateFlow()

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
        // 自动恢复登录状态
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
                    api.login(LoginRequest(username, password))
                }
                if (response.isSuccessful && response.body()?.code == 0) {
                    settings.saveVideoLogin(serverUrl, username, password)
                    _isLoggedIn.value = true
                    _toastMessage.value = "登录成功"
                    loadHomeData()
                } else {
                    _loginError.value = response.body()?.msg ?: "登录失败，请检查账号密码"
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
                api.login(LoginRequest(username, password))
            }
            if (response.isSuccessful && response.body()?.code == 0) {
                _isLoggedIn.value = true
                loadHomeData()
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
                    api.login(LoginRequest(username, password))
                }
                if (response.isSuccessful && response.body()?.code == 0) {
                    onResult(true, "连接成功 ✓")
                } else {
                    onResult(false, response.body()?.msg ?: "连接失败")
                }
            } catch (e: Exception) {
                onResult(false, "连接失败: ${e.message}")
            }
        }
    }

    // ==================== 首页推荐 ====================

    fun loadHomeData() {
        viewModelScope.launch {
            _homeLoading.value = true
            _homeError.value = null
            try {
                val doubanApi = DoubanApiClient.getApi()
                // 并行加载电影、电视剧、动漫
                val moviesResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecentHot("movie", limit = 12)
                }
                val tvResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecentHot("tv", limit = 12)
                }
                val animeResp = withContext(Dispatchers.IO) {
                    doubanApi.getRecentHot("anime", limit = 12)
                }

                val movies = moviesResp.body()?.items?.map { it.toVideoInfo("movie") } ?: emptyList()
                val tv = tvResp.body()?.items?.map { it.toVideoInfo("tv") } ?: emptyList()
                val anime = animeResp.body()?.items?.map { it.toVideoInfo("anime") } ?: emptyList()

                _homeData.value = HomeRecommendData(
                    hotMovies = movies,
                    hotTvShows = tv,
                    hotAnime = anime
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
                if (response.isSuccessful && response.body()?.code == 0) {
                    _searchResults.value = response.body()?.data ?: emptyList()
                } else {
                    _searchResults.value = emptyList()
                    _toastMessage.value = response.body()?.msg ?: "搜索失败"
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
                if (response.isSuccessful && response.body()?.code == 0) {
                    _videoDetail.value = response.body()?.data
                } else {
                    _toastMessage.value = response.body()?.msg ?: "加载详情失败"
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

    // ==================== 收藏 ====================

    fun loadFavorites() {
        viewModelScope.launch {
            _favoritesLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getFavorites() }
                if (response.isSuccessful && response.body()?.code == 0) {
                    _favorites.value = response.body()?.data ?: emptyList()
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
                    cover = video.cover,
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
                    cover = video.cover
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
                if (response.isSuccessful && response.body()?.code == 0) {
                    _playRecords.value = response.body()?.data ?: emptyList()
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

    // ==================== 分类 ====================

    fun loadCategory(type: String, page: Int = 1) {
        viewModelScope.launch {
            _categoryLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getCategory(type, page) }
                if (response.isSuccessful && response.body()?.code == 0) {
                    val list = response.body()?.data?.list ?: emptyList()
                    when (type) {
                        "movie" -> _categoryMovies.value = list
                        "tv" -> _categoryTv.value = list
                        "anime" -> _categoryAnime.value = list
                        "variety" -> _categoryVariety.value = list
                    }
                }
            } catch (_: Exception) { }
            _categoryLoading.value = false
        }
    }

    // ==================== 直播 ====================

    fun loadLiveSources() {
        viewModelScope.launch {
            _liveLoading.value = true
            try {
                val api = VideoApiClient.getApi(videoServerUrl.value)
                val response = withContext(Dispatchers.IO) { api.getLiveSources() }
                if (response.isSuccessful && response.body()?.code == 0) {
                    _liveSources.value = response.body()?.data ?: emptyList()
                    // 自动加载第一个源的频道
                    val sources = response.body()?.data
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
                if (response.isSuccessful && response.body()?.code == 0) {
                    _liveChannels.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) { }
            _liveLoading.value = false
        }
    }
}
