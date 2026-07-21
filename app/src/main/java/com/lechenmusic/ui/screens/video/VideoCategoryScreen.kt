package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel

/**
 * 分类页面 - 参考 Selene-Source MovieScreen
 * 使用豆瓣 API 做筛选浏览，点击影片时搜索 LunaTV 播放
 *
 * 布局:
 * - 顶部栏(标题+总数)
 * - 筛选区域(分类选择 + 高级筛选 pills)
 * - 内容网格(3列)
 */

// ==================== 筛选选项定义(参考 Selene-Source) ====================

data class FilterOption(val label: String, val value: String)

// 分类(一级选择器)
private val categoryOptions = listOf(
    FilterOption("全部", "热门"),
    FilterOption("热门电影", "热门"),
    FilterOption("最新电影", "最新"),
    FilterOption("豆瓣高分", "豆瓣高分"),
    FilterOption("冷门佳片", "冷门佳片"),
)

// 类型(二级筛选 pill)
private val typeOptions = listOf(
    FilterOption("全部", "全部"),
    FilterOption("喜剧", "喜剧"),
    FilterOption("爱情", "爱情"),
    FilterOption("动作", "动作"),
    FilterOption("科幻", "科幻"),
    FilterOption("悬疑", "悬疑"),
    FilterOption("犯罪", "犯罪"),
    FilterOption("惊悚", "惊悚"),
    FilterOption("冒险", "冒险"),
    FilterOption("音乐", "音乐"),
    FilterOption("历史", "历史"),
    FilterOption("奇幻", "奇幻"),
    FilterOption("恐怖", "恐怖"),
    FilterOption("战争", "战争"),
    FilterOption("传记", "传记"),
    FilterOption("歌舞", "歌舞"),
    FilterOption("武侠", "武侠"),
    FilterOption("灾难", "灾难"),
    FilterOption("纪录片", "纪录片"),
    FilterOption("短片", "短片"),
)

// 地区
private val regionOptions = listOf(
    FilterOption("全部", "全部"),
    FilterOption("华语", "华语"),
    FilterOption("欧美", "欧美"),
    FilterOption("韩国", "韩国"),
    FilterOption("日本", "日本"),
    FilterOption("中国大陆", "中国大陆"),
    FilterOption("美国", "美国"),
    FilterOption("中国香港", "中国香港"),
    FilterOption("中国台湾", "中国台湾"),
    FilterOption("英国", "英国"),
    FilterOption("法国", "法国"),
    FilterOption("德国", "德国"),
    FilterOption("印度", "印度"),
    FilterOption("泰国", "泰国"),
    FilterOption("俄罗斯", "俄罗斯"),
    FilterOption("加拿大", "加拿大"),
    FilterOption("澳大利亚", "澳大利亚"),
)

// 年份
private val yearOptions = listOf(
    FilterOption("全部", "全部"),
    FilterOption("2026", "2026"),
    FilterOption("2025", "2025"),
    FilterOption("2024", "2024"),
    FilterOption("2023", "2023"),
    FilterOption("2022", "2022"),
    FilterOption("2021", "2021"),
    FilterOption("2020", "2020"),
    FilterOption("2020年代", "2020年代"),
    FilterOption("2010年代", "2010年代"),
    FilterOption("2000年代", "2000年代"),
    FilterOption("90年代", "90年代"),
    FilterOption("更早", "更早"),
)

