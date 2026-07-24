package com.lechenmusic.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl
import coil.compose.AsyncImage

@Composable
fun TabletFavoritesScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onAudiobookClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit = {}
) {
    val starredSongs by viewModel.starredSongs.collectAsState()
    val starredAlbums by viewModel.starredAlbums.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val starredArtists by viewModel.starredArtists.collectAsState()
    val starredPlaylists by viewModel.starredPlaylists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val tabs = listOf("单曲" to starredSongs.size, "歌单" to starredPlaylists.size, "歌手" to starredArtists.size, "有声书" to starredAudiobooks.size)

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部: 标题 + Tab栏 + 搜索框 =====
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("我的收藏", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 搜索框
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("搜索我的收藏", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.width(280.dp),
                                trailingIcon = {
                                    IconButton(onClick = { searchText = ""; isSearchActive = false }) {
                                        Icon(Icons.Default.Close, "关闭搜索", modifier = Modifier.size(18.dp))
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Issue 19: 排序图标已删除
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab 栏
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    tabs.forEachIndexed { index, (label, count) ->
                        Column(
                            modifier = Modifier.clickable { selectedTab = index },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$label ($count)",
                                fontSize = 15.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (selectedTab == index) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===== 操作栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 全部播放按钮
                Button(
                    onClick = {
                        if (starredSongs.isNotEmpty()) onSongClick(starredSongs.first(), starredSongs)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("全部播放", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                // 批量操作按钮
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Checklist, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("批量操作", fontSize = 14.sp)
                }
            }

            // 统计
            when (selectedTab) {
                0 -> Text("共 ${starredSongs.size} 首", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                1 -> Text("共 ${starredPlaylists.size} 个歌单", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                2 -> Text("歌手收藏", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                3 -> Text("共 ${starredAudiobooks.size} 本有声书", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ===== 内容区域 =====
        when (selectedTab) {
            0 -> SongListTab(
                songs = starredSongs,
                searchText = searchText,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onSongClick = onSongClick
            )
            1 -> PlaylistsTab(
                playlists = starredPlaylists,
                responsiveConfig = responsiveConfig,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onPlaylistClick = onPlaylistClick
            )
            2 -> ArtistsTab(
                artists = starredArtists,
                responsiveConfig = responsiveConfig,
                serverUrl = serverUrl,
                username = username,
                password = password
            )
            3 -> AudiobooksTab(
                audiobooks = starredAudiobooks,
                responsiveConfig = responsiveConfig,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onAudiobookClick = onAudiobookClick
            )
        }
    }
}

// ==================== 单曲 Tab: 表格式布局 ====================
@Composable
private fun SongListTab(
    songs: List<Song>,
    searchText: String,
    serverUrl: String,
    username: String,
    password: String,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val filteredSongs = if (searchText.isBlank()) songs
    else songs.filter {
        it.title.contains(searchText, ignoreCase = true) ||
        it.artist.contains(searchText, ignoreCase = true) ||
        it.album.contains(searchText, ignoreCase = true)
    }

    if (filteredSongs.isEmpty()) {
        EmptyFavorites(if (searchText.isBlank()) "暂无收藏歌曲" else "未找到匹配的歌曲")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("标题", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
            Text("专辑", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
            Text("收藏时间", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
            Text("时长", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Spacer(modifier = Modifier.width(48.dp))
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // 歌曲列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            itemsIndexed(filteredSongs) { index, song ->
                SongTableRow(
                    index = index + 1,
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onSongClick(song, filteredSongs) }
                )
            }
        }
    }
}

@Composable
private fun SongTableRow(
    index: Int,
    song: Song,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isHovered) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号 (悬停显示播放按钮)
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isHovered) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "播放",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "$index",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 标题: 封面 + 歌名 + 歌手
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    coverArtId = song.coverArt ?: song.albumId,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        song.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 专辑
            Text(
                song.album,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.5f)
            )

            // 收藏时间
            Text(
                song.starred?.take(10) ?: "-",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f)
            )

            // 时长
            Text(
                formatDuration(song.duration),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            // 更多按钮
            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                if (isHovered) {
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 专辑 Tab ====================
@Composable
private fun AlbumsTab(
    albums: List<Album>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyFavorites("暂无收藏专辑")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
        contentPadding = PaddingValues(
            start = responsiveConfig.contentPadding,
            end = responsiveConfig.contentPadding,
            bottom = 160.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
    ) {
        items(albums) { album ->
            Column(modifier = Modifier.clickable { onAlbumClick(album.id) }) {
                CoverImage(
                    coverArtId = album.coverArt,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    album.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    album.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== 歌手 Tab ====================
@Composable
private fun ArtistsTab(
    artists: List<com.lechenmusic.data.model.Artist>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String
) {
    if (artists.isEmpty()) {
        EmptyFavorites("暂无收藏歌手")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
        contentPadding = PaddingValues(
            start = responsiveConfig.contentPadding,
            end = responsiveConfig.contentPadding,
            bottom = 160.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
    ) {
        items(artists) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val effectiveCoverArt = artist.coverArt ?: artist.id
                if (artist.artistImageUrl != null) {
                    AsyncImage(
                        model = artist.artistImageUrl,
                        contentDescription = artist.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverImage(
                        coverArtId = effectiveCoverArt,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    artist.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${artist.albumCount} 张专辑",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== 歌单 Tab ====================
@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onPlaylistClick: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyFavorites("暂无收藏歌单")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
        contentPadding = PaddingValues(
            start = responsiveConfig.contentPadding,
            end = responsiveConfig.contentPadding,
            bottom = 160.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
    ) {
        items(playlists) { playlist ->
            Column(modifier = Modifier.clickable { onPlaylistClick(playlist.id) }) {
                CoverImage(
                    coverArtId = playlist.coverArt,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    playlist.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${playlist.songCount} 首 · ${playlist.owner}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== 有声书 Tab ====================
@Composable
private fun AudiobooksTab(
    audiobooks: List<Audiobook>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onAudiobookClick: (String) -> Unit
) {
    if (audiobooks.isEmpty()) {
        EmptyFavorites("暂无收藏有声书")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
        contentPadding = PaddingValues(
            start = responsiveConfig.contentPadding,
            end = responsiveConfig.contentPadding,
            bottom = 160.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
    ) {
        items(audiobooks) { book ->
            Column(modifier = Modifier.clickable { onAudiobookClick(book.id) }) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MenuBook,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    book.narrator.ifEmpty { book.author },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== 空状态 ====================
@Composable
private fun EmptyFavorites(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FavoriteBorder,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        }
    }
}

// ==================== 工具函数 ====================
private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
