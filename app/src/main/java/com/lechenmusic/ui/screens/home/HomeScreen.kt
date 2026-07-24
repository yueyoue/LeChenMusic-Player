package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.InternetRadioStation
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.*
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.data.model.VideoPlayRecord
import com.lechenmusic.data.model.HomeRecommendData
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToAllSongs: () -> Unit = {},
    onNavigateToRecentPlayed: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAllPlaylists: () -> Unit = {},
    onNavigateToCachedMusic: () -> Unit = {},
    onNavigateToAudiobook: (String?) -> Unit = {},
    onNavigateToAudiobookDetail: (String) -> Unit = {},
    onNavigateToNarrator: (String) -> Unit = {},
    onNavigateToNarratorList: () -> Unit = {},
    onNavigateToVideoDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: () -> Unit = {},
    onNavigateToVideoCategory: (String) -> Unit = {},
    onNavigateToVideoSearch: () -> Unit = {},
    onNavigateToLive: () -> Unit = {},
    videoViewModel: com.lechenmusic.ui.VideoViewModel? = null,
    responsiveConfig: ResponsiveConfig? = null
) {
    val newestAlbums by viewModel.newestAlbums.collectAsState()
    val randomAlbums by viewModel.randomAlbums.collectAsState()
    val dailySongs by viewModel.dailySongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlayedSongs by viewModel.recentPlayedSongs.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val homeMode by viewModel.homeMode.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    val audiobookWithProgress by viewModel.audiobookWithProgress.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val musicSlides by viewModel.musicSlides.collectAsState()
    val audiobookSlides by viewModel.audiobookSlides.collectAsState()

    // 影视状态
    val isVideoLoggedIn = videoViewModel?.isLoggedIn?.collectAsState()?.value ?: false
    val videoHomeData = videoViewModel?.homeData?.collectAsState()?.value
    val videoHomeLoading = videoViewModel?.homeLoading?.collectAsState()?.value ?: false
    val videoPlayRecords = videoViewModel?.playRecords?.collectAsState()?.value ?: emptyList()

    // 判断是否使用平板布局
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (isTablet) {
            // ===== 平板布局：根据 homeMode 切换 =====
            when (homeMode) {
                "music" -> {
                    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                        TabletMusicHomeContent(
                            config = config!!,
                            viewModel = viewModel,
                            newestAlbums = newestAlbums,
                            randomAlbums = randomAlbums,
                            dailySongs = dailySongs,
                            playlists = playlists,
                            recentPlayedSongs = recentPlayedSongs,
                            radioStations = radioStations,
                            musicSlides = musicSlides,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onAlbumClick = onAlbumClick,
                            onSongClick = onSongClick,
                            onPlaylistClick = onPlaylistClick,
                            onNavigateToAlbums = onNavigateToAlbums,
                            onNavigateToFavorites = onNavigateToFavorites,
                            onNavigateToAllSongs = onNavigateToAllSongs,
                            onNavigateToRecentPlayed = onNavigateToRecentPlayed,
                            onNavigateToRadio = onNavigateToRadio,
                            onNavigateToSearch = onNavigateToSearch,
                            onNavigateToArtists = onNavigateToArtists,
                            onNavigateToAllPlaylists = onNavigateToAllPlaylists,
                            onNavigateToCachedMusic = onNavigateToCachedMusic,
                            onNavigateToAudiobook = onNavigateToAudiobook,
                            onNavigateToAudiobookDetail = onNavigateToAudiobookDetail,
                            onNavigateToNarrator = onNavigateToNarrator,
                            onNavigateToNarratorList = onNavigateToNarratorList,
                            onNavigateToVideoDetail = onNavigateToVideoDetail,
                            onNavigateToVideoPlayer = onNavigateToVideoPlayer,
                            onNavigateToVideoCategory = onNavigateToVideoCategory,
                            onNavigateToVideoSearch = onNavigateToVideoSearch,
                            onNavigateToLive = onNavigateToLive,
                            videoViewModel = videoViewModel
                        )
                    }
                }
                "audiobook" -> {
                    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                        TabletAudiobookHomeContent(
                            audiobooks = audiobooks,
                            audiobookWithProgress = audiobookWithProgress,
                            starredAudiobooks = starredAudiobooks,
                            viewModel = viewModel,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            slides = audiobookSlides,
                            onNavigateToAudiobook = onNavigateToAudiobook,
                            onNavigateToAudiobookDetail = onNavigateToAudiobookDetail,
                            onNavigateToNarrator = onNavigateToNarrator,
                            responsiveConfig = config,
                            onNavigateToNarratorList = onNavigateToNarratorList
                        )
                    }
                }
                "video" -> {
                    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                        TabletVideoHomeContent(
                            config = config!!,
                            videoViewModel = videoViewModel!!,
                            isVideoLoggedIn = isVideoLoggedIn,
                            videoHomeData = videoHomeData,
                            videoHomeLoading = videoHomeLoading,
                            videoPlayRecords = videoPlayRecords,
                            onNavigateToVideoDetail = onNavigateToVideoDetail,
                            onNavigateToVideoCategory = onNavigateToVideoCategory,
                            onNavigateToVideoSearch = onNavigateToVideoSearch,
                            onNavigateToLive = onNavigateToLive
                        )
                    }
                    // 影视导航逻辑
                    VideoNavigationHandler(videoViewModel, onNavigateToVideoDetail)
                }
            }
        } else {
            // ===== 手机布局（原有逻辑） =====
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { padding ->
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                // Search
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                when (homeMode) {
                                    "audiobook" -> onNavigateToAudiobook(null)
                                    "video" -> onNavigateToVideoSearch()
                                    else -> onNavigateToSearch()
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                when (homeMode) {
                                "music" -> "搜索歌曲、专辑、歌手..."
                                "audiobook" -> "搜索有声书、演播者..."
                                "video" -> "搜索影视..."
                                else -> "搜索..."
                            },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Mode switcher - Task 4: fix background width
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            ModeBtn(
                                "\uD83C\uDFB5", "音乐", homeMode == "music",
                                modifier = Modifier.weight(1f)
                            ) { viewModel.setHomeMode("music") }
                            ModeBtn(
                                "\uD83D\uDCD6", "有声书", homeMode == "audiobook",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setHomeMode("audiobook")
                                viewModel.loadAudiobooks()
                            }
                            ModeBtn(
                                "\uD83C\uDFAC", "影视", homeMode == "video",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setHomeMode("video")
                            }
                        }
                    }
                }

                // ===== MUSIC MODE =====
                if (homeMode == "music") {
                    // Music homepage slides carousel
                    item {
                        MusicSlidesCarousel(
                            slides = musicSlides,
                            serverUrl = serverUrl,
                            onSlideClick = { link ->
                                // Parse link format: "playlist:<id>" or "album:<id>"
                                when {
                                    link.startsWith("playlist:") -> {
                                        val id = link.removePrefix("playlist:")
                                        onPlaylistClick(id)
                                    }
                                    link.startsWith("album:") -> {
                                        val id = link.removePrefix("album:")
                                        onAlbumClick(id)
                                    }
                                }
                            }
                        )
                    }
                    // Quick access - Task 3: align with hero boundaries
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickBtn("\uD83D\uDC64", "歌手", Color(0xFFA78BFA), onNavigateToArtists)
                            QuickBtn("\uD83D\uDCBF", "专辑", Color(0xFF5352ED), onNavigateToAlbums)
                            QuickBtn("\uD83D\uDCCB", "歌单", Color(0xFF34D399), onNavigateToAllPlaylists)
                            QuickBtn("\uD83D\uDCFB", "电台", Color(0xFFFF4D6A), onNavigateToRadio)
                            QuickBtn(
                                "\uD83D\uDCE5",
                                "缓存",
                                Color(0xFFFBBF24),
                                onNavigateToCachedMusic
                            )
                        }
                    }
                    // Albums
                    if (newestAlbums.isNotEmpty()) {
                        item {
                            SecHd(
                                "\uD83C\uDD95 最新专辑",
                                "更多 ›",
                                onNavigateToAlbums
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(newestAlbums) {
                                    AlbumCard2(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onAlbumClick(it.id) }
                                }
                            }
                        }
                    }
                    // Daily songs
                    if (dailySongs.isNotEmpty()) {
                        item {
                            SecHd("🎯 每日推荐", "换一批 ↻") {
                                viewModel.refreshDailySongs()
                            }
                        }
                        items(dailySongs.take(5)) {
                            SongRow(
                                it,
                                serverUrl,
                                username,
                                password
                            ) { onSongClick(it, dailySongs) }
                        }
                    }
                    // Playlists
                    if (playlists.isNotEmpty()) {
                        item {
                            SecHd(
                                "\uD83D\uDCCB 歌单",
                                "更多 ›",
                                onNavigateToAllPlaylists
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(playlists.take(5)) {
                                    PlCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onPlaylistClick(it.id) }
                                }
                            }
                        }
                    }
                    // Random albums
                    if (randomAlbums.isNotEmpty()) {
                        item {
                            SecHd("\uD83C\uDFB2 随机专辑", "换一批 ↻") {
                                viewModel.refreshRandomAlbums()
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(randomAlbums) {
                                    AlbumCard2(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onAlbumClick(it.id) }
                                }
                            }
                        }
                    }
                    // Recent
                    item {
                        SecHd(
                            "⏱️ 最近播放",
                            "更多 ›",
                            onNavigateToRecentPlayed
                        )
                    }
                    if (recentPlayedSongs.isNotEmpty()) {
                        items(recentPlayedSongs.take(5)) {
                            SongRow(
                                it,
                                serverUrl,
                                username,
                                password
                            ) { onSongClick(it, recentPlayedSongs) }
                        }
                    } else {
                        item {
                            Text(
                                "播放歌曲后将显示在此处",
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                    // Radio
                    item { SecHd("📻 电台", "") }
                    if (radioStations.isNotEmpty()) {
                        items(radioStations.take(4)) {
                            RadioRow(it) { viewModel.playerManager.playRadioStation(it) }
                        }
                    }
                }

                // ===== AUDIOBOOK MODE =====
                if (homeMode == "audiobook" && isTablet) {
                    // ===== 平板有声书首页 =====
                    item {
                        TabletAudiobookHomeContent(
                            audiobooks = audiobooks,
                            audiobookWithProgress = audiobookWithProgress,
                            starredAudiobooks = starredAudiobooks,
                            viewModel = viewModel,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            slides = audiobookSlides,
                            onNavigateToAudiobook = onNavigateToAudiobook,
                            onNavigateToAudiobookDetail = onNavigateToAudiobookDetail,
                            onNavigateToNarrator = onNavigateToNarrator,
                            responsiveConfig = config,
                            onNavigateToNarratorList = onNavigateToNarratorList
                        )
                    }
                } else if (homeMode == "audiobook") {
                    // ===== 手机有声书首页 =====
                    // Audiobook homepage slides carousel (server-configured)
                    item {
                        AudiobookSlidesCarousel(
                            slides = audiobookSlides,
                            audiobooks = audiobooks,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onSlideClick = { link ->
                                // Parse link format: "audiobook:<id>"
                                when {
                                    link.startsWith("audiobook:") -> {
                                        val id = link.removePrefix("audiobook:")
                                        onNavigateToAudiobookDetail(id)
                                    }
                                }
                            },
                            onAudiobookClick = onNavigateToAudiobookDetail
                        )
                    }
                    // Continue listening (like ting-reader: always show section)
                    val booksWithProgress = audiobookWithProgress.filter { it.progress != null && !it.progress.completed }
                    item {
                        SecHd("⏱️ 继续收听", "全部 ›") {}
                    }
                    if (booksWithProgress.isNotEmpty()) {
                        items(booksWithProgress.take(20)) { bwp ->
                            ContCard(
                                bwp.toAudiobook(),
                                serverUrl,
                                username,
                                password,
                                progress = bwp.progress
                            ) {
                                viewModel.resumeAudiobook(bwp.toAudiobook())
                                onNavigateToAudiobookDetail(bwp.id)
                            }
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "还没有收听记录",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "播放有声书后将显示在此处",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    // Categories
                    item { SecHd("\uD83D\uDCC2 分类", "全部 ›") { onNavigateToAudiobook(null) } }
                    item { CatGrid { genre -> onNavigateToAudiobook(genre) } }
                    // Narrators
                    item { SecHd("\uD83C\uDFA4 演播者", "全部 ›") { onNavigateToNarratorList() } }
                    item {
                        val narrators by viewModel.narrators.collectAsState()
                        if (narrators.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(narrators.take(8)) { narr ->
                                    NarrItem(
                                        name = narr.name,
                                        count = "${narr.count}部作品",
                                        color = getNarrColor(narr.name),
                                        serverUrl = serverUrl,
                                        onClick = { onNavigateToNarrator(narr.name) }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("暂无演播者信息", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("请重新扫描有声书媒体库以读取演播者信息", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                    // Recently updated
                    item { SecHd("\uD83C\uDD95 最近更新", "更多 ›") { onNavigateToAudiobook(null) } }
                    if (audiobooks.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val recent = audiobooks.sortedByDescending { it.updatedAt }.take(5)
                                items(recent) {
                                    AbGridCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(it.id) }
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无有声书，请先扫描媒体库", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // 有声小说
                    item { SecHd("\uD83D\uDCD6 有声小说", "更多 ›") { onNavigateToAudiobook("有声读物") } }
                    item {
                        val novelBooks = audiobooks.filter { it.genre == "有声读物" || it.genre.isEmpty() }
                        if (novelBooks.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(novelBooks.take(4)) { i, b ->
                                    RankCard(
                                        b,
                                        i + 1,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(b.id) }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无有声小说", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // 相声
                    item { SecHd("\uD83C\uDFA4 相声", "更多 ›") { onNavigateToAudiobook("相声") } }
                    item {
                        val xiangshengBooks = audiobooks.filter { it.genre == "相声" }
                        if (xiangshengBooks.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(xiangshengBooks.take(5)) {
                                    AbGridCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(it.id) }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无相声", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // 评书
                    item { SecHd("\uD83C\uDFAD 评书", "更多 ›") { onNavigateToAudiobook("评书") } }
                    item {
                        val pingshuBooks = audiobooks.filter { it.genre == "评书" }
                        if (pingshuBooks.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(pingshuBooks.take(5)) {
                                    AbGridCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(it.id) }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无评书", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // 儿童读物
                    item { SecHd("\uD83D\uDC76 儿童读物", "更多 ›") { onNavigateToAudiobook("儿童") } }
                    item {
                        val childBooks = audiobooks.filter { it.genre == "儿童" }
                        if (childBooks.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(childBooks.take(5)) {
                                    AbGridCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(it.id) }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无儿童读物", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // Favorites
                    item { SecHd("❤️ 我的收藏", "更多 ›") { onNavigateToAudiobook("starred") } }
                    if (starredAudiobooks.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(starredAudiobooks.take(5)) {
                                    AbGridCard(
                                        it,
                                        serverUrl,
                                        username,
                                        password
                                    ) { onNavigateToAudiobookDetail(it.id) }
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无收藏的有声书", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // ===== VIDEO MODE =====
                if (homeMode == "video") {
                    item {
                        androidx.compose.runtime.LaunchedEffect(isVideoLoggedIn) {
                            if (isVideoLoggedIn) {
                                videoViewModel?.loadHomeData()
                                videoViewModel?.loadPlayRecords()
                            }
                        }
                    }

                    if (!isVideoLoggedIn) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Tv,
                                        null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "请先配置影视服务器",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "前往 我的 → 影视服务器 配置",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        // 横向分类导航菜单
                        item {
                            var selectedVideoTab by remember { mutableIntStateOf(0) }
                            val videoTabs = listOf("\uD83C\uDFAE 推荐", "\uD83C\uDFAC 电影", "\uD83D\uDCFA 剧集", "\uD83C\uDF8C 动漫", "\uD83C\uDFAD 综艺", "\uD83D\uDCFA 直播")
                            Column {
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(videoTabs.size) { index ->
                                        val isSelected = selectedVideoTab == index
                                        Surface(
                                            onClick = {
                                                selectedVideoTab = index
                                                when (index) {
                                                    0 -> {} // 推荐，当前页
                                                    1 -> onNavigateToVideoCategory("movie")
                                                    2 -> onNavigateToVideoCategory("tv")
                                                    3 -> onNavigateToVideoCategory("anime")
                                                    4 -> onNavigateToVideoCategory("variety")
                                                    5 -> onNavigateToLive()
                                                }
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                videoTabs[index],
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1. 最近观看（播放记录）
                        if (videoPlayRecords.isNotEmpty()) {
                            item {
                                SecHd("\u23F0 最近观看", "")
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(videoPlayRecords.take(10)) { record ->
                                        com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                            video = com.lechenmusic.data.model.VideoInfo(
                                                id = record.videoIdRaw,
                                                source = record.source,
                                                title = record.title,
                                                cover = record.cover,
                                                year = record.year,
                                                type = record.type,
                                                totalEpisodes = record.displayTotalEpisodes,
                                                playTime = record.displayPlayTime,
                                                totalTime = record.displayTotalTime
                                            ),
                                            onClick = { onNavigateToVideoDetail(record.source, record.videoIdRaw) }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. 热门电影
                        val hotMovies = videoHomeData?.hotMovies ?: emptyList()
                        if (hotMovies.isNotEmpty()) {
                            item { SecHd("\uD83C\uDFAC 热门电影", "") }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(hotMovies) { video ->
                                        com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                            video = video,
                                            onClick = {
                                                if (video.source.isNotBlank()) {
                                                    onNavigateToVideoDetail(video.source, video.id)
                                                } else {
                                                    // 豆瓣电影：先搜索 LunaTV 找到源
                                                    videoViewModel?.searchAndPlay(video.title, video.id, video.year)
                                                    
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 热门剧集
                        val hotTv = videoHomeData?.hotTvShows ?: emptyList()
                        if (hotTv.isNotEmpty()) {
                            item { SecHd("\uD83D\uDCFA 热门剧集", "") }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(hotTv) { video ->
                                        com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                            video = video,
                                            onClick = {
                                                if (video.source.isNotBlank()) {
                                                    onNavigateToVideoDetail(video.source, video.id)
                                                } else {
                                                    videoViewModel?.searchAndPlay(video.title, video.id, video.year)
                                                    
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. 热门综艺
                        val hotVariety = videoHomeData?.hotVariety ?: emptyList()
                        if (hotVariety.isNotEmpty()) {
                            item { SecHd("\uD83C\uDFAD 热门综艺", "") }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(hotVariety) { video ->
                                        com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                            video = video,
                                            onClick = {
                                                if (video.source.isNotBlank()) {
                                                    onNavigateToVideoDetail(video.source, video.id)
                                                } else {
                                                    videoViewModel?.searchAndPlay(video.title, video.id, video.year)
                                                    
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. 热门动漫
                        val hotAnime = videoHomeData?.hotAnime ?: emptyList()
                        if (hotAnime.isNotEmpty()) {
                            item { SecHd("\uD83C\uDF8C 热门动漫", "") }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(hotAnime) { video ->
                                        com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                            video = video,
                                            onClick = {
                                                if (video.source.isNotBlank()) {
                                                    onNavigateToVideoDetail(video.source, video.id)
                                                } else {
                                                    videoViewModel?.searchAndPlay(video.title, video.id, video.year)
                                                    
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 加载指示器
                        if (videoHomeLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        // 空状态
                        if (!videoHomeLoading && hotMovies.isEmpty() && hotTv.isEmpty() && videoPlayRecords.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "暂无推荐数据，下拉刷新",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } // Scaffold
            } // else (手机布局)
        } // if (isTablet) / else

        // ===== 影视导航逻辑（放在 LazyColumn 外面，避免滚动时被回收） =====
        if (homeMode == "video" && !isTablet) {
            VideoNavigationHandler(videoViewModel, onNavigateToVideoDetail)
        }

    }
}

// ==================== Music Slides Carousel ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicSlidesCarousel(
    slides: List<com.lechenmusic.data.model.SlideConfig>,
    serverUrl: String,
    onSlideClick: (String) -> Unit
) {
    if (slides.isEmpty()) {
        // Fallback: show the original static hero
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(170.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "每日推荐",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("今日精选", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("为你推荐", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("根据你的口味生成", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\uD83C\uDFB5", fontSize = 42.sp)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll
    LaunchedEffect(pagerState.currentPage) {
        delay(4000)
        val nextPage = (pagerState.currentPage + 1) % slides.size
        coroutineScope.launch {
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(170.dp)
        ) { page ->
            val slide = slides[page]
            val gradients = listOf(
                listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF)),
                listOf(Color(0xFFE94560), Color(0xFFFF6B81), Color(0xFFFF8787)),
                listOf(Color(0xFF00B894), Color(0xFF55EFC4), Color(0xFF81ECEC)),
                listOf(Color(0xFFF39C12), Color(0xFFFDCB6E), Color(0xFFFFF3CD)),
                listOf(Color(0xFF3498DB), Color(0xFF74B9FF), Color(0xFFA8D8EA))
            )
            val gradient = gradients[page % gradients.size]

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (slide.link.isNotEmpty()) onSlideClick(slide.link)
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient))
                ) {
                    // Background image if available
                    if (slide.imageUrl.isNotEmpty()) {
                        val normalizedUrl = serverUrl.trimEnd('/')
                        val fullImageUrl = if (slide.imageUrl.startsWith("http")) slide.imageUrl
                            else "$normalizedUrl${slide.imageUrl}"
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }
                    // Text overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        if (slide.title.isNotEmpty()) {
                            Text(
                                slide.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(slides.size) { index ->
                val color = if (pagerState.currentPage == index)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(
                            width = if (pagerState.currentPage == index) 16.dp else 6.dp,
                            height = 6.dp
                        )
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}

// ==================== Audiobook Slides Carousel ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudiobookSlidesCarousel(
    slides: List<com.lechenmusic.data.model.SlideConfig>,
    audiobooks: List<com.lechenmusic.data.model.Audiobook>,
    serverUrl: String,
    username: String,
    password: String,
    onSlideClick: (String) -> Unit,
    onAudiobookClick: (String) -> Unit
) {
    // If server has configured slides, use them; otherwise fallback to audiobook list
    if (slides.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { slides.size })
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % slides.size
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }

        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(170.dp)
            ) { page ->
                val slide = slides[page]
                val gradients = listOf(
                    listOf(Color(0xFFE94560), Color(0xFFFF6B81), Color(0xFFFF8787)),
                    listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF)),
                    listOf(Color(0xFF00B894), Color(0xFF55EFC4), Color(0xFF81ECEC)),
                    listOf(Color(0xFFF39C12), Color(0xFFFDCB6E), Color(0xFFFFF3CD)),
                    listOf(Color(0xFF3498DB), Color(0xFF74B9FF), Color(0xFFA8D8EA))
                )
                val gradient = gradients[page % gradients.size]

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (slide.link.isNotEmpty()) onSlideClick(slide.link)
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(gradient))
                    ) {
                        if (slide.imageUrl.isNotEmpty()) {
                            val normalizedUrl = serverUrl.trimEnd('/')
                            val fullImageUrl = if (slide.imageUrl.startsWith("http")) slide.imageUrl
                                else "$normalizedUrl${slide.imageUrl}"
                            AsyncImage(
                                model = fullImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.3f
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            if (slide.title.isNotEmpty()) {
                                Text(
                                    slide.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(slides.size) { index ->
                    val color = if (pagerState.currentPage == index)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (pagerState.currentPage == index) 16.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
    } else {
        // Fallback: use the original audiobook carousel (picks from audiobook list)
        AudiobookCarousel(
            audiobooks = audiobooks,
            serverUrl = serverUrl,
            username = username,
            password = password,
            onAudiobookClick = onAudiobookClick
        )
    }
}

// ==================== Task 1: Audiobook Carousel (Fallback) ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudiobookCarousel(
    audiobooks: List<Audiobook>,
    serverUrl: String,
    username: String,
    password: String,
    onAudiobookClick: (String) -> Unit
) {
    // Pick up to 5 audiobooks for the carousel
    val carouselItems = audiobooks.take(5)
    if (carouselItems.isEmpty()) {
        // Fallback: show placeholder
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(170.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE94560), Color(0xFFFF6B81), Color(0xFFFF8787))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "\uD83D\uDD25 热门推荐",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "有声书",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        "精彩内容等你来听",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\uD83D\uDCD6", fontSize = 36.sp)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { carouselItems.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll
    LaunchedEffect(pagerState.currentPage) {
        delay(4000)
        val nextPage = (pagerState.currentPage + 1) % carouselItems.size
        coroutineScope.launch {
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(170.dp)
        ) { page ->
            val book = carouselItems[page]
            val gradients = listOf(
                listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF)),
                listOf(Color(0xFFE94560), Color(0xFFFF6B81), Color(0xFFFF8787)),
                listOf(Color(0xFF00B894), Color(0xFF55EFC4), Color(0xFF81ECEC)),
                listOf(Color(0xFFF39C12), Color(0xFFFDCB6E), Color(0xFFFFF3CD)),
                listOf(Color(0xFF3498DB), Color(0xFF74B9FF), Color(0xFFA8D8EA)),
            )
            val gradient = gradients[page % gradients.size]

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAudiobookClick(book.id) },
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient))
                        .padding(20.dp)
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "\uD83D\uDD25 热门推荐",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            book.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val info = buildString {
                            if (book.narrator.isNotBlank()) append("${book.narrator}演播")
                            else if (book.author.isNotBlank()) append(book.author)
                            if (book.chapterCount > 0) append(" · ${book.chapterCount}章")
                        }
                        if (info.isNotEmpty()) {
                            Text(
                                info,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // Cover on the right
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val url = getAudiobookCoverUrl(serverUrl, username, password, book.id)
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("\uD83D\uDCD6", fontSize = 36.sp)
                        }
                    }
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(carouselItems.size) { index ->
                val color = if (pagerState.currentPage == index)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(
                            width = if (pagerState.currentPage == index) 16.dp else 6.dp,
                            height = 6.dp
                        )
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}

// ==================== Components ====================

@Composable
private fun ModeBtn(
    icon: String,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickBtn(icon: String, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp) }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SecHd(title: String, action: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) Text(
            action,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onClick?.invoke() })
    }
}

@Composable
private fun AlbumCard2(
    album: Album,
    s: String,
    u: String,
    p: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Surface(
            modifier = Modifier
                .width(130.dp)
                .height(130.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            if (album.coverArt != null) AsyncImage(
                model = ApiClient.getCoverArtUrl(s, u, p, album.coverArt),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Album,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Text(
            album.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(130.dp)
                .padding(top = 7.dp)
        )
        Text(
            album.artist,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SongRow(
    song: Song,
    s: String,
    u: String,
    p: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (song.coverArt != null) AsyncImage(
                model = ApiClient.getCoverArtUrl(s, u, p, song.coverArt),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            song.durationFormatted,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlCard(
    pl: Playlist,
    s: String,
    u: String,
    p: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Surface(
            modifier = Modifier
                .width(110.dp)
                .height(110.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            if (pl.coverArt != null) AsyncImage(
                model = ApiClient.getCoverArtUrl(s, u, p, pl.coverArt),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            pl.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(110.dp)
                .padding(top = 6.dp)
        )
        Text(
            "${pl.songCount}首",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RadioRow(station: InternetRadioStation, onClick: () -> Unit) {
    val colors = listOf(
        Color(0xFFFF4D6A),
        Color(0xFFA78BFA),
        Color(0xFF5352ED),
        Color(0xFF34D399),
        Color(0xFF60A5FA),
        Color(0xFFFBBF24)
    )
    val color = colors[station.name.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) { Text("\uD83D\uDCFB", fontSize = 20.sp) }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "网络电台",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ContCard(
    book: Audiobook,
    s: String,
    u: String,
    p: String,
    progress: com.lechenmusic.data.model.AudiobookProgress? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val url = getAudiobookCoverUrl(s, u, p, book.id)
                if (url != null) AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else Box(contentAlignment = Alignment.Center) {
                    Text("\uD83D\uDCD6", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val chapterNum = progress?.chapterNumber ?: 1
                Text(
                    "第${chapterNum}章 / ${book.chapterCount}章",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                val progressPercent = if (book.totalDuration > 0 && progress != null) {
                    val totalListened = (progress.chapterNumber - 1).coerceAtLeast(0) * 60 + progress.position
                    (totalListened.toFloat() / book.totalDuration).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (progress != null) {
                    val posMin = progress.position / 60
                    val posSec = progress.position % 60
                    Text(
                        "第${progress.chapterNumber}章 ${posMin}:${"%02d".format(posSec)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "播放",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getNarrColor(name: String): androidx.compose.ui.graphics.Color {
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFFE94560),
        androidx.compose.ui.graphics.Color(0xFF3498DB),
        androidx.compose.ui.graphics.Color(0xFF2ECC71),
        androidx.compose.ui.graphics.Color(0xFFF39C12),
        androidx.compose.ui.graphics.Color(0xFF8E44AD),
        androidx.compose.ui.graphics.Color(0xFF00B894)
    )
    val idx = name.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }
    return colors[idx]
}

@Composable
private fun CatGrid(onGenreClick: (String) -> Unit = {}) {
    data class CatItem(val emoji: String, val label: String, val genre: String, val color: Color)
    val cats = listOf(
        CatItem("\uD83D\uDCD6", "有声小说", "有声读物", Color(0xFF5352ED)),
        CatItem("\uD83C\uDFA4", "相声", "相声", Color(0xFF8E44AD)),
        CatItem("\uD83C\uDFAD", "评书", "评书", Color(0xFFF39C12)),
        CatItem("\uD83D\uDC76", "儿童读物", "儿童", Color(0xFF00B894))
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (row in cats.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (cat in row) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .padding(bottom = 10.dp)
                            .clickable { onGenreClick(cat.genre) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(cat.color, cat.color.copy(alpha = 0.7f))
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "${cat.emoji} ${cat.label}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NarrItem(
    name: String,
    count: String,
    color: Color,
    serverUrl: String = "",
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = color
        ) {
            val avatarUrl = if (serverUrl.isNotEmpty()) {
                com.lechenmusic.data.api.SubsonicApi.getNarratorAvatarUrl(serverUrl, name)
            } else ""
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) { Text("\uD83C\uDFA4", fontSize = 24.sp) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            count,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AbGridCard(
    book: Audiobook,
    s: String,
    u: String,
    p: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            val url = getAudiobookCoverUrl(s, u, p, book.id)
            if (url != null) AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MenuBook,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Text(
            book.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(140.dp)
                .padding(top = 7.dp)
        )
        Text(
            "${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RankCard(
    book: Audiobook,
    rank: Int,
    s: String,
    u: String,
    p: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                val url = getAudiobookCoverUrl(s, u, p, book.id)
                if (url != null) AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            val c = when (rank) {
                1 -> Color(0xFFFBBF24)
                2 -> Color(0xFF9898B8)
                3 -> Color(0xFFCD7F32)
                else -> Color(0xFF282850)
            }
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
                shape = RoundedCornerShape(8.dp),
                color = c
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        Text(
            book.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(140.dp)
                .padding(top = 7.dp)
        )
        Text(
            "${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Tablet Music Home Layout ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabletMusicHomeContent(
    config: ResponsiveConfig,
    viewModel: MainViewModel,
    newestAlbums: List<Album>,
    randomAlbums: List<Album>,
    dailySongs: List<Song>,
    playlists: List<Playlist>,
    recentPlayedSongs: List<Song>,
    radioStations: List<InternetRadioStation>,
    musicSlides: List<com.lechenmusic.data.model.SlideConfig>,
    serverUrl: String,
    username: String,
    password: String,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToAllSongs: () -> Unit,
    onNavigateToRecentPlayed: () -> Unit,
    onNavigateToRadio: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToAllPlaylists: () -> Unit,
    onNavigateToCachedMusic: () -> Unit,
    onNavigateToAudiobook: (String?) -> Unit,
    onNavigateToAudiobookDetail: (String) -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToNarratorList: () -> Unit,
    onNavigateToVideoDetail: (String, String) -> Unit,
    onNavigateToVideoPlayer: () -> Unit,
    onNavigateToVideoCategory: (String) -> Unit,
    onNavigateToVideoSearch: () -> Unit,
    onNavigateToLive: () -> Unit,
    videoViewModel: com.lechenmusic.ui.VideoViewModel?
) {
    val homeMode by viewModel.homeMode.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    val audiobookWithProgress by viewModel.audiobookWithProgress.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val audiobookSlides by viewModel.audiobookSlides.collectAsState()

    val isVideoLoggedIn = videoViewModel?.isLoggedIn?.collectAsState()?.value ?: false
    val videoHomeData = videoViewModel?.homeData?.collectAsState()?.value
    val videoHomeLoading = videoViewModel?.homeLoading?.collectAsState()?.value ?: false
    val videoPlayRecords = videoViewModel?.playRecords?.collectAsState()?.value ?: emptyList()

    val pad = config.contentPadding
    val gap = config.itemSpacing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = pad, end = pad,
            top = pad,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(config.sectionSpacing)
    ) {
        if (homeMode == "music") {
            // ===== Hero: Banner (2/3) + Hot List (1/3) =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(config.heroHeight),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Featured Banner
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF))))
                    ) {
                        if (musicSlides.isNotEmpty()) {
                            val slide = musicSlides.first()
                            if (slide.imageUrl.isNotEmpty()) {
                                val fullUrl = if (slide.imageUrl.startsWith("http")) slide.imageUrl
                                    else "${serverUrl.trimEnd('/')}${slide.imageUrl}"
                                AsyncImage(model = fullUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.35f)
                            }
                        }
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.15f)) {
                                Text("精选推荐", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = config.captionFontSize, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                musicSlides.firstOrNull()?.title ?: "每日推荐",
                                fontSize = (config.sectionTitleSize.value + 6).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("探索专为您量身打造的原创合集", fontSize = config.bodyFontSize, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val link = musicSlides.firstOrNull()?.link ?: ""
                                    when {
                                        link.startsWith("playlist:") -> onPlaylistClick(link.removePrefix("playlist:"))
                                        link.startsWith("album:") -> onAlbumClick(link.removePrefix("album:"))
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6C5CE7))
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("立即播放", fontWeight = FontWeight.SemiBold, fontSize = config.bodyFontSize)
                            }
                        }
                    }
                    // Hot List Card
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("今日热榜", fontSize = config.cardTitleSize, fontWeight = FontWeight.SemiBold)
                                Text("全球播放量最高的 50 首", fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                dailySongs.take(3).forEachIndexed { i, song ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            if (song.coverArt != null) AsyncImage(model = ApiClient.getCoverArtUrl(serverUrl, username, password, song.coverArt), contentDescription = null, contentScale = ContentScale.Crop)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, fontSize = config.cardTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.artist, fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(36.dp).align(Alignment.End))
                        }
                    }
                }
            }

            // ===== Quick Access =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data class QuickItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val color: Color, val onClick: () -> Unit)
                    val quickItems = listOf(
                        QuickItem(Icons.Default.Person, "歌手", Color(0xFFA78BFA), onNavigateToArtists),
                        QuickItem(Icons.Default.Album, "专辑", Color(0xFF5352ED), onNavigateToAlbums),
                        QuickItem(Icons.Default.MusicNote, "乐库", Color(0xFF34D399), onNavigateToAllSongs),
                        QuickItem(Icons.Default.PlaylistPlay, "歌单", Color(0xFFFBBF24), onNavigateToAllPlaylists),
                        QuickItem(Icons.Default.Radio, "电台", Color(0xFFFF4D6A), onNavigateToRadio),
                        QuickItem(Icons.Default.Download, "缓存", Color(0xFF60A5FA), onNavigateToCachedMusic)
                    )
                    quickItems.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(onClick = item.onClick)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = item.color.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.label, fontSize = config.cardSubtitleSize, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ===== Latest Albums =====
            if (newestAlbums.isNotEmpty()) {
                item { TabletSecHd("最新专辑", "更多 ›", config, onNavigateToAlbums) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(newestAlbums) { TabletAlbumCard(it, serverUrl, username, password, config) { onAlbumClick(it.id) } }
                    }
                }
            }

            // ===== Daily Recommendations - Dual Column =====
            if (dailySongs.isNotEmpty()) {
                item { TabletSecHd("每日推荐", "换一批 ↻", config) { viewModel.refreshDailySongs() } }
                item {
                    val half = (dailySongs.size + 1) / 2
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Column(modifier = Modifier.weight(1f)) {
                            dailySongs.take(half).forEach { TabletSongRow(it, serverUrl, username, password, config) { onSongClick(it, dailySongs) } }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            dailySongs.drop(half).forEach { TabletSongRow(it, serverUrl, username, password, config) { onSongClick(it, dailySongs) } }
                        }
                    }
                }
            }

            // ===== Playlists =====
            if (playlists.isNotEmpty()) {
                item { TabletSecHd("歌单", "更多 ›", config, onNavigateToAllPlaylists) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(playlists.take(8)) { TabletPlaylistCard(it, serverUrl, username, password, config) { onPlaylistClick(it.id) } }
                    }
                }
            }

            // ===== Random Albums =====
            if (randomAlbums.isNotEmpty()) {
                item { TabletSecHd("随机专辑", "换一批 ↻", config) { viewModel.refreshRandomAlbums() } }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(randomAlbums) { TabletAlbumCard(it, serverUrl, username, password, config) { onAlbumClick(it.id) } }
                    }
                }
            }

            // ===== Recent Played - Multi Column =====
            item { TabletSecHd("最近播放", "更多 ›", config, onNavigateToRecentPlayed) }
            if (recentPlayedSongs.isNotEmpty()) {
                item {
                    val cols = config.recentColumns
                    val chunkSize = (recentPlayedSongs.size + cols - 1) / cols
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until cols) {
                            Column(modifier = Modifier.weight(1f)) {
                                recentPlayedSongs.drop(col * chunkSize).take(chunkSize).forEach { song ->
                                    TabletSongRow(song, serverUrl, username, password, config) { onSongClick(song, recentPlayedSongs) }
                                }
                            }
                        }
                    }
                }
            } else {
                item { Text("播放歌曲后将显示在此处", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp)) }
            }

            // ===== Radio =====
            item { TabletSecHd("电台", "", config) }
            if (radioStations.isNotEmpty()) {
                items(radioStations.take(4)) { TabletRadioRow(it, config) { viewModel.playerManager.playRadioStation(it) } }
            }
        }
    }
}

// ==================== Tablet Sub-Components ====================


@Composable
private fun TabletSecHd(title: String, action: String, config: ResponsiveConfig, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = config.sectionTitleSize, fontWeight = FontWeight.SemiBold)
        if (action.isNotEmpty()) {
            Text(action, fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onClick?.invoke() })
        }
    }
}

@Composable
private fun TabletAlbumCard(album: Album, s: String, u: String, p: String, config: ResponsiveConfig, onClick: () -> Unit) {
    val size = config.albumCardSize
    Column(modifier = Modifier.width(size).clickable(onClick = onClick)) {
        Surface(modifier = Modifier.size(size), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp) {
            if (album.coverArt != null) AsyncImage(model = ApiClient.getCoverArtUrl(s, u, p, album.coverArt), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(48.dp)) }
        }
        Text(album.name, fontSize = config.cardTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(album.artist, fontSize = config.cardSubtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun TabletPlaylistCard(pl: Playlist, s: String, u: String, p: String, config: ResponsiveConfig, onClick: () -> Unit) {
    val size = config.playlistCardSize
    Column(modifier = Modifier.width(size).clickable(onClick = onClick)) {
        Surface(modifier = Modifier.size(size), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp) {
            if (pl.coverArt != null) AsyncImage(model = ApiClient.getCoverArtUrl(s, u, p, pl.coverArt), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text(pl.name, fontSize = config.cardTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text("${pl.songCount}首", fontSize = config.cardSubtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TabletSongRow(song: Song, s: String, u: String, p: String, config: ResponsiveConfig, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(config.songCoverSize), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            if (song.coverArt != null) AsyncImage(model = ApiClient.getCoverArtUrl(s, u, p, song.coverArt), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontSize = config.bodyFontSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(song.durationFormatted, fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TabletRadioRow(station: InternetRadioStation, config: ResponsiveConfig, onClick: () -> Unit) {
    val colors = listOf(Color(0xFFFF4D6A), Color(0xFFA78BFA), Color(0xFF5352ED), Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFFBBF24))
    val color = colors[station.name.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = color) { Box(contentAlignment = Alignment.Center) { Text("📻", fontSize = 22.sp) } }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, fontSize = config.bodyFontSize, fontWeight = FontWeight.SemiBold)
                Text("网络电台", fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.PlayArrow, "播放", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
    }
}

// ==================== Tablet Video Home Layout ====================

@Composable
private fun TabletVideoHomeContent(
    config: ResponsiveConfig,
    videoViewModel: com.lechenmusic.ui.VideoViewModel,
    isVideoLoggedIn: Boolean,
    videoHomeData: HomeRecommendData?,
    videoHomeLoading: Boolean,
    videoPlayRecords: List<VideoPlayRecord>,
    onNavigateToVideoDetail: (String, String) -> Unit,
    onNavigateToVideoCategory: (String) -> Unit,
    onNavigateToVideoSearch: () -> Unit,
    onNavigateToLive: () -> Unit
) {
    val pad = config.contentPadding
    val gap = config.itemSpacing

    if (!isVideoLoggedIn) {
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Tv, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))
                Text("请先配置影视服务器", fontSize = config.titleFontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("前往 我的 → 影视服务器 配置", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        return
    }

    fun navigateVideo(video: VideoInfo) {
        if (video.source.isNotBlank()) onNavigateToVideoDetail(video.source, video.id)
        else videoViewModel.searchAndPlay(video.title, video.id, video.year)
    }

    val hotMovies = videoHomeData?.hotMovies ?: emptyList()
    val hotTv = videoHomeData?.hotTvShows ?: emptyList()
    val hotVariety = videoHomeData?.hotVariety ?: emptyList()
    val hotAnime = videoHomeData?.hotAnime ?: emptyList()

    // 主内容 + 右侧最近播放面板
    Row(modifier = Modifier.fillMaxSize()) {
        // ===== 左侧: 主内容区 =====
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = pad, end = gap, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            // Hero 横幅 (21:9, rounded-3xl)
            val heroVideo = hotMovies.firstOrNull() ?: hotTv.firstOrNull()
            if (heroVideo != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(21f / 9f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { navigateVideo(heroVideo) }
                    ) {
                        if (heroVideo.displayCover.isNotBlank()) {
                            AsyncImage(model = heroVideo.displayCover, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(28.dp)) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFF4D6A)) {
                                Text("今日热门", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(heroVideo.title, fontSize = (config.sectionTitleSize.value + 10).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (heroVideo.desc.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(heroVideo.desc, fontSize = config.bodyFontSize, color = Color.White.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.6f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { navigateVideo(heroVideo) }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("立即播放", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { }, shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("收藏", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 热门电影 (横向卡片滚动)
            if (hotMovies.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("热门电影", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("查看全部 ›", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToVideoCategory("movie") })
                    }
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hotMovies) { video ->
                            com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                video = video,
                                onClick = { navigateVideo(video) }
                            )
                        }
                    }
                }
            }

            // 热门剧集 (横向卡片滚动)
            if (hotTv.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("热门剧集", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                        Text("查看全部 ›", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToVideoCategory("tv") })
                    }
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hotTv) { video ->
                            com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                video = video,
                                onClick = { navigateVideo(video) }
                            )
                        }
                    }
                }
            }

            // 热门综艺 (横向卡片滚动)
            if (hotVariety.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("热门综艺", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                        Text("查看全部 ›", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToVideoCategory("variety") })
                    }
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hotVariety) { video ->
                            com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                video = video,
                                onClick = { navigateVideo(video) }
                            )
                        }
                    }
                }
            }

            // 热门动漫 (横向卡片滚动)
            if (hotAnime.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("热门动漫", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("查看全部 ›", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToVideoCategory("anime") })
                    }
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hotAnime) { video ->
                            com.lechenmusic.ui.screens.video.VideoHorizontalCard(
                                video = video,
                                onClick = { navigateVideo(video) }
                            )
                        }
                    }
                }
            }

            // 加载指示器
            if (videoHomeLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) } }
            }
        }

        // ===== 右侧: 最近播放面板 =====
        if (videoPlayRecords.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(start = gap, end = pad, top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("最近播放", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(videoPlayRecords.take(10)) { record ->
                        TabletVideoContinueCard(record = record, config = config) {
                            if (record.source.isNotBlank()) onNavigateToVideoDetail(record.source, record.videoIdRaw)
                        }
                    }
                }
            }
        }
    }

    // 空状态
    if (!videoHomeLoading && hotMovies.isEmpty() && hotTv.isEmpty() && videoPlayRecords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无推荐数据", fontSize = config.bodyFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== Tablet Video Sub-Components ====================

@Composable
private fun TabletVideoCard(
    video: VideoInfo,
    config: ResponsiveConfig,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(config.cardWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(config.cardWidth)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            if (video.displayCover.isNotBlank()) {
                AsyncImage(
                    model = video.displayCover,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        video.title.take(1),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            // 评分标签
            if (video.rate != null) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 14.dp),
                    color = Color(0xFFFF6B81).copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        video.rate,
                        fontSize = config.captionFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            // 进度条
            if (video.displayPlayTime > 0 && video.displayTotalTime > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = video.progressPercent)
                            .height(3.dp)
                            .background(Color(0xFFE94560))
                    )
                }
            }
            // 集数标签
            if (video.displayTotalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "更新至${video.displayTotalEpisodes}集",
                        fontSize = config.captionFontSize,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text(
            video.title,
            fontSize = config.cardTitleSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            buildString {
                append(video.year)
                if (video.type.isNotBlank()) append(" · ${com.lechenmusic.ui.screens.video.categoryName(video.type)}")
            },
            fontSize = config.cardSubtitleSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TabletVideoContinueCard(
    record: VideoPlayRecord,
    config: ResponsiveConfig,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(135.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            if (record.cover.isNotBlank()) {
                AsyncImage(
                    model = record.cover,
                    contentDescription = record.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            // 进度条
            if (record.displayTotalTime > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = record.progressPercent)
                            .height(3.dp)
                            .background(Color(0xFFE94560))
                    )
                }
            }
            // 播放图标
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp).padding(6.dp)
                )
            }
            // 集数标签
            if (record.displayTotalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "看到第${record.displayEpisodeIndex + 1}集",
                        fontSize = config.captionFontSize,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text(
            record.title,
            fontSize = config.cardTitleSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ==================== Tablet Video Movie Card (2:3 竖向) ====================

@Composable
private fun TabletVideoMovieCard(
    video: VideoInfo,
    config: ResponsiveConfig,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        // 2:3 海报卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (video.displayCover.isNotBlank()) {
                AsyncImage(model = video.displayCover, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            // 评分标签 (右上角)
            if (video.rate != null) {
                Surface(shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 16.dp), color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(video.rate!!, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(video.title, fontSize = config.cardTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            buildString { append(video.year); if (video.type.isNotBlank()) append(" · ${com.lechenmusic.ui.screens.video.categoryName(video.type)}") },
            fontSize = config.cardSubtitleSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ==================== Tablet Video List Card (横向列表) ====================

@Composable
private fun TabletVideoListCard(
    video: VideoInfo,
    config: ResponsiveConfig,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // 封面 (竖版 3:4)
            Surface(
                modifier = Modifier.size(width = 64.dp, height = 85.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (video.displayCover.isNotBlank()) {
                    AsyncImage(model = video.displayCover, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                if (video.totalEpisodes > 1) {
                    Text("更新至 ${video.displayTotalEpisodes} 集", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(buildString { append(video.year); if (video.type.isNotBlank()) append(" · ${com.lechenmusic.ui.screens.video.categoryName(video.type)}") }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 标签
                if (video.type.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)) {
                            Text(
                                com.lechenmusic.ui.screens.video.categoryName(video.type),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (video.sourceName.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                                Text(
                                    video.sourceName,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Video Navigation Handler ====================

@Composable
private fun VideoNavigationHandler(
    videoViewModel: com.lechenmusic.ui.VideoViewModel?,
    onNavigateToVideoDetail: (String, String) -> Unit
) {
    val navigateToDetail = videoViewModel?.navigateToDetail?.collectAsState()?.value ?: false
    val searchDetail = videoViewModel?.videoDetail?.collectAsState()?.value
    androidx.compose.runtime.LaunchedEffect(navigateToDetail) {
        if (navigateToDetail && searchDetail != null) {
            videoViewModel?.consumeNavigateToDetail()
            onNavigateToVideoDetail(searchDetail.source, searchDetail.id)
        }
    }
    // 搜索播放源时显示加载弹窗
    val searchSourceLoading = videoViewModel?.searchSourceLoading?.collectAsState()?.value ?: false
    val searchSourceMsg = videoViewModel?.searchSourceMessage?.collectAsState()?.value ?: ""
    if (searchSourceLoading) {
        AlertDialog(
            onDismissRequest = { /* 不允许关闭 */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("搜索播放源", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    searchSourceMsg.ifBlank { "正在搜索，请稍候..." },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {}
        )
    }
}
