package com.lechenmusic.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.components.CoverImage

// LRC 歌词解析
private data class LyricLine(val timeMs: Long, val text: String)

private fun parseLrc(lrcText: String): List<LyricLine>? {
    val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
    val lines = mutableListOf<LyricLine>()
    for (line in lrcText.split("\n")) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue
        val match = regex.matchEntire(trimmed) ?: continue
        val min = match.groupValues[1].toLongOrNull() ?: continue
        val sec = match.groupValues[2].toLongOrNull() ?: continue
        val msStr = match.groupValues[3]
        val ms = when (msStr.length) {
            1 -> msStr.toLong() * 100
            2 -> msStr.toLong() * 10
            3 -> msStr.toLong()
            else -> 0L
        }
        val timeMs = min * 60_000 + sec * 1000 + ms
        val text = match.groupValues[4].trim()
        if (text.isNotBlank()) lines.add(LyricLine(timeMs, text))
    }
    return if (lines.size >= 2) lines.sortedBy { it.timeMs } else null
}

private fun findActiveLyricLine(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return 0
    var lo = 0; var hi = lines.size - 1; var result = 0
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (lines[mid].timeMs <= positionMs) { result = mid; lo = mid + 1 }
        else hi = mid - 1
    }
    return result
}

// ==================== Vinyl Record ====================

/**
 * 旋转唱片组件
 * - 黑色唱片 + 紫色沟纹
 * - 中心封面图
 * - isPlaying 控制旋转
 */
@Composable
fun VinylRecord(
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 360.dp
) {
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 暂停时停止旋转（用 0f 覆盖）
    val displayRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 唱片主体
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(displayRotation)
                .shadow(24.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            // 沟纹（用 Canvas 画同心圆）
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val maxRadius = this.size.minDimension / 2
                val grooveColor = Color.White.copy(alpha = 0.06f)

                // 外圈沟纹
                for (i in 0..15) {
                    val radius = maxRadius * (0.55f + i * 0.028f)
                    drawCircle(
                        color = grooveColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 0.8f)
                    )
                }
                // 内圈装饰
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = maxRadius * 0.42f,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // 中心封面
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .clip(CircleShape)
                    .background(Color(0xFF111125))
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // 封面暗色遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f))
                )
            }
            // 中心小圆点
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
            )
        }
    }
}

// ==================== Stylus Arm ====================

/**
 * 唱臂组件
 * - 播放时搭在唱片上（rotate 25°）
 * - 暂停时抬起（rotate 5°）
 */
@Composable
fun StylusArm(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val targetAngle = if (isPlaying) 25f else 5f
    val angle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "stylus"
    )

    Box(
        modifier = modifier
            .width(160.dp)
            .height(200.dp)
    ) {
        // 支点圆
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopStart)
                .offset(x = 16.dp, y = 0.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        )
        // 唱臂线
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle)
                .offset(x = 16.dp, y = 12.dp)
        ) {
            val start = Offset(12f, 12f)
            val end = Offset(size.width * 0.7f, size.height * 0.85f)
            // 臂杆
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            // 唱头
            drawLine(
                color = Color(0xFFDDDDDD),
                start = end,
                end = Offset(end.x + 12f, end.y + 8f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            // 支点
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 8f,
                center = start
            )
        }
    }
}

// ==================== Tablet Player Screen ====================

@Composable
fun TabletPlayerScreen(
    playerManager: com.lechenmusic.player.MusicPlayerManager,
    viewModel: com.lechenmusic.ui.MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
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

    // 加载歌词
    LaunchedEffect(song.id) { viewModel.loadLyrics(song) }

    val coverUrl = com.lechenmusic.data.api.ApiClient.getCoverArtUrl(
        serverUrl, username, password, song.coverArt ?: song.albumId
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 主内容区：留声机 + 歌词 =====
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：留声机
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // 唱臂（在唱片右上方）
                    StylusArm(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-40).dp, y = 20.dp)
                    )
                    // 唱片
                    VinylRecord(
                        coverUrl = coverUrl,
                        isPlaying = isPlaying,
                        size = 340.dp
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 右侧：歌曲信息 + 歌词
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 24.dp)
                ) {
                    // 歌曲信息
                    Text(
                        song.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        song.artist,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            if (song.artistId.isNotBlank()) onNavigateToArtist(song.artistId)
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 歌词滚动区
                    val lyricsText = currentLyrics
                    val lrcLines = remember(lyricsText) { lyricsText?.let { parseLrc(it) } }
                    val plainLines = remember(lyricsText) {
                        if (lrcLines == null) lyricsText?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                        else emptyList()
                    }

                    if (lrcLines != null) {
                        // LRC 格式歌词（带时间轴）
                        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                        // 自动滚动到当前行
                        LaunchedEffect(activeIndex) {
                            listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 60.dp)
                            ) {
                                itemsIndexed(lrcLines) { index, line ->
                                    val isActive = index == activeIndex
                                    Text(
                                        text = line.text,
                                        fontSize = if (isActive) 20.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .padding(vertical = if (isActive) 6.dp else 4.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            // 渐变遮罩
                            Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f), Color.Transparent))))
                            Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(
                                    Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f)))))
                        }
                    } else if (plainLines.isNotEmpty()) {
                        // 纯文本歌词（无时间轴）
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 40.dp)
                            ) {
                                items(plainLines) { line ->
                                    Text(
                                        text = line,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        // 无歌词
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无歌词",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // ===== 底部控制栏 =====
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    // 进度条
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            formatDuration(currentPosition),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = progress,
                            onValueChange = { playerManager.seekTo(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatDuration(duration),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 控制按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：收藏 / 下载 / 更多
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = { playerManager.toggleStar() }) {
                                Icon(
                                    if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isStarred) Color(0xFFFF4D6A)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "下载",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 中间：播放控制
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { playerManager.toggleShuffle() }) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "随机",
                                    tint = if (shuffleMode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { playerManager.skipPrevious() }) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "上一首",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            // 播放/暂停（大按钮）
                            FilledIconButton(
                                onClick = { playerManager.togglePlayPause() },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { playerManager.skipNext() }) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "下一首",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = { playerManager.toggleRepeat() }) {
                                Icon(
                                    when (repeatMode) {
                                        com.lechenmusic.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                                        else -> Icons.Default.Repeat
                                    },
                                    contentDescription = "循环",
                                    tint = if (repeatMode != com.lechenmusic.player.RepeatMode.OFF)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // 右侧：音量
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = 0.75f,
                                onValueChange = { },
                                modifier = Modifier.width(100.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // ===== 右侧浮动工具栏 =====
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("音", "词", "谱").forEach { label ->
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "帮助",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
