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
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    onNavigateToAudiobook: () -> Unit = {},
    onNavigateToAudiobookDetail: (String) -> Unit = {}
) {
    val newestAlbums by viewModel.newestAlbums.collectAsState()
    val randomAlbums by viewModel.randomAlbums.collectAsState()
    val dailySongs by viewModel.dailySongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlayedSongs by viewModel.recentPlayedSongs.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val homeMode by viewModel.homeMode.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    // Pull-to-refresh state


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                            .clickable { onNavigateToSearch() },
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
                                if (homeMode == "music") "搜索歌曲、专辑、歌手..." else "搜索有声书、演播者...",
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
                        }
                    }
                }

                // ===== MUSIC MODE =====
                if (homeMode == "music") {
                    // Hero
                    item {
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
                                            listOf(
                                                Color(0xFF6C5CE7),
                                                Color(0xFFA78BFA),
                                                Color(0xFFD4BBFF)
                                            )
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
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 3.dp
                                            ),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "今日精选",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        "为你推荐",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "根据你的口味生成",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
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
                if (homeMode == "audiobook") {
                    // Task 1: Carousel slider (5 slides)
                    item {
                        AudiobookCarousel(
                            audiobooks = audiobooks,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onAudiobookClick = onNavigateToAudiobookDetail
                        )
                    }
                    // Continue listening
                    if (audiobooks.isNotEmpty()) {
                        item {
                            SecHd("⏱️ 继续收听", "全部 ›") {}
                        }
                        items(audiobooks.take(2)) {
                            ContCard(
                                it,
                                serverUrl,
                                username,
                                password
                            ) {
                                viewModel.resumeAudiobook(it)
                                onNavigateToAudiobookDetail(it.id)
                            }
                        }
                    }
                    // Categories
                    item { SecHd("\uD83D\uDCC2 分类", "全部 ›", onNavigateToAudiobook) }
                    item { CatGrid { genre -> viewModel.loadAudiobooksByGenre(genre) } }
                    // Narrators
                    item { SecHd("\uD83C\uDFA4 演播者", "全部 ›") {} }
                    item {
                        val narrators by viewModel.narrators.collectAsState()
                        if (narrators.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(narrators.take(8)) { narr ->
                                    NarrItem(narr.name, "${'$'}{narr.count}部", getNarrColor(narr.name))
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无演播者", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // Recently updated
                    item { SecHd("\uD83C\uDD95 最近更新", "更多 ›") {} }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(audiobooks.take(5)) {
                                AbGridCard(
                                    it,
                                    serverUrl,
                                    username,
                                    password
                                ) { onNavigateToAudiobookDetail(it.id) }
                            }
                        }
                    }
                    // Hot ranking
                    item { SecHd("\uD83D\uDD25 热门榜单", "更多 ›") {} }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(audiobooks.take(4)) { i, b ->
                                RankCard(
                                    b,
                                    i + 1,
                                    serverUrl,
                                    username,
                                    password
                                ) { onNavigateToAudiobookDetail(b.id) }
                            }
                        }
                    }
                    // Favorites
                    item { SecHd("❤️ 我的收藏", "更多 ›") {} }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(audiobooks.take(3)) {
                                AbGridCard(
                                    it,
                                    serverUrl,
                                    username,
                                    password
                                ) { onNavigateToAudiobookDetail(it.id) }
                            }
                        }
                    }
                }
            }
        }

    }
}

// ==================== Task 1: Audiobook Carousel ====================

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
                        .size(80.dp, 107.dp)
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
                            .size(80.dp, 107.dp)
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
                modifier = Modifier
                    .width(56.dp)
                    .height(75.dp),
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
                Text(
                    "第${(book.chapterCount / 3).coerceAtLeast(1)}章",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                LinearProgressIndicator(
                    progress = 0.35f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "已听 28:15 / 45:20",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
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
private fun CatGrid() {
    val cats = listOf(
        Triple("\uD83D\uDCD6", "有声书", Color(0xFFE94560)),
        Triple("\uD83C\uDFAD", "评书", Color(0xFFF39C12)),
        Triple("\uD83C\uDFA4", "相声", Color(0xFF8E44AD)),
        Triple("\uD83D\uDC76", "儿童", Color(0xFF2ECC71))
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (row in cats.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for ((emoji, name, color) in row) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(color, color.copy(alpha = 0.7f))
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "$emoji $name",
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
private fun NarrItem(name: String, count: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = color
        ) {
            Box(contentAlignment = Alignment.Center) { Text("\uD83C\uDFA4", fontSize = 24.sp) }
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
            modifier = Modifier
                .width(140.dp)
                .height(187.dp),
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
                modifier = Modifier
                    .width(140.dp)
                    .height(187.dp),
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
