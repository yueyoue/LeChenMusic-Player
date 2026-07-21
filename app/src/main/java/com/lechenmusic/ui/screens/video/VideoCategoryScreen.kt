package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel

/**
 * 分类页面 - 参考 Selene-Source MovieScreen
 * 各分类有独立的筛选选项
 */

data class FilterOption(val label: String, val value: String)
data class FilterPillDef(val title: String, val options: List<FilterOption>)

// ===== 电影筛选 =====
private val movieCategoryOptions = listOf(
    FilterOption("热门", "热门"), FilterOption("最新", "最新"),
    FilterOption("豆瓣高分", "豆瓣高分"), FilterOption("冷门佳片", "冷门佳片"),
)
private val movieTypeOptions = listOf(
    FilterOption("全部", "全部"), FilterOption("喜剧", "喜剧"), FilterOption("爱情", "爱情"),
    FilterOption("动作", "动作"), FilterOption("科幻", "科幻"), FilterOption("悬疑", "悬疑"),
    FilterOption("犯罪", "犯罪"), FilterOption("惊悚", "惊悚"), FilterOption("冒险", "冒险"),
    FilterOption("音乐", "音乐"), FilterOption("历史", "历史"), FilterOption("奇幻", "奇幻"),
    FilterOption("恐怖", "恐怖"), FilterOption("战争", "战争"), FilterOption("传记", "传记"),
    FilterOption("歌舞", "歌舞"), FilterOption("武侠", "武侠"), FilterOption("灾难", "灾难"),
    FilterOption("纪录片", "纪录片"), FilterOption("短片", "短片"),
)
private val movieRegionOptions = listOf(
    FilterOption("全部", "全部"), FilterOption("华语", "华语"), FilterOption("欧美", "欧美"),
    FilterOption("韩国", "韩国"), FilterOption("日本", "日本"), FilterOption("中国大陆", "中国大陆"),
    FilterOption("美国", "美国"), FilterOption("中国香港", "中国香港"), FilterOption("中国台湾", "中国台湾"),
    FilterOption("英国", "英国"), FilterOption("法国", "法国"), FilterOption("德国", "德国"),
    FilterOption("印度", "印度"), FilterOption("泰国", "泰国"),
)

// ===== 剧集筛选 =====
private val tvRegionOptions = listOf(
    FilterOption("全部", "tv"), FilterOption("国产剧", "tv"), FilterOption("美剧", "美剧"),
    FilterOption("日剧", "日剧"), FilterOption("韩剧", "韩剧"),
    FilterOption("港剧", "港剧"), FilterOption("台剧", "台剧"),
    FilterOption("英剧", "英剧"), FilterOption("法剧", "法剧"),
)

// ===== 动漫筛选 =====
private val animeRegionOptions = listOf(
    FilterOption("全部", "日本"), FilterOption("日本", "日本"),
    FilterOption("美国", "美国"), FilterOption("中国大陆", "中国大陆"),
    FilterOption("欧美", "欧美"), FilterOption("韩国", "韩国"),
)

// ===== 通用筛选 =====
private val yearOptions = listOf(
    FilterOption("全部", "全部"), FilterOption("2026", "2026"), FilterOption("2025", "2025"),
    FilterOption("2024", "2024"), FilterOption("2023", "2023"), FilterOption("2022", "2022"),
    FilterOption("2021", "2021"), FilterOption("2020", "2020"),
    FilterOption("2020年代", "2020年代"), FilterOption("2010年代", "2010年代"),
    FilterOption("2000年代", "2000年代"), FilterOption("90年代", "90年代"),
    FilterOption("更早", "更早"),
)
private val sortOptions = listOf(
    FilterOption("综合排序", "T"), FilterOption("近期热度", "U"),
    FilterOption("首映时间", "R"), FilterOption("高分优先", "S"),
)

