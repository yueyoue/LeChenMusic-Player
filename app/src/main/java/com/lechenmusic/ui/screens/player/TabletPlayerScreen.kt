package com.lechenmusic.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

// ==================== Vinyl Record ====================

@Composable
fun VinylRecord(
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 360.dp
) {
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
    val displayRotation = if (isPlaying) rotation else 0f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(displayRotation)
                .shadow(24.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val maxRadius = this.size.minDimension / 2
                val grooveColor = Color.White.copy(alpha = 0.06f)
                for (i in 0..15) {
                    val radius = maxRadius * (0.55f + i * 0.028f)
                    drawCircle(color = grooveColor, radius = radius, center = center, style = Stroke(width = 0.8f))
                }
                drawCircle(color = Color.White.copy(alpha = 0.04f), radius = maxRadius * 0.42f, center = center, style = Stroke(width = 1f))
            }
            Box(
                modifier = Modifier.fillMaxSize(0.55f).clip(CircleShape).background(Color(0xFF111125))
            ) {
                if (coverUrl != null) {
                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
            }
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF222222)))
        }
    }
}

// ==================== Stylus Arm ====================

@Composable
fun StylusArm(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val targetAngle = if (isPlaying) 25f else 5f
    val angle by animateFloatAsState(targetValue = targetAngle, animationSpec = tween(500, easing = FastOutSlowInEasing), label = "stylus")

    Box(modifier = modifier.width(160.dp).height(200.dp)) {
        Box(
            modifier = Modifier.size(24.dp).align(Alignment.TopStart).offset(x = 16.dp, y = 0.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f))
        )
        Canvas(modifier = Modifier.fillMaxSize().rotate(angle).offset(x = 16.dp, y = 12.dp)) {
            val start = Offset(12f, 12f)
            val end = Offset(size.width * 0.7f, size.height * 0.85f)
            drawLine(color = Color.White.copy(alpha = 0.7f), start = start, end = end, strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFFDDDDDD), start = end, end = Offset(end.x + 12f, end.y + 8f), strokeWidth = 8f, cap = StrokeCap.Round)
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 8f, center = start)
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
    LaunchedEffect(song.id) { viewModel.loadLyrics(song) }

    val coverUrl = com.lechenmusic.data.api.ApiClient.getCoverArtUrl(serverUrl, username, password, song.coverArt ?: song.albumId)

    // Parse lyrics
    val lyricsText = currentLyrics
    val lrcLines = remember(lyricsText) { lyricsText?.let { parseLrc(it) } }
    val plainLines = remember(lyricsText) {
        if (lrcLines == null) lyricsText?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        else emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.background))
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 主内容区 =====
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：留声机
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    StylusArm(isPlaying = isPlaying, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 20.dp))
                    VinylRecord(coverUrl = coverUrl, isPlaying = isPlaying, size = 340.dp)
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 右侧：歌曲信息 + 歌词
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 24.dp)) {
                    Text(song.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        song.artist, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { if (song.artistId.isNotBlank()) onNavigateToArtist(song.artistId) }
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    // 歌词
                    if (lrcLines != null) {
                        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex) { listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0)) }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 60.dp)) {
                                itemsIndexed(lrcLines) { index, line ->
                                    val isActive = index == activeIndex
                                    Text(
                                        text = line.text,
                                        fontSize = if (isActive) 20.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = if (isActive) 6.dp else 4.dp).fillMaxWidth()
                                    )
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), Color.Transparent))))
                            Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f)))))
                        }
                    } else if (plainLines.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp)) {
                                itemsIndexed(plainLines) { _, line ->
                                    Text(line, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth())
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("暂无歌词", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // ===== 底部控制栏 =====
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)) {
                    // 进度条
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(formatTime(currentPosition), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(value = progress, onValueChange = { playerManager.seekTo(it) }, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(formatTime(duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 控制按钮
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = { playerManager.toggleStar() }) {
                                Icon(if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏",
                                    tint = if (isStarred) Color(0xFFFF4D6A) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Download, "下载", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { playerManager.toggleShuffle() }) {
                                Icon(Icons.Default.Shuffle, "随机", tint = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { playerManager.skipPrevious() }) {
                                Icon(Icons.Default.SkipPrevious, "上一首", modifier = Modifier.size(32.dp))
                            }
                            FilledIconButton(onClick = { playerManager.togglePlayPause() }, modifier = Modifier.size(56.dp), shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "暂停" else "播放", modifier = Modifier.size(28.dp))
                            }
                            IconButton(onClick = { playerManager.skipNext() }) {
                                Icon(Icons.Default.SkipNext, "下一首", modifier = Modifier.size(32.dp))
                            }
                            IconButton(onClick = { playerManager.toggleRepeat() }) {
                                Icon(
                                    when (repeatMode) { com.lechenmusic.player.RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                                    "循环",
                                    tint = if (repeatMode != com.lechenmusic.player.RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Slider(value = 0.75f, onValueChange = { }, modifier = Modifier.width(100.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurfaceVariant, activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
        }

        // ===== 右侧浮动工具栏 =====
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("音", "词", "谱").forEach { label ->
                Surface(modifier = Modifier.size(40.dp).clickable { }, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
                    Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(modifier = Modifier.size(40.dp).clickable { }, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.HelpOutline, "帮助", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
