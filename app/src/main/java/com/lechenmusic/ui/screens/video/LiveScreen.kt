package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.lechenmusic.data.model.LiveSource
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
                if (playbackState == Player.STATE_READY) {
                    isPlaying = true
                }
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLiveSources()
    }

    // 加载频道
    LaunchedEffect(selectedSourceIndex, liveSources) {
        if (liveSources.isNotEmpty()) {
            val source = liveSources.getOrNull(selectedSourceIndex)
            if (source != null) {
                viewModel.loadLiveChannels(source.key)
            }
        }
    }

    val groups = liveChannels
    // 安全重置 selectedGroupIndex
    LaunchedEffect(groups.size) {
        if (selectedGroupIndex >= groups.size) {
            selectedGroupIndex = 0
        }
    }
    val currentGroup = groups.getOrNull(selectedGroupIndex)

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

        if (isLoading) {
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
                            Icons.Default.Tv,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "选择频道开始播放",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // 播放器占位（实际需要嵌入 PlayerView）
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            selectedChannel?.name ?: "",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 播放/暂停按钮
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // 频道信息
        selectedChannel?.let { ch ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (ch.group.isNotBlank()) {
                            Text(ch.group, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // EPG 节目单
                    if (ch.epg.isNotBlank()) {
                        Text(
                            "节目单",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                // TODO: 打开 EPG 节目单
                            }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }

        // 分组 Tab
        if (groups.size > 1) {
            val safeGroupIndex = selectedGroupIndex.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
            ScrollableTabRow(
                selectedTabIndex = safeGroupIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp,
                divider = {}
            ) {
                groups.forEachIndexed { index, group ->
                    Tab(
                        selected = selectedGroupIndex == index,
                        onClick = { selectedGroupIndex = index },
                        text = {
                            Text(
                                group.name,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selectedGroupIndex == index)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    )
                }
            }
        }

        // 频道列表
        val channels = currentGroup?.channels ?: emptyList()
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无频道", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(channels, key = { it.url }) { channel ->
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        if (channel.logo.isNotBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Tv,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
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
            Icon(
                Icons.Default.PlayCircleFilled,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
