package com.lechenmusic.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.lechenmusic.data.model.Album
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import kotlinx.coroutines.launch
import com.lechenmusic.ui.responsive.ResponsiveConfig

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
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var sortType by remember { mutableStateOf("all") }
    var isLoadingAll by remember { mutableStateOf(false) }

    // Store unsorted albums for client-side sorting
    var allAlbumsUnsorted by remember { mutableStateOf<List<Album>>(emptyList()) }

    LaunchedEffect(sortType) {
        when (sortType) {
            "all" -> {
                isLoadingAll = true
                viewModel.loadAllAlbums { allAlbumsUnsorted = it; albums = it; isLoadingAll = false }
            }
            "name" -> {
                if (allAlbumsUnsorted.isNotEmpty()) {
                    albums = allAlbumsUnsorted.sortedBy { it.name.lowercase() }
                } else {
                    isLoadingAll = true
                    viewModel.loadAllAlbums { unsorted ->
                        allAlbumsUnsorted = unsorted
                        albums = unsorted.sortedBy { it.name.lowercase() }
                        isLoadingAll = false
                    }
                }
            }
            "artist" -> {
                if (allAlbumsUnsorted.isNotEmpty()) {
                    albums = allAlbumsUnsorted.sortedBy { it.artist.lowercase() }
                } else {
                    isLoadingAll = true
                    viewModel.loadAllAlbums { unsorted ->
                        allAlbumsUnsorted = unsorted
                        albums = unsorted.sortedBy { it.artist.lowercase() }
                        isLoadingAll = false
                    }
                }
            }
            "random" -> {
                if (allAlbumsUnsorted.isNotEmpty()) {
                    albums = allAlbumsUnsorted.shuffled()
                } else {
                    isLoadingAll = true
                    viewModel.loadAllAlbums { unsorted ->
                        allAlbumsUnsorted = unsorted
                        albums = unsorted.shuffled()
                        isLoadingAll = false
                    }
                }
            }
            else -> {
                viewModel.loadAlbums(sortType) { albums = it }
            }
        }
    }

    if (isTablet) {
        // ═══ 平板布局 ═══
        var searchQuery by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 搜索栏 =====
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("all" to "添加时间", "random" to "随机", "artist" to "艺术家", "name" to "名称").forEach { (type, label) ->
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 专辑网格 =====
            val filteredAlbums = if (searchQuery.isBlank()) albums
                else albums.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
                }
            if (isLoadingAll) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredAlbums.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config!!.gridColumns.coerceIn(3, 5)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = pad,
                        end = pad,
                        bottom = 160.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
                ) {
                    items(filteredAlbums, key = { it.id }) { album ->
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
            }

            // Sort tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "添加时间", "name" to "名称", "artist" to "艺术家", "random" to "随机").forEach { (type, label) ->
                    FilterChip(
                        selected = sortType == type,
                        onClick = { sortType = type },
                        label = { Text(label) }
                    )
                }
            }

            if (albums.isEmpty() && isLoadingAll) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("加载全部专辑中...", fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            } else if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 按首字母分组
                val sortedAlbums = remember(albums) { albums.sortedBy { it.name.lowercase() } }
                val grouped = remember(sortedAlbums) {
                    sortedAlbums.groupBy { album ->
                        val first = album.name.firstOrNull()?.uppercase() ?: "#"
                        if (first[0] in 'A'..'Z') first else "#"
                    }.toSortedMap()
                }
                val letters = remember(grouped) { grouped.keys.toList() }
                val gridState = rememberLazyGridState()
                val coroutineScope = rememberCoroutineScope()
                // letter -> index in sortedAlbums
                val letterIndexMap = remember(sortedAlbums) {
                    val map = mutableMapOf<String, Int>()
                    sortedAlbums.forEachIndexed { index, album ->
                        val letter = album.name.firstOrNull()?.uppercase()?.let { if (it[0] in 'A'..'Z') it else "#" } ?: "#"
                        if (letter !in map) map[letter] = index
                    }
                    map
                }

                Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedAlbums.size) { index ->
                        val album = sortedAlbums[index]
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
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                "${album.artist} · ${album.year ?: ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
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
                } // Box
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
