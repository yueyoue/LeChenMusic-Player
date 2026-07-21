package com.lechenmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.components.MiniPlayer
import com.lechenmusic.ui.navi.Screen
import com.lechenmusic.ui.screens.albums.AlbumDetailScreen
import com.lechenmusic.ui.screens.albums.AlbumsScreen
import com.lechenmusic.ui.screens.artists.ArtistDetailScreen
import com.lechenmusic.ui.screens.artists.ArtistsScreen
import com.lechenmusic.ui.screens.favorites.FavoritesScreen
import com.lechenmusic.ui.screens.home.HomeScreen
import com.lechenmusic.ui.screens.home.PlaylistDetailScreen
import com.lechenmusic.ui.screens.home.RadioScreen
import com.lechenmusic.ui.screens.login.LoginScreen
import com.lechenmusic.ui.screens.player.PlayerScreen
import com.lechenmusic.ui.screens.recent.RecentPlayedScreen
import com.lechenmusic.ui.screens.search.SearchScreen
import com.lechenmusic.ui.screens.settings.SettingsScreen
import com.lechenmusic.ui.screens.songs.AllSongsScreen
import com.lechenmusic.ui.screens.home.AllPlaylistsScreen
import com.lechenmusic.ui.screens.home.CachedMusicScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookDetailScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookPlayerScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookNarratorListScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookNarratorDetailScreen
import com.lechenmusic.ui.theme.LeChenMusicTheme
import com.lechenmusic.update.UpdateInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val videoViewModel: VideoViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = themeMode == "dark"

            LeChenMusicTheme(darkTheme = isDark) {
                // 更新弹窗（启动时自动检查 + 设置页手动检查 都会触发）
                val updateInfo by viewModel.updateInfo.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                UpdateDialog(
                    updateInfo = updateInfo,
                    updateStatus = updateStatus,
                    onDismiss = { viewModel.dismissUpdate() },
                    onUpdate = { viewModel.downloadUpdate() },
                    onSkip = { viewModel.skipUpdate() }
                )

                LeChenMusicApp(viewModel, videoViewModel)
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo?,
    updateStatus: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onSkip: () -> Unit = onDismiss
) {
    if (updateInfo == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                "发现新版本 v${updateInfo.versionName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (updateInfo.updateLog.isNotEmpty()) {
                    Text(
                        updateInfo.updateLog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (updateStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        updateStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = updateStatus.isEmpty() || updateStatus == "下载失败，请手动下载"
            ) {
                Text("立即更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("跳过该版本")
            }
        }
    )
}

@Composable
fun LeChenMusicApp(viewModel: MainViewModel, videoViewModel: VideoViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentSong by viewModel.playerManager.currentSong.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    data class BottomTab(val route: String, val label: String, val icon: ImageVector)
    val tabs = listOf(
        BottomTab(Screen.Home.route, "首页", Icons.Default.Home),
        BottomTab(Screen.Favorites.route, "收藏", Icons.Default.Favorite),
        BottomTab(Screen.AllSongs.route, "乐库", Icons.Default.MusicNote),
        BottomTab(Screen.Settings.route, "我的", Icons.Default.Person)
    )

    val showBottomBar = currentRoute in tabs.map { it.route }

    // 登录成功后自动检查更新（静默）
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.checkForUpdate(silent = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoggedIn) {
            LoginScreen(viewModel = viewModel, onLoginSuccess = { })
        } else {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar || (currentSong != null && currentRoute != Screen.Player.route && currentRoute != Screen.AudiobookPlayer.route),
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Column {
                            if (currentSong != null && currentRoute != Screen.Player.route && currentRoute != Screen.AudiobookPlayer.route) {
                                val currentBook by viewModel.currentAudiobook.collectAsState()
                                val audiobookCoverUrl by viewModel.playerManager.audiobookCoverUrl.collectAsState()
                                // 判断是否在播放有声书
                                val isAudiobookPlaying = currentBook != null || audiobookCoverUrl != null || (currentSong?.id?.startsWith("audiobook_") == true)
                                MiniPlayer(
                                    playerManager = viewModel.playerManager,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    audiobookCoverUrl = if (isAudiobookPlaying) audiobookCoverUrl else null,
                                    onClick = {
                                        if (isAudiobookPlaying) {
                                            navController.navigate(Screen.AudiobookPlayer.route)
                                        } else {
                                            navController.navigate(Screen.Player.route)
                                        }
                                    }
                                )
                            }
                            if (showBottomBar) {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        NavigationBarItem(
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, fontSize = 10.sp) },
                                            selected = currentRoute == tab.route,
                                            onClick = {
                                                if (currentRoute == tab.route) return@NavigationBarItem
                                                navController.navigate(tab.route) {
                                                    popUpTo(Screen.Home.route) { saveState = false }
                                                    launchSingleTop = true
                                                    restoreState = false
                                                }
                                                if (tab.route == Screen.Home.route) {
                                                    viewModel.loadHomeData()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) },
                            onSettingsClick = { navController.navigate(Screen.Settings.route) },
                            onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                            onNavigateToAllSongs = { navController.navigate(Screen.AllSongs.route) },
                            onNavigateToRecentPlayed = { navController.navigate(Screen.RecentPlayed.route) },
                            onNavigateToRadio = { navController.navigate(Screen.Radio.route) },
                            onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                            onNavigateToArtists = { navController.navigate(Screen.Artists.route) },
                            onNavigateToAllPlaylists = { navController.navigate(Screen.AllPlaylists.route) },
                            onNavigateToCachedMusic = { navController.navigate(Screen.CachedMusic.route) },
                            onNavigateToAudiobook = { genre -> navController.navigate(Screen.Audiobook.createRoute(genre)) },
                            onNavigateToAudiobookDetail = { id -> navController.navigate(Screen.AudiobookDetail.createRoute(id)) },
                            onNavigateToNarrator = { name -> navController.navigate(Screen.NarratorDetail.createRoute(name)) },
                            onNavigateToNarratorList = { navController.navigate(Screen.NarratorList.route) },
                            onNavigateToVideoDetail = { source, videoId -> navController.navigate(Screen.VideoDetail.createRoute(source, videoId)) },
                            onNavigateToVideoPlayer = { navController.navigate(Screen.VideoPlayerDirect.route) },
                            onNavigateToVideoCategory = { type -> navController.navigate(Screen.VideoCategory.createRoute(type)) },
                            onNavigateToLive = { navController.navigate(Screen.Live.route) },
                            videoViewModel = videoViewModel
                        )
                    }
                    composable(Screen.Favorites.route) {
                        FavoritesScreen(
                            viewModel = viewModel,
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) },
                            onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) },
                            onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
                        )
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = viewModel,
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) }
                        )
                    }
                    composable(Screen.Artists.route) {
                        ArtistsScreen(
                            viewModel = viewModel,
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) }
                        )
                    }
                    composable(Screen.Albums.route) {
                        AlbumsScreen(
                            viewModel = viewModel,
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.AllSongs.route) {
                        AllSongsScreen(
                            viewModel = viewModel,
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.AllPlaylists.route) {
                        AllPlaylistsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
                        )
                    }
                    composable(Screen.CachedMusic.route) {
                        CachedMusicScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.RecentPlayed.route) {
                        RecentPlayedScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            videoViewModel = videoViewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onLogout = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Player.route) {
                        PlayerScreen(
                            playerManager = viewModel.playerManager,
                            viewModel = viewModel,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onBack = { navController.popBackStack() },
                            onShowPlaylist = { },
                            onShowMore = { },
                            onNavigateToArtist = { artistId ->
                                navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                            },
                            onNavigateToAlbum = { albumId ->
                                navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                            }
                        )
                    }
                    composable(Screen.AlbumDetail.route) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                        AlbumDetailScreen(
                            viewModel = viewModel,
                            albumId = albumId,
                            onBack = { navController.popBackStack() },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.ArtistDetail.route) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                        ArtistDetailScreen(
                            viewModel = viewModel,
                            artistId = artistId,
                            onBack = { navController.popBackStack() },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) }
                        )
                    }
                    composable(Screen.PlaylistDetail.route) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        PlaylistDetailScreen(
                            viewModel = viewModel,
                            playlistId = playlistId,
                            onBack = { navController.popBackStack() },
                            onSongClick = { s: com.lechenmusic.data.model.Song, p: List<com.lechenmusic.data.model.Song> -> viewModel.playSong(s, p) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { aId -> navController.navigate(Screen.AlbumDetail.createRoute(aId)) }
                        )
                    }
                    composable(Screen.Radio.route) {
                        RadioScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Audiobook.route) { backStackEntry ->
                        val genre = backStackEntry.arguments?.getString("genre")
                        AudiobookScreen(
                            viewModel = viewModel,
                            genreFilter = genre,
                            onBack = { navController.popBackStack() },
                            onAudiobookClick = { id -> navController.navigate(Screen.AudiobookDetail.createRoute(id)) }
                        )
                    }
                    composable(Screen.AudiobookDetail.route) { backStackEntry ->
                        val audiobookId = backStackEntry.arguments?.getString("audiobookId") ?: ""
                        AudiobookDetailScreen(
                            viewModel = viewModel,
                            audiobookId = audiobookId,
                            onBack = { navController.popBackStack() },
                            onPlayChapter = { book, chapter, chapters ->
                                viewModel.playAudiobookChapter(book, chapter, chapters)
                                // Don't navigate - let mini player handle it
                            }
                        )
                    }
                    composable(Screen.NarratorList.route) {
                        AudiobookNarratorListScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNarratorClick = { name -> navController.navigate(Screen.NarratorDetail.createRoute(name)) }
                        )
                    }
                    composable(Screen.NarratorDetail.route) { backStackEntry ->
                        val narratorName = backStackEntry.arguments?.getString("narratorName") ?: ""
                        AudiobookNarratorDetailScreen(
                            viewModel = viewModel,
                            narratorName = narratorName,
                            onBack = { navController.popBackStack() },
                            onBookClick = { id -> navController.navigate(Screen.AudiobookDetail.createRoute(id)) }
                        )
                    }
                    composable(Screen.AudiobookPlayer.route) {
                        val currentBook by viewModel.currentAudiobook.collectAsState()
                        val chapters by viewModel.currentAudiobookChapters.collectAsState()
                        val chapterIndex by viewModel.currentChapterIndex.collectAsState()
                        val isPlaying by viewModel.audiobookIsPlaying.collectAsState()
                        val position by viewModel.audiobookPosition.collectAsState()
                        val duration by viewModel.audiobookDuration.collectAsState()
                        val serverUrl by viewModel.serverUrl.collectAsState()
                        val username by viewModel.username.collectAsState()
                        val password by viewModel.password.collectAsState()
                        val audiobookCoverUrl by viewModel.playerManager.audiobookCoverUrl.collectAsState()
                        val playbackSpeed by viewModel.audiobookPlaybackSpeed.collectAsState()
                        val timerMinutes by viewModel.audiobookTimerMinutes.collectAsState()
                        // 如果 currentBook 为 null 但有有声书封面，说明是从通知栏进入，尝试恢复状态
                        if (currentBook == null && audiobookCoverUrl != null) {
                            // 从歌曲ID中提取bookId，尝试加载
                            val currentSong by viewModel.playerManager.currentSong.collectAsState()
                            val bookId = currentSong?.id?.removePrefix("audiobook_")?.substringBefore("_")
                            if (bookId != null) {
                                LaunchedEffect(bookId) {
                                    viewModel.loadAudiobookDetail(bookId)
                                }
                            }
                            // 显示通用播放器作为临时回退
                            PlayerScreen(
                                playerManager = viewModel.playerManager,
                                viewModel = viewModel,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onBack = { navController.popBackStack() },
                                onShowPlaylist = {},
                                onShowMore = {},
                                onNavigateToArtist = {},
                                onNavigateToAlbum = {}
                            )
                        } else if (currentBook != null) {
                            AudiobookPlayerScreen(
                                book = currentBook!!,
                                chapters = chapters,
                                currentChapterIndex = chapterIndex,
                                isPlaying = isPlaying,
                                currentPositionMs = position,
                                durationMs = duration,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                coverUrl = audiobookCoverUrl,
                                playbackSpeed = playbackSpeed,
                                timerMinutes = timerMinutes,
                                onBack = { navController.popBackStack() },
                                onPlayPause = { viewModel.audiobookTogglePlayPause() },
                                onSeekTo = { viewModel.audiobookSeekTo(it) },
                                onSkipForward15s = { viewModel.audiobookSkipForward15s() },
                                onSkipBackward15s = { viewModel.audiobookSkipBackward15s() },
                                onPreviousChapter = { viewModel.audiobookPreviousChapter() },
                                onNextChapter = { viewModel.audiobookNextChapter() },
                                onChapterSelect = { viewModel.playAudiobookChapter(currentBook!!, chapters[it], chapters) },
                                onSetTimer = { viewModel.audiobookSetTimer(it) },
                                onChangeSpeed = { viewModel.audiobookChangeSpeed(it) },
                                onSaveProgress = { viewModel.saveAudiobookProgress() }
                            )
                        }
                    }

                    // ===== 影视模块路由 =====
                    composable(Screen.VideoSearch.route) {
                        com.lechenmusic.ui.screens.video.VideoSearchScreen(
                            viewModel = videoViewModel,
                            onBack = { navController.popBackStack() },
                            onVideoClick = { video ->
                                navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                            }
                        )
                    }
                    composable(Screen.VideoDetail.route) { backStackEntry ->
                        val source = backStackEntry.arguments?.getString("source") ?: ""
                        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                        val detail by videoViewModel.videoDetail.collectAsState()
                        com.lechenmusic.ui.screens.video.VideoDetailScreen(
                            viewModel = videoViewModel,
                            source = source,
                            videoId = videoId,
                            onBack = { navController.popBackStack() },
                            onPlay = { playSource, episodeIndex ->
                                navController.navigate(
                                    Screen.VideoPlayer.createRoute(
                                        detail?.title ?: "影视播放",
                                        playSource,
                                        episodeIndex
                                    )
                                )
                            },
                            onVideoClick = { video ->
                                navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                            }
                        )
                    }
                    composable(Screen.VideoPlayer.route) { backStackEntry ->
                        val videoTitle = backStackEntry.arguments?.getString("videoTitle") ?: "影视播放"
                        val source = backStackEntry.arguments?.getString("source") ?: ""
                        val episodeIndex = backStackEntry.arguments?.getString("episodeIndex")?.toIntOrNull() ?: 0
                        val detail by videoViewModel.videoDetail.collectAsState()
                        val detailLoading by videoViewModel.detailLoading.collectAsState()
                        val sources = detail?.toSources() ?: emptyList()
                        val sourceIndex = sources.indexOfFirst { it.source == source }.coerceAtLeast(0)

                        if (detailLoading || sources.isEmpty()) {
                            // 加载中或无数据时显示加载状态
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        if (detailLoading) "正在加载播放源..." else "未找到播放资源",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = { navController.popBackStack() }) {
                                        Text("返回", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            var playerError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Exception?>(null) }
                            if (playerError != null) {
                                videoViewModel.reportVideoError("VideoPlayer", "播放器初始化崩溃", playerError)
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("播放器初始化失败", color = Color.White, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(playerError?.message ?: "", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TextButton(onClick = { navController.popBackStack() }) {
                                            Text("返回", color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                com.lechenmusic.ui.screens.video.VideoPlayerScreen(
                                    videoTitle = videoTitle,
                                    sources = sources,
                                    initialSource = sourceIndex,
                                    initialEpisode = episodeIndex,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                    // 直接播放路由（搜索结果直接播放，无需参数）
                    composable(Screen.VideoPlayerDirect.route) {
                        val detail by videoViewModel.videoDetail.collectAsState()
                        val detailLoading by videoViewModel.detailLoading.collectAsState()
                        val sources = detail?.toSources() ?: emptyList()

                        if (detailLoading || sources.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        if (detailLoading) "正在搜索播放源..." else "未找到播放资源",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = { navController.popBackStack() }) {
                                        Text("返回", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            var playerError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Exception?>(null) }
                            if (playerError != null) {
                                videoViewModel.reportVideoError("VideoPlayerDirect", "播放器初始化崩溃", playerError)
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("播放器初始化失败", color = Color.White, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(playerError?.message ?: "", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TextButton(onClick = { navController.popBackStack() }) {
                                            Text("返回", color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                com.lechenmusic.ui.screens.video.VideoPlayerScreen(
                                    videoTitle = detail?.title ?: "影视播放",
                                    sources = sources,
                                    initialSource = 0,
                                    initialEpisode = 0,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    composable(Screen.Live.route) {
                        com.lechenmusic.ui.screens.video.LiveScreen(
                            viewModel = videoViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.VideoCategory.route) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: "movie"
                        // searchAndPlay 完成后跳转详情页
                        val navigateToDetail by videoViewModel.navigateToDetail.collectAsState()
                        val searchDetail by videoViewModel.videoDetail.collectAsState()
                        androidx.compose.runtime.LaunchedEffect(navigateToDetail) {
                            if (navigateToDetail && searchDetail != null) {
                                videoViewModel.consumeNavigateToDetail()
                                navController.navigate(Screen.VideoDetail.createRoute(searchDetail!!.source, searchDetail!!.id))
                            }
                        }
                        com.lechenmusic.ui.screens.video.VideoCategoryScreen(
                            viewModel = videoViewModel,
                            categoryType = type,
                            onBack = { navController.popBackStack() },
                            onVideoClick = { video ->
                                // 统一走 searchAndPlay：构造 VideoDetail 后导航
                                videoViewModel.searchAndPlay(video.title, video.id)
                            }
                        )
                    }

                }
            }
        }
    }
}
