package com.lechenmusic.ui.screens.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Album
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.launch

/**
 * 专辑页（手机 / 平板共用一套数据流）。
 *
 * 旧实现的两个坑：
 *  1) 页面内部保存了一份 `allAlbumsUnsorted` 本地副本；`LaunchedEffect(sortType)` 看到副本非空
 *     （启动时由磁盘缓存预热）就直接用副本，**从不调用 loadAllAlbums**。于是本次进程内永不刷新，
 *     新加的专辑要等缓存被别的路径重写才能出现。
 *  2) 平板页四个 chip 的 type 与文案错位（"名称" 走的是 starred，loadAlbums 又回退成 newest）。
 *
 * 现在统一改为：直接收集 ViewModel 的 `cachedAllAlbums`（唯一数据源），进入页面即触发一次
 * 节流刷新；排序/搜索只在本地派生，不再维护第二份专辑列表。
 */
@Composable
fun AlbumsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit = {},
    onAlbumClick: (String) -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val pad = config?.contentPadding ?: 20.dp

    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val cachedAllAlbums by viewModel.cachedAllAlbums.collectAsState()
    val isLoadingAll by viewModel.allAlbumsLoading.collectAsState()

    // "all" = 按添加时间（服务端 created 倒序），其余为本地排序
    var sortType by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    // 进入页面即刷新一次：缓存立刻上屏，服务端结果回来后再替换（ViewModel 内部 30 秒节流）
    LaunchedEffect(Unit) {
        viewModel.loadAllAlbums()
    }

    val albums = remember(cachedAllAlbums, sortType) {
        when (sortType) {
            "name" -> cachedAllAlbums.sortedBy { it.name.lowercase() }
            "artist" -> cachedAllAlbums.sortedBy { it.artist.lowercase() }
            "random" -> cachedAllAlbums.shuffled()
            else -> cachedAllAlbums.sortedByDescending { it.created }
        }
    }

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums
        else albums.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val sortOptions = listOf(
        "all" to "添加时间",
        "name" to "名称",
        "artist" to "艺术家",
        "random" to "随机"
    )

    if (isTablet) {
        // ═══ 平板布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 标题 + 刷新 + 搜索栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("专辑", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.width(8.dp))
                RefreshAction(isLoading = isLoadingAll) {
                    viewModel.loadAllAlbums(force = true)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 搜索框 (功能型)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(260.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text("搜索专辑、艺人...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, "清除", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ===== 筛选 Chips =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sortOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = sortType == type,
                        onClick = { sortType = type },
                        label = { Text(label, fontWeight = if (sortType == type) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AlbumCountText(count = filteredAlbums.size, total = cachedAllAlbums.size)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 专辑网格 =====
            AlbumsBody(
                albums = filteredAlbums,
                isLoading = isLoadingAll,
                hasAnyCache = cachedAllAlbums.isNotEmpty(),
                serverUrl = serverUrl,
                username = username,
                password = password,
                columns = config!!.gridColumns.coerceIn(3, 5),
                pad = pad,
                itemSpacing = config!!.itemSpacing,
                onAlbumClick = onAlbumClick,
                onRetry = { viewModel.loadAllAlbums(force = true) }
            )
        }
    } else {
        // ═══ 手机布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("专辑", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                RefreshAction(isLoading = isLoadingAll) {
                    viewModel.loadAllAlbums(force = true)
                }
                Spacer(modifier = Modifier.weight(1f))
                AlbumCountText(count = filteredAlbums.size, total = cachedAllAlbums.size)
            }

            // Sort chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = sortType == type,
                        onClick = { sortType = type },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAlbums.isNotEmpty() && (sortType == "all" || sortType == "name")) {
                // 仅在按名称/添加时间排序时显示 A-Z 索引栏
                PhoneAlbumsGridWithIndex(
                    albums = filteredAlbums,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onAlbumClick = onAlbumClick
                )
            } else {
                AlbumsBody(
                    albums = filteredAlbums,
                    isLoading = isLoadingAll,
                    hasAnyCache = cachedAllAlbums.isNotEmpty(),
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    columns = 2,
                    pad = 20.dp,
                    itemSpacing = 16.dp,
                    onAlbumClick = onAlbumClick,
                    onRetry = { viewModel.loadAllAlbums(force = true) }
                )
            }
        }
    }
}

/** 刷新按钮：加载中显示小转圈，否则显示刷新图标（手机/平板共用） */
@Composable
private fun RefreshAction(isLoading: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !isLoading) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Refresh, "刷新专辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlbumCountText(count: Int, total: Int) {
    if (total <= 0) return
    Text(
        if (count == total) "共 $total 张" else "$count / $total 张",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 专辑网格 + 加载/空态（手机、平板共用，避免两套 UI 行为不一致） */
@Composable
private fun AlbumsBody(
    albums: List<Album>,
    isLoading: Boolean,
    hasAnyCache: Boolean,
    serverUrl: String,
    username: String,
    password: String,
    columns: Int,
    pad: androidx.compose.ui.unit.Dp,
    itemSpacing: androidx.compose.ui.unit.Dp,
    onAlbumClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        albums.isNotEmpty() -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = pad, end = pad, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumGridCard(
                        album = album,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        onClick = { onAlbumClick(album.id) }
                    )
                }
            }
        }
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("加载专辑中...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新加载")
                    }
                }
            }
        }
    }
}

/** 手机端专辑网格 + 右侧 A-Z 索引栏 */
@Composable
private fun PhoneAlbumsGridWithIndex(
    albums: List<Album>,
    serverUrl: String,
    username: String,
    password: String,
    onAlbumClick: (String) -> Unit
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val letterIndexMap = remember(albums) {
        val map = mutableMapOf<String, Int>()
        albums.forEachIndexed { index, album ->
            val letter = album.name.firstOrNull()?.uppercase()?.let { if (it[0] in 'A'..'Z') it else "#" } ?: "#"
            if (letter !in map) map[letter] = index
        }
        map
    }
    val letters = remember(letterIndexMap) { letterIndexMap.keys.toList() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Column(modifier = Modifier.clickable { onAlbumClick(album.id) }) {
                    CoverImage(
                        coverArtId = album.coverArt,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                    Text(
                        album.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "${album.artist} · ${album.year ?: ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // A-Z 索引栏
        if (letters.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .width(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                letters.forEach { letter ->
                    Text(
                        text = letter,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val targetIndex = letterIndexMap[letter] ?: return@clickable
                                coroutineScope.launch { gridState.animateScrollToItem(targetIndex) }
                            }
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: Album,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            CoverImage(
                coverArtId = album.coverArt,
                serverUrl = serverUrl,
                username = username,
                password = password,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 专辑名
        Text(
            album.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 艺术家
        Text(
            album.artist,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}