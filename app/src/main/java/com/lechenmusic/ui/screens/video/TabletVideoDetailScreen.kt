package com.lechenmusic.ui.screens.video

import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lechenmusic.data.model.VideoDetail
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.data.model.VideoSource
import com.lechenmusic.data.model.VideoEpisode
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

/**
 * 影视详情页 - 平板版
 *
 * 布局（参考影视详情页_new.html）：
 * 1. 顶部: 返回按钮 + "正在播放" 标题
 * 2. 左侧 (2/3): 视频播放器区域 + 电影信息
 * 3. 右侧 (1/3): 播放线路选择 + 剧集列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletVideoDetailScreen(
    viewModel: VideoViewModel,
    source: String,
    videoId: String,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onPlay: (String, Int) -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    val detail by viewModel.videoDetail.collectAsState()
    val loading by viewModel.detailLoading.collectAsState()
    val allSearchSources by viewModel.allSearchSources.collectAsState()
    val sourceSpeeds by viewModel.sourceSpeeds.collectAsState()
    val speedTesting by viewModel.speedTesting.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val context = LocalContext.current

    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }
    var showSourcePanel by remember { mutableStateOf(false) }

    // 内联播放器控件
    var inlineControlsVisible by remember { mutableStateOf(true) }
    var inlineInteractionCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(source, videoId) {
        viewModel.loadDetail(source, videoId)
        viewModel.loadFavorites()
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val video = detail ?: return

    val isStarred = favorites.any { it.id == videoId }
    val sources = video.toSources()
    val currentSource = sources.getOrNull(selectedSource)
    val currentEpisodes = currentSource?.episodes ?: emptyList()

    // 构建显示用的源列表（合并搜索源）
    val displaySources = if (allSearchSources.size > 1) {
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

    // ExoPlayer
    val exoPlayer = remember {
        val factory = DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(factory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // 加载视频
    LaunchedEffect(video, selectedSource, selectedEpisode) {
        val src = displaySources.getOrNull(selectedSource) ?: video.toSources().firstOrNull()
        val ep = src?.episodes?.getOrNull(selectedEpisode)
        if (ep != null && ep.url.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // 控件自动隐藏
    LaunchedEffect(inlineInteractionCount) {
        if (inlineInteractionCount > 0 && exoPlayer.isPlaying) {
            inlineControlsVisible = true
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) inlineControlsVisible = false
        }
    }

    LaunchedEffect(exoPlayer.isPlaying) {
        if (exoPlayer.isPlaying) {
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) inlineControlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // ===== 顶部栏: 返回 + 正在播放 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "正在播放",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ===== 双栏布局 =====
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ===== 左侧 (2/3): 视频播放器 + 电影信息 =====
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                // 视频播放器区域 (16:9)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    // 视频画面
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

                    // 点击切换控件显隐
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    inlineControlsVisible = !inlineControlsVisible
                                    if (inlineControlsVisible) inlineInteractionCount++
                                }
                            }
                    )

                    // 叠加层控件
                    if (inlineControlsVisible) {
                        // 渐变遮罩 - 顶部
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                                .align(Alignment.TopCenter)
                        )

                        // 全屏按钮 (右下)
                        IconButton(
                            onClick = { /* TODO: 全屏切换 */ },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
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
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            IconButton(
                                onClick = {
                                    exoPlayer.playWhenReady = !exoPlayer.isPlaying
                                    inlineControlsVisible = true
                                }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // 底部进度条
                        var progress by remember { mutableFloatStateOf(0f) }
                        var durationMs by remember { mutableLongStateOf(0L) }
                        var positionMs by remember { mutableLongStateOf(0L) }
                        var isDragging by remember { mutableStateOf(false) }
                        var isSeeking by remember { mutableStateOf(false) }
                        var barWidthPx by remember { mutableFloatStateOf(1f) }

                        LaunchedEffect(exoPlayer) {
                            while (true) {
                                if (!isDragging && !isSeeking) {
                                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                                    progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                                }
                                if (isSeeking) {
                                    kotlinx.coroutines.delay(500)
                                    isSeeking = false
                                }
                                kotlinx.coroutines.delay(500)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    formatTabletTime(positionMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(45.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    isDragging = true
                                                    progress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    isSeeking = true
                                                    exoPlayer.seekTo((progress * durationMs).toLong())
                                                },
                                                onDragCancel = { isDragging = false; isSeeking = false }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                val newOffset = (progress * barWidthPx) + dragAmount.x
                                                progress = (newOffset / barWidthPx).coerceIn(0f, 1f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = { offset ->
                                                    isDragging = true
                                                    progress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                                    val success = tryAwaitRelease()
                                                    if (success) {
                                                        isSeeking = true
                                                        exoPlayer.seekTo((progress * durationMs).toLong())
                                                    }
                                                    isDragging = false
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.3f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progress)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset((progress * barWidthPx - 7).toInt(), 0) }
                                            .size(14.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                                Text(
                                    formatTabletTime(durationMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(45.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    } // end if inlineControlsVisible
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== 电影信息 =====
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 标题 + 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // 标题 (年份)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    video.title,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (video.year.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "(${video.year})",
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // 评分 + 类型 + 时长
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (video.rate != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            video.rate!!,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }
                                if (video.displayType.isNotBlank()) {
                                    Text(
                                        video.displayType,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (video.area.isNotBlank()) {
                                    Text(
                                        video.area,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        // 分享 / 收藏按钮
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val videoInfo = VideoInfo(
                                        id = video.id,
                                        source = video.source,
                                        title = video.title,
                                        cover = video.displayCover,
                                        year = video.year,
                                        type = video.displayType
                                    )
                                    if (isStarred) viewModel.removeFavorite(videoInfo)
                                    else viewModel.addFavorite(videoInfo)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isStarred) "已收藏" else "收藏", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${video.title} (${video.year})")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享"))
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("分享", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 简介
                    if (video.desc.isNotBlank()) {
                        Text(
                            video.desc,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                            maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (video.desc.length > 120) {
                            Text(
                                if (descExpanded) "收起" else "展开全部",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { descExpanded = !descExpanded }
                                    .padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 导演 / 演员
                    if (video.director.isNotBlank()) {
                        Text(
                            "导演：${video.director}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (video.actor.isNotBlank()) {
                        Text(
                            "主演：${video.actor}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ===== 右侧 (1/3): 播放线路 + 剧集列表 =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 播放线路选择面板
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "切换播放线路",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // 测速按钮
                            TextButton(
                                onClick = { viewModel.testSourceSpeeds() },
                                enabled = !speedTesting
                            ) {
                                if (speedTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    if (speedTesting) "测速中..." else "⚡ 测速",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 线路列表
                        displaySources.forEachIndexed { index, src ->
                            val speed = sourceSpeeds[src.source]
                            val isSelected = selectedSource == index
                            val isRecommended = index == 0

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val info = allSearchSources.firstOrNull { it.source == src.source }
                                        if (info != null && info.episodes.isNotEmpty()) {
                                            selectedSource = index
                                            selectedEpisode = 0
                                            viewModel.switchSource(info)
                                            val url = info.episodes[0]
                                            if (url.isNotBlank()) {
                                                exoPlayer.stop()
                                                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                                exoPlayer.prepare()
                                                exoPlayer.playWhenReady = true
                                            }
                                        } else if (src.episodes.isNotEmpty()) {
                                            selectedSource = index
                                            selectedEpisode = 0
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                } else {
                                    null
                                },
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Speed,
                                        null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            src.sourceName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            buildString {
                                                when {
                                                    speed == null -> append("延迟: --")
                                                    speed < 0 -> append("延迟: 超时")
                                                    else -> append("延迟: ${speed}ms")
                                                }
                                                if (speed != null && speed in 0..500) append(" | 稳定")
                                                else if (speed != null && speed in 501..1000) append(" | 良好")
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isRecommended && !isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                "推荐",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 3.dp
                                                )
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 剧集列表
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "选集 (共 ${currentEpisodes.size} 集)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 剧集网格
                        if (currentEpisodes.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentEpisodes.size) { index ->
                                    val isSelected = selectedEpisode == index
                                    Surface(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable {
                                                selectedEpisode = index
                                                val ep = currentEpisodes[index]
                                                if (ep.url.isNotBlank()) {
                                                    exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
                                                    exoPlayer.prepare()
                                                    exoPlayer.playWhenReady = true
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        shadowElevation = if (isSelected) 4.dp else 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    String.format("%02d", index + 1),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (isSelected) {
                                                    Text(
                                                        "Playing",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary.copy(
                                                            alpha = 0.7f
                                                        ),
                                                        letterSpacing = 1.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无剧集",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTabletTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
