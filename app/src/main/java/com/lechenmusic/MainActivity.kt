package com.lechenmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
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
import com.lechenmusic.ui.screens.player.TabletPlayerScreen
import com.lechenmusic.ui.screens.player.music.MusicPlayerContent
import com.lechenmusic.ui.screens.recent.RecentPlayedScreen
import com.lechenmusic.ui.screens.search.SearchScreen
import com.lechenmusic.ui.screens.settings.SettingsScreen
import com.lechenmusic.ui.screens.songs.AllSongsScreen
import com.lechenmusic.ui.screens.home.AllPlaylistsScreen
import com.lechenmusic.ui.screens.home.CachedMusicScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookDetailScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookPlayerScreen
import com.lechenmusic.ui.screens.audiobook.TabletAudiobookPlayerScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookNarratorListScreen
import com.lechenmusic.ui.screens.audiobook.AudiobookNarratorDetailScreen
import com.lechenmusic.ui.screens.video.VideoSearchScreen
import com.lechenmusic.ui.screens.video.VideoDetailScreen
import com.lechenmusic.ui.screens.video.VideoPlayerScreen
import com.lechenmusic.ui.screens.video.VideoCategoryScreen
import com.lechenmusic.ui.screens.video.LiveScreen
import com.lechenmusic.ui.theme.LeChenMusicTheme
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.responsive.rememberResponsiveConfig
import com.lechenmusic.update.UpdateInfo

class MainActivity : ComponentActivity() {

    /**
     * 进入沉浸式全屏（隐藏状态栏 + 导航栏）
     */
    fun enterImmersiveFullscreen() {
        val window = window
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 使用 WindowInsetsControllerCompat 隐藏所有系统栏
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // 同时使用旧 API 确保兼容性
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    /**
     * 退出沉浸式全屏（恢复系统栏）
     */
    fun exitImmersiveFullscreen() {
        val window = window
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val videoViewModel: VideoViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = themeMode == "dark"

            // Resume timer countdown when app comes to foreground
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            var timerRestored by remember { mutableStateOf(false) }
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.resumeCountdownIfNeeded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                // Restore timer on first composition
                if (!timerRestored) {
                    timerRestored = true
                    viewModel.restoreTimerIfNeeded()
                }
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

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

    // 从视频播放器返回首页时刷新最近播放记录
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Home.route || currentRoute == Screen.Video.route) {
            videoViewModel.loadPlayRecords()
        }
    }

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
            @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
            val windowSizeClass = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(
                LocalContext.current as android.app.Activity
            )
            val responsiveConfig = rememberResponsiveConfig(windowSizeClass)
            val useSideNav = responsiveConfig.useRailNav
            val showMiniPlayer = currentSong != null && currentRoute != Screen.Player.route && currentRoute != Screen.AudiobookPlayer.route && currentRoute != Screen.VideoPlayerDirect.route && currentRoute != Screen.VideoPlayer.route && currentRoute?.contains("video_detail") != true && currentRoute?.contains("live") != true

            // MiniPlayer 组件（复用）
            @Composable
            fun MiniPlayerBar(modifier: Modifier = Modifier) {
                if (showMiniPlayer) {
                    val currentBook by viewModel.currentAudiobook.collectAsState()
                    val audiobookCoverUrl by viewModel.playerManager.audiobookCoverUrl.collectAsState()
                    val isAudiobookPlaying = currentBook != null || audiobookCoverUrl != null || (currentSong?.id?.startsWith("audiobook_") == true)
                    MiniPlayer(
                        playerManager = viewModel.playerManager,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        audiobookCoverUrl = if (isAudiobookPlaying) audiobookCoverUrl else null,
                        isAudiobook = isAudiobookPlaying,
                        modifier = modifier,
                        onClick = {
                            if (isAudiobookPlaying) navController.navigate(Screen.AudiobookPlayer.route)
                            else navController.navigate(Screen.Player.route)
                        },
                        onDismiss = { viewModel.playerManager.forcePause() }
                    )
                }
            }

