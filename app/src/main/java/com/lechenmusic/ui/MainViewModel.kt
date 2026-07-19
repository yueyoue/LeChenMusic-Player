package com.lechenmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lechenmusic.LeChenApp
import com.lechenmusic.data.model.*
import com.lechenmusic.data.repository.LyricsCache
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.repository.ServerStats
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import com.lechenmusic.update.UpdateChecker
import com.lechenmusic.update.UpdateInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LeChenApp
    private val repository: MusicRepository = app.repository
    private val settings: SettingsRepository = app.settingsRepository
    val playerManager: MusicPlayerManager = app.playerManager

    // Theme
    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    // Login state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Home data
    private val _newestAlbums = MutableStateFlow<List<Album>>(emptyList())
    val newestAlbums: StateFlow<List<Album>> = _newestAlbums.asStateFlow()

    private val _randomAlbums = MutableStateFlow<List<Album>>(emptyList())
    val randomAlbums: StateFlow<List<Album>> = _randomAlbums.asStateFlow()

    private val _dailySongs = MutableStateFlow<List<Song>>(emptyList())
    val dailySongs: StateFlow<List<Song>> = _dailySongs.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs: StateFlow<List<Song>> = _recentSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Artists
    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    // Search
    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Starred
    private val _starredSongs = MutableStateFlow<List<Song>>(emptyList())
    val starredSongs: StateFlow<List<Song>> = _starredSongs.asStateFlow()
    private val _starredAlbums = MutableStateFlow<List<Album>>(emptyList())
    val starredAlbums: StateFlow<List<Album>> = _starredAlbums.asStateFlow()
    private val _starredAudiobooks = MutableStateFlow<List<Audiobook>>(emptyList())
    val starredAudiobooks: StateFlow<List<Audiobook>> = _starredAudiobooks.asStateFlow()
    private val _starredPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val starredPlaylists: StateFlow<List<Playlist>> = _starredPlaylists.asStateFlow()

    // Server stats
    private val _serverStats = MutableStateFlow(ServerStats())
    val serverStats: StateFlow<ServerStats> = _serverStats.asStateFlow()

    // Lyrics
    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    // Recent plays (song objects)
    private val _recentPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentPlayedSongs: StateFlow<List<Song>> = _recentPlayedSongs.asStateFlow()

    // All songs
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // All songs loading state
    private val _allSongsLoading = MutableStateFlow(false)
    val allSongsLoading: StateFlow<Boolean> = _allSongsLoading.asStateFlow()

    private val _allSongsLoadError = MutableStateFlow<String?>(null)
    val allSongsLoadError: StateFlow<String?> = _allSongsLoadError.asStateFlow()

    // Cached songs (songs that have been cached by ExoPlayer for offline playback)
    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs.asStateFlow()

    // Internet Radio Stations
    private val _radioStations = MutableStateFlow<List<InternetRadioStation>>(emptyList())
    val radioStations: StateFlow<List<InternetRadioStation>> = _radioStations.asStateFlow()

    // Timer countdown (seconds remaining)
    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    // Sync status
    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // App Update
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _updateStatus = MutableStateFlow("")
    val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    // AI Recommended songs (based on current song via getSimilarSongs2)
    private val _aiRecommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val aiRecommendedSongs: StateFlow<List<Song>> = _aiRecommendedSongs.asStateFlow()

    private val _aiRecommendLoading = MutableStateFlow(false)
    val aiRecommendLoading: StateFlow<Boolean> = _aiRecommendLoading.asStateFlow()

    // Toast message for user feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * 检查更新（自动或手动调用）
     * @param silent true=静默检查（无更新不提示），false=手动检查（无更新也提示）
     */
    fun checkForUpdate(silent: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            if (!silent) _updateStatus.value = "检查中..."
            try {
                val context = getApplication<Application>()
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }
                val info = UpdateChecker.check(currentVersionCode, serverUrl.value)
                if (info != null) {
                    // If silent auto-check, respect skipped version
                    if (silent) {
                        val skippedCode = settings.skippedVersionCode.first()
                        if (skippedCode >= info.versionCode) {
                            // User already skipped this version, don't show
                            _isCheckingUpdate.value = false
                            _updateStatus.value = ""
                            return@launch
                        }
                    }
                    _updateInfo.value = info
                } else if (!silent) {
                    _toastMessage.value = "当前已是最新版本 ✓"
                }
            } catch (e: Exception) {
                if (!silent) _toastMessage.value = "检查更新失败: ${e.message}"
            } finally {
                _isCheckingUpdate.value = false
                if (!silent) _updateStatus.value = ""
            }
        }
    }

    /** Skip current update version */
    fun skipUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            settings.setSkippedVersionCode(info.versionCode)
            _updateInfo.value = null
            _updateStatus.value = ""
        }
    }

    /** 下载并安装更新 */
    fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _updateStatus.value = "通过${info.source}正在下载..."
            val context = getApplication<Application>()
            val apkFile = UpdateChecker.downloadApk(
                context = context,
                apkUrl = info.apkUrl,
                source = info.source,
                onProgress = { _updateStatus.value = it }
            )
            if (apkFile != null) {
                _updateStatus.value = "下载完成，正在安装..."
                val success = UpdateChecker.installApk(context, apkFile)
                if (!success) {
                    _updateStatus.value = "安装启动失败"
                    kotlinx.coroutines.delay(3000)
                } else {
                    // 延迟后重置状态，避免卡在"正在安装"
                    kotlinx.coroutines.delay(8000)
                }
                _updateStatus.value = ""
                _updateInfo.value = null
            } else {
                // downloadApk already set error message via onProgress
                kotlinx.coroutines.delay(3000)
                _updateStatus.value = ""
            }
        }
    }

    /** 关闭更新弹窗 */
    fun dismissUpdate() {
        _updateInfo.value = null
        _updateStatus.value = ""
    }

    // Album detail
    private val _currentAlbum = MutableStateFlow<AlbumDetail?>(null)
    val currentAlbum: StateFlow<AlbumDetail?> = _currentAlbum.asStateFlow()

    // Artist detail
    private val _currentArtist = MutableStateFlow<ArtistDetail?>(null)
    val currentArtist: StateFlow<ArtistDetail?> = _currentArtist.asStateFlow()
    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    val artistSongs: StateFlow<List<Song>> = _artistSongs.asStateFlow()

    // Playlist detail
    private val _currentPlaylist = MutableStateFlow<PlaylistDetail?>(null)
    val currentPlaylist: StateFlow<PlaylistDetail?> = _currentPlaylist.asStateFlow()

    // Cache size setting
    val cacheSize: StateFlow<Int> = settings.cacheSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    // Server info
    val serverUrl: StateFlow<String> = settings.serverUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val username: StateFlow<String> = settings.username
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val password: StateFlow<String> = settings.password
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        // Check if already logged in
        viewModelScope.launch {
            combine(settings.serverUrl, settings.username, settings.password) { url, user, pass ->
                Triple(url, user, pass)
            }.collect { (url, user, pass) ->
                if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                    repository.configure(url, user, pass)
                    // Authenticate with Navidrome native API for audiobook endpoints
                    repository.authenticateNavidrome()
                    _isLoggedIn.value = true
                    loadHomeData()
                    // Auto-sync all songs in background
                    loadAllSongs()
                    // 恢复有声书播放状态（如果正在播放）
                    restoreAudiobookState()
                }
            }
        }

        // Register callback for auto-advance (lock screen / background playback)
        playerManager.onSongAutoAdvanced = { song ->
            // Only record music songs, not audiobooks or radio
            if (!song.id.startsWith("audiobook_") && !song.id.startsWith("radio_")) {
                viewModelScope.launch {
                    settings.addRecentPlay(song.id)
                    addSongToRecentCache(song)
                    // Refresh recent played songs list
                    loadRecentPlayedSongs()
                }
            }
        }

        // Register callback for audiobook auto-play next chapter
        playerManager.onPlaybackCompleted = {
            if (_currentAudiobook.value != null) {
                // Auto-advance for audiobook playback
                android.util.Log.d("LeChenMusic", "Audiobook chapter ended, auto-playing next chapter")
                audiobookNextChapter()
            } else {
                // Music playback completed — add current song to recent cache
                // This ensures daily recommendations and other playlists get cached
                val song = playerManager.currentSong.value
                if (song != null && !song.id.startsWith("radio_")) {
                    android.util.Log.d("LeChenMusic", "Music playback completed, caching song: ${song.title}")
                    viewModelScope.launch {
                        settings.addRecentPlay(song.id)
                        addSongToRecentCache(song)
                        loadRecentPlayedSongs()
                        loadCachedSongs()
                    }
                }
            }
        }

        // Update progress periodically (faster interval for accurate lyrics sync)
        viewModelScope.launch {
            var audiobookProgressSaveCounter = 0
            var lockScreenUpdateCounter = 0
            while (true) {
                kotlinx.coroutines.delay(200)
                playerManager.updateProgress()
                // Update lock screen position every ~1 second
                lockScreenUpdateCounter++
                if (lockScreenUpdateCounter >= 5) {
                    lockScreenUpdateCounter = 0
                    playerManager.updateLockScreenPosition()
                }
                // Sync audiobook progress from ExoPlayer
                if (_currentAudiobook.value != null) {
                    _audiobookPosition.value = playerManager.currentPosition.value
                    _audiobookDuration.value = playerManager.duration.value
                    _audiobookIsPlaying.value = playerManager.isPlaying.value
                    // Auto-save audiobook progress every 15 seconds during playback
                    if (_audiobookIsPlaying.value) {
                        audiobookProgressSaveCounter++
                        if (audiobookProgressSaveCounter >= 75) { // 75 * 200ms = 15s
                            audiobookProgressSaveCounter = 0
                            saveAudiobookProgress()
                        }
                    } else {
                        audiobookProgressSaveCounter = 0
                    }
                }
            }
        }
    }

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                repository.configure(serverUrl, username, password)
                val result = repository.ping()
                if (result.isSuccess) {
                    // Authenticate with Navidrome native API for audiobook endpoints
                    repository.authenticateNavidrome()
                    settings.saveLogin(serverUrl, username, password)
                    _isLoggedIn.value = true
                    loadHomeData()
                } else {
                    _loginError.value = result.exceptionOrNull()?.message ?: "连接失败"
                }
            } catch (e: Exception) {
                _loginError.value = e.message ?: "连接失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.clearLogin()
            _isLoggedIn.value = false
            // Clear all cached data when logging out / switching servers
            _newestAlbums.value = emptyList()
            _randomAlbums.value = emptyList()
            _dailySongs.value = emptyList()
            _recentSongs.value = emptyList()
            _playlists.value = emptyList()
            _artists.value = emptyList()
            _searchResults.value = null
            _searchQuery.value = ""
            _starredSongs.value = emptyList()
            _starredAlbums.value = emptyList()
            _starredAudiobooks.value = emptyList()
            _starredPlaylists.value = emptyList()
            _recentPlayedSongs.value = emptyList()
            _allSongs.value = emptyList()
            _cachedSongs.value = emptyList()
            _radioStations.value = emptyList()
            _currentAlbum.value = null
            _currentArtist.value = null
            _currentPlaylist.value = null
            _currentLyrics.value = null
            _audiobooks.value = emptyList()
            _audiobookWithProgress.value = emptyList()
            _narrators.value = emptyList()
            _narratorWorks.value = emptyList()
            _audiobookSearchResults.value = emptyList()
            _audiobookDetail.value = null
            _currentAudiobook.value = null
            _currentAudiobookChapters.value = emptyList()
            _musicSlides.value = emptyList()
            _audiobookSlides.value = emptyList()
            _serverStats.value = com.lechenmusic.data.repository.ServerStats()
            _homeMode.value = "music"
            // Clear player state
            playerManager.forcePause()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    fun setCacheSize(sizeGb: Int) {
        viewModelScope.launch {
            settings.setCacheSize(sizeGb)
            playerManager.updateCacheSize(sizeGb)
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val gson = Gson()
            val albumType = object : TypeToken<List<Album>>() {}.type
            val playlistType = object : TypeToken<List<Playlist>>() {}.type
            val radioType = object : TypeToken<List<InternetRadioStation>>() {}.type

            // Step 1: Load cached data first for instant/offline display
            try {
                val cachedNewest = settings.cachedNewestAlbumsJson.first()
                if (cachedNewest.isNotBlank()) {
                    val cached: List<Album> = gson.fromJson(cachedNewest, albumType)
                    if (cached.isNotEmpty()) _newestAlbums.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedRandom = settings.cachedRandomAlbumsJson.first()
                if (cachedRandom.isNotBlank()) {
                    val cached: List<Album> = gson.fromJson(cachedRandom, albumType)
                    if (cached.isNotEmpty()) _randomAlbums.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedPlaylists = settings.cachedPlaylistsJson.first()
                if (cachedPlaylists.isNotBlank()) {
                    val cached: List<Playlist> = gson.fromJson(cachedPlaylists, playlistType)
                    if (cached.isNotEmpty()) _playlists.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedRadio = settings.cachedRadioStationsJson.first()
                if (cachedRadio.isNotBlank()) {
                    val cached: List<InternetRadioStation> = gson.fromJson(cachedRadio, radioType)
                    if (cached.isNotEmpty()) _radioStations.value = cached
                }
            } catch (_: Exception) {}

            // Step 2: Fetch fresh data from server, update cache on success
            // Load newest albums
            repository.getNewestAlbums(10).onSuccess {
                _newestAlbums.value = it
                try { settings.saveCachedNewestAlbumsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load random albums - only if not already loaded (user must click "换一批" to refresh)
            if (_randomAlbums.value.isEmpty()) {
                repository.getRandomAlbums(10).onSuccess {
                    _randomAlbums.value = it
                    try { settings.saveCachedRandomAlbumsJson(gson.toJson(it)) } catch (_: Exception) {}
                }
            }

            // Load daily random songs - use cache if same day AND has enough songs
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val cachedDate = settings.cachedDailySongsDate.first()
            val cachedDailyJson = settings.cachedDailySongsJson.first()
            val useCache = cachedDate == today && cachedDailyJson.isNotBlank()
            if (useCache) {
                try {
                    val type = object : TypeToken<List<Song>>() {}.type
                    val cachedSongs: List<Song> = Gson().fromJson(cachedDailyJson, type)
                    // Cache must have at least 5 songs (display count), otherwise re-fetch
                    if (cachedSongs.size >= 5) {
                        _dailySongs.value = cachedSongs
                    } else {
                        repository.getRandomSongs(20).onSuccess {
                            _dailySongs.value = it
                            settings.saveCachedDailySongs(Gson().toJson(it), today)
                        }
                    }
                } catch (_: Exception) {
                    repository.getRandomSongs(20).onSuccess {
                        _dailySongs.value = it
                        settings.saveCachedDailySongs(Gson().toJson(it), today)
                    }
                }
            } else {
                repository.getRandomSongs(20).onSuccess {
                    _dailySongs.value = it
                    settings.saveCachedDailySongs(Gson().toJson(it), today)
                }
            }

            // Load playlists
            repository.getPlaylists().onSuccess {
                _playlists.value = it
                try { settings.saveCachedPlaylistsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load internet radio stations
            repository.getInternetRadioStations().onSuccess {
                _radioStations.value = it
                try { settings.saveCachedRadioStationsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load starred songs
            repository.getStarred().onSuccess { 
                _starredSongs.value = it.songs
                _starredAlbums.value = it.albums
                _starredPlaylists.value = it.playlists
            }
            loadStarredAudiobooks()
            loadAudiobooks()
            loadNarrators()
            loadSlides()

            // Load recent played songs from stored IDs
            loadRecentPlayedSongs()

            // Load cached songs (songs that are in ExoPlayer cache + recently played)
            loadCachedSongs()

            // Load server stats
            repository.getServerStats().onSuccess { _serverStats.value = it }
        }
    }

    fun loadCachedSongs() {
        viewModelScope.launch {
            val cachedIds = playerManager.getCachedSongIds()
            // Recently played songs that are also in the ExoPlayer cache
            val recentCached = _recentPlayedSongs.value.filter { it.id in cachedIds }
            // Also include all cached IDs matched from allSongs
            val allCached = _allSongs.value.filter { it.id in cachedIds }
            // Merge, deduplicate, recently played first
            val merged = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()
            for (song in recentCached) {
                if (seenIds.add(song.id)) merged.add(song)
            }
            for (song in allCached) {
                if (seenIds.add(song.id)) merged.add(song)
            }
            _cachedSongs.value = merged
        }
    }

    private suspend fun loadRecentPlayedSongs() {
        // Try cached song objects first (most reliable)
        try {
            val cachedJson = settings.cachedRecentSongsJson.first()
            if (cachedJson.isNotBlank()) {
                val type = object : TypeToken<List<Song>>() {}.type
                val cachedSongs: List<Song> = Gson().fromJson(cachedJson, type) ?: emptyList()
                // Filter out audiobooks and radio — only show music
                val musicOnly = cachedSongs.filter { !it.id.startsWith("audiobook_") && !it.id.startsWith("radio_") }
                if (musicOnly.isNotEmpty()) {
                    _recentPlayedSongs.value = musicOnly.take(20)
                    return
                }
            }
        } catch (_: Exception) { }

        // Fallback: search through albums by IDs
        val idsStr = settings.recentPlayIds.first()
        if (idsStr.isBlank()) return
        val ids = idsStr.split(",").filter { it.isNotEmpty() && !it.startsWith("audiobook_") && !it.startsWith("radio_") }
        if (ids.isEmpty()) return
        val recentSongs = mutableListOf<Song>()

        // First try: search through allSongs cache (fast, usually available)
        val allCached = _allSongs.value
        if (allCached.isNotEmpty()) {
            for (id in ids) {
                val found = allCached.find { it.id == id }
                if (found != null && recentSongs.none { it.id == found.id }) {
                    recentSongs.add(found)
                }
            }
        }

        // Second try: search through albums if allSongs cache didn't cover all IDs
        if (recentSongs.size < ids.size) {
            val albumSources = mutableListOf<List<Album>>()
            repository.getRecentAlbums(20).onSuccess { albumSources.add(it) }
            repository.getNewestAlbums(20).onSuccess { albumSources.add(it) }
            repository.getFrequentAlbums(20).onSuccess { albumSources.add(it) }
            for (albums in albumSources) {
                for (album in albums) {
                    if (recentSongs.size >= ids.size) break
                    repository.getAlbum(album.id).onSuccess { detail ->
                        detail.song?.forEach { song ->
                            if (ids.contains(song.id) && recentSongs.none { it.id == song.id }) {
                                recentSongs.add(song)
                            }
                        }
                    }
                }
                if (recentSongs.size >= ids.size) break
            }
        }

        val sorted = ids.mapNotNull { id -> recentSongs.find { it.id == id } }
        if (sorted.isNotEmpty()) {
            _recentPlayedSongs.value = sorted
            // Save to cache for future fast loading
            try {
                settings.saveCachedRecentSongsJson(Gson().toJson(sorted.take(50)))
            } catch (_: Exception) { }
        }
    }

    fun loadLyrics(song: Song) {
        _currentLyrics.value = null
        val lyricsCache = (getApplication() as LeChenApp).lyricsCache

        viewModelScope.launch {
            // 1. 先读本地缓存
            val cached = lyricsCache.get(song.id)
            if (cached != null) {
                _currentLyrics.value = cached
                return@launch
            }

            // 2. 尝试获取结构化歌词（带时间戳）
            try {
                repository.getLyricsBySongId(song.id).onSuccess { lyricsResponse ->
                    val syncedLyric = lyricsResponse?.structuredLyrics
                        ?.firstOrNull { it.synced && !it.line.isNullOrEmpty() }
                    if (syncedLyric != null) {
                        val lrcBuilder = StringBuilder()
                        for (line in syncedLyric.line!!) {
                            val totalMs = line.start
                            val min = (totalMs / 60000).toInt()
                            val sec = ((totalMs % 60000) / 1000).toInt()
                            val ms = ((totalMs % 1000) / 10).toInt()
                            lrcBuilder.append(String.format("[%02d:%02d.%02d] %s\n", min, sec, ms, line.value))
                        }
                        val lrcText = lrcBuilder.toString()
                        if (lrcText.isNotBlank()) {
                            _currentLyrics.value = lrcText
                            lyricsCache.put(song.id, lrcText)
                            return@launch
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. 从服务器获取普通歌词
            var serverLyrics: String? = null
            repository.getLyrics(song.artist, song.title).onSuccess { lyrics ->
                serverLyrics = lyrics
            }

            // 4. 使用服务器歌词
            val finalLyrics = serverLyrics
            if (finalLyrics != null) {
                _currentLyrics.value = finalLyrics
                lyricsCache.put(song.id, finalLyrics)
            }
        }
    }

    /** Load similar songs based on current song */
    fun loadSimilarSongs(songId: String) {
        viewModelScope.launch {
            repository.getSimilarSongs2(songId, 20).onSuccess { songs ->
                val currentSong = playerManager.currentSong.value
                val newList = mutableListOf<Song>()
                if (currentSong != null) newList.add(currentSong)
                newList.addAll(songs.filter { it.id != currentSong?.id })
                if (newList.isNotEmpty()) {
                    playerManager.playSong(newList.first(), newList)
                }
            }
        }
    }

    /** Load AI recommended songs based on current song for player recommendation tab */
    fun loadAIRecommendedSongs(songId: String) {
        viewModelScope.launch {
            _aiRecommendLoading.value = true
            _aiRecommendedSongs.value = emptyList()
            repository.getSimilarSongs2(songId, 20).onSuccess { songs ->
                _aiRecommendedSongs.value = songs.filter { it.id != songId }.take(15)
            }.onFailure {
                android.util.Log.w("LeChenMusic", "loadAIRecommendedSongs failed: ${it.message}")
            }
            _aiRecommendLoading.value = false
        }
    }

    fun loadArtists() {
        viewModelScope.launch {
            repository.getArtists().onSuccess { _artists.value = it }
        }
    }

    fun loadAlbums(type: String = "newest", callback: (List<Album>) -> Unit) {
        viewModelScope.launch {
            when (type) {
                "newest" -> repository.getNewestAlbums(50)
                "recent" -> repository.getRecentAlbums(50)
                "random" -> repository.getRandomAlbums(50)
                else -> repository.getNewestAlbums(50)
            }.onSuccess { callback(it) }
        }
    }

    fun loadAllAlbums(callback: (List<Album>) -> Unit) {
        viewModelScope.launch {
            val allAlbums = mutableListOf<Album>()
            val seenIds = mutableSetOf<String>()
            val types = listOf("newest", "recent", "frequent", "random", "starred", "alphabeticalByName")
            for (type in types) {
                var offset = 0
                val pageSize = 500
                while (true) {
                    try {
                        val albums = repository.getAlbumList2(type, pageSize, offset).getOrNull() ?: break
                        for (album in albums) {
                            if (seenIds.add(album.id)) {
                                allAlbums.add(album)
                            }
                        }
                        if (albums.size < pageSize) break
                        offset += pageSize
                    } catch (_: Exception) { break }
                }
            }
            callback(allAlbums)
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        viewModelScope.launch {
            repository.search(query).onSuccess { _searchResults.value = it }
        }
    }

    fun loadAlbumDetail(albumId: String) {
        viewModelScope.launch {
            repository.getAlbum(albumId).onSuccess { _currentAlbum.value = it }
        }
    }

    fun loadArtistDetail(artistId: String) {
        viewModelScope.launch {
            repository.getArtist(artistId).onSuccess { detail ->
                _currentArtist.value = detail
                // Collect all songs from artist's albums
                val allSongs = mutableListOf<Song>()
                detail.album?.forEach { album ->
                    repository.getAlbum(album.id).onSuccess { albumDetail ->
                        albumDetail.song?.let { allSongs.addAll(it) }
                    }
                }
                _artistSongs.value = allSongs
            }
        }
    }

    fun loadPlaylistDetail(playlistId: String) {
        viewModelScope.launch {
            repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        // Save audiobook progress before switching to music
        saveAudiobookProgress()

        // Clear audiobook state when playing music
        _currentAudiobook.value = null
        _currentAudiobookChapters.value = emptyList()
        _audiobookIsPlaying.value = false
        playerManager.clearAudiobookCoverUrl()

        playerManager.playSong(song, playlist)
        viewModelScope.launch {
            settings.addRecentPlay(song.id)
            repository.scrobble(song.id)
            // Cache the song object for recent plays
            addSongToRecentCache(song)
            // Refresh recent played songs
            loadRecentPlayedSongs()
            // Refresh cached songs after a delay (allow ExoPlayer to cache)
            kotlinx.coroutines.delay(3000)
            loadCachedSongs()
        }
    }

    private suspend fun addSongToRecentCache(song: Song) {
        try {
            val cachedJson = settings.cachedRecentSongsJson.first()
            val type = object : TypeToken<List<Song>>() {}.type
            val existing: List<Song> = if (cachedJson.isNotBlank()) {
                Gson().fromJson(cachedJson, type) ?: emptyList()
            } else emptyList()
            val updated = listOf(song) + existing.filter { it.id != song.id }
            settings.saveCachedRecentSongsJson(Gson().toJson(updated.take(50)))
        } catch (_: Exception) { }
    }

    fun refreshRandomAlbums() {
        viewModelScope.launch {
            repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it }
        }
    }

    fun refreshDailySongs() {
        viewModelScope.launch {
            repository.getRandomSongs(20).onSuccess {
                _dailySongs.value = it
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                settings.saveCachedDailySongs(Gson().toJson(it), today)
            }
        }
    }

    fun loadAllSongs(showToast: Boolean = false) {
        viewModelScope.launch {
            _allSongsLoading.value = true
            _allSongsLoadError.value = null

            // Step 1: Load cached songs first for instant display
            val cachedJson = settings.cachedAllSongsJson.first()
            if (cachedJson.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<Song>>() {}.type
                    val cachedSongs: List<Song> = Gson().fromJson(cachedJson, type)
                    if (cachedSongs.isNotEmpty()) {
                        _allSongs.value = cachedSongs
                        _allSongsLoading.value = false
                    }
                } catch (_: Exception) { }
            }

            // Step 2: Fetch fresh data from server
            try {
                repository.getAllSongs().onSuccess { serverSongs ->
                    val cachedList = _allSongs.value
                    val cachedIds = cachedList.map { it.id }.toSet()
                    val newSongs = serverSongs.filter { it.id !in cachedIds }

                    if (newSongs.isNotEmpty()) {
                        // Merge: keep cached + add new songs (always grow, never shrink)
                        val merged = cachedList + newSongs
                        _allSongs.value = merged
                        if (showToast) _toastMessage.value = "发现 ${newSongs.size} 首新歌曲"
                        saveSongsToCache(merged)
                    } else if (serverSongs.size >= cachedList.size) {
                        // Server has more (or equal) songs — safe to update
                        _allSongs.value = serverSongs
                        saveSongsToCache(serverSongs)
                    }
                    // else: server returned fewer songs (non-deterministic search),
                    //       keep cached list — do NOT replace with smaller data
                    _allSongsLoading.value = false
                }.onFailure {
                    if (_allSongs.value.isEmpty()) {
                        // No cache and server failed
                        repository.getRandomSongs(500).onSuccess { songs ->
                            _allSongs.value = songs
                            saveSongsToCache(songs)
                        }.onFailure { e ->
                            _allSongsLoadError.value = e.message
                        }
                    }
                    _allSongsLoading.value = false
                }
            } catch (e: Exception) {
                if (_allSongs.value.isEmpty()) {
                    _allSongsLoadError.value = e.message
                }
                _allSongsLoading.value = false
            }
        }
    }

    private suspend fun saveSongsToCache(songs: List<Song>) {
        try {
            val json = Gson().toJson(songs)
            settings.saveCachedAllSongsJson(json)
        } catch (_: Exception) { }
    }

    fun addToPlaylist(playlistId: String, songId: String, playlistOwner: String = "") {
        viewModelScope.launch {
            // Check if this is someone else's playlist
            val currentUser = username.value
            if (playlistOwner.isNotBlank() && playlistOwner != currentUser) {
                _toastMessage.value = "不能添加歌曲到别人的歌单"
                return@launch
            }
            repository.addToPlaylist(playlistId, songId).onSuccess {
                _toastMessage.value = "已添加到歌单"
                // Refresh playlist detail if viewing
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
            }.onFailure {
                _toastMessage.value = "添加失败: ${it.message}"
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, songIndex: Int) {
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, songIndex).onSuccess {
                _toastMessage.value = "已从歌单移除"
                // Refresh playlist detail
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                // Also refresh playlist list
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "移除失败: ${it.message}"
            }
        }
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
        _toastMessage.value = "已添加到播放队列"
    }

    fun createPlaylistAndAddSong(name: String, songId: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            val idToSend = songId.ifBlank { null }
            repository.createPlaylist(name, idToSend, isPublic).onSuccess {
                _toastMessage.value = "歌单创建成功"
                // Refresh playlists after creating
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "创建失败: ${it.message}"
            }
        }
    }

    // Timer countdown job
    private var countdownJob: kotlinx.coroutines.Job? = null

    fun setTimerWithCountdown(minutes: Int) {
        // Cancel any existing timer
        cancelTimerWithCountdown()
        // Clear timer expired flag for new timer
        playerManager.clearTimerExpired()
        // Also set alarm as backup (works even if app is killed)
        playerManager.setTimer(minutes)
        _timerRemainingSeconds.value = minutes * 60L
        val targetTime = System.currentTimeMillis() + minutes * 60 * 1000L
        countdownJob = viewModelScope.launch {
            // Use absolute time to avoid drift when app goes to background
            while (true) {
                kotlinx.coroutines.delay(500)
                val remaining = ((targetTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                _timerRemainingSeconds.value = remaining
                if (remaining <= 0) break
            }
            // Timer reached zero - force pause playback
            try {
                playerManager.forcePause()
                // If still playing after forcePause, toggle it
                kotlinx.coroutines.delay(300)
                if (playerManager.isPlaying.value) {
                    playerManager.togglePlayPause()
                }
                // Final force pause for reliability
                kotlinx.coroutines.delay(200)
                playerManager.forcePause()
            } catch (_: Exception) {
                try { playerManager.forcePause() } catch (_: Exception) {}
            }
        }
    }

    fun cancelTimerWithCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        playerManager.cancelTimer()
        _timerRemainingSeconds.value = 0
    }

    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = "同步中..."
            try {
                // Sync playlists
                _syncStatus.value = "同步歌单 (1/4)..."
                repository.getPlaylists().onSuccess { _playlists.value = it }

                // Sync radio stations
                repository.getInternetRadioStations().onSuccess { _radioStations.value = it }

                // Sync artists
                _syncStatus.value = "同步歌手 (2/4)..."
                repository.getArtists().onSuccess { _artists.value = it }

                // Sync starred
                _syncStatus.value = "同步收藏 (3/4)..."
                repository.getStarred().onSuccess { 
                    _starredSongs.value = it.songs
                    _starredAlbums.value = it.albums
                }
                loadStarredAudiobooks()

                // Refresh home data (daily songs and random albums only refresh on user click "换一批")
                _syncStatus.value = "同步歌曲和专辑 (4/4)..."
                repository.getNewestAlbums(10).onSuccess { _newestAlbums.value = it }

                // Refresh server stats
                repository.getServerStats().onSuccess { _serverStats.value = it }

                // Refresh recent played
                loadRecentPlayedSongs()

                _syncStatus.value = "同步完成 ✓\n已同步: 歌单、歌手、收藏、专辑、歌曲、服务器统计"
            } catch (e: Exception) {
                _syncStatus.value = "同步失败: ${e.message}"
            }
            kotlinx.coroutines.delay(5000)
            _syncStatus.value = ""
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().onSuccess { _playlists.value = it }
            repository.getStarred().onSuccess { 
                _starredSongs.value = it.songs
                _starredAlbums.value = it.albums
            }
            loadStarredAudiobooks()
        }
    }

    fun updatePlaylistPublic(playlistId: String, isPublic: Boolean) {
        viewModelScope.launch {
            repository.updatePlaylistPublic(playlistId, isPublic).onSuccess {
                _toastMessage.value = if (isPublic) "歌单已设为公开" else "歌单已设为私密"
                // Refresh playlist detail
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                // Refresh playlist list
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "修改失败: ${it.message}"
            }
        }
    }

    // ===== Slide State =====
    private val _musicSlides = MutableStateFlow<List<com.lechenmusic.data.model.SlideConfig>>(emptyList())
    val musicSlides: StateFlow<List<com.lechenmusic.data.model.SlideConfig>> = _musicSlides.asStateFlow()

    private val _audiobookSlides = MutableStateFlow<List<com.lechenmusic.data.model.SlideConfig>>(emptyList())
    val audiobookSlides: StateFlow<List<com.lechenmusic.data.model.SlideConfig>> = _audiobookSlides.asStateFlow()

    fun loadSlides() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return@launch
                val authHeader = "Bearer $token"
                val api = com.lechenmusic.data.api.ApiClient.getAudiobookApi(serverUrl.value)
                val response = api.getAppConfig(authHeader)
                if (response.isSuccessful) {
                    val json = response.body()?.asJsonObject
                    val data = json?.getAsJsonObject("data")
                    if (data != null) {
                        val gson = com.google.gson.Gson()
                        // Parse music slides
                        val musicSlidesArr = data.getAsJsonArray("musicSlides")
                        if (musicSlidesArr != null) {
                            _musicSlides.value = musicSlidesArr.map { el ->
                                gson.fromJson(el, com.lechenmusic.data.model.SlideConfig::class.java)
                            }
                        } else {
                            // Fallback to legacy slides
                            val legacySlides = data.getAsJsonArray("slides")
                            if (legacySlides != null) {
                                _musicSlides.value = legacySlides.map { el ->
                                    gson.fromJson(el, com.lechenmusic.data.model.SlideConfig::class.java)
                                }
                            }
                        }
                        // Parse audiobook slides
                        val audiobookSlidesArr = data.getAsJsonArray("audiobookSlides")
                        if (audiobookSlidesArr != null) {
                            _audiobookSlides.value = audiobookSlidesArr.map { el ->
                                gson.fromJson(el, com.lechenmusic.data.model.SlideConfig::class.java)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LeChenMusic", "loadSlides failed: ${e.message}")
            }
        }
    }

    // ===== Audiobook State =====
    private val _homeMode = MutableStateFlow("music")
    val homeMode: StateFlow<String> = _homeMode.asStateFlow()
    fun setHomeMode(mode: String) { _homeMode.value = mode }

    private val _audiobooks = MutableStateFlow<List<com.lechenmusic.data.model.Audiobook>>(emptyList())
    val audiobooks: StateFlow<List<com.lechenmusic.data.model.Audiobook>> = _audiobooks.asStateFlow()
    private val _audiobookWithProgress = MutableStateFlow<List<com.lechenmusic.data.model.AudiobookWithProgress>>(emptyList())
    val audiobookWithProgress: StateFlow<List<com.lechenmusic.data.model.AudiobookWithProgress>> = _audiobookWithProgress.asStateFlow()
    private val _audiobookError = MutableStateFlow<String?>(null)
    val audiobookError: StateFlow<String?> = _audiobookError.asStateFlow()

    private val _audiobookDetail = MutableStateFlow<com.lechenmusic.data.model.AudiobookDetail?>(null)
    val audiobookDetail: StateFlow<com.lechenmusic.data.model.AudiobookDetail?> = _audiobookDetail.asStateFlow()

    private val _currentAudiobook = MutableStateFlow<com.lechenmusic.data.model.Audiobook?>(null)
    val currentAudiobook: StateFlow<com.lechenmusic.data.model.Audiobook?> = _currentAudiobook.asStateFlow()

    private val _currentAudiobookChapters = MutableStateFlow<List<com.lechenmusic.data.model.AudiobookChapter>>(emptyList())
    val currentAudiobookChapters: StateFlow<List<com.lechenmusic.data.model.AudiobookChapter>> = _currentAudiobookChapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _audiobookIsPlaying = MutableStateFlow(false)
    val audiobookIsPlaying: StateFlow<Boolean> = _audiobookIsPlaying.asStateFlow()

    private val _audiobookPosition = MutableStateFlow(0L)
    val audiobookPosition: StateFlow<Long> = _audiobookPosition.asStateFlow()

    private val _audiobookDuration = MutableStateFlow(0L)
    val audiobookDuration: StateFlow<Long> = _audiobookDuration.asStateFlow()

    private val _audiobookPlaybackSpeed = MutableStateFlow(1f)
    val audiobookPlaybackSpeed: StateFlow<Float> = _audiobookPlaybackSpeed.asStateFlow()

    private val _audiobookTimerMinutes = MutableStateFlow(0)
    val audiobookTimerMinutes: StateFlow<Int> = _audiobookTimerMinutes.asStateFlow()

    fun audiobookSetTimer(minutes: Int) {
        _audiobookTimerMinutes.value = minutes
        if (minutes > 0) {
            playerManager.setTimer(minutes)
        } else {
            playerManager.cancelTimer()
        }
    }

    fun audiobookChangeSpeed(speed: Float) {
        _audiobookPlaybackSpeed.value = speed
        playerManager.setPlaybackSpeed(speed)
    }

        /**
     * 从当前播放歌曲恢复有声书状态
     * 用于从通知栏/锁屏进入APP时恢复状态
     */
    private fun restoreAudiobookState() {
        val song = playerManager.currentSong.value ?: return
        if (!song.id.startsWith("audiobook_")) return
        
        // 从歌曲ID中提取bookId: audiobook_{bookId}_{chapterId}
        val parts = song.id.removePrefix("audiobook_").split("_")
        if (parts.size < 2) return
        val bookId = parts[0]
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getAudiobookDetail(bookId)
                if (result.isSuccess) {
                    val detail = result.getOrNull()
                    if (detail != null) {
                        _currentAudiobook.value = detail.book
                        _currentAudiobookChapters.value = detail.chapters ?: emptyList()
                        _currentChapterIndex.value = detail.chapters?.indexOfFirst { 
                            it.id == detail.progress?.chapterId 
                        }?.coerceAtLeast(0) ?: 0
                    }
                }
            } catch (e: Exception) {
                // 静默失败，不影响正常使用
            }
        }
    }
    
fun loadAudiobooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("LeChenMusic", "loadAudiobooks: Starting...")
                _audiobookError.value = null
                val result = repository.getAudiobooks()
                val size = result.getOrNull()?.size ?: 0
                android.util.Log.d("LeChenMusic", "loadAudiobooks: success=${result.isSuccess}, size=$size")
                if (result.isSuccess) {
                    _audiobooks.value = result.getOrNull() ?: emptyList()
                    if (size == 0) {
                        _audiobookError.value = "有声书数据为空，请检查服务器认证设置"
                    }
                } else {
                    android.util.Log.w("LeChenMusic", "loadAudiobooks: Failed", result.exceptionOrNull())
                    _audiobookError.value = "加载失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "loadAudiobooks: Exception", e)
                _audiobookError.value = "加载异常: ${e.message}"
            }
        }
        // Also load audiobooks with progress for "继续收听" (after audiobooks loaded)
        viewModelScope.launch(Dispatchers.IO) {
            // Wait for audiobooks to be loaded first
            for (i in 1..50) {
                if (_audiobooks.value.isNotEmpty()) break
                kotlinx.coroutines.delay(200)
            }
            // Try recent-progress first (only returns books with saved progress, like ting-reader)
            try {
                val result = repository.getRecentAudiobookProgress()
                if (result.isSuccess) {
                    val books = result.getOrNull() ?: emptyList()
                    android.util.Log.d("LeChenMusic", "recent-progress: ${books.size} books")
                    if (books.isNotEmpty()) {
                        _audiobookWithProgress.value = books
                        return@launch
                    }
                } else {
                    android.util.Log.w("LeChenMusic", "recent-progress API failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("LeChenMusic", "recent-progress exception: ${e.message}")
            }
            // Fallback to with-progress (returns ALL books, progress may be null)
            try {
                val result = repository.getAudiobooksWithProgress()
                if (result.isSuccess) {
                    val books = result.getOrNull() ?: emptyList()
                    android.util.Log.d("LeChenMusic", "with-progress: ${books.size} books, ${books.count { it.progress != null }} with progress")
                    if (books.isNotEmpty()) {
                        _audiobookWithProgress.value = books
                        return@launch
                    }
                } else {
                    android.util.Log.w("LeChenMusic", "with-progress API failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("LeChenMusic", "with-progress exception: ${e.message}")
            }
            // Final fallback: load progress for each book individually
            loadAudiobookProgressFallback()
        }
    }

    private suspend fun loadAudiobookProgressFallback() {
        try {
            val books = _audiobooks.value
            if (books.isEmpty()) return
            android.util.Log.d("LeChenMusic", "loadAudiobookProgressFallback: checking ${books.size} books")
            val booksWithProgress = mutableListOf<com.lechenmusic.data.model.AudiobookWithProgress>()
            for (book in books) {
                try {
                    val detailResult = repository.getAudiobookDetail(book.id)
                    val progress = if (detailResult.isSuccess) detailResult.getOrNull()?.progress else null
                    if (progress != null) {
                        android.util.Log.d("LeChenMusic", "loadAudiobookProgressFallback: ${book.title} has progress ch=${progress.chapterNumber} pos=${progress.position}s")
                    }
                    booksWithProgress.add(
                        com.lechenmusic.data.model.AudiobookWithProgress(
                            id = book.id, title = book.title, author = book.author,
                            narrator = book.narrator, description = book.description,
                            genre = book.genre, year = book.year, coverPath = book.coverPath,
                            totalDuration = book.totalDuration, chapterCount = book.chapterCount,
                            libraryId = book.libraryId, path = book.path, size = book.size,
                            starred = book.starred, createdAt = book.createdAt, updatedAt = book.updatedAt,
                            progress = progress
                        )
                    )
                } catch (_: Exception) {
                    booksWithProgress.add(
                        com.lechenmusic.data.model.AudiobookWithProgress(
                            id = book.id, title = book.title, author = book.author,
                            narrator = book.narrator, description = book.description,
                            genre = book.genre, year = book.year, coverPath = book.coverPath,
                            totalDuration = book.totalDuration, chapterCount = book.chapterCount,
                            libraryId = book.libraryId, path = book.path, size = book.size,
                            starred = book.starred, createdAt = book.createdAt, updatedAt = book.updatedAt
                        )
                    )
                }
            }
            if (booksWithProgress.isNotEmpty()) {
                val withProgress = booksWithProgress.filter { it.progress != null }
                android.util.Log.d("LeChenMusic", "loadAudiobookProgressFallback: ${withProgress.size}/${booksWithProgress.size} books have progress")
                _audiobookWithProgress.value = booksWithProgress
            }
        } catch (e: Exception) {
            android.util.Log.e("LeChenMusic", "loadAudiobookProgressFallback failed: ${e.message}")
        }
    }

    fun loadStarredAudiobooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getStarredAudiobooks()
                if (result.isSuccess) _starredAudiobooks.value = result.getOrNull() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    // ===== Narrator State =====
    private val _narrators = MutableStateFlow<List<MusicRepository.NarratorInfo>>(emptyList())
    val narrators: StateFlow<List<MusicRepository.NarratorInfo>> = _narrators.asStateFlow()

    private val _narratorWorks = MutableStateFlow<List<com.lechenmusic.data.model.Audiobook>>(emptyList())
    val narratorWorks: StateFlow<List<com.lechenmusic.data.model.Audiobook>> = _narratorWorks.asStateFlow()

    private val _audiobookSearchResults = MutableStateFlow<List<com.lechenmusic.data.model.Audiobook>>(emptyList())
    val audiobookSearchResults: StateFlow<List<com.lechenmusic.data.model.Audiobook>> = _audiobookSearchResults.asStateFlow()

    fun loadNarrators() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getNarrators()
                if (result.isSuccess) _narrators.value = result.getOrNull() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun loadNarratorDetail(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getNarratorDetail(name)
                if (result.isSuccess) _narratorWorks.value = result.getOrNull() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun searchAudiobooks(query: String) {
        if (query.isBlank()) {
            _audiobookSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.searchAudiobooks(query)
                if (result.isSuccess) _audiobookSearchResults.value = result.getOrNull() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun star(id: String) {
        // Optimistic update
        val current = _currentAlbum.value
        if (current != null && current.id == id) {
            _currentAlbum.value = current.copy(starred = java.time.Instant.now().toString())
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.star(id).onSuccess {
                repository.getStarred().onSuccess {
                    _starredSongs.value = it.songs
                    _starredAlbums.value = it.albums
                    _starredPlaylists.value = it.playlists
                }
            }.onFailure {
                if (current != null) _currentAlbum.value = current
            }
        }
    }

    fun unstar(id: String) {
        val current = _currentAlbum.value
        if (current != null && current.id == id) {
            _currentAlbum.value = current.copy(starred = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.unstar(id).onSuccess {
                repository.getStarred().onSuccess {
                    _starredSongs.value = it.songs
                    _starredAlbums.value = it.albums
                    _starredPlaylists.value = it.playlists
                }
            }.onFailure {
                if (current != null) _currentAlbum.value = current
            }
        }
    }

    fun starAudiobook(id: String) {
        // Optimistic update: immediately reflect in UI
        val currentDetail = _audiobookDetail.value
        if (currentDetail != null && currentDetail.book.id == id) {
            _audiobookDetail.value = currentDetail.copy(
                book = currentDetail.book.copy(starred = java.time.Instant.now().toString())
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.starAudiobook(id)
                if (result.isSuccess) {
                    loadStarredAudiobooks()
                    loadAudiobookDetail(id)
                } else {
                    // Revert on failure
                    android.util.Log.e("LeChenMusic", "starAudiobook failed: ${result.exceptionOrNull()?.message}")
                    if (currentDetail != null) _audiobookDetail.value = currentDetail
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "starAudiobook exception: ${e.message}")
                if (currentDetail != null) _audiobookDetail.value = currentDetail
            }
        }
    }

    fun unstarAudiobook(id: String) {
        // Optimistic update: immediately reflect in UI
        val currentDetail = _audiobookDetail.value
        if (currentDetail != null && currentDetail.book.id == id) {
            _audiobookDetail.value = currentDetail.copy(
                book = currentDetail.book.copy(starred = null)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.unstarAudiobook(id)
                if (result.isSuccess) {
                    loadStarredAudiobooks()
                    loadAudiobookDetail(id)
                } else {
                    android.util.Log.e("LeChenMusic", "unstarAudiobook failed: ${result.exceptionOrNull()?.message}")
                    if (currentDetail != null) _audiobookDetail.value = currentDetail
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "unstarAudiobook exception: ${e.message}")
                if (currentDetail != null) _audiobookDetail.value = currentDetail
            }
        }
    }

    // Star/Unstar playlist (for #4: favorites sync)
    fun starPlaylist(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.star(id)
                loadPlaylists()
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "starPlaylist exception: ${e.message}")
            }
        }
    }

    fun unstarPlaylist(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.unstar(id)
                loadPlaylists()
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "unstarPlaylist exception: ${e.message}")
            }
        }
    }

    fun loadAudiobookDetail(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getAudiobookDetail(id)
                if (result.isSuccess) {
                    val detail = result.getOrNull()
                    val book = detail?.book
                    android.util.Log.d("LeChenMusic", "loadAudiobookDetail: book=${book?.title}, starred=${book?.starred}, isStarred=${book?.isStarred}, progress=${detail?.progress != null}, progressPos=${detail?.progress?.position}")
                    _audiobookDetail.value = detail
                } else {
                    android.util.Log.e("LeChenMusic", "loadAudiobookDetail failed: ${result.exceptionOrNull()?.message}")
                    _audiobookDetail.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "loadAudiobookDetail exception: ${e.message}")
                _audiobookDetail.value = null
            }
        }
    }

    fun playAudiobookChapter(book: com.lechenmusic.data.model.Audiobook, chapter: com.lechenmusic.data.model.AudiobookChapter, chapters: List<com.lechenmusic.data.model.AudiobookChapter>) {
        _currentAudiobook.value = book
        _currentAudiobookChapters.value = chapters
        _currentChapterIndex.value = chapters.indexOfFirst { it.id == chapter.id }.coerceAtLeast(0)
        val url = repository.getAudiobookChapterStreamUrl(book.id, chapter.id)
        val coverUrl = repository.getAudiobookCoverUrl(book.id)
        playerManager.playUrl(url, chapter.title, book.title, "audiobook_${book.id}_${chapter.id}", coverUrl)
        _audiobookIsPlaying.value = true
    }

    fun audiobookPreviousChapter() {
        val idx = _currentChapterIndex.value
        if (idx > 0) {
            val book = _currentAudiobook.value ?: return
            val chapters = _currentAudiobookChapters.value
            playAudiobookChapter(book, chapters[idx - 1], chapters)
        }
    }

    fun audiobookNextChapter() {
        val idx = _currentChapterIndex.value
        val chapters = _currentAudiobookChapters.value
        if (idx < chapters.size - 1) {
            val book = _currentAudiobook.value ?: return
            playAudiobookChapter(book, chapters[idx + 1], chapters)
        }
    }

    fun audiobookSeekTo(positionMs: Long) { playerManager.seekTo(positionMs) }
    fun audiobookSkipForward15s() { playerManager.seekTo(playerManager.currentPosition.value + 15000) }
    fun audiobookSkipBackward15s() { playerManager.seekTo((playerManager.currentPosition.value - 15000).coerceAtLeast(0)) }
    fun audiobookTogglePlayPause() { playerManager.togglePlayPause() }

    // ===== Audiobook Progress =====
    private val _audiobookProgressMap = MutableStateFlow<Map<String, com.lechenmusic.data.model.AudiobookProgress>>(emptyMap())
    val audiobookProgressMap: StateFlow<Map<String, com.lechenmusic.data.model.AudiobookProgress>> = _audiobookProgressMap.asStateFlow()

    fun loadAudiobookProgress(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.getAudiobookProgress(bookId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        _audiobookProgressMap.value = _audiobookProgressMap.value + (bookId to progress)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun saveAudiobookProgress() {
        val book = _currentAudiobook.value ?: return
        val chapters = _currentAudiobookChapters.value
        val idx = _currentChapterIndex.value
        val chapter = chapters.getOrNull(idx) ?: return
        val positionMs = playerManager.currentPosition.value
        val positionSeconds = (positionMs / 1000).toInt()
        android.util.Log.d("LeChenMusic", "saveAudiobookProgress: book=${book.id}, chapter=${chapter.id}, pos=${positionSeconds}s")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.saveAudiobookProgress(book.id, chapter.id, chapter.chapterNumber, positionSeconds)
                if (result.isSuccess) {
                    android.util.Log.d("LeChenMusic", "saveAudiobookProgress: OK")
                } else {
                    android.util.Log.e("LeChenMusic", "saveAudiobookProgress: FAILED: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "saveAudiobookProgress: EXCEPTION: ${e.message}")
            }
        }
    }

    fun resumeAudiobook(book: com.lechenmusic.data.model.Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First try to use already-loaded detail (avoid extra API call)
                val cachedDetail = _audiobookDetail.value
                val detail = if (cachedDetail != null && cachedDetail.book.id == book.id) {
                    cachedDetail
                } else {
                    val detailResult = repository.getAudiobookDetail(book.id)
                    if (detailResult.isSuccess) detailResult.getOrNull() else null
                }
                if (detail == null) return@launch

                val progress = detail.progress
                val chapters = detail.chapters
                if (chapters.isEmpty()) return@launch

                // Find resume chapter (inspired by ting-reader)
                // 1. Use progress.chapterId if available
                // 2. Otherwise use the first chapter
                val resumeChapter = if (progress != null && progress.chapterId.isNotEmpty()) {
                    chapters.find { it.id == progress.chapterId }
                } else null

                if (resumeChapter != null) {
                    val chapterIndex = chapters.indexOfFirst { it.id == resumeChapter.id }.coerceAtLeast(0)
                    val url = repository.getAudiobookChapterStreamUrl(book.id, resumeChapter.id)
                    val coverUrl = repository.getAudiobookCoverUrl(book.id)
                    val seekToMs = progress!!.position * 1000L

                    android.util.Log.d("LeChenMusic", "resumeAudiobook: chapter=${resumeChapter.title}, seekTo=${seekToMs}ms")

                    // Switch to Main thread for ExoPlayer operations
                    // Pass initialSeekMs to playUrl so ExoPlayer seeks when ready (STATE_READY)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        _currentAudiobook.value = book
                        _currentAudiobookChapters.value = chapters
                        _currentChapterIndex.value = chapterIndex
                        playerManager.playUrl(url, resumeChapter.title, book.title, "audiobook_${book.id}_${resumeChapter.id}", coverUrl, initialSeekMs = seekToMs)
                        _audiobookIsPlaying.value = true
                    }
                } else {
                    // No progress found, play from beginning
                    android.util.Log.d("LeChenMusic", "resumeAudiobook: no progress, playing from start")
                    val firstChapter = chapters[0]
                    val url = repository.getAudiobookChapterStreamUrl(book.id, firstChapter.id)
                    val coverUrl = repository.getAudiobookCoverUrl(book.id)

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        _currentAudiobook.value = book
                        _currentAudiobookChapters.value = chapters
                        _currentChapterIndex.value = 0
                        playerManager.playUrl(url, firstChapter.title, book.title, "audiobook_${book.id}_${firstChapter.id}", coverUrl)
                        _audiobookIsPlaying.value = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LeChenMusic", "resumeAudiobook exception: ${e.message}")
            }
        }
    }

}
