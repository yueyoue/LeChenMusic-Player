package com.lechenmusic.ui.screens.home.music

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.InternetRadioStation
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song
import com.lechenmusic.data.model.SlideConfig
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════
// MusicHomeContent — 音乐首页（手机+平板自适应）
//
// 设计参考: lechen-ui-demo/responsive-complete.html
// 布局顺序:
//   0. 头部区域（搜索栏+模式切换，仅手机）
//   1. Hero Banner
//   2. 快捷入口 (歌手/专辑/歌单/电台/缓存)
//   3. 为你精选 (横向滚动大卡片)
//   4. 歌单广场 (横向滚动封面)
//   5. 每日推荐 (歌曲列表)
//   6. 最新专辑 (横向滚动封面)
//   7. 随机专辑 (仅平板，手机已移除)
//   8. 最近播放 (歌曲列表)
//   9. 电台 (网格卡片)
// ═══════════════════════════════════════════════════════════

@Composable
fun MusicHomeContent(
    config: ResponsiveConfig,
    // 数据
    newestAlbums: List<Album>,
    randomAlbums: List<Album>,
    dailySongs: List<Song>,
    topPlayedSongs: List<Song>,
    playlists: List<Playlist>,
    recentPlayedSongs: List<Song>,
    radioStations: List<InternetRadioStation>,
    musicSlides: List<SlideConfig>,
    serverUrl: String,
    username: String,
    password: String,
    // 回调
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
    onRefreshDaily: () -> Unit = {},
    onPlayRadio: (InternetRadioStation) -> Unit = {},
    onSongMenu: ((Song) -> Unit)? = null,
    starredRadioIds: Set<String> = emptySet(),
    // 手机头部（搜索栏+模式切换，仅手机传入）
    headerContent: (@Composable () -> Unit)? = null
) {
    val pad = config.contentPadding
    val gap = config.itemSpacing
    val isTablet = config.isMedium || config.isExpanded

    // Sort radio stations: starred (pinned) first
    val sortedRadioStations = remember(radioStations, starredRadioIds) {
        radioStations.sortedByDescending { starredRadioIds.contains(it.id) }
    }
    // 歌单随机排序（每次进入首页重新打乱）
    val shuffledPlaylists = remember(playlists) { playlists.shuffled() }

    if (isTablet) {
        // ═══ 平板布局：左侧主内容 + 右侧边栏 ═══
        Row(modifier = Modifier.fillMaxSize().padding(start = pad, end = pad, top = pad)) {
            // ── 左侧主内容 ──
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(end = gap, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(config.sectionSpacing)
            ) {
                // Hero
                item {
                    MusicHero(
                        slides = musicSlides,
                        playlists = playlists,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        heroHeight = config.heroHeight,
                        titleSize = config.sectionTitleSize,
                        bodySize = config.bodyFontSize,
                        onPlaylistClick = onPlaylistClick
                    )
                }
                // 快捷入口按钮
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        val quickItems = listOf(
                            Triple(Icons.Default.Person, "歌手", onNavigateToArtists),
                            Triple(Icons.Default.Album, "专辑", onNavigateToAlbums),
                            Triple(Icons.Default.QueueMusic, "歌单", onNavigateToAllPlaylists),
                            Triple(Icons.Default.Radio, "电台", onNavigateToRadio),
                            Triple(Icons.Default.Download, "缓存", onNavigateToCachedMusic)
                        )
                        quickItems.forEach { (icon, label, onClick) ->
                            Surface(
                                onClick = onClick,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = config.cardSubtitleSize, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                // 歌单广场
                item { SectionHead(title = "歌单广场", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToAllPlaylists) }
                item { PlaylistRow(playlists = shuffledPlaylists, coverSize = config.playlistCardSize, titleSize = config.cardTitleSize, subtitleSize = config.cardSubtitleSize, gap = gap, serverUrl = serverUrl, username = username, password = password, onClick = onPlaylistClick) }
                // 每日推荐（单列，和手机一致）
                item { SectionHead(title = "每日推荐", action = "换一批 ↻", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onRefreshDaily) }
                items(dailySongs.take(6)) { song ->
                    SongListItem(song = song, serverUrl = serverUrl, username = username, password = password, titleSize = config.bodyFontSize, subtitleSize = config.captionFontSize, coverSize = config.songCoverSize, onClick = { onSongClick(song, dailySongs) })
                }
                // 排行榜（最多播放）
                item { SectionHead(title = "排行榜", action = "", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize) }
                itemsIndexed(topPlayedSongs.take(5)) { index, song ->
                    SongListItem(
                        index = index + 1,
                        song = song,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        titleSize = config.bodyFontSize,
                        subtitleSize = config.captionFontSize,
                        coverSize = config.songCoverSize,
                        onClick = { onSongClick(song, topPlayedSongs) }
                    )
                }
                // 随机专辑
                item { SectionHead(title = "随机专辑", action = "换一批 ↻", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize) }
                item { AlbumRow(albums = randomAlbums, coverSize = config.albumCardSize, titleSize = config.cardTitleSize, subtitleSize = config.cardSubtitleSize, gap = gap, serverUrl = serverUrl, username = username, password = password, onClick = onAlbumClick) }
            }

            // ── 右侧边栏 ──
            LazyColumn(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(config.sectionSpacing)
            ) {
                // 最近播放
                item { SectionHead(title = "最近播放", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToRecentPlayed) }
                items(recentPlayedSongs.take(6)) { song ->
                    SongListItem(song = song, serverUrl = serverUrl, username = username, password = password, titleSize = config.cardSubtitleSize, subtitleSize = config.captionFontSize, coverSize = 40.dp, onClick = { onSongClick(song, recentPlayedSongs) })
                }
                // 电台
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHead(title = "电台", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToRadio) }
                item { RadioGrid(stations = sortedRadioStations.take(6), titleSize = config.cardSubtitleSize, subtitleSize = config.captionFontSize, serverUrl = serverUrl, username = username, password = password, onClick = onPlayRadio) }
            }
        }
    } else {
        // ═══ 手机布局：单列 ═══
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = pad, end = pad, top = pad, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(config.sectionSpacing)
        ) {
            // ── 0. 头部（搜索栏+模式切换） ──
            if (headerContent != null) {
                item { headerContent() }
            }

            // ── 1. Hero Banner ──
            item {
                MusicHero(
                    slides = musicSlides,
                    playlists = playlists,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    heroHeight = config.heroHeight,
                    titleSize = config.sectionTitleSize,
                    bodySize = config.bodyFontSize,
                    onPlaylistClick = onPlaylistClick
                )
            }

            // ── 2. 快捷入口 ──
            item {
                QuickActions(
                    iconSize = 48.dp, labelSize = config.cardSubtitleSize, gap = 12.dp,
                    onArtists = onNavigateToArtists, onAlbums = onNavigateToAlbums,
                    onPlaylists = onNavigateToAllPlaylists, onAllSongs = onNavigateToAllSongs,
                    onRadio = onNavigateToRadio, onCached = onNavigateToCachedMusic
                )
            }

        // ── 4. 歌单广场 ──
        item {
            SectionHead(title = "歌单广场", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToAllPlaylists)
        }
        item {
            PlaylistGrid(
                playlists = shuffledPlaylists,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = onPlaylistClick
            )
        }

        // ── 5. 每日推荐（横向滑动，每页3首） ──
        item {
            SectionHead(title = "每日推荐", action = "换一批 ↻", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onRefreshDaily)
        }
        item {
            HotSongPager(
                songs = dailySongs,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { song -> onSongClick(song, dailySongs) }
            )
        }

        // ── 6. 排行榜（横向滑动，每页3首） ──
        item {
            SectionHead(title = "排行榜", action = "", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize)
        }
        item {
            HotSongPager(
                songs = topPlayedSongs,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { song -> onSongClick(song, topPlayedSongs) }
            )
        }

        // ── 8. 最近播放 ──
        item {
            SectionHead(title = "最近播放", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToRecentPlayed)
        }
        items(recentPlayedSongs.take(6)) { song ->
            SongListItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                titleSize = config.bodyFontSize,
                subtitleSize = config.captionFontSize,
                coverSize = config.songCoverSize,
                onClick = { onSongClick(song, recentPlayedSongs) }
            )
        }

        // ── 9. 电台 ──
        item {
            SectionHead(title = "电台", action = "更多 ›", titleSize = config.sectionTitleSize, captionSize = config.captionFontSize, onClick = onNavigateToRadio)
        }
        item {
            RadioGrid(
                stations = sortedRadioStations.take(6),
                titleSize = config.cardSubtitleSize,
                subtitleSize = config.captionFontSize,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = onPlayRadio
            )
        }
    }
    } // else (手机布局)
}


