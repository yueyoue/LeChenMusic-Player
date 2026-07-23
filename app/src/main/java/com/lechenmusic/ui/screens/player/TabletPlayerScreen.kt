package com.lechenmusic.ui.screens.player

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.lechenmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ==================== Color Extraction ====================

/**
 * 从封面图提取主色调，生成渐变背景色
 */
@Composable
fun rememberCoverColors(coverUrl: String?): Pair<Color, Color> {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF1a1a2e)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFF6C5CE7)) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) return@LaunchedEffect
        try {
            val request = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
            val drawable = coil.ImageLoader(context).execute(request).drawable
            val bitmap = drawable?.toBitmap(128, 128)
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).generate()
                }
                palette.dominantSwatch?.rgb?.let {
                    dominantColor = Color(it).copy(alpha = 0.3f)
                }
                palette.vibrantSwatch?.rgb?.let {
                    vibrantColor = Color(it).copy(alpha = 0.15f)
                } ?: palette.mutedSwatch?.rgb?.let {
                    vibrantColor = Color(it).copy(alpha = 0.15f)
                }
            }
        } catch (_: Exception) {}
    }

    return dominantColor to vibrantColor
}

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
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val displayRotation = if (isPlaying) rotation else 0f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxSize().rotate(displayRotation).shadow(24.dp, CircleShape).clip(CircleShape).background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val maxRadius = this.size.minDimension / 2
                for (i in 0..18) {
                    val radius = maxRadius * (0.52f + i * 0.025f)
                    val alpha = if (i % 3 == 0) 0.08f else 0.04f
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = radius, center = center, style = Stroke(width = 0.6f))
                }
                // 标签区域圆环
                drawCircle(color = Color.White.copy(alpha = 0.06f), radius = maxRadius * 0.38f, center = center, style = Stroke(width = 1.5f))
            }
            // 中心封面
            Box(modifier = Modifier.fillMaxSize(0.52f).clip(CircleShape).background(Color(0xFF111125))) {
                if (coverUrl != null) {
                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                    }
                }
            }
            // 中心轴
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1a1a1a)))
        }
    }
}

// ==================== Realistic Stylus Arm ====================

/**
 * 真实风格唱臂
 * - 支点在唱片右上方
 * - 唱臂斜跨唱片表面（z-index 高于唱片）
 * - 播放时唱针搭在唱片内圈，暂停时抬起
 */
@Composable
fun StylusArm(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val targetAngle = if (isPlaying) 28f else 8f
    val angle by animateFloatAsState(targetValue = targetAngle, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "stylus")

    // 唱臂总长（从支点到唱针）
    val armLength = 220.dp
    val armWidth = 180.dp

    Box(modifier = modifier.size(armWidth, armLength)) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(angle, pivot = Offset(0f, 0f))) {
            val pivotX = 0f
            val pivotY = 0f
            val armEndX = size.width * 0.85f
            val armEndY = size.height * 0.9f

            // 支点底座（金属质感）
            drawCircle(color = Color(0xFF555555), radius = 16f, center = Offset(pivotX, pivotY))
            drawCircle(color = Color(0xFF888888), radius = 12f, center = Offset(pivotX, pivotY))
            drawCircle(color = Color(0xFFAAAAAA), radius = 6f, center = Offset(pivotX, pivotY))

            // 唱臂主体（两段式：水平臂 + 垂直唱头壳）
            val midX = armEndX * 0.6f
            val midY = armEndY * 0.55f

            // 第一段：从支点到拐角（银色金属管）
            drawLine(
                color = Color(0xFFC0C0C0),
                start = Offset(pivotX, pivotY),
                end = Offset(midX, midY),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            // 第一段高光线
            drawLine(
                color = Color(0xFFE8E8E8),
                start = Offset(pivotX + 2f, pivotY - 1f),
                end = Offset(midX + 2f, midY - 1f),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )

            // 第二段：从拐角到唱头（略细）
            drawLine(
                color = Color(0xFFB0B0B0),
                start = Offset(midX, midY),
                end = Offset(armEndX, armEndY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // 唱头壳（黑色方块）
            val headX = armEndX
            val headY = armEndY
            drawRoundRect(
                color = Color(0xFF222222),
                topLeft = Offset(headX - 8f, headY - 4f),
                size = androidx.compose.ui.geometry.Size(16f, 24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            // 唱针（白色小点）
            drawCircle(color = Color(0xFFEEEEEE), radius = 2.5f, center = Offset(headX, headY + 22f))
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

    // 从封面提取颜色
    val (dominantColor, vibrantColor) = rememberCoverColors(coverUrl)

    // Parse lyrics
    val lyricsText = currentLyrics
    val lrcLines = remember(lyricsText) { lyricsText?.let { parseLrc(it) } }
    val plainLines = remember(lyricsText) {
        if (lrcLines == null) lyricsText?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        else emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(dominantColor, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 左上角返回按钮
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "返回", modifier = Modifier.size(32.dp))
            }

            // ===== 主内容区 =====
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：留声机（唱片 + 唱臂，唱臂在上层）
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    VinylRecord(coverUrl = coverUrl, isPlaying = isPlaying, size = 320.dp)
                    // 唱臂在唱片右上方，z-index 更高
                    StylusArm(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-20).dp, y = 40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 右侧：歌曲信息 + 歌词
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 16.dp)) {
                    Text(song.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        song.artist, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { if (song.artistId.isNotBlank()) onNavigateToArtist(song.artistId) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 歌词（无底部背景色填充）
                    if (lrcLines != null) {
                        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex) { listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0)) }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp)) {
                                itemsIndexed(lrcLines) { index, line ->
                                    val isActive = index == activeIndex
                                    Text(
                                        text = line.text,
                                        fontSize = if (isActive) 20.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.padding(vertical = if (isActive) 6.dp else 4.dp).fillMaxWidth()
                                    )
                                }
                            }
                            // 仅顶部渐变遮罩（去掉底部遮罩）
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0.8f), Color.Transparent))))
                        }
                    } else if (plainLines.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp)) {
                                itemsIndexed(plainLines) { _, line ->
                                    Text(line, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(formatTime(currentPosition), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(value = progress, onValueChange = { playerManager.seekToProgress(it) }, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(formatTime(duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

        // 右侧浮动工具栏
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
