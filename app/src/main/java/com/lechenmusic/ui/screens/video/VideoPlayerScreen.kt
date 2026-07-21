package com.lechenmusic.ui.screens.video

import android.app.Activity
import com.lechenmusic.ErrorReporter
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.lechenmusic.data.model.VideoEpisode
import com.lechenmusic.data.model.VideoSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    sources: List<VideoSource>,
    initialSource: Int = 0,
    initialEpisode: Int = 0,
    onBack: () -> Unit
) {
    // Bug修复：sources 为空时显示提示而不是闪退
    if (sources.isEmpty() || sources.all { it.episodes.isEmpty() }) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "该源暂无播放资源",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    videoTitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBack) {
                    Text("返回")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var selectedSource by remember { mutableIntStateOf(initialSource) }
    var selectedEpisode by remember { mutableIntStateOf(initialEpisode) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showEpisodePanel by remember { mutableStateOf(false) }

    // 手势控制状态
    var brightnessOffset by remember { mutableFloatStateOf(0f) }
    var volumeOffset by remember { mutableFloatStateOf(0f) }
    var seekOffset by remember { mutableLongStateOf(0L) }
    var isGestureActive by remember { mutableStateOf(false) }
    var gestureType by remember { mutableStateOf("") } // "brightness", "volume", "seek"

    val currentSource = sources.getOrNull(selectedSource)
    val currentEpisode = currentSource?.episodes?.getOrNull(selectedEpisode)
    val episodeTitle = currentEpisode?.title ?: "第${selectedEpisode + 1}集"

    // ExoPlayer - 使用 DefaultMediaSourceFactory 支持 HLS/DASH/RTSP 等格式
    val exoPlayer = remember {
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // 加载视频
    LaunchedEffect(selectedSource, selectedEpisode) {
        val episode = sources.getOrNull(selectedSource)?.episodes?.getOrNull(selectedEpisode)
        try {
            logFile.appendText("[${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())}] [Player] 加载集数: source=$selectedSource, ep=$selectedEpisode, url=${episode?.url?.take(80)}\n")
        } catch (_: Exception) {}
        if (episode != null && episode.url.isNotBlank()) {
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(episode.url))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            } catch (e: Exception) {
                try {
                    logFile.appendText("[Player] ExoPlayer 异常: ${e.javaClass.simpleName}: ${e.message}\n")
                } catch (_: Exception) {}
            }
        } else {
            try {
                logFile.appendText("[Player] episode 为空或 URL 为空\n")
            } catch (_: Exception) {}
        }
    }

    // 更新进度
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
    }

    // 监听播放状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    // 自动播放下一集
                    val maxEp = currentSource?.episodes?.size ?: 0
                    if (selectedEpisode < maxEp - 1) {
                        selectedEpisode++
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Bug修复: 播放器错误上报到 WEB 管理端
                ErrorReporter.reportError(
                    level = "error",
                    message = "[影视播放] ${error.errorCodeName}: ${error.message}",
                    throwable = error,
                    screen = "video_player_${currentSource?.sourceName ?: "unknown"}"
                )
                // 尝试播放下一集
                val maxEp = currentSource?.episodes?.size ?: 0
                if (selectedEpisode < maxEp - 1) {
                    selectedEpisode++
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 全屏模式
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (isFullscreen) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    // 控制栏自动隐藏
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // 手势偏移量文字
    val gestureText = when (gestureType) {
        "brightness" -> "亮度 ${(brightnessOffset * 100).toInt()}%"
        "volume" -> "音量 ${(volumeOffset * 100).toInt()}%"
        "seek" -> {
            val targetPos = (currentPosition + seekOffset).coerceIn(0, duration)
            "${formatVideoTime(targetPos)} / ${formatVideoTime(duration)}"
        }
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 视频播放区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (!isLocked) showControls = !showControls
                }
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isGestureActive = true
                            gestureType = "seek"
                            seekOffset = 0L
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            seekOffset += (dragAmount * 200).toLong()
                        },
                        onDragEnd = {
                            if (gestureType == "seek") {
                                val targetPos = (currentPosition + seekOffset).coerceIn(0, duration)
                                exoPlayer.seekTo(targetPos)
                            }
                            isGestureActive = false
                            gestureType = ""
                            seekOffset = 0L
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isGestureActive = true
                            gestureType = if (offset.x < size.width / 2) "brightness" else "volume"
                        },
                        onVerticalDrag = { _, dragAmount ->
                            when (gestureType) {
                                "brightness" -> {
                                    brightnessOffset = (brightnessOffset - dragAmount / size.height).coerceIn(-1f, 1f)
                                }
                                "volume" -> {
                                    volumeOffset = (volumeOffset - dragAmount / size.height).coerceIn(0f, 1f)
                                }
                            }
                        },
                        onDragEnd = {
                            isGestureActive = false
                            gestureType = ""
                        }
                    )
                }
        ) {
            // ExoPlayer 的实际渲染需要 AndroidView，这里用纯 Compose 占位
            // 实际项目中需要嵌入 AndroidView { PlayerView }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!isPlaying && currentPosition == 0L) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$videoTitle - $episodeTitle",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 手势提示
        if (isGestureActive && gestureText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    gestureText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // 控制层
        if (showControls && !isLocked) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        videoTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        episodeTitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // 底部控制栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 进度条
                if (duration > 0) {
                    Slider(
                        value = currentPosition.toFloat() / duration.coerceAtLeast(1L),
                        onValueChange = { fraction ->
                            val newPos = (fraction * duration).toLong()
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE94560),
                            activeTrackColor = Color(0xFFE94560),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatVideoTime(currentPosition), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(formatVideoTime(duration), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一集
                    IconButton(
                        onClick = {
                            if (selectedEpisode > 0) {
                                selectedEpisode--
                            }
                        },
                        enabled = selectedEpisode > 0
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            "上一集",
                            tint = if (selectedEpisode > 0) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // 后退15秒
                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 15000).coerceAtLeast(0))
                    }) {
                        Icon(Icons.Default.Replay10, "后退15秒", tint = Color.White)
                    }

                    // 播放/暂停
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color(0xFFE94560)
                    ) {
                        IconButton(onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // 前进15秒
                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 15000).coerceAtMost(duration))
                    }) {
                        Icon(Icons.Default.Forward30, "前进15秒", tint = Color.White)
                    }

                    // 下一集
                    IconButton(
                        onClick = {
                            val maxEp = currentSource?.episodes?.size ?: 0
                            if (selectedEpisode < maxEp - 1) {
                                selectedEpisode++
                            }
                        },
                        enabled = currentSource != null && selectedEpisode < currentSource.episodes.size - 1
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            "下一集",
                            tint = if (currentSource != null && selectedEpisode < currentSource.episodes.size - 1)
                                Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 功能按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 倍速
                    Text(
                        "${playbackSpeed}x",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showSpeedMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    Row {
                        // 选集
                        IconButton(onClick = { showEpisodePanel = true }) {
                            Icon(Icons.Default.List, "选集", tint = Color.White)
                        }

                        // 锁定
                        IconButton(onClick = { isLocked = true }) {
                            Icon(Icons.Default.Lock, "锁定", tint = Color.White)
                        }

                        // 全屏切换
                        IconButton(onClick = {
                            isFullscreen = !isFullscreen
                        }) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                "全屏",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 锁定按钮（锁定状态下显示解锁按钮）
        if (isLocked) {
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    "解锁",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 倍速选择菜单
        if (showSpeedMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showSpeedMenu = false }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("播放速度", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                        speeds.forEach { speed ->
                            Text(
                                "${speed}x",
                                fontSize = 15.sp,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        exoPlayer.playbackParameters = PlaybackParameters(speed)
                                        showSpeedMenu = false
                                    }
                                    .padding(vertical = 10.dp)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 选集面板
        if (showEpisodePanel) {
            ModalBottomSheet(
                onDismissRequest = { showEpisodePanel = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "选集 (${currentSource?.episodes?.size ?: 0})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 播放源切换
                    if (sources.size > 1) {
                        Text("播放源", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sources.forEachIndexed { index, src ->
                                FilterChip(
                                    selected = selectedSource == index,
                                    onClick = {
                                        selectedSource = index
                                        selectedEpisode = 0
                                    },
                                    label = { Text(src.sourceName, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 集数网格
                    currentSource?.episodes?.chunked(6)?.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { ep ->
                                val isSelected = ep.index == selectedEpisode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedEpisode = ep.index
                                            showEpisodePanel = false
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "${ep.index + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            repeat(6 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