// ═══════════════════════════════════════════════════════════
// 私有组件 — 各区块
// ═══════════════════════════════════════════════════════════

// ── Hero Banner ──────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicHero(
    slides: List<SlideConfig>,
    playlists: List<Playlist> = emptyList(),
    serverUrl: String,
    username: String = "",
    password: String = "",
    heroHeight: Dp,
    titleSize: TextUnit,
    bodySize: TextUnit,
    onPlaylistClick: ((String) -> Unit)? = null
) {
    // 取最多5个随机歌单用于幻灯显示
    val heroPlaylists = remember(playlists) {
        if (playlists.isNotEmpty()) playlists.shuffled().take(5) else emptyList()
    }
    // 合并歌单和幻灯作为展示项
    val carouselItems = remember(heroPlaylists, slides) {
        if (heroPlaylists.isNotEmpty()) {
            heroPlaylists.map { pl ->
                CarouselItem(
                    title = pl.name,
                    coverArt = pl.coverArt,
                    imageUrl = null,
                    playlistId = pl.id
                )
            }
        } else {
            slides.map { slide ->
                CarouselItem(
                    title = slide.title.ifEmpty { "每日推荐" },
                    coverArt = null,
                    imageUrl = slide.imageUrl,
                    playlistId = null
                )
            }
        }
    }

    if (carouselItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    // 自动轮播 — 使用 snapshotFlow 避免动画期间的竞态条件
    LaunchedEffect(carouselItems.size) {
        while (true) {
            delay(4000)
            if (pagerState.isScrollInProgress) continue
            val nextPage = (pagerState.currentPage + 1) % carouselItems.size
            try {
                pagerState.animateScrollToPage(nextPage)
            } catch (_: Exception) {
                // 动画被中断时安全恢复，继续下一轮
            }
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(heroHeight)
        ) { page ->
            val item = carouselItems[page]
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
                    .then(
                        if (item.playlistId != null && onPlaylistClick != null)
                            Modifier.clickable { onPlaylistClick(item.playlistId) }
                        else Modifier
                    ),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient))
                ) {
                    // 背景图
                    if (item.coverArt != null && item.coverArt.isNotEmpty()) {
                        AsyncImage(
                            model = ApiClient.getCoverArtUrl(serverUrl, username, password, item.coverArt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    } else if (item.imageUrl != null && item.imageUrl.isNotEmpty()) {
                        val fullUrl = if (item.imageUrl.startsWith("http")) item.imageUrl
                        else "${serverUrl.trimEnd('/')}${item.imageUrl}"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }

                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    )

                    // 内容
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "✨ 精选推荐",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            item.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("根据你的听歌口味，AI 智能推荐", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // 页面指示器
        if (carouselItems.size > 1) {
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
}

/** 幻灯展示项 */
private data class CarouselItem(
    val title: String,
    val coverArt: String?,
    val imageUrl: String?,
    val playlistId: String?
)


// ── 快捷入口 ─────────────────────────────────────────────

@Composable
private fun QuickActions(
    iconSize: Dp,
    labelSize: TextUnit,
    gap: Dp,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onPlaylists: () -> Unit,
    onAllSongs: () -> Unit,
    onRadio: () -> Unit,
    onCached: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickItem(Icons.Default.Person, "歌手", Color(0xFFA855F7), iconSize, labelSize, onArtists)
        QuickItem(Icons.Default.Album, "专辑", Color(0xFF4A9EFF), iconSize, labelSize, onAlbums)
        QuickItem(Icons.Default.LibraryMusic, "歌单", Color(0xFF00E68A), iconSize, labelSize, onPlaylists)
        QuickItem(Icons.Default.Radio, "电台", Color(0xFFFF4D6A), iconSize, labelSize, onRadio)
        QuickItem(Icons.Default.Download, "缓存", Color(0xFFFFD93D), iconSize, labelSize, onCached)
    }
}

@Composable
private fun QuickItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    iconSize: Dp,
    labelSize: TextUnit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = labelSize, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// ── 区块标题 ─────────────────────────────────────────────

@Composable
private fun SectionHead(
    title: String,
    action: String,
    titleSize: TextUnit,
    captionSize: TextUnit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = titleSize, fontWeight = FontWeight.SemiBold)
        if (action.isNotEmpty()) {
            Text(
                action,
                fontSize = captionSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
        }
    }
}


// ── 为你精选（大卡片横向滚动）────────────────────────────

@Composable
private fun FeaturedSection(
    playlists: List<Playlist>,
    cardWidth: Dp,
    cardHeight: Dp,
    titleSize: TextUnit,
    captionSize: TextUnit,
    gap: Dp,
    onPlaylistClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        items(playlists.take(5)) { pl ->
            FeaturedCard(
                tag = "歌单",
                title = pl.name,
                subtitle = "${pl.songCount}首",
                gradient = Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF3CAC))),
                width = cardWidth,
                height = cardHeight,
                titleSize = (titleSize.value + 2).sp,
                captionSize = captionSize,
                onClick = { onPlaylistClick(pl.id) }
            )
        }
    }
}

