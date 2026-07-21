package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
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
import com.lechenmusic.ui.VideoViewModel

/**
 * 分类页面 - 搜索并展示某类影视内容(带缓存+分页+筛选)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCategoryScreen(
    viewModel: VideoViewModel,
    categoryType: String,
    onBack: () -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    val categoryResults by viewModel.categoryResults.collectAsState()
    val isLoading by viewModel.categoryLoading.collectAsState()
    val hasMore by viewModel.categoryHasMore.collectAsState()
    val totalCount by viewModel.categoryTotalCount.collectAsState()
    val searchSourceLoading by viewModel.searchSourceLoading.collectAsState()
    val searchSourceMsg by viewModel.searchSourceMessage.collectAsState()
    val filters by viewModel.categoryFilters.collectAsState()
    val areas by viewModel.categoryAreas.collectAsState()
    val sources by viewModel.categorySources.collectAsState()

    val title = when (categoryType) {
        "movie" -> "电影"
        "tv" -> "剧集"
        "anime" -> "动漫"
        "variety" -> "综艺"
        else -> categoryType
    }

    val searchKeyword = when (categoryType) {
        "movie" -> "电影"
        "tv" -> "电视剧"
        "anime" -> "动漫"
        "variety" -> "综艺"
        else -> categoryType
    }

    val listState = rememberLazyListState()
    var showFilterPanel by remember { mutableStateOf(false) }

    LaunchedEffect(categoryType) {
        viewModel.searchCategory(searchKeyword)
    }

    // 滚动到底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3 && hasMore && !isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreCategory()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索播放源弹窗
        if (searchSourceLoading) {
            AlertDialog(
                onDismissRequest = { /* 不允许关闭 */ },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("搜索播放源", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        searchSourceMsg.ifBlank { "正在搜索，请稍候..." },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {}
            )
        }

        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    if (totalCount > 0) {
                        Text(
                            "共 $totalCount 部",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                // 筛选按钮
                IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = if (showFilterPanel || filters != VideoViewModel.CategoryFilters())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        // 筛选面板
        if (showFilterPanel) {
            FilterPanel(
                filters = filters,
                areas = areas,
                sources = sources,
                onFilterChange = { viewModel.updateCategoryFilters(it) },
                onReset = { viewModel.resetCategoryFilters() }
            )
        }

        // 已选筛选条件标签
        if (filters != VideoViewModel.CategoryFilters() && !showFilterPanel) {
            ActiveFilterChips(
                filters = filters,
                onRemoveYear = { viewModel.updateCategoryFilters(filters.copy(year = "全部")) },
                onRemoveArea = { viewModel.updateCategoryFilters(filters.copy(area = "全部")) },
                onRemoveSource = { viewModel.updateCategoryFilters(filters.copy(source = "全部")) },
                onReset = { viewModel.resetCategoryFilters() }
            )
        }

        if (isLoading && categoryResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (categoryResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (filters != VideoViewModel.CategoryFilters()) "没有符合筛选条件的内容"
                        else "暂无${title}内容",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filters != VideoViewModel.CategoryFilters()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.resetCategoryFilters() }) {
                            Text("清除筛选")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categoryResults.chunked(3)) { row ->
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

                // 底部加载更多指示器
                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // 已加载全部提示
                if (!hasMore && categoryResults.size > 20) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "已显示全部 $totalCount 部",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 筛选面板 ====================

@Composable
private fun FilterPanel(
    filters: VideoViewModel.CategoryFilters,
    areas: List<String>,
    sources: List<String>,
    onFilterChange: (VideoViewModel.CategoryFilters) -> Unit,
    onReset: () -> Unit
) {
    val yearOptions = listOf("全部", "2026", "2025", "2024", "2023", "更早")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("筛选", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                TextButton(onClick = onReset) {
                    Text("重置", fontSize = 12.sp)
                }
            }

            // 年份
            FilterRow(
                label = "年份",
                options = yearOptions,
                selected = filters.year,
                onSelect = { onFilterChange(filters.copy(year = it)) }
            )

            // 地区
            if (areas.size > 1) {
                FilterRow(
                    label = "地区",
                    options = areas.take(10),  // 最多显示10个
                    selected = filters.area,
                    onSelect = { onFilterChange(filters.copy(area = it)) }
                )
            }

            // 来源
            if (sources.size > 1) {
                FilterRow(
                    label = "来源",
                    options = sources.take(10),
                    selected = filters.source,
                    onSelect = { onFilterChange(filters.copy(source = it)) }
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        option,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ==================== 已选筛选标签 ====================

@Composable
private fun ActiveFilterChips(
    filters: VideoViewModel.CategoryFilters,
    onRemoveYear: () -> Unit,
    onRemoveArea: () -> Unit,
    onRemoveSource: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (filters.year != "全部") {
            FilterChip(
                selected = true,
                onClick = onRemoveYear,
                label = { Text(filters.year, fontSize = 11.sp) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(28.dp)
            )
        }
        if (filters.area != "全部") {
            FilterChip(
                selected = true,
                onClick = onRemoveArea,
                label = { Text(filters.area, fontSize = 11.sp) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(28.dp)
            )
        }
        if (filters.source != "全部") {
            FilterChip(
                selected = true,
                onClick = onRemoveSource,
                label = { Text(filters.source, fontSize = 11.sp) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(28.dp)
            )
        }
    }
}
