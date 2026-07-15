package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookChapter
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.screens.player.PlayerProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookPlayerScreen(
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
    var showChapterSheet by remember { mutableStateOf(false) }

    // Save progress when leaving
    DisposableEffect(Unit) {
        onDispose { onSaveProgress() }
    }

    // Auto-save progress every 5 seconds during playback (like ting-reader)
    LaunchedEffect(isPlaying, currentChapterIndex) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            if (isPlaying) onSaveProgress()
        }
    }

    // Save immediately when paused (ting-reader: flush on pause)
    var prevIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (prevIsPlaying && !isPlaying) {
            // Just paused - flush progress immediately
            onSaveProgress()
        }
        prevIsPlaying = isPlaying
    }

    // Save when chapter changes
    LaunchedEffect(currentChapterIndex) {
        onSaveProgress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSaveProgress(); onBack() }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "返回")
                }
                Text("正在播放", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(280.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }
            }

            // Book & Chapter Info
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    book.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    currentChapter?.title ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "第 ${currentChapterIndex + 1} / ${chapters.size} 章",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Progress bar
            var isDragging by remember { mutableStateOf(false) }
            var sliderPosition by remember { mutableStateOf(0f) }
            var barWidthPx by remember { mutableFloatStateOf(1f) }

            LaunchedEffect(currentPositionMs, durationMs) {
                if (!isDragging && durationMs > 0) {
                    sliderPosition = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                }
            }

            val displayPosition = if (isDragging) (sliderPosition * durationMs).toLong() else currentPositionMs

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                PlayerProgressBar(
                    dragProgress = sliderPosition,
                    duration = durationMs,
                    barWidthPx = barWidthPx,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    Text(formatTime(displayPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(durationMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip backward 15s
                IconButton(onClick = onSkipBackward15s, modifier = Modifier.size(48.dp)) {
                    Image(
                        painter = painterResource(id = com.lechenmusic.R.drawable.ic_skip_backward_15),
                        contentDescription = "后退15秒",
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Previous chapter
                IconButton(onClick = onPreviousChapter, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一章", modifier = Modifier.size(30.dp))
                }

                // Play/Pause
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Next chapter
                IconButton(onClick = onNextChapter, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一章", modifier = Modifier.size(30.dp))
                }

                // Skip forward 15s
                IconButton(onClick = onSkipForward15s, modifier = Modifier.size(48.dp)) {
                    Image(
                        painter = painterResource(id = com.lechenmusic.R.drawable.ic_skip_forward_15),
                        contentDescription = "前进15秒",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Bottom bar: Timer | Speed | Queue
            var showTimerSheet by remember { mutableStateOf(false) }
            var showSpeedSheet by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer
                IconButton(onClick = { showTimerSheet = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "定时",
                            modifier = Modifier.size(22.dp),
                            tint = if (timerMinutes > 0) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (timerMinutes > 0) {
                            Text("${timerMinutes}分", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Speed
                IconButton(onClick = { showSpeedSheet = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${playbackSpeed}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playbackSpeed != 1f) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("倍速", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Queue (chapter list)
                IconButton(onClick = { showChapterSheet = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = "播放列表",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("列表", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timer bottom sheet
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                presets.take(3).forEach { (min, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerSheet = false }
                                    ) {
                                        Text(label, fontSize = 13.sp,
                                            color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                presets.drop(3).forEach { (min, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); showTimerSheet = false }
                                    ) {
                                        Text(label, fontSize = 13.sp,
                                            color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                    }
                                }
                                // Cancel timer
                                if (timerMinutes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(0); showTimerSheet = false }
                                    ) {
                                        Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
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

            // Speed bottom sheet
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

    // Chapter list sheet
    if (showChapterSheet) {
        ModalBottomSheet(onDismissRequest = { showChapterSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("章节列表", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "共 ${chapters.size} 章",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val listState = rememberLazyListState()
                // Auto scroll to current chapter
                LaunchedEffect(Unit) {
                    if (currentChapterIndex in chapters.indices) {
                        listState.animateScrollToItem(currentChapterIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChapterSelect(index)
                                    showChapterSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 13.sp,
                                color = if (index == currentChapterIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(30.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                chapter.title,
                                fontSize = 14.sp,
                                color = if (index == currentChapterIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (index == currentChapterIndex) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
