package com.lechenmusic.ui.screens.audiobook

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookChapter
import com.lechenmusic.ui.components.SkipBackward15Button
import com.lechenmusic.ui.components.SkipForward15Button
import com.lechenmusic.ui.screens.player.CoverImageDisplay
import com.lechenmusic.ui.screens.player.rememberCoverColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletAudiobookPlayerScreen(
    book: Audiobook,
    chapters: List<AudiobookChapter>,
    currentChapterIndex: Int,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    serverUrl: String,
    username: String,
    password: String,
    coverUrl: String?,
    playbackSpeed: Float = 1f,
    timerMinutes: Int = 0,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipForward15s: () -> Unit,
    onSkipBackward15s: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapterSelect: (Int) -> Unit,
    onSetTimer: (Int) -> Unit = {},
    onChangeSpeed: (Float) -> Unit = {},
    onShowQueue: () -> Unit = {},
    onSaveProgress: () -> Unit = {}
) {
    val currentChapter = chapters.getOrNull(currentChapterIndex)

    // Save progress when leaving
    DisposableEffect(Unit) {
        onDispose { onSaveProgress() }
    }

    // Auto-save progress every 5 seconds during playback
    LaunchedEffect(isPlaying, currentChapterIndex) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            if (isPlaying) onSaveProgress()
        }
    }

    // Save immediately when paused
    var prevIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (prevIsPlaying && !isPlaying) {
            onSaveProgress()
        }
        prevIsPlaying = isPlaying
    }

    // Save when chapter changes
    LaunchedEffect(currentChapterIndex) {
        onSaveProgress()
    }

    // 从封面提取背景色
    val coverBgColor = rememberCoverColor(coverUrl)

    // Progress calculation
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier.fillMaxSize().background(coverBgColor)
    ) {
        // 半透明遮罩
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // 左上角返回按钮
            IconButton(
                onClick = { onSaveProgress(); onBack() },
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "返回", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // ===== 主内容区: 左侧封面 + 右侧章节列表 =====
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：封面图 (与音乐播放器相同的居中布局)
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (coverUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(320.dp)
                                .shadow(16.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(320.dp)
                                .shadow(16.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 右侧：章节信息 + 章节列表 (替代歌词)
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(book.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        currentChapter?.title ?: "",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 章节列表 (替代歌词区域)
                    val chapterListState = rememberLazyListState()
                    LaunchedEffect(Unit) {
                        if (currentChapterIndex in chapters.indices) {
                            chapterListState.animateScrollToItem(currentChapterIndex)
                        }
                    }
                    LaunchedEffect(currentChapterIndex) {
                        if (currentChapterIndex in chapters.indices) {
                            chapterListState.animateScrollToItem(currentChapterIndex)
                        }
                    }

                    LazyColumn(state = chapterListState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 40.dp)) {
                        itemsIndexed(chapters) { index, chapter ->
                            val isActive = index == currentChapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChapterSelect(index) }
                                    .padding(vertical = if (isActive) 6.dp else 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 序号/播放图标
                                if (isActive && isPlaying) {
                                    Icon(
                                        Icons.Default.Equalizer,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isActive) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.width(24.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 章节标题
                                Text(
                                    chapter.title,
                                    fontSize = if (isActive) 20.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) Color.White
                                        else Color.White.copy(alpha = 0.35f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // 时长
                                if (chapter.duration > 0) {
                                    val durMin = chapter.duration / 60
                                    val durSec = chapter.duration % 60
                                    Text(
                                        "%d:%02d".format(durMin, durSec),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = if (isActive) 0.7f else 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== 底部控制栏 (与音乐播放器相同的布局) =====
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // 进度条
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(formatTime(currentPositionMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = progress,
                            onValueChange = { onSeekTo((it * durationMs).toLong()) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(formatTime(durationMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 控制按钮 (居中): 后退15s | 定时 | 上一章 播放/暂停 下一章 | 倍速 | 前进15s
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 后退15秒
                        SkipBackward15Button(
                            onClick = onSkipBackward15s,
                            tint = Color.White.copy(alpha = 0.7f),
                            size = 44.dp
                        )

                        // 定时
                        var showTimerDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showTimerDialog = true }) {
                            Icon(Icons.Default.Timer, "定时",
                                tint = if (timerMinutes > 0) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp))
                        }
                        if (showTimerDialog) {
                            var customMinutes by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showTimerDialog = false },
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("睡眠定时", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        val presets = listOf(15 to "15分钟", 30 to "30分钟", 45 to "45分钟", 60 to "60分钟", 90 to "90分钟")
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                            presets.take(3).forEach { (min, label) ->
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (timerMinutes == min) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerDialog = false }
                                                ) {
                                                    Text(label, fontSize = 13.sp,
                                                        color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                            presets.drop(3).forEach { (min, label) ->
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (timerMinutes == min) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerDialog = false }
                                                ) {
                                                    Text(label, fontSize = 13.sp,
                                                        color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                                }
                                            }
                                            if (timerMinutes > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(0); showTimerDialog = false }
                                                ) {
                                                    Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = customMinutes,
                                                onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                                                placeholder = { Text("自定义分钟数", fontSize = 14.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                            )
                                            Button(onClick = {
                                                val min = customMinutes.toIntOrNull()
                                                if (min != null && min > 0) { onSetTimer(min); showTimerDialog = false }
                                            }) { Text("设置", fontSize = 14.sp) }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = { TextButton(onClick = { showTimerDialog = false }) { Text("关闭") } }
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 上一章
                        IconButton(onClick = onPreviousChapter) {
                            Icon(Icons.Default.SkipPrevious, "上一章", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        // 播放/暂停
                        FilledIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "暂停" else "播放",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        // 下一章
                        IconButton(onClick = onNextChapter) {
                            Icon(Icons.Default.SkipNext, "下一章", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 倍速
                        var showSpeedSheet by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSpeedSheet = true }) {
                            Text(
                                "${playbackSpeed}x",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (playbackSpeed != 1f) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                        if (showSpeedSheet) {
                            ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("播放速度", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
                                    speeds.forEach { speed ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().clickable { onChangeSpeed(speed); showSpeedSheet = false }.padding(vertical = 2.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                        ) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${speed}x", fontSize = 15.sp)
                                                if (speed == 1f) Text("  正常", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (playbackSpeed == speed) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // 前进15秒
                        SkipForward15Button(
                            onClick = onSkipForward15s,
                            tint = Color.White.copy(alpha = 0.7f),
                            size = 44.dp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
