package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.lechenmusic.data.model.VideoInfo

// ==================== 影视主页面 ====================

@Composable
fun VideoScreen(
    onSearchClick: () -> Unit = {},
    onVideoClick: (VideoInfo) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("推荐", "电影", "电视剧", "综艺", "动漫")

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题 + 搜索
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("影视", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索影视")
            }
        }

        // Tab 栏
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 内容区
        when (selectedTab) {
            0 -> VideoRecommendTab(onVideoClick)
            1 -> VideoCategoryTab("movie", onVideoClick)
            2 -> VideoCategoryTab("tv", onVideoClick)
            3 -> VideoCategoryTab("show", onVideoClick)
            4 -> VideoCategoryTab("anime", onVideoClick)
        }
    }
}

// ==================== 推荐Tab ====================

@Composable
private fun VideoRecommendTab(
    onVideoClick: (VideoInfo) -> Unit
) {
    // 模拟数据，后续接入API
    val hotMovies = remember {
        listOf(
            VideoInfo(id = "1", title = "流浪地球3", year = "2026", cover = "", rate = "8.5", type = "movie", totalEpisodes = 1),
            VideoInfo(id = "2", title = "封神第三部", year = "2026", cover = "", rate = "7.8", type = "movie", totalEpisodes = 1),
            VideoInfo(id = "3", title = "唐探4", year = "2025", cover = "", rate = "7.2", type = "movie", totalEpisodes = 1),
            VideoInfo(id = "4", title = "哪吒之魔童闹海", year = "2025", cover = "", rate = "8.8", type = "movie", totalEpisodes = 1),
            VideoInfo(id = "5", title = "满江红2", year = "2026", cover = "", rate = "7.5", type = "movie", totalEpisodes = 1),
            VideoInfo(id = "6", title = "三体", year = "2024", cover = "", rate = "8.7", type = "tv", totalEpisodes = 30),
        )
    }

    val hotTvShows = remember {
        listOf(
            VideoInfo(id = "7", title = "庆余年3", year = "2026", cover = "", rate = "8.2", type = "tv", totalEpisodes = 36),
            VideoInfo(id = "8", title = "长相思3", year = "2026", cover = "", rate = "7.9", type = "tv", totalEpisodes = 40),
            VideoInfo(id = "9", title = "繁花", year = "2024", cover = "", rate = "8.6", type = "tv", totalEpisodes = 30),
            VideoInfo(id = "10", title = "狂飙2", year = "2026", cover = "", rate = "8.1", type = "tv", totalEpisodes = 39),
            VideoInfo(id = "11", title = "漫长的季节2", year = "2026", cover = "", rate = "9.0", type = "tv", totalEpisodes = 12),
            VideoInfo(id = "12", title = "隐秘的角落2", year = "2025", cover = "", rate = "8.4", type = "tv", totalEpisodes = 12),
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 热门电影标题
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Text(
                "🔥 热门电影",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        items(hotMovies) { video ->
            VideoCard(video = video, onClick = { onVideoClick(video) })
        }

        // 热门剧集标题
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Text(
                "📺 热门剧集",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }
        items(hotTvShows) { video ->
            VideoCard(video = video, onClick = { onVideoClick(video) })
        }
    }
}

// ==================== 分类Tab ====================

@Composable
private fun VideoCategoryTab(
    category: String,
    onVideoClick: (VideoInfo) -> Unit
) {
    // 模拟数据
    val videos = remember(category) {
        (1..18).map { i ->
            VideoInfo(
                id = "${category}_$i",
                title = "${categoryName(category)}$i",
                year = "${2020 + (i % 6)}",
                cover = "",
                rate = "${(6..9).random()}.${(0..9).random()}",
                type = category,
                totalEpisodes = if (category == "movie") 1 else (12..40).random()
            )
        }
    }

    // 筛选状态
    var selectedType by remember { mutableStateOf("全部") }
    var selectedRegion by remember { mutableStateOf("全部") }
    var selectedYear by remember { mutableStateOf("全部") }

    Column {
        // 筛选栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == "全部",
                onClick = { selectedType = "全部" },
                label = { Text("类型", fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedRegion == "全部",
                onClick = { selectedRegion = "全部" },
                label = { Text("地区", fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedYear == "全部",
                onClick = { selectedYear = "全部" },
                label = { Text("年份", fontSize = 12.sp) }
            )
        }

        // 视频网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(videos) { video ->
                VideoCard(video = video, onClick = { onVideoClick(video) })
            }
        }
    }
}

// ==================== 视频卡片组件 ====================

@Composable
fun VideoCard(
    video: VideoInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            if (video.cover.isNotBlank()) {
                AsyncImage(
                    model = video.cover,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 占位图
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        video.title.take(1),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // 评分标签
            if (video.rate != null) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 10.dp),
                    color = Color(0xFFFF6B81).copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        video.rate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 进度条
            if (video.playTime > 0 && video.totalTime > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = video.progressPercent)
                            .height(3.dp)
                            .background(Color(0xFFE94560))
                    )
                }
            }

            // 集数标签（电视剧/动漫）
            if (video.totalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "更新至${video.totalEpisodes}集",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 标题
        Text(
            video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )

        // 年份 + 类型
        Text(
            buildString {
                append(video.year)
                if (video.type.isNotBlank()) append(" · ${categoryName(video.type)}")
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun categoryName(type: String): String = when (type) {
    "movie" -> "电影"
    "tv" -> "电视剧"
    "show" -> "综艺"
    "anime" -> "动漫"
    "live" -> "直播"
    else -> type
}
