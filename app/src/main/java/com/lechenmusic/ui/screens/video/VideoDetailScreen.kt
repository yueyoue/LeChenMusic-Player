package com.lechenmusic.ui.screens.video

import android.content.Intent
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lechenmusic.ErrorReporter
import com.lechenmusic.R
import com.lechenmusic.data.model.*
import com.lechenmusic.dlna.*
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.launch

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
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit,
    onPlay: (source: String, episodeIndex: Int) -> Unit,
    onVideoClick: (VideoInfo) -> Unit = {}
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val detail by viewModel.videoDetail.collectAsState()
    val isLoading by viewModel.detailLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val allSearchSources by viewModel.allSearchSources.collectAsState()
    val playRecords by viewModel.playRecords.collectAsState()
    val resumePositionMs by viewModel.resumePositionMs.collectAsState()
    val context = LocalContext.current

    // 从播放记录中找到当前视频的记录
    val currentRecord = playRecords.find { it.source == source && it.videoIdRaw == videoId }

    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }
    // 播放器控件自动隐藏
    var inlineControlsVisible by remember { mutableStateOf(true) }
    var inlineInteractionCount by remember { mutableIntStateOf(0) }
    // 投屏状态
    var showCastSheet by remember { mutableStateOf(false) }
    var castDevice by remember { mutableStateOf<DlnaDevice?>(null) }
    var castController by remember { mutableStateOf<DlnaController?>(null) }
    // 标记是否正在切换源（切换源时跳过 LaunchedEffect(video) 的自动播放，避免重置位置）
    var isSwitchingSource by remember { mutableStateOf(false) }

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

    // 内联播放器控件自动隐藏(3秒无操作)
    LaunchedEffect(inlineInteractionCount) {
        if (inlineInteractionCount > 0 && exoPlayer.isPlaying) {
            inlineControlsVisible = true
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) inlineControlsVisible = false
        }
    }

    // 视频开始播放后自动隐藏控件
    LaunchedEffect(exoPlayer.isPlaying) {
        if (exoPlayer.isPlaying) {
            kotlinx.coroutines.delay(3000)
            if (exoPlayer.isPlaying) inlineControlsVisible = false
        }
    }
    // 当视频详情变化时加载视频（包括初始加载）
    // 切换源时跳过（由 SourceList 的 onSourceSelect 处理，避免重置播放位置）
    LaunchedEffect(currentDetail) {
        if (isSwitchingSource) {
            isSwitchingSource = false
            return@LaunchedEffect
        }
        val detail = currentDetail ?: return@LaunchedEffect
        val src = detail.toSources().firstOrNull()
        val ep = src?.episodes?.getOrNull(selectedEpisode)
        if (ep != null && ep.url.isNotBlank()) {
            try {
                // 检查是否需要恢复播放位置（从全屏返回时）
                val currentUrl = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
                val newUrl = ep.url

                // 如果URL没变且有恢复位置，只seek不重新加载
                if (currentUrl == newUrl && resumePositionMs > 0) {
                    exoPlayer.seekTo(resumePositionMs)
                    viewModel.clearResumePosition()
                    return@LaunchedEffect
                }

                exoPlayer.setMediaItem(MediaItem.fromUri(newUrl))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                // 优先从 resumePosition 恢复（全屏返回）
                if (resumePositionMs > 0) {
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                exoPlayer.seekTo(resumeMs)
                                viewModel.clearResumePosition()
                                exoPlayer.removeListener(this)
                            }
                        }
                    })
                } else {
                    // 从播放记录恢复播放位置
                    val record = currentRecord
                    if (record != null && record.displayPlayTime > 0) {
                        exoPlayer.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    exoPlayer.seekTo(record.displayPlayTime * 1000L)
                                    exoPlayer.removeListener(this)
                                }
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                ErrorReporter.reportError("error", "视频加载失败", e, "video_detail")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // 定期保存播放记录
    LaunchedEffect(currentDetail) {
        val detail = currentDetail ?: return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(10000)
            if (exoPlayer.isPlaying && exoPlayer.duration > 0) {
                viewModel.savePlayRecord(
                    PlayRecordRequest(
                        source = detail.source,
                        id = detail.id,
                        title = detail.title,
                        cover = detail.displayCover,
                        year = detail.year,
                        sourceName = detail.displaySourceName,
                        index = selectedEpisode + 1,
                        total_episodes = detail.episodes.size,
                        play_time = (exoPlayer.currentPosition / 1000).toInt(),
                        total_time = (exoPlayer.duration / 1000).toInt(),
                        type = detail.typeName
                    )
                )
            }
        }
    }

    // 退出时保存播放记录
    DisposableEffect(Unit) {
        onDispose {
            val detail = currentDetail
            if (detail != null && exoPlayer.currentPosition > 1000) {
                viewModel.savePlayRecord(
                    PlayRecordRequest(
                        source = detail.source,
                        id = detail.id,
                        title = detail.title,
                        cover = detail.displayCover,
                        year = detail.year,
                        sourceName = detail.displaySourceName,
                        index = selectedEpisode + 1,
                        total_episodes = detail.episodes.size,
                        play_time = (exoPlayer.currentPosition / 1000).toInt(),
                        total_time = (exoPlayer.duration / 1000).toInt(),
                        type = detail.typeName
                    )
                )
            }
        }
    }

    // 返回时立即停止播放+隐藏画面
    var isNavigatingBack by remember { mutableStateOf(false) }
    BackHandler {
        isNavigatingBack = true
        exoPlayer.stop()
        onBack()
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
    val sourceSpeeds by viewModel.sourceSpeeds.collectAsState()
    val speedTesting by viewModel.speedTesting.collectAsState()
    var showSourcePanel by remember { mutableStateOf(false) }

    // 计算显示源
    val displaySources = if (isTablet) {
        val normalizedTitle = currentDetail.title.replace(" ", "").lowercase()
        val relevantSources = allSearchSources.filter { src ->
            val srcTitle = src.title.replace(" ", "").lowercase()
            srcTitle == normalizedTitle || srcTitle.contains(normalizedTitle) || normalizedTitle.contains(srcTitle)
        }
        if (relevantSources.size > 1) {
            relevantSources
                .groupBy { it.source }
                .values.map { group -> group.first() }
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
    } else {
        if (allSearchSources.size > 1) {
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
    }

    Scaffold { padding ->
        if (isTablet) {
            // ═══ 平板布局: 左侧(2/3) + 右侧(1/3) ═══
            var rightTab by remember { mutableIntStateOf(0) }
            val currentSource = displaySources.getOrNull(selectedSource)
            val episodes = currentSource?.episodes ?: emptyList()

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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

                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = config!!.contentPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(config.itemSpacing)
                ) {
                    // ===== 左侧: 视频播放器 + 影片信息 (flex-2) =====
                    Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        // 视频播放器 (16:9, rounded-2xl)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                        ) {
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
                            Box(
                                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                    detectTapGestures {
                                        inlineControlsVisible = !inlineControlsVisible
                                        if (inlineControlsVisible) inlineInteractionCount++
                                    }
                                }
                            )
                            if (inlineControlsVisible) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Spacer(modifier = Modifier.size(40.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (exoPlayer.currentPosition > 0) {
                                                    viewModel.setResumePosition(exoPlayer.currentPosition)
                                                }
                                                onPlay(currentDetail.source, selectedEpisode)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }
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
                                    var currentPosition by remember { mutableLongStateOf(0L) }
                                    var duration by remember { mutableLongStateOf(0L) }
                                    LaunchedEffect(exoPlayer) {
                                        while (true) {
                                            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            duration = exoPlayer.duration.coerceAtLeast(0L)
                                            kotlinx.coroutines.delay(500)
                                        }
                                    }
                                    Text(formatTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
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
                                    Text(formatTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 影片信息
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(buildString { append(currentDetail.title); if (currentDetail.year.isNotBlank()) append(" (${currentDetail.year})") }, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (currentDetail.rate != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(currentDetail.rate!!, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                        }
                                        Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (currentDetail.displayType.isNotBlank()) {
                                        Text(categoryName(currentDetail.displayType), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (currentDetail.year.isNotBlank()) {
                                        Text(currentDetail.year, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                        if (currentDetail.desc.isNotBlank()) {
                            var tabletDescExpanded by remember { mutableStateOf(false) }
                            Column {
                                Text(currentDetail.desc, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp, maxLines = if (tabletDescExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                                if (currentDetail.desc.length > 100) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (tabletDescExpanded) "收起" else "展开全部",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { tabletDescExpanded = !tabletDescExpanded }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (currentDetail.director.isNotBlank() || currentDetail.actor.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(3) {
                                    Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(buildString {
                                    if (currentDetail.director.isNotBlank()) append("导演：${currentDetail.director}")
                                    if (currentDetail.director.isNotBlank() && currentDetail.actor.isNotBlank()) append(" / ")
                                    if (currentDetail.actor.isNotBlank()) append("主演：${currentDetail.actor}")
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
                                0 -> TabletEpisodeGrid(episodes, selectedEpisode, exoPlayer, currentDetail.source) { index ->
                                    selectedEpisode = index
                                    val ep = episodes.getOrNull(index)
                                    if (ep != null && ep.url.isNotBlank()) {
                                        exoPlayer.setMediaItem(MediaItem.fromUri(ep.url))
                                        exoPlayer.prepare()
                                        exoPlayer.playWhenReady = true
                                    }
                                }
                                1 -> TabletSourceList(
                                    displaySources = displaySources,
                                    allSearchSources = allSearchSources,
                                    selectedSource = selectedSource,
                                    sourceSpeeds = sourceSpeeds,
                                    speedTesting = speedTesting,
                                    onSourceSelect = { index, info ->
                                        val savedPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                        isSwitchingSource = true
                                        selectedSource = index
                                        selectedEpisode = 0
                                        viewModel.switchSource(info)
                                        val ep = info.episodes.firstOrNull()
                                        if (ep != null && ep.isNotBlank()) {
                                            exoPlayer.stop()
                                            exoPlayer.setMediaItem(MediaItem.fromUri(ep))
                                            exoPlayer.prepare()
                                            exoPlayer.playWhenReady = true
                                            if (savedPosition > 0) {
                                                exoPlayer.addListener(object : Player.Listener {
                                                    override fun onPlaybackStateChanged(state: Int) {
                                                        if (state == Player.STATE_READY) {
                                                            exoPlayer.seekTo(savedPosition)
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
        } else {
            // ═══ 手机布局 ═══
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
                        if (isNavigatingBack) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                            return@item
                        }
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
                        if (inlineControlsVisible) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 16.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                            }
                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 16.dp)) {
                                IconButton(onClick = { showCastSheet = true }) {
                                    Icon(Icons.Default.Cast, "投屏", tint = if (castDevice != null) MaterialTheme.colorScheme.primary else Color.White)
                                }
                            }
                            IconButton(
                                onClick = {
                                    onPlay(currentDetail.source, selectedEpisode)
                                },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White)
                            }
                            var isPlaying by remember { mutableStateOf(false) }
                            LaunchedEffect(exoPlayer) {
                                while (true) {
                                    isPlaying = exoPlayer.isPlaying
                                    kotlinx.coroutines.delay(500)
                                }
                            }
                            IconButton(
                                onClick = {
                                    exoPlayer.playWhenReady = !exoPlayer.isPlaying
                                    inlineControlsVisible = true
                                },
                                modifier = Modifier.align(Alignment.Center).size(56.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
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
                        if (inlineControlsVisible) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    formatTime(positionMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(40.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
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
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.3f))
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxWidth(fraction = progress).height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset((progress * barWidthPx - 8).toInt(), 0) }
                                            .size(16.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                                Text(
                                    formatTime(durationMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        } // end if inlineControlsVisible
                    }
                }

                // ===== 影片信息 =====
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
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
                            Text(currentDetail.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                            Text("简介", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(currentDetail.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp, maxLines = if (descExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                            if (currentDetail.desc.length > 80) {
                                Text(if (descExpanded) "收起" else "展开全部", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { descExpanded = !descExpanded }.padding(top = 2.dp))
                            }
                        }
                    }
                }

                // ===== 片源选择 =====
                if (displaySources.size > 1) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("片源 (${allSearchSources.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (speedTesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("测速中", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(
                                        onClick = { viewModel.testSourceSpeeds() },
                                        enabled = !speedTesting
                                    ) {
                                        if (speedTesting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(if (speedTesting) "测速中..." else "\u26A1 测速", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { showSourcePanel = true }) {
                                        Text("展开 \u25B6", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                displaySources.forEachIndexed { index, src ->
                                    val speed = sourceSpeeds[src.source]
                                    FilterChip(
                                        selected = selectedSource == index,
                                        onClick = {
                                            val info = allSearchSources.firstOrNull { src.source == it.source }
                                            if (info != null && info.episodes.isNotEmpty()) {
                                                val savedPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                                selectedSource = index
                                                selectedEpisode = 0
                                                viewModel.switchSource(info)
                                                val url = info.episodes[0]
                                                if (url.isNotBlank()) {
                                                    exoPlayer.stop()
                                                    exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                                    exoPlayer.prepare()
                                                    exoPlayer.playWhenReady = true
                                                    if (savedPosition > 0) {
                                                        val seekPos = savedPosition
                                                        exoPlayer.addListener(object : Player.Listener {
                                                            override fun onPlaybackStateChanged(state: Int) {
                                                                if (state == Player.STATE_READY) {
                                                                    exoPlayer.seekTo(seekPos)
                                                                    exoPlayer.removeListener(this)
                                                                }
                                                            }
                                                        })
                                                    }
                                                }
                                            }
                                        },
                                        label = {
                                            Text(
                                                buildString {
                                                    append("${src.sourceName}")
                                                    if (speed != null && speed > 0) append(" ${speed}ms")
                                                    else if (speed == -1L) append(" 超时")
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ===== 选集 =====
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
                                modifier = Modifier.padding(horizontal = 16.dp).clickable {
                                    selectedEpisode = 0
                                    val url = currentEpisodes[0]
                                    if (url.isNotBlank()) {
                                        exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                        exoPlayer.prepare()
                                        exoPlayer.playWhenReady = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedEpisode == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("播放", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        val rows = currentEpisodes.chunked(6)
                        itemsIndexed(rows) { rowIndex, rowEpisodes ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowEpisodes.forEachIndexed { colIndex, ep ->
                                    val globalIndex = rowIndex * 6 + colIndex
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable {
                                            selectedEpisode = globalIndex
                                            if (ep.isNotBlank()) {
                                                exoPlayer.setMediaItem(MediaItem.fromUri(ep))
                                                exoPlayer.prepare()
                                                exoPlayer.playWhenReady = true
                                            }
                                        },
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
                                repeat(6 - rowEpisodes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ===== 源列表弹窗 =====
            if (showSourcePanel) {
                ModalBottomSheet(
                    onDismissRequest = { showSourcePanel = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    // 找到最快源
                    val fastestInPanel = allSearchSources.filter { (sourceSpeeds[it.source] ?: -1L) > 0 }
                        .minByOrNull { sourceSpeeds[it.source] ?: Long.MAX_VALUE }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("全部源 (${allSearchSources.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (speedTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("自动测速中...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(
                                    onClick = { viewModel.testSourceSpeeds() },
                                    enabled = !speedTesting
                                ) {
                                    Text(if (speedTesting) "测速中..." else "\u26A1 测速排序")
                                }
                                IconButton(onClick = { showSourcePanel = false }) {
                                    Icon(Icons.Default.Close, "关闭")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(allSearchSources.size) { index ->
                                val info = allSearchSources[index]
                                val speed = sourceSpeeds[info.source]
                                val isCurrent = currentDetail?.source == info.source
                                val isFastest = fastestInPanel != null && info.source == fastestInPanel.source && (speed ?: -1L) > 0
                                Surface(
                                    onClick = {
                                        if (info.episodes.isNotEmpty()) {
                                            val savedPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            selectedEpisode = 0
                                            viewModel.switchSource(info)
                                            val url = info.episodes[0]
                                            if (url.isNotBlank()) {
                                                exoPlayer.stop()
                                                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                                exoPlayer.prepare()
                                                exoPlayer.playWhenReady = true
                                                if (savedPosition > 0) {
                                                    val seekPos = savedPosition
                                                    exoPlayer.addListener(object : Player.Listener {
                                                        override fun onPlaybackStateChanged(state: Int) {
                                                            if (state == Player.STATE_READY) {
                                                                exoPlayer.seekTo(seekPos)
                                                                exoPlayer.removeListener(this)
                                                            }
                                                        }
                                                    })
                                                }
                                            }
                                            showSourcePanel = false
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            info.displaySourceName.ifBlank { info.source },
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${info.episodes.size}集",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            when {
                                                speed == null -> "-"
                                                speed < 0 -> "超时"
                                                else -> "${speed}ms"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = when {
                                                speed == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                                speed < 0 -> Color.Red
                                                speed < 500 -> Color(0xFF4CAF50)
                                                speed < 1000 -> Color(0xFFFF9800)
                                                else -> Color.Red
                                            }
                                        )
                                        if (isFastest) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4CAF50)) {
                                                Text("最快", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end phone layout
    }

    // ===== 投屏设备选择弹窗 =====
    val castScope = rememberCoroutineScope()
    DlnaCastSheet(
        isVisible = showCastSheet,
        onDismiss = { showCastSheet = false },
        onDeviceSelected = { device ->
            castDevice = device
            val controller = DlnaController(device)
            castController = controller
            val currentEpisodes = detail?.episodes ?: emptyList()
            val url = currentEpisodes.getOrNull(0)
            if (!url.isNullOrBlank()) {
                castScope.launch {
                    controller.setUriAndPlay(url, detail?.title ?: "LeChenMusic")
                }
            }
        }
    )
}

// ==================== 平板选集网格 ====================
@Composable
private fun TabletEpisodeGrid(
    episodes: List<VideoEpisode>,
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

// ==================== 平板播放源列表 ====================
@Composable
private fun TabletSourceList(
    displaySources: List<VideoSource>,
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

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
