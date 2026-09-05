package com.lechenmusic.ui.screens.player.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.DisposableEffect

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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

        // Load lyrics for current visible page (only trigger load, don't clear here)
        // Lyrics loading is handled by the outer LaunchedEffect(pageSong.id)

        val pLrcLines = if (page == pagerState.currentPage) lrcLines else null
        val pPlainLines = if (page == pagerState.currentPage) plainLines else emptyList()

        Box(modifier = Modifier.fillMaxSize().background(pBgColor)) {
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
                            SongInfo(song = pSong, titleSize = 24.sp, artistSize = 18.sp, onArtistClick = { if (pSong.artistId.isNotBlank()) onNavigateToArtist(pSong.artistId) }, center = true, playerTextColor = pTextColor, playerTextSecondary = pTextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LyricsPanel(lrcLines = pLrcLines, plainLines = pPlainLines, currentPosition = currentPosition, playerTextColor = pTextColor, playerTextTertiary = pTextTertiary, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    // ── 手机：封面+歌曲信息+歌词 全部可左右滑动 ──
                    val hPagerState = rememberPagerState(pageCount = { 2 })

                    HorizontalPager(
                        state = hPagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { hPage ->
                        when (hPage) {
                            0 -> {
                                // 第1页：封面图(上方) + 歌词预览 + 歌曲信息
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 封面图（上方，Fit模式完整显示，背景色自然填充周围空间）
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(pBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pCoverUrl != null) {
                                            AsyncImage(
                                                model = pCoverUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                            }
                                        }
                                    }
                                    // 歌词预览（封面下方）— 固定高度防止封面跳动
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(96.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SmoothLyricsDisplay(
                                            lrcLines = pLrcLines,
                                            plainLines = pPlainLines,
                                            currentPosition = currentPosition,
                                            playerTextColor = pTextColor,
                                            playerTextTertiary = pTextTertiary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp)
                                        )
                                    }
                                    // 歌曲信息
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        SongInfo(
                                            song = pSong,
                                            titleSize = 20.sp,
                                            artistSize = 14.sp,
                                            onArtistClick = { if (pSong.artistId.isNotBlank()) onNavigateToArtist(pSong.artistId) },
                                            onArtistNameClick = { _ -> },
                                            center = false,
                                            playerTextColor = pTextColor,
                                            playerTextSecondary = pTextSecondary,
                                            showQuality = true
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // 第2页：全屏歌词（整个页面滑入）
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(pBgColor)
                                ) {
                                    LyricsPanel(
                                        lrcLines = pLrcLines,
                                        plainLines = pPlainLines,
                                        currentPosition = currentPosition,
                                        playerTextColor = pTextColor,
                                        playerTextTertiary = pTextTertiary,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                                    )
                                }
                            }
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
    playerTextSecondary: Color = Color.White.copy(alpha = 0.7f),
    showQuality: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
        // 歌手行：歌手横向可滚动，品质图标始终可见
        val artistParts = song.artist.split("·", "、", "/").map { it.trim() }.filter { it.isNotEmpty() }
        val qualityText = if (showQuality) com.lechenmusic.ui.components.getQualityText(song) else ""
        val qualityColor = if (showQuality) com.lechenmusic.ui.components.getQualityColor(song) else Color.Transparent

        val artistText = if (artistParts.size > 1) artistParts.joinToString(" · ") else song.artist

        // 使用 SubcomposeLayout：先无约束测量文字自然宽度，再决定是否滚动
        SubcomposeLayout(
            modifier = Modifier.fillMaxWidth()
        ) { constraints ->
            val looseConstraints = constraints.copy(minWidth = 0)

            // 1. 测量品质图标（固定宽度）
            val badgePlaceable = subcompose("badge") {
                if (qualityText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = qualityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            qualityText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = qualityColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }.mapNotNull { it.measure(looseConstraints) }.firstOrNull()

            val badgeWidth = (badgePlaceable?.width ?: 0) + if (badgePlaceable != null) 6.dp.roundToPx() else 0

            // 2. 无约束测量文字自然宽度
            val measureText = subcompose("measureText") {
                Text(text = artistText, fontSize = artistSize, maxLines = 1)
            }.first().measure(Constraints()) // 无约束
            val naturalTextWidth = measureText.width

            // 3. 计算歌手区域可用宽度
            val availableForArtist = looseConstraints.maxWidth - badgeWidth
            val needsScroll = naturalTextWidth > availableForArtist

            // 4. 用 tight 约束测量实际内容，确保 Row 不超出可用宽度
            val contentPlaceables = subcompose("content") {
                Row(
                    modifier = if (needsScroll) Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()) else Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artistText,
                        fontSize = artistSize,
                        color = playerTextSecondary,
                        maxLines = 1
                    )
                    if (qualityText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = qualityColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                qualityText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = qualityColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }.map {
                it.measure(
                    if (needsScroll) {
                        // tight约束：Row宽度限制为可用宽度，内容会触发水平滚动
                        Constraints.fixedWidth(availableForArtist.coerceAtLeast(0))
                    } else {
                        looseConstraints
                    }
                )
            }

            val width = looseConstraints.maxWidth
            val height = contentPlaceables.maxOfOrNull { it.height } ?: 0

            layout(width, height) {
                contentPlaceables.forEach { it.place(0, 0) }
            }
        }
    }
}


// ── 封面图下方歌词预览（2行显示 + 缩放淡变切换） ──
//
// 方案：缩放 + 淡变（Scale + Emphasis Shift）
// 参考 Apple Music / 网易云音乐
// - 当前行：大字 22sp，完全不透明
// - 下一行：小字 15sp，低透明度
// - 切换时：旧行缩小 + 变暗 + 轻微上移，新行放大 + 变亮 + 从下方升起
//

@Composable
private fun SmoothLyricsDisplay(
    lrcLines: List<com.lechenmusic.ui.screens.player.LyricLine>?,
    plainLines: List<String>,
    currentPosition: Long,
    playerTextColor: Color = Color.White,
    playerTextTertiary: Color = Color.White.copy(alpha = 0.4f),
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center
) {
    val activeColor = playerTextColor.copy(alpha = 0.85f)
    val inactiveColor = playerTextColor.copy(alpha = 0.3f)
    val activeFontSize = 18.sp
    val inactiveFontSize = 13.sp
    val lineH = 42.dp

    if (lrcLines != null && lrcLines.isNotEmpty()) {
        val rawActiveIndex = findActiveLyricLine(lrcLines, currentPosition)
        val firstRealLyricIndex = remember(lrcLines) {
            var idx = 0
            while (idx < lrcLines.size - 1 &&
                lrcLines[idx + 1].timeMs - lrcLines[idx].timeMs < 500L &&
                lrcLines[idx].timeMs < 1000L
            ) { idx++ }
            idx
        }
        val activeIndex = if (rawActiveIndex <= firstRealLyricIndex && currentPosition < 1000L) {
            firstRealLyricIndex
        } else {
            rawActiveIndex
        }

        // 切换方向标识：+1 向上滚，-1 向下滚，0 无切换
        var switchDirection by remember { mutableIntStateOf(0) }
        var prevActiveIndex by remember { mutableIntStateOf(activeIndex) }

        // 旧行退出动画（缩小 + 变暗 + 上移）
        val exitAlpha = remember { Animatable(1f) }
        val exitScale = remember { Animatable(1f) }
        val exitOffsetY = remember { Animatable(0f) }

        // 新行进入动画（从下方升起 + 放大到正常）
        val enterAlpha = remember { Animatable(1f) }
        val enterScale = remember { Animatable(1f) }
        val enterOffsetY = remember { Animatable(0f) }

        // 行切换时：启动进入/退出动画
        LaunchedEffect(activeIndex) {
            if (activeIndex != prevActiveIndex && prevActiveIndex >= 0) {
                switchDirection = if (activeIndex > prevActiveIndex) 1 else -1
                prevActiveIndex = activeIndex

                // 重置进入状态（新行从下方、小尺寸、透明开始）
                enterAlpha.snapTo(0f)
                enterScale.snapTo(0.85f)
                enterOffsetY.snapTo(18f * switchDirection)

                // 退出动画：旧行缩小、变暗、向上飘走
                launch {
                    exitAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                }
                launch {
                    exitScale.animateTo(0.88f, tween(320, easing = FastOutSlowInEasing))
                }
                launch {
                    exitOffsetY.animateTo(-12f * switchDirection, tween(320, easing = FastOutSlowInEasing))
                }

                // 进入动画：新行放大到正常、淡入、升起
                launch {
                    enterAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                }
                launch {
                    enterScale.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                }
                launch {
                    enterOffsetY.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                }
            } else {
                prevActiveIndex = activeIndex
            }
        }

        // 首次加载时重置为正常状态
        LaunchedEffect(Unit) {
            exitAlpha.snapTo(0f)
            exitScale.snapTo(0.88f)
            exitOffsetY.snapTo(0f)
            enterAlpha.snapTo(1f)
            enterScale.snapTo(1f)
            enterOffsetY.snapTo(0f)
        }

        Box(modifier = modifier, contentAlignment = contentAlignment) {
            // === 2行歌词区域（固定高度） ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineH * 2),
                contentAlignment = Alignment.TopCenter
            ) {
                // ── 当前行（大字，强调） ──
                if (activeIndex in lrcLines.indices) {
                    val currentText = lrcLines[activeIndex].text
                    // 判断是否正在切换中（有旧行在退出）
                    val isTransitioning = exitAlpha.value < 1f

                    Text(
                        text = currentText,
                        fontSize = activeFontSize,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineH)
                            .padding(horizontal = 16.dp)
                            .graphicsLayer {
                                if (isTransitioning) {
                                    // 新行：使用进入动画参数
                                    alpha = enterAlpha.value
                                    scaleX = enterScale.value
                                    scaleY = enterScale.value
                                    translationY = enterOffsetY.value
                                }
                                // 静止时：identity（alpha=1, scale=1, offset=0）
                            },
                        lineHeight = lineH.value.sp
                    )
                }

                // ── 旧行退出（大字状态缩小飘走） ──
                if (prevActiveIndex in lrcLines.indices && prevActiveIndex != activeIndex && exitAlpha.value > 0.01f) {
                    Text(
                        text = lrcLines[prevActiveIndex].text,
                        fontSize = activeFontSize,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineH)
                            .padding(horizontal = 16.dp)
                            .graphicsLayer {
                                alpha = exitAlpha.value
                                scaleX = exitScale.value
                                scaleY = exitScale.value
                                translationY = exitOffsetY.value
                            },
                        lineHeight = lineH.value.sp
                    )
                }

                // ── 下一行（小字，弱化） ──
                if (activeIndex + 1 < lrcLines.size) {
                    Text(
                        text = lrcLines[activeIndex + 1].text,
                        fontSize = inactiveFontSize,
                        fontWeight = FontWeight.Normal,
                        color = inactiveColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineH)
                            .padding(horizontal = 16.dp)
                            .offset(y = lineH),
                        lineHeight = lineH.value.sp
                    )
                }
            }
        }
    } else if (plainLines.isNotEmpty()) {
        Box(modifier = modifier, contentAlignment = contentAlignment) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                plainLines.take(2).forEachIndexed { index, line ->
                    Text(
                        text = line.trim(),
                        fontSize = if (index == 0) 18.sp else 13.sp,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == 0) activeColor else inactiveColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    } else {
        Box(modifier = modifier, contentAlignment = contentAlignment) {
            Text("暂无歌词", fontSize = 13.sp, color = inactiveColor, textAlign = TextAlign.Center)
        }
    }
}


