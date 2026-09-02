package com.lechenmusic.ui.screens.player.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
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
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.DisposableEffect

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Song
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.screens.player.parseLrc
import com.lechenmusic.ui.screens.player.findActiveLyricLine
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════
// MusicPlayerContent — 音乐播放器（手机+平板自适应）
//
// 设计参考: TabletPlayerScreen.kt + HTML模板
// 布局:
//   - 手机: 竖排（封面在上，信息+歌词在下）
//   - 平板: 横排（封面在左，信息+歌词在右）
//   - 底部: 进度条 + 控制按钮（共用）
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicPlayerContent(
    config: ResponsiveConfig,
    playerManager: MusicPlayerManager,
    viewModel: com.lechenmusic.ui.MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToArtistByName: (suspend (String) -> String?)? = null,
    onShowAddToPlaylist: () -> Unit = {},
    onShowQueue: () -> Unit = {},
    timerMinutes: Int = 0,
    timerRemainingSeconds: Long = 0L,
    onSetTimer: (Int) -> Unit = {}
) {
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val progress by playerManager.progress.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val shuffleMode by playerManager.shuffleMode.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val isStarred by playerManager.isStarred.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()

    val song = currentSong ?: return
    val isTablet = config.isMedium || config.isExpanded
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()

    // VerticalPager state - sync with current song index
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = currentIndex.coerceAtLeast(0),
        pageCount = { playlist.size.coerceAtLeast(1) }
    )

    // 标记：是否由用户滑动触发的切歌（防止双向同步导致回弹）
    var isUserSwiping by remember { mutableStateOf(false) }

    // When the pager settles on a new page, play that song
    LaunchedEffect(pagerState.settledPage) {
        val targetIndex = pagerState.settledPage
        if (targetIndex in playlist.indices && targetIndex != currentIndex) {
            isUserSwiping = true
            playerManager.playSong(playlist[targetIndex], playlist)
        }
    }

    // When the current song changes externally (e.g. from notification), sync pager
    LaunchedEffect(currentIndex) {
        if (isUserSwiping) {
            // 用户滑动触发的切歌，跳过同步，重置标记
            isUserSwiping = false
        } else if (currentIndex in playlist.indices && currentIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // Current page's song data
    val pageSong = playlist.getOrNull(pagerState.currentPage) ?: song
    LaunchedEffect(pageSong.id) { viewModel.loadLyrics(pageSong) }

    val coverUrl = ApiClient.getCoverArtUrl(serverUrl, username, password, pageSong.coverArt ?: pageSong.albumId)
    val coverBgColor = rememberCoverColor(coverUrl)

    // 始终使用白色文字和按钮（背景色已保证足够暗）
    val playerTextColor = Color.White
    val playerTextSecondary = Color.White.copy(alpha = 0.7f)
    val playerTextTertiary = Color.White.copy(alpha = 0.4f)
    val playerIconTint = Color.White
    val playerIconTintSecondary = Color.White.copy(alpha = 0.6f)
    val sliderActiveColor = Color.White
    val sliderInactiveColor = Color.White.copy(alpha = 0.3f)

    // 设置状态栏和导航栏颜色跟随播放器背景色
    val context = LocalContext.current
    DisposableEffect(coverBgColor) {
        com.lechenmusic.ui.theme.PlayerStatusBarColor.value = coverBgColor
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            window.navigationBarColor = coverBgColor.toArgb()
        }
        onDispose {
            com.lechenmusic.ui.theme.PlayerStatusBarColor.value = null
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    // Parse lyrics
    val lrcLines = remember(currentLyrics) { currentLyrics?.let { parseLrc(it) } }
    val plainLines = remember(currentLyrics) {
        if (lrcLines == null) currentLyrics?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        else emptyList()
    }

    // VerticalPager - TikTok-style swipe between songs
    // Each page renders the full player content for its song
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondBoundsPageCount = 0
    ) { page ->
        val pSong = playlist.getOrNull(page) ?: song
        val pCoverUrl = ApiClient.getCoverArtUrl(serverUrl, username, password, pSong.coverArt ?: pSong.albumId)
        val pBgColor = rememberCoverColor(pCoverUrl)
        // 始终使用白色（背景色已保证足够暗）
        val pTextColor = Color.White
        val pTextSecondary = Color.White.copy(alpha = 0.7f)
        val pTextTertiary = Color.White.copy(alpha = 0.4f)
        val pIconTint = Color.White
        val pIconTintSecondary = Color.White.copy(alpha = 0.6f)
        val pSliderActive = Color.White
        val pSliderInactive = Color.White.copy(alpha = 0.3f)

        // Update status bar for current visible page
        LaunchedEffect(pBgColor) {
            if (page == pagerState.currentPage) {
                com.lechenmusic.ui.theme.PlayerStatusBarColor.value = pBgColor
                val window = (context as? android.app.Activity)?.window
                window?.navigationBarColor = pBgColor.toArgb()
            }
        }

        // Load lyrics for current visible page
        LaunchedEffect(pSong.id) {
            if (page == pagerState.currentPage) viewModel.loadLyrics(pSong)
        }

        val pLrcLines = if (page == pagerState.currentPage) lrcLines else null
        val pPlainLines = if (page == pagerState.currentPage) plainLines else emptyList()

        Box(modifier = Modifier.fillMaxSize().background(pBgColor)) {
            // ── 全屏封面背景（酷狗/QQ音乐风格：封面图铺满，上下渐变遮罩） ──
            if (!isTablet && pCoverUrl != null) {
                AsyncImage(
                    model = pCoverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f
                )
                // 顶部渐变遮罩（状态栏区域加深）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    pBgColor.copy(alpha = 0.95f),
                                    pBgColor.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // 底部渐变遮罩（过渡到底部控制区背景色）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    pBgColor.copy(alpha = 0.6f),
                                    pBgColor
                                )
                            )
                        )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ── 顶部返回按钮 ──
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "返回", tint = pIconTint, modifier = Modifier.size(32.dp))
                }

                // ── 主内容区 ──
                if (isTablet) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            CoverImageDisplay(coverUrl = pCoverUrl, size = 320)
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            SongInfo(song = pSong, titleSize = 28.sp, artistSize = 18.sp, onArtistClick = { if (pSong.artistId.isNotBlank()) onNavigateToArtist(pSong.artistId) }, center = true, playerTextColor = pTextColor, playerTextSecondary = pTextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LyricsPanel(lrcLines = pLrcLines, plainLines = pPlainLines, currentPosition = currentPosition, playerTextColor = pTextColor, playerTextTertiary = pTextTertiary, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    // 手机：HorizontalPager 左右滑动
                    val hPagerState = rememberPagerState(pageCount = { 2 })
                    HorizontalPager(state = hPagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { hPage ->
                        when (hPage) {
                            0 -> Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 封面图（带阴影，居中显示，不裁切全屏——全屏背景已由上面的AsyncImage实现）
                                CoverImageDisplay(coverUrl = pCoverUrl, size = 260)
                                Spacer(modifier = Modifier.height(32.dp))
                                SongInfo(song = pSong, titleSize = 22.sp, artistSize = 14.sp, onArtistClick = { if (pSong.artistId.isNotBlank()) onNavigateToArtist(pSong.artistId) }, center = true, playerTextColor = pTextColor, playerTextSecondary = pTextSecondary)
                            }
                            1 -> Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    CoverImageDisplay(coverUrl = pCoverUrl, size = 48)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(pSong.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                        Text(pSong.artist, fontSize = 12.sp, color = pTextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                                LyricsPanel(lrcLines = pLrcLines, plainLines = pPlainLines, currentPosition = currentPosition, playerTextColor = pTextColor, playerTextTertiary = pTextTertiary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    // 页码指示器
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(2) { idx ->
                            Box(modifier = Modifier.padding(horizontal = 4.dp).size(if (hPagerState.currentPage == idx) 8.dp else 6.dp).clip(CircleShape).background(if (hPagerState.currentPage == idx) pTextColor else pTextTertiary))
                        }
                    }
                }

                // ── 底部控制栏 ──
                var showSleepTimerDialog by remember { mutableStateOf(false) }
                PlayerControls(
                    isTablet = isTablet, progress = progress, currentPosition = currentPosition, duration = duration,
                    isPlaying = isPlaying, isStarred = isStarred, shuffleMode = shuffleMode, repeatMode = repeatMode,
                    playerTextColor = pTextColor, playerTextSecondary = pTextSecondary, playerTextTertiary = pTextTertiary,
                    playerIconTint = pIconTint, playerIconTintSecondary = pIconTintSecondary, sliderActiveColor = pSliderActive, sliderInactiveColor = pSliderInactive,
                    timerMinutes = timerMinutes, timerRemainingSeconds = timerRemainingSeconds,
                    onSeek = { playerManager.seekToProgress(it) }, onPlayPause = { playerManager.togglePlayPause() },
                    onPrevious = { playerManager.skipPrevious() }, onNext = { playerManager.skipNext() },
                    onToggleStar = { playerManager.toggleStar() }, onToggleShuffle = { playerManager.toggleShuffle() }, onToggleRepeat = { playerManager.toggleRepeat() },
                    onShowAddToPlaylist = onShowAddToPlaylist, onShowQueue = onShowQueue, onAddToQueue = { playerManager.addToQueue(pSong) },
                    onShowTimer = { showSleepTimerDialog = true }
                )
                if (showSleepTimerDialog) {
                    com.lechenmusic.ui.components.SleepTimerDialog(timerMinutes = timerMinutes, onSetTimer = onSetTimer, onDismiss = { showSleepTimerDialog = false })
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
// 私有组件
// ═══════════════════════════════════════════════════════════

// ── 封面图 ───────────────────────────────────────────────

@Composable
fun CoverImageDisplay(coverUrl: String?, size: Int = 320) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
        }
    }
}


// ── 歌曲信息 ─────────────────────────────────────────────

@Composable
private fun SongInfo(
    song: Song,
    titleSize: TextUnit,
    artistSize: TextUnit,
    onArtistClick: () -> Unit,
    onArtistNameClick: ((String) -> Unit)? = null,
    center: Boolean = false,
    playerTextColor: Color = Color.White,
    playerTextSecondary: Color = Color.White.copy(alpha = 0.7f)
) {
    Column(
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            song.title,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = playerTextColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (center) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
        )
        Spacer(modifier = Modifier.height(6.dp))
        // 多歌手支持：用 · 分隔，每个歌手可独立点击
        val artistParts = song.artist.split("·", "、", "/").map { it.trim() }.filter { it.isNotEmpty() }
        if (artistParts.size > 1 && onArtistNameClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                artistParts.forEachIndexed { index, artistName ->
                    if (index > 0) {
                        Text(
                            " · ",
                            fontSize = artistSize,
                            color = playerTextSecondary
                        )
                    }
                    Text(
                        artistName,
                        fontSize = artistSize,
                        color = playerTextSecondary,
                        modifier = Modifier.clickable { onArtistNameClick(artistName) }
                    )
                }
            }
        } else {
            Text(
                song.artist,
                fontSize = artistSize,
                color = playerTextSecondary,
                modifier = Modifier.clickable(onClick = onArtistClick)
            )
        }
    }
}


// ── 歌词面板 ─────────────────────────────────────────────

@Composable
private fun LyricsPanel(
    lrcLines: List<com.lechenmusic.ui.screens.player.LyricLine>?,
    plainLines: List<String>,
    currentPosition: Long,
    playerTextColor: Color = Color.White,
    playerTextTertiary: Color = Color.White.copy(alpha = 0.4f),
    modifier: Modifier = Modifier
) {
    if (lrcLines != null) {
        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
        val listState = rememberLazyListState()
        // 每次 currentPosition 变化时滚动到当前歌词行
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0))
            }
        }

        LazyColumn(state = listState, modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
            itemsIndexed(lrcLines) { index, line ->
                val isActive = index == activeIndex
                Text(
                    text = line.text,
                    fontSize = if (isActive) 20.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) playerTextColor else playerTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = if (isActive) 6.dp else 4.dp).fillMaxWidth()
                )
            }
        }
    } else if (plainLines.isNotEmpty()) {
        LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
            itemsIndexed(plainLines) { _, line ->
                Text(line, fontSize = 16.sp, color = playerTextTertiary, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth())
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", fontSize = 16.sp, color = playerTextTertiary)
        }
    }
}


