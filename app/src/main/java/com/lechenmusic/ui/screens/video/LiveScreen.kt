package com.lechenmusic.ui.screens.video

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
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
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun LiveScreen(
    viewModel: VideoViewModel,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit
) {
    val liveSources by viewModel.liveSources.collectAsState()
    val liveChannels by viewModel.liveChannels.collectAsState()
    val isLoading by viewModel.liveLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedSourceIndex by remember { mutableIntStateOf(0) }
    var selectedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    SideEffect {
        com.lechenmusic.ErrorReporter.setCurrentScreen("LiveScreen")
    }

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

    LaunchedEffect(selectedSourceIndex, liveSources) {
        if (liveSources.isNotEmpty()) {
            val source = liveSources.getOrNull(selectedSourceIndex)
            if (source != null) {
                viewModel.loadLiveChannels(source.key)
            }
        }
    }

    var cachedGroups by remember { mutableStateOf(emptyList<LiveChannelGroup>()) }
    LaunchedEffect(liveChannels) {
        if (liveChannels.isNotEmpty()) {
            cachedGroups = liveChannels
        }
    }
    val groups = if (cachedGroups.isNotEmpty()) cachedGroups else liveChannels

    if (groups.isNotEmpty() && selectedGroupIndex >= groups.size) {
        selectedGroupIndex = 0
    }
    val safeGroupIndex = selectedGroupIndex.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    if (selectedGroupIndex != safeGroupIndex && groups.isNotEmpty()) {
        selectedGroupIndex = safeGroupIndex
    }
    val currentGroup = groups.getOrNull(safeGroupIndex)

    var cachedChannels by remember { mutableStateOf(emptyList<LiveChannel>()) }
    LaunchedEffect(currentGroup) {
        cachedChannels = currentGroup?.channels ?: emptyList()
    }
    val channels = cachedChannels

    // ═══ 全屏模式 ═══
    if (isFullscreen && selectedChannel != null) {
        DisposableEffect(Unit) {
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.Default.FullscreenExit, "退出全屏", tint = Color.White)
            }
            Text(
                selectedChannel?.name ?: "",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 56.dp, top = 20.dp)
            )
            var fsIsPlaying by remember { mutableStateOf(false) }
            LaunchedEffect(exoPlayer) {
                while (true) {
                    fsIsPlaying = exoPlayer.isPlaying
                    kotlinx.coroutines.delay(200)
                }
            }
            IconButton(
                onClick = { if (fsIsPlaying) exoPlayer.pause() else exoPlayer.play() },
                modifier = Modifier.align(Alignment.Center).size(64.dp)
            ) {
                Icon(
                    if (fsIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(48.dp)
                )
            }
        }
        return
    }

    // ═══ 主布局：上下结构 ═══
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Text("电视直播", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── 顶部：内联播放器（16:9）──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    if (selectedChannel == null) {
                        // 未选择频道
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Tv, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("选择频道开始播放", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                        }
                    } else {
                        // 播放器
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // 渐变遮罩
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                        )
                        // 频道名 + 控制
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF4D6A)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(selectedChannel?.name ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { isFullscreen = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        // 底部播放按钮
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedChannel?.name ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(selectedChannel?.group ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            IconButton(onClick = { exoPlayer.stop(); selectedChannel = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "停止", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // ── 直播源选择 ──
            if (liveSources.isNotEmpty()) {
                item {
                    var sourceExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Surface(
                            onClick = { sourceExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Router, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(liveSources.getOrNull(selectedSourceIndex)?.name ?: "选择直播源", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                            liveSources.forEachIndexed { index, source ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(source.name)
                                            if (index == selectedSourceIndex) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedSourceIndex = index
                                        selectedGroupIndex = 0
                                        selectedChannel = null
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 分类标签 ──
            if (groups.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups.size) { index ->
                            val group = groups[index]
                            val isSelected = selectedGroupIndex == index
                            Surface(
                                onClick = { selectedGroupIndex = index },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    group.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 频道列表 ──
            if (isLoading && channels.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (channels.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("暂无频道", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(channels, key = { it.url }) { channel ->
                    val isSelected = selectedChannel?.url == channel.url
                    LiveChannelItem(
                        channel = channel,
                        isSelected = isSelected,
                        onClick = {
                            selectedChannel = channel
                            exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        }
                    )
                }
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo, contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (channel.group.isNotBlank()) {
                    Text(channel.group, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isSelected) {
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "播放中", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}
