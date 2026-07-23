package com.lechenmusic.ui.screens.player

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
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

// ==================== Cover Image Display ====================

@Composable
fun CoverImageDisplay(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    size: Int = 320
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.MusicNote, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
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
                // 左侧：封面图
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    CoverImageDisplay(coverUrl = coverUrl, size = 320)
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

                    // 歌词（无背景，使用默认背景）
                    if (lrcLines != null) {
                        val activeIndex = findActiveLyricLine(lrcLines, currentPosition)
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex) { listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0)) }

                        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
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
                    } else if (plainLines.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
                            itemsIndexed(plainLines) { _, line ->
                                Text(line, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth())
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("暂无歌词", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // ===== 底部控制栏（无额外背景，使用默认背景） =====
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
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

