package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
// PullToRefreshBox not available in this BOM version
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
import com.lechenmusic.data.model.*
import com.lechenmusic.ui.VideoViewModel

// ==================== 影视主页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onSearchClick: () -> Unit = {},
    onVideoClick: (VideoInfo) -> Unit = {},
    onLiveClick: () -> Unit = {},
    onRecordClick: (VideoPlayRecord) -> Unit = {}
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val homeData by viewModel.homeData.collectAsState()
    val homeLoading by viewModel.homeLoading.collectAsState()
    val playRecords by viewModel.playRecords.collectAsState()

    // Tab 状态
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("推荐", "电影", "电视剧", "动漫", "综艺", "直播")

    // 分类数据
    val categoryResults by viewModel.categoryResults.collectAsState()
    val categoryLoading by viewModel.categoryLoading.collectAsState()

    // 当前分类关键词
    var currentCategoryKeyword by remember { mutableStateOf("") }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.loadPlayRecords()
        }
    }

    LaunchedEffect(selectedTab, isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        when (selectedTab) {
            1 -> if (currentCategoryKeyword != "电影") { currentCategoryKeyword = "电影"; viewModel.searchCategory("电影") }
            2 -> if (currentCategoryKeyword != "电视剧") { currentCategoryKeyword = "电视剧"; viewModel.searchCategory("电视剧") }
            3 -> if (currentCategoryKeyword != "动漫") { currentCategoryKeyword = "动漫"; viewModel.searchCategory("动漫") }
            4 -> if (currentCategoryKeyword != "综艺") { currentCategoryKeyword = "综艺"; viewModel.searchCategory("综艺") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏（无标题）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 空出左侧空间
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索影视")
            }
        }

        // 紧凑胶囊 Tab 栏（横向滚动）
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        if (title == "直播" && isLoggedIn) {
                            onLiveClick()
                        }
                    },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedTab == index)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 内容区
        if (!isLoggedIn) {
            // 未登录提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Tv,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "请先在设置中配置影视服务器",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            when (selectedTab) {
                0 -> VideoRecommendTab(
                    homeData = homeData,
                    playRecords = playRecords,
                    isLoading = homeLoading,
                    onRefresh = { viewModel.refreshHome() },
                    onVideoClick = onVideoClick,
                    onRecordClick = onRecordClick
                )
                1 -> VideoCategoryList(categoryResults, categoryLoading, onVideoClick)
                2 -> VideoCategoryList(categoryResults, categoryLoading, onVideoClick)
                3 -> VideoCategoryList(categoryResults, categoryLoading, onVideoClick)
                4 -> VideoCategoryList(categoryResults, categoryLoading, onVideoClick)
            }
        }
    }
}

// ==================== 推荐Tab ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoRecommendTab(
    homeData: HomeRecommendData?,
    playRecords: List<VideoPlayRecord>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onVideoClick: (VideoInfo) -> Unit,
    onRecordClick: (VideoPlayRecord) -> Unit
) {
    // TODO: Add pull-to-refresh when BOM is updated
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 继续观看
            if (playRecords.isNotEmpty()) {
                item {
                    SectionHeader("继续观看")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playRecords.take(10)) { record ->
                            ContinueWatchCard(record = record, onClick = {
                                onRecordClick(record)
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 即将上映
            val comingSoon = homeData?.comingSoon
            if (!comingSoon.isNullOrEmpty()) {
                item {
                    SectionHeader("即将上映")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comingSoon) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 热门推荐（3列网格）
            val hotAll = buildList {
                homeData?.hotMovies?.let { addAll(it) }
                homeData?.hotTvShows?.let { addAll(it) }
            }.distinctBy { it.id }.take(12)

            if (hotAll.isNotEmpty()) {
                item {
                    SectionHeader("🔥 热门推荐")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    // 使用固定高度的网格
                    val chunked = hotAll.chunked(3)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunked.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { video ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        VideoCard(video = video, onClick = { onVideoClick(video) })
                                    }
                                }
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 热门电影（横向滚动）
            val hotMovies = homeData?.hotMovies
            if (!hotMovies.isNullOrEmpty()) {
                item {
                    SectionHeader("🎬 热门电影")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hotMovies) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 热门剧集
            val hotTv = homeData?.hotTvShows
            if (!hotTv.isNullOrEmpty()) {
                item {
                    SectionHeader("📺 热门剧集")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hotTv) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 热门综艺
            val hotVariety = homeData?.hotVariety
            if (!hotVariety.isNullOrEmpty()) {
                item {
                    SectionHeader("🎭 热门综艺")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hotVariety) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 新番放送
            val hotAnime = homeData?.hotAnime
            if (!hotAnime.isNullOrEmpty()) {
                item {
                    SectionHeader("🌸 新番放送")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hotAnime) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 热门短剧
            val hotShort = homeData?.hotShortDrama
            if (!hotShort.isNullOrEmpty()) {
                item {
                    SectionHeader("📱 热门短剧")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hotShort) { video ->
                            VideoHorizontalCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 空状态
            if (homeData == null && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无数据，下拉刷新", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ==================== 分类列表 ====================

@Composable
private fun VideoCategoryList(
    videos: List<VideoInfo>,
    isLoading: Boolean,
    onVideoClick: (VideoInfo) -> Unit
) {
    if (isLoading && videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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

// ==================== 组件 ====================

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )
}

@Composable
fun VideoCard(
    video: VideoInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
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
            if (video.displayCover.isNotBlank()) {
                AsyncImage(
                    model = video.displayCover,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
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
            if (video.displayPlayTime > 0 && video.displayTotalTime > 0) {
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

            // 集数标签
            if (video.displayTotalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "更新至${video.displayTotalEpisodes}集",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
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

@Composable
fun VideoHorizontalCard(
    video: VideoInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(168.dp)
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
            if (video.displayCover.isNotBlank()) {
                AsyncImage(
                    model = video.displayCover,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        video.title.take(1),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            if (video.rate != null) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 10.dp),
                    color = Color(0xFFFF6B81).copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        video.rate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            if (video.displayTotalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "更新至${video.displayTotalEpisodes}集",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            video.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            video.year,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueWatchCard(
    record: VideoPlayRecord,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(112.dp)
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
            if (record.cover.isNotBlank()) {
                AsyncImage(
                    model = record.cover,
                    contentDescription = record.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // 进度条
            if (record.displayTotalTime > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = record.progressPercent)
                            .height(3.dp)
                            .background(Color(0xFFE94560))
                    )
                }
            }

            // 播放图标
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                )
            }

            // 集数标签
            if (record.displayTotalEpisodes > 1) {
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "看到第${record.episodeIndex + 1}集",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            record.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

fun categoryName(type: String): String = when (type) {
    "movie" -> "电影"
    "tv" -> "电视剧"
    "show" -> "综艺"
    "variety" -> "综艺"
    "anime" -> "动漫"
    "live" -> "直播"
    "short" -> "短剧"
    "电影" -> "电影"
    "电视剧" -> "电视剧"
    "动漫" -> "动漫"
    "综艺" -> "综艺"
    "纪录片" -> "纪录片"
    else -> type
}
