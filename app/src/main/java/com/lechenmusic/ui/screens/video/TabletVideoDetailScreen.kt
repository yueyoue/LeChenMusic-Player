package com.lechenmusic.ui.screens.video

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

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
    val context = LocalContext.current

    LaunchedEffect(source, videoId) {
        viewModel.loadDetail(source, videoId)
        viewModel.loadFavorites()
    }

    // ExoPlayer - 内联播放器
    val exoPlayer = remember {
        val factory = DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(factory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // 控件显隐
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionCount by remember { mutableIntStateOf(0) }

    // 控件自动隐藏
    LaunchedEffect(interactionCount) {
        if (interactionCount > 0 && exoPlayer.isPlaying) {
            controlsVisible = true
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) controlsVisible = false
        }
    }
    LaunchedEffect(exoPlayer.isPlaying) {
        if (exoPlayer.isPlaying) {
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) controlsVisible = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val video = detail ?: return

    // 当详情变化时自动加载第一个源的第一集
    LaunchedEffect(video) {
        val src = video.toSources().firstOrNull()
        val ep = src?.episodes?.getOrNull(0)
        if (ep != null && ep.url.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // 退出时释放播放器 + 保存位置
    DisposableEffect(Unit) {
        onDispose {
            if (exoPlayer.currentPosition > 0) {
                viewModel.setResumePosition(exoPlayer.currentPosition)
            }
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    val videoSources = video.toSources()
    val displaySources = if (allSearchSources.size > 1) {
        allSearchSources
            .groupBy { it.source }
            .values.map { group -> group.first() }
            .take(20)
            .map { info ->
                com.lechenmusic.data.model.VideoSource(
                    sourceName = info.displaySourceName.ifBlank { info.source },
                    source = info.source,
                    episodes = info.episodes.mapIndexed { idx, url ->
                        com.lechenmusic.data.model.VideoEpisode(index = idx, title = info.episodesTitles.getOrNull(idx) ?: "第${idx + 1}集", url = url)
                    }
                )
            }
    } else {
        videoSources
    }

    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(0) }
    var rightTab by remember { mutableIntStateOf(0) }

    val currentSource = displaySources.getOrNull(selectedSource)
    val episodes = currentSource?.episodes ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部返回按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("影视详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // ===== 主内容: 左侧(2/3) + 右侧(1/3) =====
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = responsiveConfig.contentPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
        ) {
            // ===== 左侧: 视频播放器 + 影片信息 (flex-2) =====
            Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
                // 视频播放器 (16:9, rounded-2xl) - 自动播放
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    // ExoPlayer 渲染
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
                        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTapGestures {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) interactionCount++
                            }
                        }
                    )
                    // 控件叠加层
                    if (controlsVisible) {
                        // 顶部: 返回 + 全屏
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 空占位（返回按钮在外层）
                            Spacer(modifier = Modifier.size(40.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onPlay(video.source, selectedEpisode) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                        // 底部: 播放/暂停 + 进度
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var isPlaying by remember { mutableStateOf(false) }
                            LaunchedEffect(exoPlayer) {
                                while (true) {
                                    isPlaying = exoPlayer.isPlaying
                                    kotlinx.coroutines.delay(500)
                                }
                            }
                            FilledIconButton(
                                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.8f),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null, modifier = Modifier.size(20.dp)
                                )
                            }
                            // 进度文字
                            var currentPosition by remember { mutableLongStateOf(0L) }
                            var duration by remember { mutableLongStateOf(0L) }
                            LaunchedEffect(exoPlayer) {
                                while (true) {
                                    currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                    duration = exoPlayer.duration.coerceAtLeast(0L)
                                    kotlinx.coroutines.delay(500)
                                }
                            }
                            Text(formatVideoTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            // 进度条
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { exoPlayer.seekTo((it * duration).toLong()) },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(formatVideoTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 影片信息
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(buildString { append(video.title); if (video.year.isNotBlank()) append(" (${video.year})") }, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (video.rate != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(video.rate!!, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                }
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (video.displayType.isNotBlank()) {
                                Text(categoryName(video.displayType), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (video.year.isNotBlank()) {
                                Text(video.year, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分享")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (video.desc.isNotBlank()) {
                    var descExpanded by remember { mutableStateOf(false) }
                    Text(video.desc, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp, maxLines = if (descExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                    if (video.desc.length > 100) {
                        Text(if (descExpanded) "收起" else "展开全部", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { descExpanded = !descExpanded }.padding(top = 4.dp))
                    }
                }

                if (video.director.isNotBlank() || video.actor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) {
                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(buildString {
                            if (video.director.isNotBlank()) append("导演：${video.director}")
                            if (video.director.isNotBlank() && video.actor.isNotBlank()) append(" / ")
                            if (video.actor.isNotBlank()) append("主演：${video.actor}")
                        }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ===== 右侧: Tab 切换选集/播放源 (flex-1) =====
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TabRow(
                    selectedTabIndex = rightTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = rightTab == 0, onClick = { rightTab = 0 }, text = { Text("选集 (${episodes.size})") })
                    Tab(selected = rightTab == 1, onClick = { rightTab = 1 }, text = { Text("播放源 (${displaySources.size})") })
                }

                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (rightTab) {
                        0 -> EpisodeGrid(episodes, selectedEpisode, exoPlayer, video.source) { index ->
                            selectedEpisode = index
                            val ep = episodes.getOrNull(index)
                            if (ep != null && ep.url.isNotBlank()) {
                                exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                        }
                        1 -> SourceList(
                            displaySources = displaySources,
                            allSearchSources = allSearchSources,
                            selectedSource = selectedSource,
                            sourceSpeeds = sourceSpeeds,
                            speedTesting = speedTesting,
                            onSourceSelect = { index, info ->
                                selectedSource = index
                                selectedEpisode = 0
                                viewModel.switchSource(info)
                                // 切换源后自动播放第一集（从上次位置继续）
                                val ep = info.episodes.firstOrNull()
                                if (ep != null && ep.isNotBlank()) {
                                    exoPlayer.setMediaItem(MediaItem.fromUri(ep))
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                    val resumeMs = viewModel.resumePositionMs.value
                                    if (resumeMs > 0) {
                                        exoPlayer.addListener(object : Player.Listener {
                                            override fun onPlaybackStateChanged(state: Int) {
                                                if (state == Player.STATE_READY) {
                                                    exoPlayer.seekTo(resumeMs)
                                                    viewModel.clearResumePosition()
                                                    exoPlayer.removeListener(this)
                                                }
                                            }
                                        })
                                    }
                                }
                            },
                            onTestSpeed = { viewModel.testSourceSpeeds() }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 选集网格 ====================
@Composable
private fun EpisodeGrid(
    episodes: List<com.lechenmusic.data.model.VideoEpisode>,
    selectedEpisode: Int,
    exoPlayer: ExoPlayer,
    videoSource: String,
    onEpisodeClick: (Int) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无剧集", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(episodes.size) { index ->
            val isActive = index == selectedEpisode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                modifier = Modifier.aspectRatio(1f).clickable { onEpisodeClick(index) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(episodes.getOrNull(index)?.title ?: "${index + 1}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        if (isActive) {
                            Text("播放中", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ==================== 播放源列表（带测速） ====================
@Composable
private fun SourceList(
    displaySources: List<com.lechenmusic.data.model.VideoSource>,
    allSearchSources: List<VideoInfo>,
    selectedSource: Int,
    sourceSpeeds: Map<String, Long>,
    speedTesting: Boolean,
    onSourceSelect: (Int, VideoInfo) -> Unit,
    onTestSpeed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${displaySources.size} 个源", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onTestSpeed, enabled = !speedTesting) {
                if (speedTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(if (speedTesting) "测速中..." else "\u26A1 测速排序", fontSize = 13.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(displaySources.size) { index ->
                val src = displaySources[index]
                val speed = sourceSpeeds[src.source]
                val isCurrent = index == selectedSource
                val matchingInfo = allSearchSources.firstOrNull { it.source == src.source }

                Surface(
                    onClick = {
                        if (matchingInfo != null && matchingInfo.episodes.isNotEmpty()) {
                            onSourceSelect(index, matchingInfo)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(src.sourceName.ifBlank { src.source }, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${src.episodes.size}集", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when { speed == null -> ""; speed < 0 -> "超时"; else -> "${speed}ms" },
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = when { speed == null -> MaterialTheme.colorScheme.onSurfaceVariant; speed < 0 -> Color.Red; speed < 500 -> Color(0xFF4CAF50); speed < 1500 -> Color(0xFFFFC107); else -> Color(0xFFFF5722) }
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("当前", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