// ── 歌词面板（全屏歌词页使用） ─────────────────────────────

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
                    fontSize = if (isActive) 18.sp else 14.sp,
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
                Text(line, fontSize = 14.sp, color = playerTextTertiary, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth())
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", fontSize = 14.sp, color = playerTextTertiary)
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
            // 进度条（QQ音乐/酷狗风格：细轨道 + 小圆点）
            ThinProgressBar(
                progress = progress,
                onSeek = onSeek,
                activeColor = sliderActiveColor,
                inactiveColor = sliderInactiveColor,
                thumbColor = playerTextColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPosition), fontSize = 11.sp, color = playerTextSecondary)
                Text(formatTime(duration), fontSize = 11.sp, color = playerTextSecondary)
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
                // 选色优先级：DarkMuted > DarkVibrant > Muted > Vibrant > Dominant
                // 优先选深色系，与封面图自然融合
                val swatch = palette.darkMutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.vibrantSwatch
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


// ── 细进度条（QQ音乐/酷狗风格） ─────────────────────────

@Composable
private fun ThinProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }
    var barWidthPx by remember { mutableStateOf(1f) }

    // 同步外部进度（非拖动时）
    LaunchedEffect(progress) {
        if (!isDragging) dragProgress = progress
    }

    val displayProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        dragProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        isDragging = true
                        val success = tryAwaitRelease()
                        if (success) onSeek(dragProgress)
                        isDragging = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        isDragging = true
                    },
                    onDragEnd = { onSeek(dragProgress); isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (dragProgress * barWidthPx) + dragAmount.x
                        dragProgress = (newOffset / barWidthPx).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景轨道（细：2dp）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(inactiveColor)
        )
        // 激活轨道
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = displayProgress)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(activeColor)
        )
        // 小圆点（6dp，比默认小很多）
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = displayProgress)
                .wrapContentWidth(Alignment.End)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}
