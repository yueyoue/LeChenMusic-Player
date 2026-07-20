package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import coil.compose.AsyncImage
import com.lechenmusic.data.model.VideoDetail
import com.lechenmusic.data.model.VideoEpisode
import com.lechenmusic.data.model.VideoSource

@Composable
fun VideoDetailScreen(
    videoId: String,
    onBack: () -> Unit,
    onPlay: (source: String, episodeIndex: Int) -> Unit
) {
    // 模拟数据，后续接入API
    val detail = remember {
        VideoDetail(
            id = videoId,
            title = "流浪地球3",
            year = "2026",
            cover = "",
            desc = "太阳即将毁灭，人类在地球表面建造出巨大的推进器，将地球推离太阳系，踏上长达2500年的星际流浪之旅。本片为流浪地球系列第三部。",
            type = "movie",
            rate = "8.5",
            sources = listOf(
                VideoSource(
                    sourceName = "量子资源",
                    source = "quantum",
                    episodes = listOf(
                        VideoEpisode(0, "正片", "https://example.com/play1.m3u8")
                    )
                ),
                VideoSource(
                    sourceName = "非凡资源",
                    source = "feifan",
                    episodes = listOf(
                        VideoEpisode(0, "正片", "https://example.com/play2.m3u8")
                    )
                )
            )
        )
    }

    var selectedSource by remember { mutableIntStateOf(0) }
    var isStarred by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        // 顶部封面 + 渐变背景
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                // 背景图 + 渐变遮罩
                Box(modifier = Modifier.fillMaxSize()) {
                    if (detail.cover.isNotBlank()) {
                        AsyncImage(
                            model = detail.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }

                // 电影信息
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // 封面
                    Surface(
                        modifier = Modifier
                            .width(110.dp)
                            .height(155.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (detail.cover.isNotBlank()) {
                            AsyncImage(
                                model = detail.cover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    detail.title.take(1),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 信息
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            detail.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (detail.rate != null) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(detail.rate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(detail.year, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (detail.type) {
                                    "movie" -> "电影"
                                    "tv" -> "电视剧"
                                    "show" -> "综艺"
                                    "anime" -> "动漫"
                                    else -> detail.type
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 操作按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 播放按钮
                Button(
                    onClick = {
                        val source = detail.sources.getOrNull(selectedSource)
                        if (source != null && source.episodes.isNotEmpty()) {
                            onPlay(source.source, 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即播放", fontSize = 15.sp)
                }

                // 收藏按钮
                OutlinedButton(
                    onClick = { isStarred = !isStarred },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isStarred) "已收藏" else "收藏")
                }
            }
        }

        // 简介
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("简介", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    detail.desc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        // 播放源选择
        if (detail.sources.size > 1) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("播放源", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(detail.sources) { index, source ->
                            FilterChip(
                                selected = selectedSource == index,
                                onClick = { selectedSource = index },
                                label = { Text(source.sourceName, fontSize = 13.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 集数列表
        val currentSource = detail.sources.getOrNull(selectedSource)
        if (currentSource != null) {
            item {
                Text(
                    "选集 (${currentSource.episodes.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (currentSource.episodes.size == 1) {
                // 电影：直接显示单集
                item {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                currentSource.episodes.first().title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                // 剧集网格
                items(currentSource.episodes.chunked(6)) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { ep ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPlay(currentSource.source, ep.index) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "${ep.index + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                        // 填充空位
                        repeat(6 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
