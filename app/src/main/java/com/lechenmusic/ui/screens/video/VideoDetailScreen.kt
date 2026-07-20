package com.lechenmusic.ui.screens.video

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.*
import com.lechenmusic.ui.VideoViewModel

@Composable
fun VideoDetailScreen(
    viewModel: VideoViewModel,
    source: String,
    videoId: String,
    onBack: () -> Unit,
    onPlay: (source: String, episodeIndex: Int) -> Unit,
    onVideoClick: (VideoInfo) -> Unit = {}
) {
    val detail by viewModel.videoDetail.collectAsState()
    val isLoading by viewModel.detailLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val context = LocalContext.current

    var selectedSource by remember { mutableIntStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(source, videoId) {
        if (source != "searching") {
            viewModel.loadDetail(source, videoId)
        }
        viewModel.loadFavorites()
    }

    val currentDetail = detail
    val isStarred = favorites.any { it.id == videoId }

    if (isLoading && currentDetail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentDetail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        // 顶部封面 + 渐变背景
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // 背景图 + 渐变遮罩
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentDetail.displayCover.isNotBlank()) {
                        AsyncImage(
                            model = currentDetail.displayCover,
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
                            .width(120.dp)
                            .height(170.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (currentDetail.displayCover.isNotBlank()) {
                            AsyncImage(
                                model = currentDetail.displayCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    currentDetail.title.take(1),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentDetail.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentDetail.rate != null) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentDetail.rate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(currentDetail.year, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                categoryName(currentDetail.displayType),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currentDetail.area.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                currentDetail.area,
                                fontSize = 12.sp,
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
                        val src = currentDetail.toSources().getOrNull(selectedSource)
                        if (src != null && src.episodes.isNotEmpty()) {
                            onPlay(src.source, 0)
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
                    onClick = {
                        val videoInfo = VideoInfo(
                            id = currentDetail.id,
                            source = currentDetail.source,
                            title = currentDetail.title,
                            cover = currentDetail.displayCover,
                            year = currentDetail.year,
                            type = currentDetail.displayType
                        )
                        if (isStarred) viewModel.removeFavorite(videoInfo)
                        else viewModel.addFavorite(videoInfo)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 分享按钮
                OutlinedButton(
                    onClick = {
                        val shareText = "${currentDetail.title} (${currentDetail.year})\n${currentDetail.desc.take(100)}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享影视"))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                }
            }
        }

        // 简介（可展开/收起）
        if (currentDetail.desc.isNotBlank()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("简介", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentDetail.desc,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentDetail.desc.length > 80) {
                        Text(
                            if (descExpanded) "收起" else "展开全部",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { descExpanded = !descExpanded }
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // 额外信息（导演、演员）
        if (currentDetail.director.isNotBlank() || currentDetail.actor.isNotBlank()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    if (currentDetail.director.isNotBlank()) {
                        Text("导演：${currentDetail.director}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (currentDetail.actor.isNotBlank()) {
                        Text(
                            "演员：${currentDetail.actor}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 播放源选择
        if (currentDetail.toSources().size > 1) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("播放源", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(currentDetail.toSources()) { index, src ->
                            FilterChip(
                                selected = selectedSource == index,
                                onClick = { selectedSource = index },
                                label = { Text(src.sourceName, fontSize = 13.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 选集
        val currentSrc = currentDetail.toSources().getOrNull(selectedSource)
        if (currentSrc != null) {
            item {
                Text(
                    "选集 (${currentSrc.episodes.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (currentSrc.episodes.size == 1) {
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
                            modifier = Modifier
                                .clickable { onPlay(currentSrc.source, 0) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                currentSrc.episodes.first().title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                // 剧集网格
                items(currentSrc.episodes.chunked(6)) { row ->
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
                                    .clickable { onPlay(currentSrc.source, ep.index) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "${ep.index + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                        repeat(6 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 相关推荐
        if (currentDetail.related.isNotEmpty()) {
            item {
                Text(
                    "相关推荐",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentDetail.related) { video ->
                        VideoHorizontalCard(video = video, onClick = {
                            onVideoClick(video)
                        })
                    }
                }
            }
        }
    }
}