            // 导航点击处理
            val onNavClick: (String) -> Unit = { route ->
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                    if (route == Screen.Home.route) viewModel.loadHomeData()
                }
            }

            // 播放页隐藏左侧导航栏
            val hideSideNav = currentRoute == Screen.Player.route || currentRoute == Screen.AudiobookPlayer.route || currentRoute == Screen.VideoPlayerDirect.route || currentRoute == Screen.VideoPlayer.route

            if (useSideNav) {
                // ===== 平板/车机: 左侧导航栏 + 内容区 =====
                Row(modifier = Modifier.fillMaxSize()) {
                    // 左侧 NavigationRail（播放页隐藏）
                    if (!hideSideNav) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(64.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 音乐/有声书/影视模式按钮
                        data class ModeItem(val icon: ImageVector, val label: String, val mode: String)
                        val modeItems = listOf(
                            ModeItem(Icons.Default.Headphones, "听歌", "music"),
                            ModeItem(Icons.Default.MenuBook, "听书", "audiobook"),
                            ModeItem(Icons.Default.LocalMovies, "电影", "video"),
                            ModeItem(Icons.Default.LiveTv, "电视", "live")
                        )
                        val currentHomeMode by viewModel.homeMode.collectAsState()
                        modeItems.forEach { item ->
                            val active = currentRoute == Screen.Home.route && (
                                (item.mode == "music" && currentHomeMode == "music") ||
                                (item.mode == "audiobook" && currentHomeMode == "audiobook") ||
                                (item.mode == "video" && currentHomeMode == "video") ||
                                (item.mode == "live" && false)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        when (item.mode) {
                                            "live" -> {
                                                onNavClick(Screen.Home.route)
                                                navController.navigate(Screen.Live.route)
                                            }
                                            else -> {
                                                if (currentRoute != Screen.Home.route) {
                                                    onNavClick(Screen.Home.route)
                                                }
                                                viewModel.setHomeMode(item.mode)
                                                if (item.mode == "audiobook") viewModel.loadAudiobooks()
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    item.icon, item.label,
                                    tint = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(item.label, fontSize = 10.sp,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 搜索
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onNavClick(Screen.Search.route) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Search, "搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text("搜索", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 收藏
                        val favSelected = currentRoute == Screen.Favorites.route
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onNavClick(Screen.Favorites.route) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite, "收藏",
                                tint = if (favSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text("收藏", fontSize = 10.sp,
                                color = if (favSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (favSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 我的
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onNavClick(Screen.Settings.route) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Person, "我的",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text("我的", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    } // end if (!hideSideNav)
                    // 右侧内容
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // 播放页不应用 innerPadding，避免沉浸模式下顶部残留间隙
                        val applyPadding = currentRoute != Screen.Player.route && currentRoute != Screen.AudiobookPlayer.route && currentRoute != Screen.VideoPlayerDirect.route && currentRoute != Screen.VideoPlayer.route
                        Scaffold { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                modifier = if (applyPadding) Modifier.fillMaxSize().padding(innerPadding) else Modifier.fillMaxSize(),
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
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
                                        onNavigateToVideoSearch = { navController.navigate(Screen.VideoSearch.route) },
                                        onNavigateToLive = { navController.navigate(Screen.Live.route) },
                                        videoViewModel = videoViewModel,
                                        responsiveConfig = responsiveConfig
                                    )
                                }
                                sharedNavRoutes(navController, viewModel, videoViewModel, windowSizeClass)
                            }
                        }
                        // 平板模式: MiniPlayer 在右下角
                        MiniPlayerBar(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            } else {
                // ===== 手机: 标准 Scaffold + 底部导航栏 + 浮动播放器 =====
                Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                tabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { onNavClick(tab.route) },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
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
                                onNavigateToVideoSearch = { navController.navigate(Screen.VideoSearch.route) },
                                onNavigateToLive = { navController.navigate(Screen.Live.route) },
                                videoViewModel = videoViewModel,
                                responsiveConfig = responsiveConfig
                            )
                        }
                        sharedNavRoutes(navController, viewModel, videoViewModel, windowSizeClass)
                    }
                }
                // MiniPlayer 浮动在 Scaffold 之上，背景透明
                MiniPlayerBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (showBottomBar) 96.dp else 16.dp)
                )
                } // Box
            }
        }
    }
}