// ── 底部控制栏 ───────────────────────────────────────────

@Composable
private fun PlayerControls(
    isTablet: Boolean = false,
    progress: Float,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isStarred: Boolean,
    shuffleMode: Boolean,
    repeatMode: RepeatMode,
    playerTextColor: Color = Color.White,
    playerTextSecondary: Color = Color.White.copy(alpha = 0.7f),
    playerTextTertiary: Color = Color.White.copy(alpha = 0.4f),
    playerIconTint: Color = Color.White,
    playerIconTintSecondary: Color = Color.White.copy(alpha = 0.6f),
    sliderActiveColor: Color = Color.White,
    sliderInactiveColor: Color = Color.White.copy(alpha = 0.3f),
    timerMinutes: Int = 0,
    timerRemainingSeconds: Long = 0L,
    onSeek: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowAddToPlaylist: () -> Unit,
    onShowQueue: () -> Unit,
    onAddToQueue: () -> Unit,
    onShowTimer: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 进度条
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(currentPosition), fontSize = 12.sp, color = playerTextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = playerTextColor, activeTrackColor = sliderActiveColor, inactiveTrackColor = sliderInactiveColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(formatTime(duration), fontSize = 12.sp, color = playerTextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isTablet) {
                // ── 平板：一排显示 ──
                // [定时 收藏] [随机 上一首 ▶ 下一首 循环] [添加到 播放队列]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：定时 + 收藏
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onShowTimer, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Timer, "定时",
                                tint = if (timerMinutes > 0) playerIconTint else playerIconTintSecondary,
                                modifier = Modifier.size(20.dp))
                        }
                        if (timerMinutes > 0 && timerRemainingSeconds > 0) {
                            val remMin = (timerRemainingSeconds / 60).toInt()
                            val remSec = (timerRemainingSeconds % 60).toInt()
                            Text("%02d:%02d".format(remMin, remSec), fontSize = 9.sp, color = playerIconTint)
                        } else if (timerMinutes > 0) {
                            Text("${timerMinutes}分", fontSize = 9.sp, color = playerIconTint)
                        }
                    }
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(40.dp)) {
                        Icon(if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏",
                            tint = if (isStarred) Color(0xFFFF4D6A) else playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 中间：主控制
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Shuffle, "随机", tint = if (shuffleMode) playerIconTint else playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "上一首", tint = playerIconTint, modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF1A1A2E))
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "下一首", tint = playerIconTint, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                        Icon(
                            when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                            "循环",
                            tint = if (repeatMode != RepeatMode.OFF) playerIconTint else playerIconTintSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 右侧：添加到 + 播放队列
                    var showAddMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showAddMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlaylistAdd, "添加到", tint = playerIconTintSecondary, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text("添加到歌单") }, onClick = { showAddMenu = false; onShowAddToPlaylist() }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) })
                            DropdownMenuItem(text = { Text("添加到播放队列") }, onClick = { showAddMenu = false; onAddToQueue() }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) })
                        }
                    }
                    IconButton(onClick = onShowQueue, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.QueueMusic, "播放队列", tint = playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // ── 手机：两排显示 ──
                // 第1行：主控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Shuffle, "随机", tint = if (shuffleMode) playerIconTint else playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "上一首", tint = playerIconTint, modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF1A1A2E))
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "下一首", tint = playerIconTint, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                        Icon(
                            when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                            "循环",
                            tint = if (repeatMode != RepeatMode.OFF) playerIconTint else playerIconTintSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 第2行：辅助控制
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(40.dp)) {
                        Icon(if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏",
                            tint = if (isStarred) Color(0xFFFF4D6A) else playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onShowTimer, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Timer, "定时",
                                tint = if (timerMinutes > 0) playerIconTint else playerIconTintSecondary,
                                modifier = Modifier.size(20.dp))
                        }
                        if (timerMinutes > 0 && timerRemainingSeconds > 0) {
                            val remMin = (timerRemainingSeconds / 60).toInt()
                            val remSec = (timerRemainingSeconds % 60).toInt()
                            Text("%02d:%02d".format(remMin, remSec), fontSize = 9.sp, color = playerIconTint)
                        } else if (timerMinutes > 0) {
                            Text("${timerMinutes}分", fontSize = 9.sp, color = playerIconTint)
                        }
                    }
                    var showAddMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showAddMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlaylistAdd, "添加到", tint = playerIconTintSecondary, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text("添加到歌单") }, onClick = { showAddMenu = false; onShowAddToPlaylist() }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) })
                            DropdownMenuItem(text = { Text("添加到播放队列") }, onClick = { showAddMenu = false; onAddToQueue() }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) })
                        }
                    }
                    IconButton(onClick = onShowQueue, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.QueueMusic, "播放队列", tint = playerIconTintSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}


