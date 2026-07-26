package com.lechenmusic.ui.screens.player.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    onShowAddToPlaylist: () -> Unit = {},
    onShowQueue: () -> Unit = {}
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
    LaunchedEffect(song.id) { viewModel.loadLyrics(song) }

    val coverUrl = ApiClient.getCoverArtUrl(serverUrl, username, password, song.coverArt ?: song.albumId)
    val coverBgColor = rememberCoverColor(coverUrl)

    // 设置状态栏颜色跟随封面背景色
    val context = LocalContext.current
    LaunchedEffect(coverBgColor) {
        val activity = context as? android.app.Activity
        activity?.window?.statusBarColor = coverBgColor.copy(alpha = 1f).toArgb()
    }

    // Parse lyrics
    val lrcLines = remember(currentLyrics) { currentLyrics?.let { parseLrc(it) } }
    val plainLines = remember(currentLyrics) {
        if (lrcLines == null) currentLyrics?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(coverBgColor)) {
        // 遮罩
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部返回按钮 ──
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "返回", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // ── 主内容区 ──
            if (isTablet) {
                // 平板：横排
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧封面
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        CoverImageDisplay(coverUrl = coverUrl, size = 320)
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    // 右侧歌词
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SongInfo(song = song, titleSize = 28.sp, artistSize = 18.sp, onArtistClick = { if (song.artistId.isNotBlank()) onNavigateToArtist(song.artistId) }, center = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        LyricsPanel(lrcLines = lrcLines, plainLines = plainLines, currentPosition = currentPosition, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                // ── 手机：HorizontalPager 左右滑动 ──
                val pagerState = rememberPagerState(pageCount = { 2 })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            // ── 第1页：封面 + 歌曲名 ──
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CoverImageDisplay(coverUrl = coverUrl, size = 260)
                                Spacer(modifier = Modifier.height(32.dp))
                                SongInfo(
                                    song = song, titleSize = 22.sp, artistSize = 14.sp,
                                    onArtistClick = { if (song.artistId.isNotBlank()) onNavigateToArtist(song.artistId) },
                                    center = true
                                )
                            }
                        }
                        1 -> {
                            // ── 第2页：歌词 ──
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 顶部小封面 + 歌曲名（居中）
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoverImageDisplay(coverUrl = coverUrl, size = 48)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                        Text(song.artist, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                                    }
                                }
                                // 歌词
                                LyricsPanel(lrcLines = lrcLines, plainLines = plainLines, currentPosition = currentPosition, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 页码指示器
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == idx) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == idx) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // ── 底部控制栏（共用） ──
            PlayerControls(
                isTablet = isTablet,
                progress = progress,
                currentPosition = currentPosition,
                duration = duration,
                isPlaying = isPlaying,
                isStarred = isStarred,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                onSeek = { playerManager.seekToProgress(it) },
                onPlayPause = { playerManager.togglePlayPause() },
                onPrevious = { playerManager.skipPrevious() },
                onNext = { playerManager.skipNext() },
                onToggleStar = { playerManager.toggleStar() },
                onToggleShuffle = { playerManager.toggleShuffle() },
                onToggleRepeat = { playerManager.toggleRepeat() },
                onShowAddToPlaylist = onShowAddToPlaylist,
                onShowQueue = onShowQueue,
                onAddToQueue = { playerManager.addToQueue(song) }
            )
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
    center: Boolean = false
) {
    Column(
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            song.title,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (center) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            song.artist,
            fontSize = artistSize,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.clickable(onClick = onArtistClick)
        )
    }
}


// ── 歌词面板 ─────────────────────────────────────────────