/** 根据分类类型返回对应的筛选 pills */
private fun getFilterPills(categoryType: String): List<FilterPillDef> {
    return when (categoryType) {
        "movie" -> listOf(
            FilterPillDef("类型", movieTypeOptions),
            FilterPillDef("地区", movieRegionOptions),
            FilterPillDef("年代", yearOptions),
            FilterPillDef("排序", sortOptions),
        )
        "tv" -> listOf(
            FilterPillDef("地区", tvRegionOptions),
            FilterPillDef("年代", yearOptions),
        )
        "anime" -> listOf(
            FilterPillDef("地区", animeRegionOptions),
            FilterPillDef("年代", yearOptions),
        )
        "variety" -> listOf(
            FilterPillDef("年代", yearOptions),
        )
        else -> emptyList()
    }
}

/** 获取一级分类选项(仅电影有) */
private fun getCategoryOptions(categoryType: String): List<FilterOption> {
    return when (categoryType) {
        "movie" -> movieCategoryOptions
        else -> emptyList()
    }
}

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

    val filterPills = remember(categoryType) { getFilterPills(categoryType) }
    val categoryOpts = remember(categoryType) { getCategoryOptions(categoryType) }

    var activeFilterIndex by remember { mutableIntStateOf(-1) } // 当前展开的筛选
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
        if (shouldLoadMore) viewModel.loadMoreCategory()
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
                text = { Text(searchSourceMsg.ifBlank { "正在搜索，请稍候..." }, fontSize = 14.sp) },
                confirmButton = {}
            )
        }

        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    if (totalCount > 0) {
                        Text("来自豆瓣的精选内容", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        // ========== 筛选区域 ==========
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                // 一级分类(仅电影)
                if (categoryOpts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOpts.forEach { option ->
                            FilterChip(
                                selected = filters.category == option.value,
                                onClick = { viewModel.updateCategoryFilters(filters.copy(category = option.value)) },
                                label = { Text(option.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 筛选 pills
                if (filterPills.isNotEmpty()) {
                    Text("筛选", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterPills.forEachIndexed { index, pill ->
                            val currentValue = when (pill.title) {
                                "类型" -> filters.category
                                "地区" -> filters.region
                                "年代" -> filters.year
                                "排序" -> filters.sort
                                else -> ""
                            }
                            val isDefault = currentValue == "全部" || currentValue == "热门" || currentValue == "T" ||
                                    currentValue == "tv" || currentValue == "日本" || currentValue == "show"
                            FilterPillButton(
                                title = pill.title,
                                options = pill.options,
                                selectedValue = currentValue,
                                isDefault = isDefault,
                                onClick = { activeFilterIndex = index }
                            )
                        }
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
                if (hasMore) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                if (!hasMore && categoryResults.size > 25) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("— 已显示全部 $totalCount 部 —", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // ========== 筛选底部弹窗 ==========
    if (activeFilterIndex >= 0 && activeFilterIndex < filterPills.size) {
        val pill = filterPills[activeFilterIndex]
        val currentValue = when (pill.title) {
            "类型" -> filters.category
            "地区" -> filters.region
            "年代" -> filters.year
            "排序" -> filters.sort
            else -> ""
        }
        FilterBottomSheet(
            title = pill.title,
            options = pill.options,
            selectedValue = currentValue,
            onSelect = { value ->
                val newFilters = when (pill.title) {
                    "类型" -> filters.copy(category = value)
                    "地区" -> filters.copy(region = value)
                    "年代" -> filters.copy(year = value)
                    "排序" -> filters.copy(sort = value)
                    else -> filters
                }
                viewModel.updateCategoryFilters(newFilters)
                activeFilterIndex = -1
            },
            onDismiss = { activeFilterIndex = -1 }
        )
    }
}

// ==================== 筛选 Pill 按钮 ====================

@Composable
private fun FilterPillButton(
    title: String,
    options: List<FilterOption>,
    selectedValue: String,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    val displayText = if (isDefault) title
    else options.firstOrNull { it.value == selectedValue }?.label ?: title

    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.Transparent) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(displayText, fontSize = 13.sp,
                fontWeight = if (isDefault) FontWeight.Normal else FontWeight.Medium,
                color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(2.dp))
            Text("▾", fontSize = 10.sp,
                color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        }
    }
}

// ==================== 筛选底部弹窗 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    title: String,
    options: List<FilterOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            val rows = options.chunked(4)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { option ->
                        val isSelected = option.value == selectedValue
                        Surface(onClick = { onSelect(option.value) }, shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(option.label, fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
