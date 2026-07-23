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
import androidx.compose.ui.graphics.Brush
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
import com.lechenmusic.ui.screens.player.PlayerProgressBar
import com.lechenmusic.ui.screens.player.rememberCoverColor

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

    // 进度条拖拽状态
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(currentPositionMs, durationMs) {
        if (!isDragging && durationMs > 0) {
            sliderPosition = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    val displayPosition = if (isDragging) (sliderPosition * durationMs).toLong() else currentPositionMs

    Box(
        modifier = Modifier.fillMaxSize().background(coverBgColor)
    ) {
        // 半透明遮罩
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部: 返回按钮
            IconButton(
                onClick = { onSaveProgress(); onBack() },
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "返回", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // ===== 主内容区 =====
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ===== 左侧: 封面图 + 播放控制 =====
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 封面图
                    Box(
                        modifier = Modifier.size(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(16.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 书名 + 章节信息
                    Text(
                        book.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currentChapter?.title ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "第 ${currentChapterIndex + 1} / ${chapters.size} 章",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 进度条
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PlayerProgressBar(
                            dragProgress = sliderPosition,
                            duration = durationMs,
                            barWidthPx = barWidthPx,
                            activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = 0.3f),
                            onProgressChanged = { newProgress ->
                                isDragging = true
                                sliderPosition = newProgress
                            },
                            onSeek = { positionMs -> onSeekTo(positionMs) },
                            onDragEnd = { isDragging = false },
                            onWidthMeasured = { w -> barWidthPx = w.coerceAtLeast(1f) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(displayPosition), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(formatTime(durationMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 播放控制按钮
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

                        Spacer(modifier = Modifier.width(12.dp))

                        // 上一章
                        IconButton(onClick = onPreviousChapter, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipPrevious, "上一章", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 播放/暂停
                        FilledIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "暂停" else "播放",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 下一章
                        IconButton(onClick = onNextChapter, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipNext, "下一章", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 前进15秒
                        SkipForward15Button(
                            onClick = onSkipForward15s,
                            tint = Color.White.copy(alpha = 0.7f),
                            size = 44.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 底部工具栏: 定时 | 倍速
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showTimerSheet by remember { mutableStateOf(false) }
                        var showSpeedSheet by remember { mutableStateOf(false) }

                        // 定时
                        IconButton(onClick = { showTimerSheet = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Timer,
                                    "定时",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (timerMinutes > 0) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.7f)
                                )
                                if (timerMinutes > 0) {
                                    Text("${timerMinutes}分", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // 倍速
                        IconButton(onClick = { showSpeedSheet = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${playbackSpeed}x",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playbackSpeed != 1f) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.7f)
                                )
                                Text("倍速", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }

                        // Timer dialog
                        if (showTimerSheet) {
                            var customMinutes by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showTimerSheet = false },
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
                                                    color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerSheet = false }
                                                ) {
                                                    Text(
                                                        label, fontSize = 13.sp,
                                                        color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                            presets.drop(3).forEach { (min, label) ->
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerSheet = false }
                                                ) {
                                                    Text(
                                                        label, fontSize = 13.sp,
                                                        color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                            if (timerMinutes > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                    modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(0); showTimerSheet = false }
                                                ) {
                                                    Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = customMinutes,
                                                onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                                                placeholder = { Text("自定义分钟数", fontSize = 14.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                            )
                                            Button(
                                                onClick = {
                                                    val min = customMinutes.toIntOrNull()
                                                    if (min != null && min > 0) {
                                                        onSetTimer(min)
                                                        showTimerSheet = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("设置", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = { TextButton(onClick = { showTimerSheet = false }) { Text("关闭") } }
                            )
                        }

                        // Speed dialog
                        if (showSpeedSheet) {
                            ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("播放速度", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
                                    speeds.forEach { speed ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onChangeSpeed(speed); showSpeedSheet = false }
                                                .padding(vertical = 2.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${speed}x", fontSize = 15.sp)
                                                if (speed == 1f) {
                                                    Text("  正常", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
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
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // ===== 右侧: 章节列表 =====
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // 标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "章节列表",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "共 ${chapters.size} 章",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 章节列表
                    val chapterListState = rememberLazyListState()
                    LaunchedEffect(Unit) {
                        if (currentChapterIndex in chapters.indices) {
                            chapterListState.animateScrollToItem(currentChapterIndex)
                        }
                    }
                    // Also scroll when chapter changes
                    LaunchedEffect(currentChapterIndex) {
                        if (currentChapterIndex in chapters.indices) {
                            chapterListState.animateScrollToItem(currentChapterIndex)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = chapterListState,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            itemsIndexed(chapters) { index, chapter ->
                                val isActive = index == currentChapterIndex
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clickable { onChapterSelect(index) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                                            fontSize = 14.sp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
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
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
