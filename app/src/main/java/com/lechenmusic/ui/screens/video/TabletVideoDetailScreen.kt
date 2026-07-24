package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletVideoDetailScreen(
    viewModel: VideoViewModel,
    source: String,
    videoId: String,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onPlay: (String, Int) -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    val detail by viewModel.videoDetail.collectAsState()
    val loading by viewModel.detailLoading.collectAsState()
    val allSearchSources by viewModel.allSearchSources.collectAsState()
    val sourceSpeeds by viewModel.sourceSpeeds.collectAsState()
    val speedTesting by viewModel.speedTesting.collectAsState()

    LaunchedEffect(source, videoId) {
        viewModel.loadDetail(source, videoId)
        viewModel.loadFavorites()
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val video = detail ?: return

    val videoSources = video.toSources()
    val displaySources = if (allSearchSources.size > 1) {
        allSearchSources
            .groupBy { it.source }
            .values.map { group -> group.first() }
            .take(20)
            .map { info ->
                com.lechenmusic.data.model.VideoSource(
                    sourceName = info.displaySourceName.ifBlank { info.source },
                    source = info.source,
                    episodes = info.episodes.mapIndexed { idx, url ->
                        com.lechenmusic.data.model.VideoEpisode(index = idx, title = info.episodesTitles.getOrNull(idx) ?: "第${idx + 1}集", url = url)
                    }
                )
            }
    } else {
        videoSources
    }

    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(0) }
    // 右侧 Tab: 0=选集, 1=播放源
    var rightTab by remember { mutableIntStateOf(0) }

    val currentSource = displaySources.getOrNull(selectedSource)
    val episodes = currentSource?.episodes ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("影视详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // ===== 主内容: 左侧(2/3) + 右侧(1/3) =====
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
        ) {
            // ===== 左侧: 视频区域 + 影片信息 (flex-2) =====
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                // 视频播放区域 (16:9, rounded-2xl)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .clickable {
                            if (episodes.isNotEmpty()) onPlay(video.source, selectedEpisode)
                        }
                ) {
                    if (video.displayCover.isNotBlank()) {
                        AsyncImage(
                            model = video.displayCover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, "播放", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(Color.White.copy(alpha = 0.2f)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 影片信息
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(buildString { append(video.title); if (video.year.isNotBlank()) append(" (${video.year})") }, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (video.rate != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(video.rate!!, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                }
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (video.displayType.isNotBlank()) {
                                Text(categoryName(video.displayType), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (video.year.isNotBlank()) {
                                Text(video.year, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分享")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (video.desc.isNotBlank()) {
                    var descExpanded by remember { mutableStateOf(false) }
                    Text(video.desc, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp, maxLines = if (descExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                    if (video.desc.length > 100) {
                        Text(if (descExpanded) "收起" else "展开全部", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { descExpanded = !descExpanded }.padding(top = 4.dp))
                    }
                }

                if (video.director.isNotBlank() || video.actor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) {
                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(buildString {
                            if (video.director.isNotBlank()) append("导演：${video.director}")
                            if (video.director.isNotBlank() && video.actor.isNotBlank()) append(" / ")
                            if (video.actor.isNotBlank()) append("主演：${video.actor}")
                        }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ===== 右侧: Tab 切换选集/播放源 (flex-1) =====
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                // Tab 栏
                TabRow(
                    selectedTabIndex = rightTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = rightTab == 0, onClick = { rightTab = 0 }, text = { Text("选集 (${episodes.size})") })
                    Tab(selected = rightTab == 1, onClick = { rightTab = 1 }, text = { Text("播放源 (${displaySources.size})") })
                }

                // Tab 内容
                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (rightTab) {
                        0 -> EpisodeGrid(episodes, selectedEpisode) { index ->
                            selectedEpisode = index
                            onPlay(video.source, index)
                        }
                        1 -> SourceList(
                            displaySources = displaySources,
                            allSearchSources = allSearchSources,
                            selectedSource = selectedSource,
                            sourceSpeeds = sourceSpeeds,
                            speedTesting = speedTesting,
                            onSourceSelect = { index, info ->
                                selectedSource = index
                                selectedEpisode = 0
                                viewModel.switchSource(info)
                            },
                            onTestSpeed = { viewModel.testSourceSpeeds() }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 选集网格 ====================
@Composable
private fun EpisodeGrid(
    episodes: List<com.lechenmusic.data.model.VideoEpisode>,
    selectedEpisode: Int,
    onEpisodeClick: (Int) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无剧集", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(episodes.size) { index ->
            val isActive = index == selectedEpisode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                modifier = Modifier.aspectRatio(1f).clickable { onEpisodeClick(index) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(episodes.getOrNull(index)?.title ?: "${index + 1}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        if (isActive) {
                            Text("播放中", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ==================== 播放源列表（带测速） ====================
@Composable
private fun SourceList(
    displaySources: List<com.lechenmusic.data.model.VideoSource>,
    allSearchSources: List<VideoInfo>,
    selectedSource: Int,
    sourceSpeeds: Map<String, Long>,
    speedTesting: Boolean,
    onSourceSelect: (Int, VideoInfo) -> Unit,
    onTestSpeed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 测速按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${displaySources.size} 个源", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onTestSpeed, enabled = !speedTesting) {
                if (speedTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(if (speedTesting) "测速中..." else "\u26A1 测速排序", fontSize = 13.sp)
            }
        }

        // 源列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(displaySources.size) { index ->
                val src = displaySources[index]
                val speed = sourceSpeeds[src.source]
                val isCurrent = index == selectedSource
                val matchingInfo = allSearchSources.firstOrNull { it.source == src.source }

                Surface(
                    onClick = {
                        if (matchingInfo != null && matchingInfo.episodes.isNotEmpty()) {
                            onSourceSelect(index, matchingInfo)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 源名称
                        Text(
                            src.sourceName.ifBlank { src.source },
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 集数
                        Text("${src.episodes.size}集", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        // 速度
                        Text(
                            when {
                                speed == null -> ""
                                speed < 0 -> "超时"
                                else -> "${speed}ms"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                speed == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                speed < 0 -> Color.Red
                                speed < 500 -> Color(0xFF4CAF50)
                                speed < 1500 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF5722)
                            }
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("当前", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