// ===== 共享导航路由（平板/手机复用） =====
fun NavGraphBuilder.sharedNavRoutes(
    navController: NavHostController,
    viewModel: MainViewModel,
    videoViewModel: VideoViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val onBack: () -> Unit = { navController.popBackStack() }

    composable(Screen.Favorites.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.favorites.TabletFavoritesScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onSongClick = { s, p -> viewModel.playSong(s, p) },
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) },
                onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
            )
        } else {
            FavoritesScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onSongClick = { s, p -> viewModel.playSong(s, p) },
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) },
                onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.Search.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        SearchScreen(
            viewModel = viewModel,
            videoViewModel = videoViewModel,
            responsiveConfig = responsiveCfg,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) },
            onVideoClick = { source, videoId -> navController.navigate(Screen.VideoDetail.createRoute(source, videoId)) }
        )
    }

    composable(Screen.Artists.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.artists.TabletArtistsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
            )
        } else {
            ArtistsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.Albums.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.albums.TabletAlbumsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        } else {
            AlbumsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.AllSongs.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        AllSongsScreen(
            viewModel = viewModel,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
            onBack = if (responsiveCfg.isMedium || responsiveCfg.isExpanded) onBack else null,
            responsiveConfig = if (responsiveCfg.isMedium || responsiveCfg.isExpanded) responsiveCfg else null
        )
    }

    composable(Screen.AllPlaylists.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.home.TabletAllPlaylistsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
            )
        } else {
            AllPlaylistsScreen(
                viewModel = viewModel,
                onBack = onBack,
                onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.CachedMusic.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.home.TabletCachedMusicScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onSongClick = { s, p -> viewModel.playSong(s, p) },
                onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        } else {
            CachedMusicScreen(
                viewModel = viewModel,
                onBack = onBack,
                onSongClick = { s, p -> viewModel.playSong(s, p) },
                onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.RecentPlayed.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        RecentPlayedScreen(
            viewModel = viewModel,
            responsiveConfig = responsiveCfg,
            onBack = onBack,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
        )
    }

    composable(Screen.Settings.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.settings.TabletSettingsScreen(
                viewModel = viewModel,
                videoViewModel = videoViewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        } else {
            SettingsScreen(
                viewModel = viewModel,
                videoViewModel = videoViewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    composable(Screen.Player.route) {
        val srvUrl by viewModel.serverUrl.collectAsState()
        val usr by viewModel.username.collectAsState()
        val pwd by viewModel.password.collectAsState()
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            // 平板全屏播放器：隐藏系统栏 + 保持屏幕常亮
            val context = LocalContext.current
            DisposableEffect(Unit) {
                val activity = context as? android.app.Activity
                val mainActivity = context as? com.lechenmusic.MainActivity
                activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                mainActivity?.enterImmersiveFullscreen()
                onDispose {
                    activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    mainActivity?.exitImmersiveFullscreen()
                }
            }
            var showAddToPlaylistDialog by remember { mutableStateOf(false) }
            var showQueueDialog by remember { mutableStateOf(false) }
            val musicTimerMinutes by viewModel.musicTimerMinutes.collectAsState()
            val musicTimerRemaining by viewModel.timerRemainingSeconds.collectAsState()

            MusicPlayerContent(
                config = responsiveCfg,
                playerManager = viewModel.playerManager,
                viewModel = viewModel,
                serverUrl = srvUrl,
                username = usr,
                password = pwd,
                onBack = onBack,
                onNavigateToArtist = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onShowAddToPlaylist = { showAddToPlaylistDialog = true },
                onShowQueue = { showQueueDialog = true },
                timerMinutes = musicTimerMinutes,
                timerRemainingSeconds = musicTimerRemaining,
                onSetTimer = { viewModel.musicSetTimer(it) }
            )

            // 添加到歌单弹窗
            if (showAddToPlaylistDialog) {
                val playlists by viewModel.playlists.collectAsState()
                val currentSong by viewModel.playerManager.currentSong.collectAsState()
                AlertDialog(
                    onDismissRequest = { showAddToPlaylistDialog = false },
                    title = { Text("添加到歌单") },
                    text = {
                        Column {
                            if (playlists.isEmpty()) {
                                Text("暂无歌单，请先创建歌单")
                            } else {
                                playlists.forEach { pl ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            currentSong?.let { viewModel.addToPlaylist(pl.id, it.id) }
                                            showAddToPlaylistDialog = false
                                        }.padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(pl.name, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAddToPlaylistDialog = false }) { Text("取消") }
                    }
                )
            }

            // 播放队列弹窗
            if (showQueueDialog) {
                val queueSongs by viewModel.playerManager.playlist.collectAsState()
                AlertDialog(
                    onDismissRequest = { showQueueDialog = false },
                    title = { Text("播放队列 (${queueSongs.size}首)") },
                    text = {
                        Column {
                            if (queueSongs.isEmpty()) {
                                Text("播放队列为空")
                            } else {
                                val currentIndex by viewModel.playerManager.currentIndex.collectAsState()
                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                    items(queueSongs.size) { index ->
                                        val song = queueSongs[index]
                                        val isCurrent = index == currentIndex
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable {
                                                    viewModel.playerManager.playSong(song, queueSongs)
                                                    showQueueDialog = false
                                                }
                                                .background(
                                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isCurrent) {
                                                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text("${index + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                                Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                            if (song.duration > 0) {
                                                Text("${song.duration / 60}:${"%02d".format(song.duration % 60)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQueueDialog = false }) { Text("关闭") }
                    }
                )
            }
        } else {
            var showQueueDialogPhone by remember { mutableStateOf(false) }
            var showAddToPlaylistDialogPhone by remember { mutableStateOf(false) }
            val musicTimerMinutesPhone by viewModel.musicTimerMinutes.collectAsState()
            val musicTimerRemainingPhone by viewModel.timerRemainingSeconds.collectAsState()

            MusicPlayerContent(
                config = responsiveCfg,
                playerManager = viewModel.playerManager,
                viewModel = viewModel,
                serverUrl = srvUrl,
                username = usr,
                password = pwd,
                onBack = onBack,
                onNavigateToArtist = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onShowAddToPlaylist = { showAddToPlaylistDialogPhone = true },
                onShowQueue = { showQueueDialogPhone = true },
                timerMinutes = musicTimerMinutesPhone,
                timerRemainingSeconds = musicTimerRemainingPhone,
                onSetTimer = { viewModel.musicSetTimer(it) }
            )

            // 手机端播放队列弹窗
            if (showQueueDialogPhone) {
                val queueSongs by viewModel.playerManager.playlist.collectAsState()
                AlertDialog(
                    onDismissRequest = { showQueueDialogPhone = false },
                    title = { Text("播放队列 (${queueSongs.size}首)") },
                    text = {
                        Column {
                            if (queueSongs.isEmpty()) {
                                Text("播放队列为空")
                            } else {
                                val currentIndex by viewModel.playerManager.currentIndex.collectAsState()
                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                    items(queueSongs.size) { index ->
                                        val song = queueSongs[index]
                                        val isCurrent = index == currentIndex
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable {
                                                    viewModel.playerManager.playSong(song, queueSongs)
                                                    showQueueDialogPhone = false
                                                }
                                                .background(
                                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isCurrent) {
                                                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text("${index + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                                Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                            if (song.duration > 0) {
                                                Text("${song.duration / 60}:${"%02d".format(song.duration % 60)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQueueDialogPhone = false }) { Text("关闭") }
                    }
                )
            }

            // 手机端添加到歌单弹窗
            if (showAddToPlaylistDialogPhone) {
                val playlists by viewModel.playlists.collectAsState()
                val currentSong by viewModel.playerManager.currentSong.collectAsState()
                AlertDialog(
                    onDismissRequest = { showAddToPlaylistDialogPhone = false },
                    title = { Text("添加到歌单") },
                    text = {
                        Column {
                            if (playlists.isEmpty()) {
                                Text("暂无歌单，请先创建歌单")
                            } else {
                                playlists.forEach { pl ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            currentSong?.let { viewModel.addToPlaylist(pl.id, it.id) }
                                            showAddToPlaylistDialogPhone = false
                                        }.padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(pl.name, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAddToPlaylistDialogPhone = false }) { Text("取消") }
                    }
                )
            }
        }
    }

    composable(Screen.AlbumDetail.route) { backStackEntry ->
        val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        AlbumDetailScreen(
            viewModel = viewModel,
            albumId = albumId,
            responsiveConfig = responsiveCfg,
            onBack = onBack,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
        )
    }

    composable(Screen.ArtistDetail.route) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.artists.TabletArtistDetailScreen(
                viewModel = viewModel,
                artistId = artistId,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onSongClick = { s, p -> viewModel.playSong(s, p) }
            )
        } else {
            ArtistDetailScreen(
                viewModel = viewModel,
                artistId = artistId,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onSongClick = { s, p -> viewModel.playSong(s, p) }
            )
        }
    }

    composable(Screen.PlaylistDetail.route) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
        PlaylistDetailScreen(
            viewModel = viewModel,
            playlistId = playlistId,
            onBack = onBack,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
        )
    }

    composable(Screen.Radio.route) {
        RadioScreen(
            viewModel = viewModel,
            onBack = onBack
        )
    }

    composable(Screen.Audiobook.route) { backStackEntry ->
        val genre = backStackEntry.arguments?.getString("genre")
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.audiobook.TabletAudiobookScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                genreFilter = genre,
                onBack = onBack,
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) }
            )
        } else {
            AudiobookScreen(
                viewModel = viewModel,
                genreFilter = genre,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.AudiobookDetail.route) { backStackEntry ->
        val audiobookId = backStackEntry.arguments?.getString("audiobookId") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.audiobook.TabletAudiobookDetailScreen(
                viewModel = viewModel,
                audiobookId = audiobookId,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onPlayChapter = { book, chapter, chapters ->
                    viewModel.playAudiobookChapter(book, chapter, chapters)
                    navController.navigate(Screen.AudiobookPlayer.route)
                }
            )
        } else {
            AudiobookDetailScreen(
                viewModel = viewModel,
                audiobookId = audiobookId,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onPlayChapter = { book, chapter, chapters ->
                    viewModel.playAudiobookChapter(book, chapter, chapters)
                    navController.navigate(Screen.AudiobookPlayer.route)
                }
            )
        }
    }

    composable(Screen.AudiobookPlayer.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        // 平板全屏播放器：隐藏系统栏 + 保持屏幕常亮
        val abContext = LocalContext.current
        DisposableEffect(Unit) {
            val activity = abContext as? android.app.Activity
            val window = activity?.window
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            onDispose {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        val srvUrl by viewModel.serverUrl.collectAsState()
        val usr by viewModel.username.collectAsState()
        val pwd by viewModel.password.collectAsState()
        val currentBook by viewModel.currentAudiobook.collectAsState()
        val chapters by viewModel.currentAudiobookChapters.collectAsState()
        val chapterIndex by viewModel.currentChapterIndex.collectAsState()
        val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
        val position by viewModel.audiobookPosition.collectAsState()
        val duration by viewModel.audiobookDuration.collectAsState()
        val audiobookCoverUrl by viewModel.playerManager.audiobookCoverUrl.collectAsState()
        val playbackSpeed by viewModel.audiobookPlaybackSpeed.collectAsState()
        val timerMinutes by viewModel.audiobookTimerMinutes.collectAsState()
        val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()

        val audiobookCallbacks: @Composable (com.lechenmusic.data.model.Audiobook) -> Unit = { book ->
            if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
                TabletAudiobookPlayerScreen(
                    book = book,
                    chapters = chapters,
                    currentChapterIndex = chapterIndex,
                    isPlaying = isPlaying,
                    currentPositionMs = position,
                    durationMs = duration,
                    serverUrl = srvUrl,
                    username = usr,
                    password = pwd,
                    coverUrl = audiobookCoverUrl,
                    playbackSpeed = playbackSpeed,
                    timerMinutes = timerMinutes,
                    timerRemainingSeconds = timerRemainingSeconds,
                    onBack = onBack,
                    onPlayPause = { viewModel.audiobookTogglePlayPause() },
                    onSeekTo = { viewModel.audiobookSeekTo(it) },
                    onSkipForward30s = { viewModel.audiobookSkipForward30s() },
                    onSkipBackward30s = { viewModel.audiobookSkipBackward30s() },
                    onPreviousChapter = { viewModel.audiobookPreviousChapter() },
                    onNextChapter = { viewModel.audiobookNextChapter() },
                    onChapterSelect = { idx ->
                        val ch = chapters.getOrNull(idx) ?: return@TabletAudiobookPlayerScreen
                        viewModel.playAudiobookChapter(book, ch, chapters)
                    },
                    onSetTimer = { viewModel.audiobookSetTimer(it) },
                    onChangeSpeed = { viewModel.audiobookChangeSpeed(it) },
                    onSaveProgress = { viewModel.saveAudiobookProgress() }
                )
            } else {
                AudiobookPlayerScreen(
                    responsiveConfig = responsiveCfg,
                    book = book,
                    chapters = chapters,
                    currentChapterIndex = chapterIndex,
                    isPlaying = isPlaying,
                    currentPositionMs = position,
                    durationMs = duration,
                    serverUrl = srvUrl,
                    username = usr,
                    password = pwd,
                    coverUrl = audiobookCoverUrl,
                    playbackSpeed = playbackSpeed,
                    timerMinutes = timerMinutes,
                    timerRemainingSeconds = timerRemainingSeconds,
                    onBack = onBack,
                    onPlayPause = { viewModel.audiobookTogglePlayPause() },
                    onSeekTo = { viewModel.audiobookSeekTo(it) },
                    onSkipForward30s = { viewModel.audiobookSkipForward30s() },
                    onSkipBackward30s = { viewModel.audiobookSkipBackward30s() },
                    onPreviousChapter = { viewModel.audiobookPreviousChapter() },
                    onNextChapter = { viewModel.audiobookNextChapter() },
                    onChapterSelect = { idx ->
                        val ch = chapters.getOrNull(idx) ?: return@AudiobookPlayerScreen
                        viewModel.playAudiobookChapter(book, ch, chapters)
                    },
                    onSetTimer = { viewModel.audiobookSetTimer(it) },
                    onChangeSpeed = { viewModel.audiobookChangeSpeed(it) },
                    onSaveProgress = { viewModel.saveAudiobookProgress() }
                )
            }
        }

        if (currentBook != null) {
            audiobookCallbacks(currentBook!!)
        } else if (audiobookCoverUrl != null) {
            // 通知栏入口: 有封面 URL 但没有 book 数据，尝试恢复播放状态
            val song by viewModel.playerManager.currentSong.collectAsState()
            if (song != null) {
                audiobookCallbacks(com.lechenmusic.data.model.Audiobook(id = "", title = song!!.title, coverPath = audiobookCoverUrl))
            } else {
                navController.popBackStack()
            }
        } else {
            navController.popBackStack()
        }
    }

    composable(Screen.NarratorList.route) {
        AudiobookNarratorListScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNarratorClick = { navController.navigate(Screen.NarratorDetail.createRoute(it)) }
        )
    }

    composable(Screen.NarratorDetail.route) { backStackEntry ->
        val narratorName = backStackEntry.arguments?.getString("narratorName") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        AudiobookNarratorDetailScreen(
            viewModel = viewModel,
            narratorName = narratorName,
            onBack = onBack,
            onBookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) },
            responsiveConfig = responsiveCfg
        )
    }

    // ===== 影视模块 =====

    composable(Screen.VideoSearch.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.video.TabletVideoSearchScreen(
                viewModel = videoViewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                }
            )
        } else {
            VideoSearchScreen(
                viewModel = videoViewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                }
            )
        }
    }

    composable(Screen.VideoDetail.route) { backStackEntry ->
        val source = backStackEntry.arguments?.getString("source") ?: return@composable
        val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.video.TabletVideoDetailScreen(
                viewModel = videoViewModel,
                source = source,
                videoId = videoId,
                responsiveConfig = responsiveCfg,
                onBack = {
                    // 返回时立即刷新播放记录（平板UI需要显式刷新）
                    videoViewModel.loadPlayRecords()
                    onBack()
                },
                onPlay = { playSource, episodeIndex ->
                    // 找到对应源在 sources 列表中的索引
                    val sources = videoViewModel.videoDetail.value?.toSources() ?: emptyList()
                    val sourceIdx = sources.indexOfFirst { it.source == playSource }.coerceAtLeast(0)
                    videoViewModel.setInitialPlayParams(sourceIdx, episodeIndex)
                    navController.navigate(Screen.VideoPlayerDirect.route)
                },
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                }
            )
        } else {
            VideoDetailScreen(
                viewModel = videoViewModel,
                source = source,
                videoId = videoId,
                responsiveConfig = responsiveCfg,
                onBack = onBack,
                onPlay = { playSource, episodeIndex ->
                    // 找到对应源在 sources 列表中的索引
                    val sources = videoViewModel.videoDetail.value?.toSources() ?: emptyList()
                    val sourceIdx = sources.indexOfFirst { it.source == playSource }.coerceAtLeast(0)
                    videoViewModel.setInitialPlayParams(sourceIdx, episodeIndex)
                    navController.navigate(Screen.VideoPlayerDirect.route)
                },
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                }
            )
        }
    }

    composable(Screen.VideoPlayerDirect.route) {
        // 播放视频时停止音乐/有声书
        LaunchedEffect(Unit) {
            viewModel.playerManager.forcePause()
        }
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        val detail by videoViewModel.videoDetail.collectAsState()
        val initialSource by videoViewModel.initialPlaySourceIndex.collectAsState()
        val initialEpisode by videoViewModel.initialPlayEpisodeIndex.collectAsState()
        if (detail != null) {
            val sources = detail!!.toSources()
            VideoPlayerScreen(
                videoTitle = detail!!.title,
                sources = sources,
                initialSource = initialSource,
                initialEpisode = initialEpisode,
                videoSource = detail!!.source,
                videoId = detail!!.id,
                videoCover = detail!!.displayCover,
                videoYear = detail!!.year,
                videoSourceName = detail!!.displaySourceName,
                videoViewModel = videoViewModel,
                onBack = onBack
            )
        } else {
            navController.popBackStack()
        }
    }

    composable(Screen.VideoPlayer.route) { backStackEntry ->
        val videoTitle = backStackEntry.arguments?.getString("videoTitle") ?: ""
        val source = backStackEntry.arguments?.getString("source") ?: ""
        val episodeIndex = backStackEntry.arguments?.getString("episodeIndex")?.toIntOrNull() ?: 0
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        val detail by videoViewModel.videoDetail.collectAsState()
        val sources = detail?.toSources() ?: emptyList()
        VideoPlayerScreen(
            videoTitle = videoTitle,
            sources = sources,
            initialSource = sources.indexOfFirst { it.source == source }.coerceAtLeast(0),
            initialEpisode = episodeIndex,
            videoSource = detail?.source ?: source,
            videoId = detail?.id ?: "",
            videoCover = detail?.displayCover ?: "",
            videoYear = detail?.year ?: "",
            videoSourceName = detail?.displaySourceName ?: "",
            videoViewModel = videoViewModel,
            onBack = onBack
        )
    }

    composable(Screen.Live.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.video.TabletLiveScreen(
                viewModel = videoViewModel,
                responsiveConfig = responsiveCfg,
                onBack = onBack
            )
        } else {
            LiveScreen(
                viewModel = videoViewModel,
                onBack = onBack
            )
        }
    }

    composable(Screen.VideoCategory.route) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: return@composable
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        VideoCategoryScreen(
            viewModel = videoViewModel,
            categoryType = type,
            responsiveConfig = responsiveCfg,
            onBack = onBack,
            onVideoClick = { video ->
                navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
            },
            windowSizeClass = windowSizeClass
        )
    }
}