@Composable
private fun FeaturedCard(
    tag: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    width: Dp,
    height: Dp,
    titleSize: TextUnit,
    captionSize: TextUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF6B35)
            ) {
                Text(
                    tag,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = captionSize, color = Color.White.copy(alpha = 0.7f))
        }
    }
}


// ── 歌单横向滚动 ─────────────────────────────────────────

@Composable
private fun PlaylistRow(
    playlists: List<Playlist>,
    coverSize: Dp,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    gap: Dp,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
        items(playlists) { pl ->
            Column(
                modifier = Modifier
                    .width(coverSize)
                    .clickable { onClick(pl.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!pl.coverArt.isNullOrEmpty()) {
                        AsyncImage(
                            model = ApiClient.getCoverArtUrl(serverUrl, username, password, pl.coverArt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(40.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(pl.name, fontSize = titleSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pl.songCount}首", fontSize = subtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


// ── 歌单网格（手机：一排两个，12项列表样式）──

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        playlists.take(12).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { pl ->
                    PlaylistGridItem(pl, serverUrl, username, password, onClick)
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.PlaylistGridItem(
    pl: Playlist,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick(pl.id) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!pl.coverArt.isNullOrEmpty()) {
                    AsyncImage(
                        model = ApiClient.getCoverArtUrl(serverUrl, username, password, pl.coverArt),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.LibraryMusic,
                        null,
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pl.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${pl.songCount}首",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}


// ── 歌曲列表项 ───────────────────────────────────────────

@Composable
private fun SongListItem(
    index: Int? = null,
    song: Song,
    serverUrl: String,
    username: String,
    password: String,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    coverSize: Dp,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号
        if (index != null) {
            Text(
                "%02d".format(index),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (index <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }

        // 封面
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val coverUrl = if (!song.coverArt.isNullOrEmpty()) {
                ApiClient.getCoverArtUrl(serverUrl, username, password, song.coverArt)
            } else if (!song.albumId.isNullOrEmpty()) {
                ApiClient.getCoverArtUrl(serverUrl, username, password, song.albumId)
            } else null

            if (coverUrl != null) {
                AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(20.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 歌曲信息
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontSize = titleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 品质图标
                val qualityText = com.lechenmusic.ui.components.getQualityText(song)
                if (qualityText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = com.lechenmusic.ui.components.getQualityColor(song).copy(alpha = 0.15f)
                    ) {
                        Text(
                            qualityText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.lechenmusic.ui.components.getQualityColor(song),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(song.artist, fontSize = subtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // 自定义尾部内容（如三个点菜单）
        if (trailingContent != null) {
            trailingContent()
        }

        // 时长
        if (song.duration > 0) {
            val min = song.duration / 60
            val sec = song.duration % 60
            Text(
                "%d:%02d".format(min, sec),
                fontSize = subtitleSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ── 专辑横向滚动 ─────────────────────────────────────────

@Composable
private fun AlbumRow(
    albums: List<Album>,
    coverSize: Dp,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    gap: Dp,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .width(coverSize)
                    .clickable { onClick(album.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!album.coverArt.isNullOrEmpty()) {
                        AsyncImage(
                            model = ApiClient.getCoverArtUrl(serverUrl, username, password, album.coverArt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Album, null, modifier = Modifier.size(40.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(album.name, fontSize = titleSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, fontSize = subtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── 专辑网格（手机：一排两个）──

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        albums.take(6).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { album ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(album.id) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (!album.coverArt.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ApiClient.getCoverArtUrl(serverUrl, username, password, album.coverArt),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Album, null, modifier = Modifier.size(40.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(album.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(album.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}


// ── 热歌横向滑动（每日推荐/排行榜） ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HotSongPager(
    songs: List<Song>,
    serverUrl: String,
    username: String,
    password: String,
    onClick: (Song) -> Unit
) {
    val pages = songs.take(18).chunked(3)
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pages[page].forEachIndexed { index, song ->
                    val globalIndex = page * 3 + index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(song) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 序号
                            Text(
                                "%02d".format(globalIndex + 1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (globalIndex < 3) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            // 封面
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val coverUrl = if (!song.coverArt.isNullOrEmpty()) {
                                    ApiClient.getCoverArtUrl(serverUrl, username, password, song.coverArt)
                                } else if (!song.albumId.isNullOrEmpty()) {
                                    ApiClient.getCoverArtUrl(serverUrl, username, password, song.albumId)
                                } else null

                                if (coverUrl != null) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        null,
                                        modifier = Modifier.size(20.dp).align(Alignment.Center),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            // 歌曲信息
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val qualityText = com.lechenmusic.ui.components.getQualityText(song)
                                    if (qualityText.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = com.lechenmusic.ui.components.getQualityColor(song).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                qualityText,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = com.lechenmusic.ui.components.getQualityColor(song),
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        song.artist,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // 时长
                            if (song.duration > 0) {
                                val min = song.duration / 60
                                val sec = song.duration % 60
                                Text(
                                    "%d:%02d".format(min, sec),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 页面指示器
        if (pages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
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
}

// ── 电台网格 ─────────────────────────────────────────────

@Composable
private fun RadioGrid(
    stations: List<InternetRadioStation>,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    serverUrl: String = "",
    username: String = "",
    password: String = "",
    onClick: (InternetRadioStation) -> Unit
) {
    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF3CAC))),
        Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))),
        Brush.linearGradient(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
        Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
        Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
        Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1)))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stations.take(6).chunked(2).forEachIndexed { rowIdx, rowStations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStations.forEachIndexed { colIdx, station ->
                    val colorIdx = rowIdx * 2 + colIdx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(station) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(gradients[colorIdx % gradients.size]),
                                contentAlignment = Alignment.Center
                            ) {
                                // 优先显示电台封面图
                                if (!station.coverArt.isNullOrEmpty() && serverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ApiClient.getCoverArtUrl(serverUrl, username, password, station.coverArt),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Radio, null, modifier = Modifier.size(20.dp), tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(station.name, fontSize = titleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                // 奇数个时填充空白
                if (rowStations.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
