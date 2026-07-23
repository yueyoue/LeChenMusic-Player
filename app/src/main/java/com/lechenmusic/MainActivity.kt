package com.lechenmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
            val windowSizeClass = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(
                LocalContext.current as android.app.Activity
            )
            val responsiveConfig = rememberResponsiveConfig(windowSizeClass)
            val useSideNav = responsiveConfig.useRailNav
            val showMiniPlayer = currentSong != null && currentRoute != Screen.Player.route && currentRoute != Screen.AudiobookPlayer.route

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
                        modifier = modifier,
                        onClick = {
                            if (isAudiobookPlaying) navController.navigate(Screen.AudiobookPlayer.route)
                            else navController.navigate(Screen.Player.route)
                        }
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
            val hideSideNav = currentRoute == Screen.Player.route

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
                        // 首页
                        val homeSelected = currentRoute == Screen.Home.route
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onNavClick(Screen.Home.route) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Home, "首页",
                                tint = if (homeSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text("首页", fontSize = 10.sp,
                                color = if (homeSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (homeSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                        Scaffold { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                modifier = Modifier.fillMaxSize().padding(innerPadding)
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
                // ===== 手机: 标准 Scaffold + 底部导航栏 =====
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                MiniPlayerBar()
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
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
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
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) }
            )
        } else {
            FavoritesScreen(
                viewModel = viewModel,
                onSongClick = { s, p -> viewModel.playSong(s, p) },
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onAudiobookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.Search.route) {
        SearchScreen(
            viewModel = viewModel,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
        )
    }

    composable(Screen.Artists.route) {
        val responsiveCfg = com.lechenmusic.ui.responsive.rememberResponsiveConfig(windowSizeClass)
        if (responsiveCfg.isMedium || responsiveCfg.isExpanded) {
            com.lechenmusic.ui.screens.artists.TabletArtistsScreen(
                viewModel = viewModel,
                responsiveConfig = responsiveCfg,
                onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
            )
        } else {
            ArtistsScreen(
                viewModel = viewModel,
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
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        } else {
            AlbumsScreen(
                viewModel = viewModel,
                onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.AllSongs.route) {
        AllSongsScreen(
            viewModel = viewModel,
            onSongClick = { s, p -> viewModel.playSong(s, p) },
            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
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
        RecentPlayedScreen(
            viewModel = viewModel,
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
            TabletPlayerScreen(
                playerManager = viewModel.playerManager,
                viewModel = viewModel,
                serverUrl = srvUrl,
                username = usr,
                password = pwd,
                onBack = onBack,
                onNavigateToArtist = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onNavigateToAlbum = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                onShowAddToPlaylist = { navController.navigate(Screen.AllPlaylists.route) },
                onShowQueue = { navController.navigate(Screen.AllPlaylists.route) }
            )
        } else {
            PlayerScreen(
                playerManager = viewModel.playerManager,
                viewModel = viewModel,
                serverUrl = srvUrl,
                username = usr,
                password = pwd,
                onBack = onBack,
                onShowPlaylist = { },
                onShowMore = { },
                onNavigateToArtist = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                onNavigateToAlbum = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
            )
        }
    }

    composable(Screen.AlbumDetail.route) { backStackEntry ->
        val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
        AlbumDetailScreen(
            viewModel = viewModel,
            albumId = albumId,
            onBack = onBack,
            onSongClick = { s, p -> viewModel.playSong(s, p) }
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
            onSongClick = { s, p -> viewModel.playSong(s, p) }
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
                    onBack = onBack,
                    onPlayPause = { viewModel.audiobookTogglePlayPause() },
                    onSeekTo = { viewModel.audiobookSeekTo(it) },
                    onSkipForward15s = { viewModel.audiobookSkipForward15s() },
                    onSkipBackward15s = { viewModel.audiobookSkipBackward15s() },
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
                    onBack = onBack,
                    onPlayPause = { viewModel.audiobookTogglePlayPause() },
                    onSeekTo = { viewModel.audiobookSeekTo(it) },
                    onSkipForward15s = { viewModel.audiobookSkipForward15s() },
                    onSkipBackward15s = { viewModel.audiobookSkipBackward15s() },
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
        AudiobookNarratorDetailScreen(
            viewModel = viewModel,
            narratorName = narratorName,
            onBack = onBack,
            onBookClick = { navController.navigate(Screen.AudiobookDetail.createRoute(it)) }
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
                onBack = onBack,
                onPlay = { playSource, episodeIndex ->
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
                onBack = onBack,
                onPlay = { playSource, episodeIndex ->
                    navController.navigate(Screen.VideoPlayerDirect.route)
                },
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
                }
            )
        }
    }

    composable(Screen.VideoPlayerDirect.route) {
        val detail by videoViewModel.videoDetail.collectAsState()
        if (detail != null) {
            val sources = detail!!.toSources()
            VideoPlayerScreen(
                videoTitle = detail!!.title,
                sources = sources,
                initialSource = 0,
                initialEpisode = 0,
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
        val detail by videoViewModel.videoDetail.collectAsState()
        val sources = detail?.toSources() ?: emptyList()
        VideoPlayerScreen(
            videoTitle = videoTitle,
            sources = sources,
            initialSource = sources.indexOfFirst { it.source == source }.coerceAtLeast(0),
            initialEpisode = episodeIndex,
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
        VideoCategoryScreen(
            viewModel = videoViewModel,
            categoryType = type,
            onBack = onBack,
            onVideoClick = { video ->
                navController.navigate(Screen.VideoDetail.createRoute(video.source, video.id))
            },
            windowSizeClass = windowSizeClass
        )
    }
}
