package com.lechenmusic.ui.screens.video

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lechenmusic.ErrorReporter
import com.lechenmusic.R
import com.lechenmusic.data.model.*
import com.lechenmusic.ui.VideoViewModel

/**
 * 影视详情页 - 带内联播放器
 *
 * 交互流程:
 * 1. 点击影片 → 搜索播放源 → 进入本页
 * 2. 顶部内联播放器（小窗，不强制全屏）
 * 3. 下方显示影片信息、片源选择、选集
 * 4. 用户可点击全屏按钮进入全屏播放
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    viewModel: VideoViewModel,
    source: String,
    videoId: String,
    onBack: () -> Unit,
    onPlay: (source: String, episodeIndex: Int) -> Unit,
    onVideoClick: (VideoInfo) -> Unit = {}
) {
    val detail by viewModel.videoDetail.collectAsState()
    val isLoading by viewModel.detailLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val allSearchSources by viewModel.allSearchSources.collectAsState()
    val context = LocalContext.current

    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }
    var isPlayerFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(source, videoId) {
        if (source != "searching") {
            viewModel.loadDetail(source, videoId)
        }
        viewModel.loadFavorites()
    }

    val currentDetail = detail
    val isStarred = favorites.any { it.id == videoId }

    // ExoPlayer - 内联播放器
    val exoPlayer = remember {
        val factory = DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(factory)
            .build().apply {
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // 当 source/episode 变化时加载视频
    LaunchedEffect(selectedSource, selectedEpisode, currentDetail) {
        // currentDetail.toSources() 通常只有1项（当前源），直接取第一项
        val src = currentDetail?.toSources()?.firstOrNull()
        val ep = src?.episodes?.getOrNull(selectedEpisode)
        if (ep != null && ep.url.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // 全屏模式处理 - 用 SideEffect 避免 LaunchedEffect 重组问题
    val activity = context as? android.app.Activity
    DisposableEffect(isPlayerFullscreen) {
        activity?.requestedOrientation = if (isPlayerFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (isPlayerFullscreen) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        // 播放时保持屏幕常亮
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 全屏模式下显示纯播放器
    if (isPlayerFullscreen) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 视频画面（无内置控制器）
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // 自定义叠加层
            // 退出全屏 (左上)
            IconButton(
                onClick = { isPlayerFullscreen = false },
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "退出全屏", tint = Color.White)
            }
            // 播放/暂停 (居中)
            var fsIsPlaying by remember { mutableStateOf(false) }
            LaunchedEffect(exoPlayer) {
                while (true) {
                    fsIsPlaying = exoPlayer.isPlaying
                    kotlinx.coroutines.delay(200)
                }
            }
            IconButton(
                onClick = { exoPlayer.playWhenReady = !exoPlayer.isPlaying },
                modifier = Modifier.align(Alignment.Center).size(64.dp)
            ) {
                Icon(
                    if (fsIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (fsIsPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            // 底部进度条 + 时间（可拖动）
            var fsProgress by remember { mutableFloatStateOf(0f) }
            var fsDuration by remember { mutableLongStateOf(0L) }
            var fsPosition by remember { mutableLongStateOf(0L) }
            var fsDragging by remember { mutableStateOf(false) }
            LaunchedEffect(exoPlayer) {
                while (true) {
                    if (!fsDragging) {
                        fsDuration = exoPlayer.duration.coerceAtLeast(0L)
                        fsPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                        fsProgress = if (fsDuration > 0) fsPosition.toFloat() / fsDuration else 0f
                    }
                    kotlinx.coroutines.delay(300)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Slider(
                    value = fsProgress,
                    onValueChange = { newValue ->
                        fsDragging = true
                        fsProgress = newValue
                    },
                    onValueChangeFinished = {
                        exoPlayer.seekTo((fsProgress * fsDuration).toLong())
                        fsDragging = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(fsPosition), color = Color.White, fontSize = 11.sp)
                    Text(formatTime(fsDuration), color = Color.White, fontSize = 11.sp)
                }
            }
        }
        BackHandler { isPlayerFullscreen = false }
        return
    }

    if (isLoading && currentDetail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentDetail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val sources = currentDetail.toSources()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ===== 内联播放器（小窗，16:9） =====
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    // 视频画面（无内置控制器）
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // 自定义叠加层: 返回 + 全屏 + 播放/暂停
                    // 返回按钮 (左上)
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                    // 全屏按钮 (右下)
                    IconButton(
                        onClick = { isPlayerFullscreen = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White)
                    }
                    // 播放/暂停按钮 (居中)
                    var isPlaying by remember { mutableStateOf(false) }
                    LaunchedEffect(exoPlayer) {
                        while (true) {
                            isPlaying = exoPlayer.isPlaying
                            kotlinx.coroutines.delay(500)
                        }
                    }
                    IconButton(
                        onClick = { exoPlayer.playWhenReady = !exoPlayer.isPlaying },
                        modifier = Modifier.align(Alignment.Center).size(56.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    // 底部进度条（可拖动）
                    var progress by remember { mutableFloatStateOf(0f) }
                    var durationMs by remember { mutableLongStateOf(0L) }
                    var positionMs by remember { mutableLongStateOf(0L) }
                    var isDragging by remember { mutableStateOf(false) }
                    LaunchedEffect(exoPlayer) {
                        while (true) {
                            if (!isDragging) {
                                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                                progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                            }
                            kotlinx.coroutines.delay(300)
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 48.dp, end = 48.dp, bottom = 4.dp)) {
                        Slider(
                            value = progress,
                            onValueChange = { newValue ->
                                isDragging = true
                                progress = newValue
                            },
                            onValueChangeFinished = {
                                val seekTo = (progress * durationMs).toLong()
                                exoPlayer.seekTo(seekTo)
                                isDragging = false
                            },
                            modifier = Modifier.fillMaxWidth().height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // ===== 影片信息 =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 封面
                    Surface(
                        modifier = Modifier.width(100.dp).height(140.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (currentDetail.displayCover.isNotBlank()) {
                            AsyncImage(
                                model = currentDetail.displayCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentDetail.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentDetail.rate != null) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(currentDetail.rate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(currentDetail.year, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(categoryName(currentDetail.displayType), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (currentDetail.area.isNotBlank()) {
                            Text(currentDetail.area, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (currentDetail.director.isNotBlank()) {
                            Text("导演: ${currentDetail.director}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (currentDetail.actor.isNotBlank()) {
                            Text("演员: ${currentDetail.actor}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // ===== 操作按钮 =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 收藏
                    OutlinedButton(
                        onClick = {
                            val videoInfo = VideoInfo(id = currentDetail.id, source = currentDetail.source, title = currentDetail.title, cover = currentDetail.displayCover, year = currentDetail.year, type = currentDetail.displayType)
                            if (isStarred) viewModel.removeFavorite(videoInfo) else viewModel.addFavorite(videoInfo)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isStarred) "已收藏" else "收藏", fontSize = 13.sp)
                    }
                    // 分享
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${currentDetail.title} (${currentDetail.year})") }
                            context.startActivity(Intent.createChooser(intent, "分享"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("分享", fontSize = 13.sp)
                    }
                }
            }

            // ===== 简介 =====
            if (currentDetail.desc.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("简介", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(currentDetail.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp, maxLines = if (descExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                        if (currentDetail.desc.length > 80) {
                            Text(if (descExpanded) "收起" else "展开全部", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { descExpanded = !descExpanded }.padding(top = 2.dp))
                        }
                    }
                }
            }

            // ===== 片源选择（去重+限制数量） =====
            val displaySources = if (allSearchSources.size > 1) {
                // 去重：同名源只保留第一个，限制最多20个
                allSearchSources
                    .groupBy { it.source }
                    .values.map { group -> group.first() }
                    .take(20)
                    .map { info ->
                        VideoSource(
                            sourceName = info.displaySourceName.ifBlank { info.source },
                            source = info.source,
                            episodes = info.episodes.mapIndexed { idx, url ->
                                VideoEpisode(index = idx, title = info.episodesTitles.getOrNull(idx) ?: "第${idx + 1}集", url = url)
                            }
                        )
                    }
            } else {
                sources
            }
            if (displaySources.size > 1) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("片源 (${displaySources.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(displaySources) { index, src ->
                                FilterChip(
                                    selected = selectedSource == index,
                                    onClick = {
                                        viewModel.logDebug("片源切换", "点击: index=$index, source=${src.source}, name=${src.sourceName}, eps=${src.episodes.size}")
                                        selectedSource = index
                                        selectedEpisode = 0
                                        val info = allSearchSources.firstOrNull { it.source == src.source }
                                        viewModel.logDebug("片源切换", "匹配info: ${info != null}, source=${info?.source}, eps=${info?.episodes?.size}")
                                        if (info != null) {
                                            viewModel.switchSource(info)
                                        }
                                    },
                                    label = { Text("${src.sourceName} (${src.episodes.size}集)", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // ===== 选集（直接用当前 VideoDetail 的 episodes） =====
            val currentEpisodes = detail?.episodes ?: emptyList()
            val currentEpisodesTitles = detail?.episodesTitles ?: emptyList()
            if (currentEpisodes.isNotEmpty()) {
                item {
                    Text(
                        "选集 (${currentEpisodes.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (currentEpisodes.size == 1) {
                    item {
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp).clickable { selectedEpisode = 0 },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedEpisode == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text("播放", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    // 网格选集
                    val rows = currentEpisodes.chunked(6)
                    itemsIndexed(rows) { rowIndex, rowEpisodes ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowEpisodes.forEachIndexed { colIndex, ep ->
                                val globalIndex = rowIndex * 6 + colIndex
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { selectedEpisode = globalIndex },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedEpisode == globalIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        (globalIndex + 1).toString(),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedEpisode == globalIndex) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            // 填充空位
                            repeat(6 - rowEpisodes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
