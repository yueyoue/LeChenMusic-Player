package com.lechenmusic.player

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lechenmusic.LeChenApp
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as Media3Metadata
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder
import okhttp3.OkHttpClient
import com.lechenmusic.MainActivity
import com.lechenmusic.R
import com.lechenmusic.data.model.Song
import com.lechenmusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.delay
import java.net.URL

enum class RepeatMode { OFF, ONE, ALL }

class MusicPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSessionCompat: MediaSessionCompat? = null

    // Music disk cache
    private var musicCache: SimpleCache? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null
    private var currentCacheSizeBytes: Long = 4L * 1024 * 1024 * 1024 // default 4GB

    // 播放时长统计
    private val playTimePrefs = context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
    private var playStartTimeMs: Long = 0L
    private val _totalPlayMinutes = MutableStateFlow(playTimePrefs.getLong("total_play_seconds", 0) / 60)
    val totalPlayMinutes: StateFlow<Long> = _totalPlayMinutes.asStateFlow()

    // Fully played song IDs — only these should appear in cache list
    private val fullyPlayedPrefs by lazy { context.getSharedPreferences("fully_played_songs", Context.MODE_PRIVATE) }
    private val fullyPlayedSongIds: MutableSet<String> by lazy {
        fullyPlayedPrefs.getStringSet("ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var alarmReceiver: BroadcastReceiver? = null

    // Timer stop flag: when true, player should not auto-resume
    @Volatile
    var timerExpired: Boolean = false
        private set

    var onSongAutoAdvanced: ((Song) -> Unit)? = null

    // 定时到点停止播放后回调（用于清理定时相关的UI状态）
    var onTimerExpired: (() -> Unit)? = null

    // Called when current media item playback completes (STATE_ENDED)
    var onPlaybackCompleted: (() -> Unit)? = null

    companion object {
        const val ACTION_STOP_PLAYBACK = "com.lechenmusic.STOP_PLAYBACK"
        const val ACTION_TOGGLE_FAVORITE = "com.lechenmusic.TOGGLE_FAVORITE"
        const val ACTION_PREV = "com.lechenmusic.PREV"
        const val ACTION_NEXT = "com.lechenmusic.NEXT"
        const val ACTION_PLAY_PAUSE = "com.lechenmusic.PLAY_PAUSE"
        const val ACTION_REWIND_30 = "com.lechenmusic.REWIND_30"
        const val ACTION_FORWARD_30 = "com.lechenmusic.FORWARD_30"
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    fun init(repo: MusicRepository) {
        repository = repo
        initCache()
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory!!))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        // 播放时长统计
                        if (isPlaying) {
                            playStartTimeMs = System.currentTimeMillis()
                        } else if (playStartTimeMs > 0) {
                            val elapsed = (System.currentTimeMillis() - playStartTimeMs) / 1000
                            playStartTimeMs = 0
                            if (elapsed > 0) {
                                addPlayTime(elapsed)
                            }
                        }
                        // If timer expired and player auto-resumed (e.g. audio focus regain), force pause
                        if (isPlaying && timerExpired) {
                            player?.pause()
                            return
                        }
                        updateNotification()
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // 电台流媒体：强制时长为0，防止ExoPlayer报告异常大数值
                            val isRadio = _currentSong.value?.id?.startsWith("radio_") == true
                            _duration.value = if (isRadio) 0L else duration.coerceAtLeast(0)
                            // Consume pending seek position (for audiobook resume)
                            val seekTarget = pendingSeekMs
                            if (seekTarget > 0) {
                                pendingSeekMs = 0L
                                player?.seekTo(seekTarget)
                                android.util.Log.d("LeChenMusic", "Pending seek applied: ${seekTarget}ms")
                            }
                        }
                        if (playbackState == Player.STATE_READY && _isPlaying.value) {
                            updateNotification()
                        }
                        // When playback ends, mark song as fully played and notify
                        if (playbackState == Player.STATE_ENDED) {
                            // Track fully played song for cache filtering
                            val endedSong = _currentSong.value
                            if (endedSong != null && !endedSong.id.startsWith("audiobook_") && !endedSong.id.startsWith("radio_")) {
                                fullyPlayedSongIds.add(endedSong.id)
                                fullyPlayedPrefs.edit().putStringSet("ids", fullyPlayedSongIds).apply()
                            }
                            android.util.Log.d("LeChenMusic", "Playback STATE_ENDED, calling onPlaybackCompleted")
                            onPlaybackCompleted?.invoke()
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Capture the PREVIOUS song before updating to the new one
                        val previousSong = _currentSong.value
                        updateCurrentFromPlayer()
                        updateNotification()

                        // When auto-advancing (previous song finished naturally),
                        // mark the previous song as fully played so it appears in cache.
                        // STATE_ENDED only fires for the LAST song in a playlist,
                        // so intermediate songs need to be caught here.
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                            if (previousSong != null &&
                                !previousSong.id.startsWith("audiobook_") &&
                                !previousSong.id.startsWith("radio_")) {
                                fullyPlayedSongIds.add(previousSong.id)
                                fullyPlayedPrefs.edit().putStringSet("ids", fullyPlayedSongIds).apply()
                            }
                        }

                        val song = _currentSong.value
                        if (song != null) {
                            scope.launch(Dispatchers.IO) {
                                try { repository?.scrobble(song.id) } catch (_: Exception) {}
                            }
                            onSongAutoAdvanced?.invoke(song)
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        android.util.Log.e("LeChenMusic", "onPlayerError: ${error.errorCodeName} - ${error.message}")
                        // #19: Report playback error to server
                        try {
                            LeChenApp.sendErrorToServer(
                                "error",
                                "Playback error: ${error.errorCodeName} - ${error.message}",
                                error.stackTrace.take(10).joinToString("\n") { "at $it" },
                                "player"
                            )
                        } catch (_: Exception) {}
                        skipNext()
                    }
                })
            }

        createNotificationChannel()

        // MediaSessionCompat for notification lock screen controls
        mediaSessionCompat = MediaSessionCompat(context, "LeChenMusicSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onFastForward() { seekRelative(30000L) }
                override fun onRewind() { seekRelative(-30000L) }
                override fun onStop() { forcePause() }
                override fun onCustomAction(action: String, extras: android.os.Bundle?) {
                    when (action) {
                        "REWIND_30" -> seekRelative(-30000L)
                        "FORWARD_30" -> seekRelative(30000L)
                    }
                }
            })
        }

        MusicPlaybackService.sharedSessionToken = mediaSessionCompat?.sessionToken

        startForegroundService()

        alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_STOP_PLAYBACK -> {
                        timerExpired = true
                        player?.pause()
                        onTimerExpired?.invoke()
                    }
                    ACTION_TOGGLE_FAVORITE -> toggleStar()
                    ACTION_PREV -> skipPrevious()
                    ACTION_NEXT -> skipNext()
                    ACTION_PLAY_PAUSE -> togglePlayPause()
                    ACTION_REWIND_30 -> seekRelative(-30000L)
                    ACTION_FORWARD_30 -> seekRelative(30000L)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_STOP_PLAYBACK)
            addAction(ACTION_TOGGLE_FAVORITE)
            addAction(ACTION_PREV)
            addAction(ACTION_NEXT)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_REWIND_30)
            addAction(ACTION_FORWARD_30)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(alarmReceiver, filter)
        }
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) { }
    }

    private fun initCache() {
        val cacheDir = java.io.File(context.cacheDir, "music_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val evictor = LeastRecentlyUsedCacheEvictor(currentCacheSizeBytes)
        musicCache = SimpleCache(cacheDir, evictor, androidx.media3.database.StandaloneDatabaseProvider(context))
        val okHttpClient = OkHttpClient.Builder().build()
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(musicCache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Update cache max size (called when user changes cache setting) */
    fun updateCacheSize(sizeGb: Int) {
        val newBytes = sizeGb.toLong() * 1024 * 1024 * 1024
        if (newBytes == currentCacheSizeBytes) return
        currentCacheSizeBytes = newBytes
        // Re-create cache with new size
        musicCache?.release()
        val cacheDir = java.io.File(context.cacheDir, "music_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(newBytes)
        musicCache = SimpleCache(cacheDir, evictor, androidx.media3.database.StandaloneDatabaseProvider(context))
        val okHttpClient = OkHttpClient.Builder().build()
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(musicCache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Get current cache size in bytes */
    fun getCacheBytes(): Long {
        return try { musicCache?.cacheSpace ?: 0 } catch (_: Exception) { 0 }
    }

    /** Get song IDs that are fully cached locally AND fully played */
    fun getCachedSongIds(): Set<String> {
        return try {
            val cache = musicCache ?: return emptySet()
            val cachedIds = mutableSetOf<String>()
            for (key in cache.keys) {
                val match = Regex("[?&]id=([^&]+)").find(key)
                if (match != null) {
                    val songId = match.groupValues[1]
                    // #18: Verify the song is fully cached (not partial)
                    try {
                        val cachedBytes = cache.getCachedBytes(key, 0, Long.MAX_VALUE)
                        // Only consider songs with at least 100KB cached (skip tiny/partial)
                        if (cachedBytes > 100 * 1024) {
                            cachedIds.add(songId)
                        }
                    } catch (_: Exception) {
                        // If we can't check bytes, include it if fully played
                        cachedIds.add(songId)
                    }
                }
            }
            // Only return songs that are both in cache AND fully played
            cachedIds.intersect(fullyPlayedSongIds)
        } catch (_: Exception) { emptySet() }
    }

    /** Clear all cached music files */
    fun clearMusicCache() {
        try {
            musicCache?.let { cache ->
                val keys = cache.keys
                for (key in keys) {
                    cache.removeResource(key)
                }
            }
        } catch (_: Exception) {
            // Fallback: delete cache directory
            try {
                val cacheDir = java.io.File(context.cacheDir, "music_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                }
                // Re-init cache after clearing
                musicCache?.release()
                initCache()
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "悦音播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // 播放时长统计
    private fun addPlayTime(seconds: Long) {
        val current = playTimePrefs.getLong("total_play_seconds", 0)
        playTimePrefs.edit().putLong("total_play_seconds", current + seconds).apply()
        _totalPlayMinutes.value = (current + seconds) / 60
        // 异步上报到服务端（粗略统计，每累积60秒上报一次）
        if (seconds >= 60 || (current + seconds) % 60 < seconds) {
            scope.launch(Dispatchers.IO) {
                try {
                    val repo = repository ?: return@launch
                    val song = _currentSong.value
                    repo.reportPlayDuration(
                        itemType = if (song?.id?.startsWith("radio_") == true) "music" else "music",
                        itemId = song?.id ?: "unknown",
                        itemTitle = song?.title ?: "unknown",
                        itemArtist = song?.artist ?: "unknown",
                        duration = seconds.toInt()
                    )
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sessionCompat = mediaSessionCompat ?: return

        val isAudiobook = _audiobookCoverUrl.value != null || song.id.startsWith("audiobook_")
        val currentPositionMs = player?.currentPosition ?: _currentPosition.value
        val isRadio = song.id.startsWith("radio_")
        val currentDurationMs = if (isRadio) 0L else (player?.duration?.takeIf { it > 0 } ?: song.duration * 1000L).coerceAtLeast(0)

        // Update MediaSessionCompat metadata (for lock screen display)
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurationMs)
        sessionCompat.setMetadata(metadataBuilder.build())

        // Build PlaybackStateCompat actions based on content type
        var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE

        if (isAudiobook) {
            // Audiobook: enable fast forward / rewind (±15s) on lock screen
            actions = actions or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_REWIND
        } else {
            // Music: prev/next only, no ±15s on lock screen
            actions = actions or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentPositionMs,
                1.0f
            )

        // Add custom 15s skip actions for audiobook lock screen
        if (isAudiobook) {
            stateBuilder
                .addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder("REWIND_30", "后退30秒", R.drawable.ic_notif_replay_30).build()
                )
                .addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder("FORWARD_30", "前进30秒", R.drawable.ic_notif_forward_30).build()
                )
        }

        sessionCompat.setPlaybackState(stateBuilder.build())

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(ACTION_PREV).setPackage(context.packageName)
        val prevPending = PendingIntent.getBroadcast(
            context, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName)
        val playPausePending = PendingIntent.getBroadcast(
            context, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(ACTION_NEXT).setPackage(context.packageName)
        val nextPending = PendingIntent.getBroadcast(
            context, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favIntent = Intent(ACTION_TOGGLE_FAVORITE).setPackage(context.packageName)
        val favPending = PendingIntent.getBroadcast(
            context, 4, favIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scope.launch(Dispatchers.IO) {
            val albumArt = loadAlbumArt(song.coverArt)

            if (albumArt != null) {
                val metaWithArt = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurationMs)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .build()
                sessionCompat.setMetadata(metaWithArt)
            }

            val playPauseIcon = if (_isPlaying.value) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            val favIcon = if (_isStarred.value) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSubText(song.album)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(_isPlaying.value)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setLargeIcon(albumArt)
                .setStyle(
                    MediaStyle()
                        .setMediaSession(sessionCompat.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )

            if (isAudiobook) {
                // Audiobook: prev, rewind 15s, play/pause, forward 15s, next
                val rewindIntent = Intent(ACTION_REWIND_30).setPackage(context.packageName)
                val rewindPending = PendingIntent.getBroadcast(
                    context, 5, rewindIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val forwardIntent = Intent(ACTION_FORWARD_30).setPackage(context.packageName)
                val forwardPending = PendingIntent.getBroadcast(
                    context, 6, forwardIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_prev, "上一章", prevPending))
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_replay_30, "后退30秒", rewindPending))
                    .addAction(NotificationCompat.Action(playPauseIcon, if (_isPlaying.value) "暂停" else "播放", playPausePending))
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_forward_30, "前进30秒", forwardPending))
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_next, "下一章", nextPending))
                builder.setStyle(
                    MediaStyle()
                        .setMediaSession(sessionCompat.sessionToken)
                        .setShowActionsInCompactView(1, 2, 3)
                )
            } else {
                builder
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_prev, "上一曲", prevPending))
                    .addAction(NotificationCompat.Action(playPauseIcon, if (_isPlaying.value) "暂停" else "播放", playPausePending))
                    .addAction(NotificationCompat.Action(R.drawable.ic_notif_next, "下一曲", nextPending))
                    .addAction(NotificationCompat.Action(favIcon, if (_isStarred.value) "取消收藏" else "收藏", favPending))
                builder.setStyle(
                    MediaStyle()
                        .setMediaSession(sessionCompat.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
            }

            val notification = builder.build()

            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun loadAlbumArt(coverArtId: String?): Bitmap? {
        if (coverArtId.isNullOrBlank()) return null
        return try {
            val repo = repository ?: return null
            val url = repo.getCoverArtUrl(coverArtId) ?: return null
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val size = (128 * context.resources.displayMetrics.density).toInt()
            Bitmap.createScaledBitmap(bitmap, size, size, true)
        } catch (e: Exception) {
            null
        }
    }

    // 存储电台流地址，用于播放列表切换
    private val radioStreamUrls = mutableMapOf<String, String>()

    // 原始播放列表顺序（关闭随机时恢复用）；_playlist 始终是实际播放顺序（随机模式下已打乱）
    private val _playlistBase = MutableStateFlow<List<Song>>(emptyList())

    /**
     * 计算播放顺序（下标排列，指向 _playlistBase 的下标）。
     *
     * 核心约束：**切换随机时绝对不能增删/替换播放器里的媒体项**。
     * media3 的 setShuffleOrder 只重建 Timeline，不碰任何 MediaSource
     * （ExoPlayerImpl#setShuffleOrder -> MediaSourceList#setShuffleOrder -> createTimeline()），
     * 因此结构性不可能造成 rebuffer；而 setMediaItems / replaceMediaItems / moveMediaItems
     * 都要 add+remove MediaSourceHolder，在流媒体上就是一下可听见的中断。
     *
     * @param currentIndex 当前曲目在 _playlistBase 中的下标（-1 表示未知）
     * @param pinIndex 开随机时把当前曲目固定在播放顺序的第几位，
     *   这样翻页位置不动，用户只看到前后歌曲被重排。关随机时忽略，直接返回自然顺序。
     */
    private fun buildPlayPerm(size: Int, currentIndex: Int, pinIndex: Int): IntArray {
        if (size <= 0) return IntArray(0)
        if (!_shuffleMode.value || size == 1 || currentIndex < 0) return IntArray(size) { it }
        val rest = (0 until size).filter { it != currentIndex }.shuffled()
        val at = pinIndex.coerceIn(0, rest.size)
        return (rest.take(at) + currentIndex + rest.drop(at)).toIntArray()
    }

    /** 当前播放顺序 -> _playlistBase 下标排列（喂给 ShuffleOrder） */
    private fun playOrderIndices(): IntArray {
        val base = _playlistBase.value
        val pos = HashMap<String, Int>(base.size * 2)
        for (i in base.indices) pos.putIfAbsent(base[i].id, i)
        return _playlist.value.mapNotNull { s -> pos[s.id] }.toIntArray()
    }

    /**
     * 应用新的播放顺序。**只改导航顺序，不改播放器里的媒体项**，因此绝不会打断当前播放。
     */
    private fun applyPlayOrder(perm: IntArray) {
        val base = _playlistBase.value
        _playlist.value = perm.toList().mapNotNull { b -> base.getOrNull(b) }
        val curBase = base.indexOfFirst { it.id == _currentSong.value?.id }
        val ui = perm.indexOf(curBase)
        _currentIndex.value = if (ui >= 0) ui else 0
        val p = player
        if (p != null && perm.size == base.size && p.mediaItemCount == base.size) {
            p.shuffleModeEnabled = _shuffleMode.value
            p.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(perm, System.nanoTime()))
        }
    }

    /** 构建媒体项列表（电台使用存储的流地址，普通歌曲使用 repository） */
    private fun buildMediaItems(songs: List<Song>): List<MediaItem> {
        return songs.map { s ->
            val url = if (s.id.startsWith("radio_")) {
                radioStreamUrls[s.id] ?: repository!!.getStreamUrl(s.id)
            } else {
                repository!!.getStreamUrl(s.id)
            }
            MediaItem.Builder()
                .setUri(url)
                .setMediaId(s.id)
                .setMediaMetadata(
                    Media3Metadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .build()
                )
                .build()
        }
    }

    fun playSong(song: Song, songs: List<Song> = listOf(song)) {
        _playlistBase.value = songs
        val baseIndex = songs.indexOfFirst { it.id == song.id }
        // 媒体项永远按 _playlistBase 的自然顺序装载；随机只体现在导航顺序上
        val perm = buildPlayPerm(songs.size, baseIndex, baseIndex)
        _playlist.value = perm.toList().mapNotNull { b -> songs.getOrNull(b) }
        _currentIndex.value = perm.indexOf(baseIndex).coerceAtLeast(0)

        player?.apply {
            shuffleModeEnabled = _shuffleMode.value
            setMediaItems(buildMediaItems(songs), baseIndex.coerceAtLeast(0), 0)
            setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(perm, System.nanoTime()))
            prepare()
            play()
        }
        _currentSong.value = song
        // 电台不检查收藏状态（走单独的逻辑）
        if (!song.id.startsWith("radio_")) {
            checkStarred(song.id)
        } else {
            _isStarred.value = false
            scope.launch(Dispatchers.IO) {
                try {
                    val repo = repository ?: return@launch
                    val starredResult = repo.getStarred()
                    if (starredResult.isSuccess) {
                        val starredRadios = starredResult.getOrNull()?.radios ?: emptyList()
                        val radioId = song.id.removePrefix("radio_")
                        _isStarred.value = starredRadios.any { it.id == radioId }
                    }
                } catch (_: Exception) {}
            }
        }
        updateNotification()
    }

    fun togglePlayPause() {
        // User manually pressed play, clear timer expired flag
        timerExpired = false
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                // If playback ended (single song, no repeat), seek to start before playing
                if (it.playbackState == Player.STATE_ENDED) {
                    it.seekTo(0)
                }
                it.play()
            }
        }
    }

    fun forcePause() {
        try {
            player?.let {
                if (it.isPlaying) it.pause()
            }
        } catch (_: Exception) { }
    }

    // Add song to end of current queue (with deduplication)
    fun addToQueue(song: Song) {
        // Check if song already exists in queue
        if (_playlist.value.any { it.id == song.id }) return
        player?.apply {
            addMediaItem(buildMediaItems(listOf(song)).first())
            _playlistBase.value = _playlistBase.value + song
            _playlist.value = _playlist.value + song
            // 新歌排到播放顺序末尾（不打断当前播放）
            shuffleModeEnabled = _shuffleMode.value
            setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(playOrderIndices(), System.nanoTime()))
        }
    }

    fun skipNext() {
        val p = player ?: return
        // seekToNext 跟随 shuffleModeEnabled + ShuffleOrder，即我们的播放顺序 _playlist
        if (p.hasNextMediaItem()) {
            p.seekToNext()
        } else if (_repeatMode.value == RepeatMode.ALL && _playlist.value.isNotEmpty()) {
            playAt(0)
            return
        }
        updateCurrentFromPlayer()
    }

    /**
     * 跳转到播放列表中的指定下标继续播放。
     * 不重建媒体列表（保留随机播放顺序），只做 seek + 立即同步状态。
     */
    fun playAt(index: Int, autoPlay: Boolean = true) {
        if (index !in _playlist.value.indices) return
        val p = player
        // 电台等媒体项与播放列表不一致的场景：重建播放列表播放
        if (p == null || p.mediaItemCount != _playlistBase.value.size) {
            // 队列与播放器内部列表对不上时整队重建后跳转。
            // 这里仍传原始队列顺序，别把「播放顺序」当成队列顺序，否则关闭随机会恢复成打乱后的顺序。
            playSong(_playlist.value[index], _playlistBase.value.ifEmpty { _playlist.value })
            return
        }
        val song = _playlist.value[index]
        _currentIndex.value = index
        _currentSong.value = song
        if (!song.id.startsWith("radio_")) {
            checkStarred(song.id)
        } else {
            _isStarred.value = false
        }
        // 播放器内部是 _playlistBase 的自然顺序，按 mediaId 定位到真正的下标
        val baseIndex = _playlistBase.value.indexOfFirst { it.id == song.id }
        p.seekTo(if (baseIndex >= 0) baseIndex else index, 0)
        if (autoPlay) p.play()
        updateNotification()
    }

    /**
     * 播放页竖向滑动切歌：播放目标页的歌曲（一次到位，不二次跳转）。
     */
    fun playFromSwipe(targetIndex: Int) {
        playAt(targetIndex)
    }

    fun skipPrevious() {
        player?.let {
            if (it.currentPosition > 3000) {
                it.seekTo(0)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            }
        }
        updateCurrentFromPlayer()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun seekToProgress(progress: Float) {
        player?.let {
            val pos = (it.duration * progress).toLong().coerceIn(0, it.duration)
            it.seekTo(pos)
        }
    }

    /** Seek relative to current position (positive = forward, negative = backward) */
    fun seekRelative(deltaMs: Long) {
        player?.let {
            val maxPos = if (it.duration > 0) it.duration else it.currentPosition + deltaMs + 1
            val newPos = (it.currentPosition + deltaMs).coerceIn(0, maxPos)
            it.seekTo(newPos)
        }
    }

    /**
     * 切换随机播放：**只重排导航顺序，一个媒体项都不碰**，
     * 所以结构性不可能打断正在播放的曲目。
     *
     * 开启随机：当前曲目固定在当前翻页位置，其余歌曲打乱（页面不动，只有前后歌曲换了）。
     * 关闭随机：恢复 _playlistBase 的自然顺序，当前曲目回到它在源队列中的位置。
     */
    fun toggleShuffle() {
        _shuffleMode.value = !_shuffleMode.value
        val base = _playlistBase.value.ifEmpty { _playlist.value }
        if (base.isEmpty()) return
        _playlistBase.value = base
        val curBase = base.indexOfFirst { it.id == _currentSong.value?.id }
        applyPlayOrder(buildPlayPerm(base.size, curBase, _currentIndex.value.coerceAtLeast(0)))
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player?.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleStar() {
        val song = _currentSong.value ?: return
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            try {
                // 电台ID需要去掉radio_前缀
                val apiId = if (song.id.startsWith("radio_")) song.id.removePrefix("radio_") else song.id
                val result = if (_isStarred.value) {
                    repo.unstar(apiId)
                } else {
                    repo.star(apiId)
                }
                if (result.isSuccess) {
                    _isStarred.value = !_isStarred.value
                    updateNotification()
                }
            } catch (_: Exception) { }
        }
    }

    private fun checkStarred(songId: String) {
        val song = _currentSong.value
        _isStarred.value = song?.isStarred == true
    }

    fun setTimer(minutes: Int) {
        cancelTimer()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_STOP_PLAYBACK)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L
        // setAlarmClock：精确定时，不受 Doze/厂商后台限制，后台播放也能准时停止
        // （普通 set() 是非精确闹钟，后台会被系统批量推迟）
        val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)
    }

    fun cancelTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_STOP_PLAYBACK)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        timerJob?.cancel()
        timerJob = null
        timerExpired = false
    }

    fun clearTimerExpired() {
        timerExpired = false
    }

    private fun updateCurrentFromPlayer() {
        val p = player ?: return
        val baseIdx = p.currentMediaItemIndex
        val base = _playlistBase.value
        val song = base.getOrNull(baseIdx) ?: _playlist.value.getOrNull(baseIdx) ?: return
        _currentSong.value = song
        val ui = _playlist.value.indexOfFirst { it.id == song.id }
        _currentIndex.value = if (ui >= 0) ui else baseIdx
        checkStarred(song.id)
    }

    fun updateProgress() {
        player?.let {
            _currentPosition.value = it.currentPosition
            // 电台流媒体：强制时长为0，防止ExoPlayer报告异常大数值
            val isRadio = _currentSong.value?.id?.startsWith("radio_") == true
            val rawDuration = if (isRadio) 0L else it.duration.coerceAtLeast(0)
            _duration.value = rawDuration
            _progress.value = if (rawDuration > 0) it.currentPosition.toFloat() / rawDuration else 0f
        }
    }

    /**
     * Lightweight lock screen position update.
     * Called periodically to keep the lock screen progress bar and time in sync.
     * Does NOT reload album art (use updateNotification() for full refresh).
     */
    fun updateLockScreenPosition() {
        val sessionCompat = mediaSessionCompat ?: return
        val song = _currentSong.value ?: return
        val isAudiobook = _audiobookCoverUrl.value != null || song.id.startsWith("audiobook_")
        val currentPositionMs = player?.currentPosition ?: _currentPosition.value
        val isRadio = song.id.startsWith("radio_")
        val currentDurationMs = if (isRadio) 0L else (player?.duration?.takeIf { it > 0 } ?: song.duration * 1000L).coerceAtLeast(0)

        // Update metadata duration if it changed significantly
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurationMs)
        sessionCompat.setMetadata(metadataBuilder.build())

        // Update playback state with current position
        var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
        if (isAudiobook) {
            actions = actions or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_REWIND
        } else {
            actions = actions or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentPositionMs,
                1.0f
            )

        // Add custom 15s skip actions for audiobook lock screen
        if (isAudiobook) {
            stateBuilder
                .addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder("REWIND_30", "后退30秒", R.drawable.ic_notif_replay_30).build()
                )
                .addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder("FORWARD_30", "前进30秒", R.drawable.ic_notif_forward_30).build()
                )
        }

        sessionCompat.setPlaybackState(stateBuilder.build())
    }

    /**
     * Get current position directly from ExoPlayer (most accurate).
     * Use this for lyrics sync and other time-critical displays.
     */
    fun getExactPosition(): Long {
        return player?.currentPosition ?: _currentPosition.value
    }

    /**
     * Get current duration directly from ExoPlayer.
     */
    fun getExactDuration(): Long {
        return player?.duration?.coerceAtLeast(0) ?: _duration.value
    }

    fun release() {
        // 保存未结算的播放时长
        if (playStartTimeMs > 0) {
            val elapsed = (System.currentTimeMillis() - playStartTimeMs) / 1000
            playStartTimeMs = 0
            if (elapsed > 0) addPlayTime(elapsed)
        }
        alarmReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) { }
        }
        mediaSessionCompat?.let {
            it.isActive = false
            it.release()
        }
        mediaSessionCompat = null
        player?.release()
        player = null
        musicCache?.release()
        musicCache = null
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
        } catch (_: Exception) { }
    }

    fun playRadioStation(station: com.lechenmusic.data.model.InternetRadioStation, allStations: List<com.lechenmusic.data.model.InternetRadioStation> = emptyList()) {
        // 清除有声书状态，避免MiniPlayer/PlayerPage显示有声书内容
        _audiobookCoverUrl.value = null
        // 先存储所有电台流地址（buildMediaItems 依赖）
        radioStreamUrls.clear()
        radioStreamUrls["radio_${station.id}"] = station.streamUrl
        allStations.forEach { s -> radioStreamUrls["radio_${s.id}"] = s.streamUrl }

        // 设置播放列表为所有电台，支持上下滑动切换（只传单个电台时列表就一首）
        val stations = if (allStations.isNotEmpty()) allStations else listOf(station)
        val songs = stations.map { s ->
            com.lechenmusic.data.model.Song(
                id = "radio_${s.id}",
                title = s.name,
                artist = "电台",
                album = "电台",
                duration = 0,
                coverArt = s.coverArt
            )
        }
        _playlistBase.value = songs
        _playlist.value = songs
        val index = stations.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        _currentIndex.value = index
        _currentSong.value = songs[index]

        player?.apply {
            shuffleModeEnabled = false
            setMediaItems(buildMediaItems(songs), index, 0)
            prepare()
            play()
        }
        _isStarred.value = false
        updateNotification()
        // 异步检查收藏状态
        scope.launch(Dispatchers.IO) {
            try {
                val repo = repository ?: return@launch
                val starredResult = repo.getStarred()
                if (starredResult.isSuccess) {
                    val starredRadios = starredResult.getOrNull()?.radios ?: emptyList()
                    _isStarred.value = starredRadios.any { it.id == station.id }
                    updateNotification()
                }
            } catch (_: Exception) {}
        }
    }

    // ===== Audiobook playback =====
    private val _audiobookCoverUrl = MutableStateFlow<String?>(null)
    val audiobookCoverUrl: StateFlow<String?> = _audiobookCoverUrl.asStateFlow()

    // Pending seek position (ms) for resume playback.
    // Set when playUrl is called with initialSeekMs > 0, consumed when ExoPlayer reaches STATE_READY.
    @Volatile
    private var pendingSeekMs: Long = 0L

    fun playUrl(url: String, title: String, artist: String, mediaId: String, coverUrl: String? = null, initialSeekMs: Long = 0) {
        // Create a virtual Song for UI display
        _currentSong.value = Song(
            id = mediaId,
            title = title,
            artist = artist,
            album = "有声书",
            duration = 0
        )
        _audiobookCoverUrl.value = coverUrl
        _playlist.value = emptyList()
        _playlistBase.value = emptyList()
        _currentIndex.value = 0
        _isStarred.value = false
        pendingSeekMs = initialSeekMs

        player?.apply {
            // Reset player state before loading new media
            stop()
            clearMediaItems()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(mediaId)
                .setMediaMetadata(
                    Media3Metadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle("有声书")
                        .build()
                )
                .build()
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        updateNotification()
    }

    fun clearAudiobookCoverUrl() {
        _audiobookCoverUrl.value = null
    }

}
