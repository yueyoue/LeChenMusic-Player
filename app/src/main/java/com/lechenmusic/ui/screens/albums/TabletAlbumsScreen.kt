package com.lechenmusic.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.lechenmusic.data.model.Album
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletAlbumsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var allAlbumsUnsorted by remember { mutableStateOf<List<Album>>(emptyList()) }
    var sortType by remember { mutableStateOf("newest") }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(sortType) {
        when (sortType) {
            "newest" -> {
                isLoading = true
                viewModel.loadAllAlbums { unsorted ->
                    allAlbumsUnsorted = unsorted
                    albums = unsorted.sortedByDescending { it.created }
                    isLoading = false
                }
            }
            "random" -> {
                if (allAlbumsUnsorted.isNotEmpty()) {
                    albums = allAlbumsUnsorted.shuffled()
                } else {
                    isLoading = true
                    viewModel.loadAllAlbums { unsorted ->
                        allAlbumsUnsorted = unsorted
                        albums = unsorted.shuffled()
                        isLoading = false
                    }
                }
            }
            "frequent" -> {
                if (allAlbumsUnsorted.isNotEmpty()) {
                    albums = allAlbumsUnsorted.sortedByDescending { it.playCount }
                } else {
                    isLoading = true
                    viewModel.loadAllAlbums { unsorted ->
                        allAlbumsUnsorted = unsorted
                        albums = unsorted.sortedByDescending { it.playCount }
                        isLoading = false
                    }
                }
            }
            "starred" -> {
                isLoading = true
                viewModel.loadAlbums("starred") { albums = it; isLoading = false }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部: 返回按钮 + 搜索栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
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
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("newest" to "最新", "random" to "随机", "frequent" to "艺术家", "starred" to "名称").forEach { (type, label) ->
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (filteredAlbums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = responsiveConfig.contentPadding,
                    end = responsiveConfig.contentPadding,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
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
