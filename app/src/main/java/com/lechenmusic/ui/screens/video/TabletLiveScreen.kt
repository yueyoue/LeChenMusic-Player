package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun TabletLiveScreen(
    viewModel: VideoViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit
) {
    val liveSources by viewModel.liveSources.collectAsState()
    val liveChannels by viewModel.liveChannels.collectAsState()
    val liveLoading by viewModel.liveLoading.collectAsState()
    val liveDebug by viewModel.liveDebug.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedSourceIndex by remember { mutableIntStateOf(0) }
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    var selectedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // ExoPlayer
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

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

    // 加载频道（根据选中的源）
    LaunchedEffect(selectedSourceIndex, liveSources) {
        if (liveSources.isNotEmpty()) {
            val source = liveSources.getOrNull(selectedSourceIndex)
            if (source != null) {
                viewModel.loadLiveChannels(source.source)
            }
        }
    }

    val groups = liveChannels

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
        // ===== 左侧：频道列表 (35%) =====
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = responsiveConfig.contentPadding)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("电视直播", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 直播源选择
            if (liveSources.isNotEmpty()) {
                var sourceExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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
                                Text(
                                    liveSources.getOrNull(selectedSourceIndex)?.name ?: "选择直播源",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
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

            // 分类标签
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEachIndexed { index, group ->
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

            // 频道列表
            val channels = groups.getOrNull(selectedGroupIndex)?.channels ?: emptyList()

            if (liveLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (channels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无频道", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(channels) { channel ->
                        val isSelected = selectedChannel?.url == channel.url
                        ChannelListItem(
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

        // ===== 右侧：播放器区域 (65%) =====
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(responsiveConfig.contentPadding),
            verticalArrangement = Arrangement.Center
        ) {
            // 播放器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedChannel == null) {
                    // 未选择频道
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Tv, null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("选择频道开始播放", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                } else {
                    // 播放中 - ExoPlayer View
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
                        // 顶部：直播状态 + 全屏
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF4D6A))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "正在直播: ${selectedChannel?.name}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        exoPlayer.stop()
                                        selectedChannel = null
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, "停止", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // 底部：播放控制
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 播放/暂停
                            FilledIconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 频道信息
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    selectedChannel?.name ?: "",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    selectedChannel?.group ?: "",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }

                            // 音量
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 当前频道详情
            selectedChannel?.let { ch ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ch.logo.isNotBlank()) {
                            AsyncImage(
                                model = ch.logo,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
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
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (ch.group.isNotBlank()) {
                                Text(ch.group, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (ch.epg.isNotBlank()) {
                            TextButton(onClick = { }) {
                                Text("节目单", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // 调试信息覆盖层
    if (liveDebug.isNotEmpty()) {
        Surface(
            modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(0.6f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("\uD83D\uDD0D 调试信息", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Text(liveDebug, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun ChannelListItem(
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
                    contentScale = ContentScale.Crop
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.group.isNotBlank()) {
                    Text(
                        channel.group,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