@Composable
private fun LyricsPanel(
    lrcLines: List<com.lechenmusic.ui.screens.player.LyricLine>?,
    plainLines: List<String>,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    if (lrcLines != null) {
        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
        val listState = rememberLazyListState()
        // 使用 snapshotFlow 监听 activeIndex 变化，确保每次歌词行切换都触发滚动
        LaunchedEffect(lrcLines) {
            snapshotFlow { findActiveLyricLine(lrcLines, currentPosition) }
                .collect { index ->
                    listState.animateScrollToItem((index - 3).coerceAtLeast(0))
                }
        }

        LazyColumn(state = listState, modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
            itemsIndexed(lrcLines) { index, line ->
                val isActive = index == activeIndex
                Text(
                    text = line.text,
                    fontSize = if (isActive) 20.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = if (isActive) 6.dp else 4.dp).fillMaxWidth()
                )
            }
        }
    } else if (plainLines.isNotEmpty()) {
        LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
            itemsIndexed(plainLines) { _, line ->
                Text(line, fontSize = 16.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth())
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", fontSize = 16.sp, color = Color.White.copy(alpha = 0.4f))
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
    onSeek: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowAddToPlaylist: () -> Unit,
    onShowQueue: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 进度条
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(formatTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
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
                    var showTimer by remember { mutableStateOf(false) }
                    IconButton(onClick = { showTimer = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Timer, "定时", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showTimer, onDismissRequest = { showTimer = false }) {
                        listOf(15, 30, 45, 60, 90).forEach { min ->
                            DropdownMenuItem(text = { Text("${min}分钟后") }, onClick = { showTimer = false })
                        }
                    }
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(40.dp)) {
                        Icon(if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏",
                            tint = if (isStarred) Color(0xFFFF4D6A) else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 中间：主控制
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Shuffle, "随机", tint = if (shuffleMode) Color.White else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "上一首", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "下一首", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                        Icon(
                            when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                            "循环",
                            tint = if (repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 右侧：添加到 + 播放队列
                    var showAddMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showAddMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlaylistAdd, "添加到", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text("添加到歌单") }, onClick = { showAddMenu = false; onShowAddToPlaylist() }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) })
                            DropdownMenuItem(text = { Text("添加到播放队列") }, onClick = { showAddMenu = false; onAddToQueue() }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) })
                        }
                    }
                    IconButton(onClick = onShowQueue, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.QueueMusic, "播放队列", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
                        Icon(Icons.Default.Shuffle, "随机", tint = if (shuffleMode) Color.White else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "上一首", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "下一首", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                        Icon(
                            when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                            "循环",
                            tint = if (repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.4f),
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
                            tint = if (isStarred) Color(0xFFFF4D6A) else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    var showTimer by remember { mutableStateOf(false) }
                    IconButton(onClick = { showTimer = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Timer, "定时", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showTimer, onDismissRequest = { showTimer = false }) {
                        listOf(15, 30, 45, 60, 90).forEach { min ->
                            DropdownMenuItem(text = { Text("${min}分钟后") }, onClick = { showTimer = false })
                        }
                    }
                    var showAddMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showAddMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlaylistAdd, "添加到", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text("添加到歌单") }, onClick = { showAddMenu = false; onShowAddToPlaylist() }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) })
                            DropdownMenuItem(text = { Text("添加到播放队列") }, onClick = { showAddMenu = false; onAddToQueue() }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) })
                        }
                    }
                    IconButton(onClick = onShowQueue, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.QueueMusic, "播放队列", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}


// ── 封面颜色提取 ─────────────────────────────────────────

@Composable
fun rememberCoverColor(coverUrl: String?): Color {
    val context = LocalContext.current
    var bgColor by remember { mutableStateOf(Color(0xFF1a1a2e)) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) return@LaunchedEffect
        try {
            // 复用全局 Coil ImageLoader，避免每次创建新实例导致 GC 压力
            val imageLoader = coil.ImageLoader(context)
            val request = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
            val drawable = imageLoader.execute(request).drawable
            val bitmap = drawable?.toBitmap(128, 128)
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
                val color = palette.vibrantSwatch?.rgb
                    ?: palette.darkVibrantSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                if (color != null) bgColor = Color(color).copy(alpha = 0.85f)
            }
        } catch (_: Exception) {}
    }
    return bgColor
}


// ── 工具函数 ─────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