// 排序
private val sortOptions = listOf(
    FilterOption("综合排序", "T"),
    FilterOption("近期热度", "U"),
    FilterOption("首映时间", "R"),
    FilterOption("高分优先", "S"),
)

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
    val filters by viewModel.categoryFilters.collectAsState()
    val searchSourceLoading by viewModel.searchSourceLoading.collectAsState()
    val searchSourceMsg by viewModel.searchSourceMessage.collectAsState()

    val title = when (categoryType) {
        "movie" -> "电影"
        "tv" -> "剧集"
        "anime" -> "动漫"
        "variety" -> "综艺"
        else -> categoryType
    }

    // 筛选面板展开状态
    var showTypeFilter by remember { mutableStateOf(false) }
    var showRegionFilter by remember { mutableStateOf(false) }
    var showYearFilter by remember { mutableStateOf(false) }
    var showSortFilter by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    LaunchedEffect(categoryType) {
        viewModel.fetchDoubanCategory(categoryType)
    }

    // 滚动到底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 6 && hasMore && !isLoading
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
                onDismissRequest = {},
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("搜索播放源", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(searchSourceMsg.ifBlank { "正在搜索，请稍候..." },
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("来自豆瓣的精选内容",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        // ========== 筛选区域(参考 Selene-Source) ==========
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                // 一级: 分类选择(横向滚动胶囊)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { option ->
                        val isSelected = filters.category == option.value && option.label != "全部"
                        val isDefault = filters.category == "热门" && option.label == "全部"
                        val active = isSelected || isDefault
                        FilterChip(
                            selected = active,
                            onClick = {
                                viewModel.updateCategoryFilters(
                                    filters.copy(category = option.value)
                                )
                            },
                            label = { Text(option.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 二级: 高级筛选 pills(参考 Selene-Source 的 _buildFilterPill)
                // 只在"全部"分类下显示高级筛选
                val showAdvanced = filters.category == "热门" ||
                        !categoryOptions.any { it.label == filters.category && it.label != "全部" }

                if (showAdvanced) {
                    Text("筛选", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 类型 pill
                        FilterPill(
                            title = "类型",
                            options = typeOptions,
                            selectedValue = filters.category,
                            isDefault = filters.category == "热门",
                            onClick = { showTypeFilter = true }
                        )
                        // 地区 pill
                        FilterPill(
                            title = "地区",
                            options = regionOptions,
                            selectedValue = filters.region,
                            isDefault = filters.region == "全部",
                            onClick = { showRegionFilter = true }
                        )
                        // 年代 pill
                        FilterPill(
                            title = "年代",
                            options = yearOptions,
                            selectedValue = filters.year,
                            isDefault = filters.year == "全部",
                            onClick = { showYearFilter = true }
                        )
                        // 排序 pill
                        FilterPill(
                            title = "排序",
                            options = sortOptions,
                            selectedValue = filters.sort,
                            isDefault = filters.sort == "T",
                            onClick = { showSortFilter = true }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ========== 内容网格 ==========
        if (isLoading && categoryResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (categoryResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("暂无${title}内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categoryResults) { video ->
                    VideoCard(video = video, onClick = { onVideoClick(video) })
                }
                // 底部加载更多
                if (hasMore) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                // 已到底部
                if (!hasMore && categoryResults.size > PAGE_SIZE) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("— 已显示全部 $totalCount 部 —",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // ========== 筛选弹窗(参考 Selene-Source 的 showFilterOptionsSelector) ==========
    if (showTypeFilter) {
        FilterBottomSheet("类型", typeOptions, filters.category) { value ->
            viewModel.updateCategoryFilters(filters.copy(category = value))
            showTypeFilter = false
        }
        { showTypeFilter = false }
    }
    if (showRegionFilter) {
        FilterBottomSheet("地区", regionOptions, filters.region) { value ->
            viewModel.updateCategoryFilters(filters.copy(region = value))
            showRegionFilter = false
        }
        { showRegionFilter = false }
    }
    if (showYearFilter) {
        FilterBottomSheet("年代", yearOptions, filters.year) { value ->
            viewModel.updateCategoryFilters(filters.copy(year = value))
            showYearFilter = false
        }
        { showYearFilter = false }
    }
    if (showSortFilter) {
        FilterBottomSheet("排序", sortOptions, filters.sort) { value ->
            viewModel.updateCategoryFilters(filters.copy(sort = value))
            showSortFilter = false
        }
        { showSortFilter = false }
    }
}

// ==================== 筛选 Pill(参考 Selene-Source FilterPillHover) ====================

@Composable
private fun FilterPill(
    title: String,
    options: List<FilterOption>,
    selectedValue: String,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    val displayText = if (isDefault) {
        title
    } else {
        options.firstOrNull { it.value == selectedValue }?.label ?: title
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                displayText,
                fontSize = 13.sp,
                fontWeight = if (isDefault) FontWeight.Normal else FontWeight.Medium,
                color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text("▾", fontSize = 10.sp,
                color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary)
        }
    }
}

// ==================== 筛选底部弹窗(参考 Selene-Source showFilterOptionsSelector) ====================

@Composable
private fun FilterBottomSheet(
    title: String,
    options: List<FilterOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp))

            // 4列网格
            val rows = options.chunked(4)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { option ->
                        val isSelected = option.value == selectedValue
                        Surface(
                            onClick = { onSelect(option.value) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    option.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // 填充空位
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private const val PAGE_SIZE = 25
