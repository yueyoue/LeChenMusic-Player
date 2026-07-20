package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.VideoEpisode
import com.lechenmusic.data.model.VideoSource

/**
 * 影视播放器页面
 * 布局：上方播放器区域 + 下方选集/详情
 */
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    sources: List<VideoSource>,
    initialSource: Int = 0,
    initialEpisode: Int = 0,
    onBack: () -> Unit
) {
    var selectedSource by remember { mutableIntStateOf(initialSource) }
    var selectedEpisode by remember { mutableIntStateOf(initialEpisode) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val currentSource = sources.getOrNull(selectedSource)
    val currentEpisode = currentSource?.episodes?.getOrNull(selectedEpisode)
    val episodeTitle = currentEpisode?.title ?: "第${selectedEpisode + 1}集"

    Column(modifier = Modifier.fillMaxSize()) {
        // ========== 播放器区域 ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .clickable { showControls = !showControls }
        ) {
            // TODO: 实际播放器（ExoPlayer）在这里

            // 播放器占位
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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

            // 控制层
            if (showControls) {
                // 顶部栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                        )
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 进度条
                    if (duration > 0) {
                        LinearProgressIndicator(
                            progress = { (currentPosition.toFloat() / duration).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE94560),
                            trackColor = Color.White.copy(alpha = 0.3f)
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
                                    currentPosition = 0
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
                        IconButton(onClick = { currentPosition = (currentPosition - 15000).coerceAtLeast(0) }) {
                            Icon(Icons.Default.Replay10, "后退15秒", tint = Color.White)
                        }

                        // 播放/暂停
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = Color(0xFFE94560)
                        ) {
                            IconButton(onClick = { isPlaying = !isPlaying }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // 前进15秒
                        IconButton(onClick = { currentPosition = (currentPosition + 15000).coerceAtMost(duration) }) {
                            Icon(Icons.Default.Forward30, "前进15秒", tint = Color.White)
                        }

                        // 下一集
                        IconButton(
                            onClick = {
                                val maxEp = currentSource?.episodes?.size ?: 0
                                if (selectedEpisode < maxEp - 1) {
                                    selectedEpisode++
                                    currentPosition = 0
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
                }

                // 倍速按钮（右上角）
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(48.dp)
                        .clickable {
                            playbackSpeed = when (playbackSpeed) {
                                0.5f -> 0.75f
                                0.75f -> 1.0f
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.5f
                            }
                        }
                ) {
                    Text(
                        "${playbackSpeed}x",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ========== 下方信息区 ==========
        LazyColumn(modifier = Modifier.weight(1f)) {
            // 标题信息
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        videoTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "正在播放：$episodeTitle",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 播放源切换
            if (sources.size > 1) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("播放源", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(sources) { index, source ->
                                FilterChip(
                                    selected = selectedSource == index,
                                    onClick = {
                                        selectedSource = index
                                        selectedEpisode = 0
                                        currentPosition = 0
                                    },
                                    label = { Text(source.sourceName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 选集
            item {
                Text(
                    "选集 (${currentSource?.episodes?.size ?: 0})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (currentSource != null) {
                itemsIndexed(currentSource.episodes.chunked(6)) { _, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { ep ->
                            val isSelected = ep.index == selectedEpisode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedEpisode = ep.index
                                        currentPosition = 0
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
