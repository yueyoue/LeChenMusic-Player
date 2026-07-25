package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.lechenmusic.data.model.LiveChannel
import com.lechenmusic.data.model.LiveChannelGroup
import com.lechenmusic.ui.VideoViewModel

@Composable
fun LiveScreen(
    viewModel: VideoViewModel,
    onBack: () -> Unit
) {
    val liveSources by viewModel.liveSources.collectAsState()
    val liveChannels by viewModel.liveChannels.collectAsState()
    val isLoading by viewModel.liveLoading.collectAsState()
    val liveDebug by viewModel.liveDebug.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 追踪当前屏幕（用于崩溃定位）
    androidx.compose.runtime.SideEffect {
        com.lechenmusic.ErrorReporter.setCurrentScreen("LiveScreen")
        com.lechenmusic.ErrorReporter.setExtraContext("groups=${liveChannels.size}, loading=$isLoading")
    }

    var selectedSourceIndex by remember { mutableIntStateOf(0) }
    var selectedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = playbackState == Player.STATE_READY
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) { viewModel.loadLiveSources() }

    // 加载频道
    LaunchedEffect(selectedSourceIndex, liveSources) {
        if (liveSources.isNotEmpty()) {
            val source = liveSources.getOrNull(selectedSourceIndex)
            if (source != null) {
                viewModel.loadLiveChannels(source.key)
            }
        }
    }

    // 缓存频道数据，避免 StateFlow 更新导致 mid-frame 崩溃
    // 只在 LaunchedEffect 中更新，确保数据变化发生在帧之间而非帧内
    var cachedGroups by remember { mutableStateOf(emptyList<LiveChannelGroup>()) }
    LaunchedEffect(liveChannels) {
        if (liveChannels.isNotEmpty()) {
            cachedGroups = liveChannels
        }
    }
    val groups = if (cachedGroups.isNotEmpty()) cachedGroups else liveChannels

    // 安全同步 selectedGroupIndex
    if (groups.isNotEmpty() && selectedGroupIndex >= groups.size) {
        selectedGroupIndex = 0
    }
    if (groups.isEmpty()) {
        selectedGroupIndex = 0
    }
    val safeGroupIndex = selectedGroupIndex.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    if (selectedGroupIndex != safeGroupIndex && groups.isNotEmpty()) {
        selectedGroupIndex = safeGroupIndex
    }
    val currentGroup = groups.getOrNull(safeGroupIndex)
    val channels = currentGroup?.channels ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Text("直播", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))

            // 源选择
            if (liveSources.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(liveSources.getOrNull(selectedSourceIndex)?.name ?: "选择源")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        liveSources.forEachIndexed { index, source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    selectedSourceIndex = index
                                    selectedGroupIndex = 0
                                    selectedChannel = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // 播放器区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black)
        ) {
            if (selectedChannel == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Tv, null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("选择频道开始播放", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                // ExoPlayer 渲染
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 悬浮控制层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                ) {
                    // 顶部：直播状态 + 停止
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF4D6A))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "正在直播: ${selectedChannel?.name}",
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                exoPlayer.stop()
                                selectedChannel = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, "停止", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    // 底部：播放/暂停 + 频道信息
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledIconButton(
                            onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedChannel?.name ?: "",
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1
                            )
                            Text(
                                selectedChannel?.group ?: "",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 分类标签（LazyRow，参考平板UI，避免 ScrollableTabRow 崩溃）
        if (groups.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups.size) { index ->
                    val group = groups[index]
                    val isSelected = selectedGroupIndex == index
                    Surface(
                        onClick = {
                            selectedGroupIndex = index
                            selectedChannel = null
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            group.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 频道列表
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无频道", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(channels.size) { index ->
                    val channel = channels[index]
                    LiveChannelItem(
                            channel = channel,
                            isSelected = selectedChannel?.url == channel.url,
                            onClick = {
                                selectedChannel = channel
                                try {
                                    if (channel.url.isNotBlank()) {
                                        exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
                                        exoPlayer.prepare()
                                        exoPlayer.playWhenReady = true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("LiveScreen", "ExoPlayer error: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }

        // Debug overlay
        if (liveDebug.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(liveDebug, fontSize = 11.sp, modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun LiveChannelItem(
    channel: LiveChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (channel.group.isNotBlank()) {
                    Text(channel.group, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, "播放中", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