// ── 封面颜色提取 ─────────────────────────────────────────

// 封面主色缓存（URL -> 提取后的暗化背景色）
private val coverColorCache = object : android.util.LruCache<String, Color>(100) {
    override fun sizeOf(key: String, value: Color) = 1
}

// 默认深色背景（封面加载前 / 提取失败时使用）
private val defaultCoverColor = Color(0xFF1A1A2E)

/**
 * 从封面图提取主色作为播放器背景色。
 *
 * 参考业界做法（Apple Music / Spotify / 网易云）：
 * 1. 用 Palette 提取封面图的 Dominant / Muted / Vibrant 色
 * 2. 优先选 Muted（柔和）或 Dominant，避免高饱和色刺眼
 * 3. 降低亮度（确保白色文字可读）+ 稍微增加饱和度（避免灰蒙蒙）
 * 4. 如果提取结果亮度仍然过高，则进一步压暗
 */
@Composable
fun rememberCoverColor(coverUrl: String?): Color {
    val context = LocalContext.current
    // 默认深色，避免闪烁
    var color by remember(coverUrl) { mutableStateOf(coverColorCache.get(coverUrl) ?: defaultCoverColor) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) {
            color = defaultCoverColor
            return@LaunchedEffect
        }
        // 先检查缓存
        coverColorCache.get(coverUrl)?.let {
            color = it
            return@LaunchedEffect
        }
        // 异步加载封面图并提取颜色
        try {
            val bitmap = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .size(128) // 小图即可，只要提取颜色
                    .allowHardware(false) // Palette 需要 software bitmap
                    .build()
                val result = coil.ImageLoader(context).execute(request)
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(16)
                        .resizeBitmapArea(128 * 128) // 控制计算量
                        .generate()
                }
                // 选色优先级：Muted > Vibrant > DarkMuted > DarkVibrant > Dominant
                // Muted 色柔和不刺眼，最适合作为背景
                val swatch = palette.mutedSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.darkMutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch

                if (swatch != null) {
                    val extracted = Color(swatch.rgb)
                    val darkened = ensureDarkBackground(extracted)
                    color = darkened
                    coverColorCache.put(coverUrl, darkened)
                } else {
                    // Palette 没提取到颜色，用封面图主色的近似值
                    color = defaultCoverColor
                    coverColorCache.put(coverUrl, defaultCoverColor)
                }
            }
        } catch (_: Exception) {
            // 加载失败，保持默认色
        }
    }
    return color
}

/**
 * 确保背景色足够暗，白色文字可读。
 *
 * 策略（参考 Spotify / Apple Music）：
 * - 目标亮度 (L in HSL) ≤ 0.35（白色文字 WCAG 对比度 ≥ 4.5:1）
 * - 如果亮度过高，直接压暗到目标亮度
 * - 同时稍微提高饱和度，避免压暗后颜色发灰
 * - 如果原始色已是深色，保持不变
 */
private fun ensureDarkBackground(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val hue = hsv[0]        // 0-360
    val saturation = hsv[1] // 0-1
    val brightness = hsv[2] // 0-1

    // 目标：亮度 ≤ 0.35，饱和度保持或略增
    val targetBrightness = 0.35f
    val newBrightness = brightness.coerceAtMost(targetBrightness)
    // 如果压暗了亮度，适当增加饱和度（避免发灰）
    val newSaturation = if (brightness > targetBrightness) {
        (saturation * 1.3f).coerceAtMost(1.0f)
    } else {
        saturation
    }

    val newColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, newSaturation, newBrightness))
    return Color(newColor)
}


// ── 工具函数 ─────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
