package com.lechenmusic.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItem
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onAudiobookClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {}
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val starredSongs by viewModel.starredSongs.collectAsState()
    val starredAlbums by viewModel.starredAlbums.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val starredPlaylists by viewModel.starredPlaylists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Lifecycle-aware refresh: reload data when screen becomes visible
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadStarredSongs()
        viewModel.loadStarredAlbums()
        viewModel.loadStarredAudiobooks()
        viewModel.loadPlaylists()
    }

    if (isTablet) {
        // ═══ 平板布局 ═══
        val starredArtists by viewModel.starredArtists.collectAsState()
        var searchText by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }

        val tabs = listOf("单曲" to starredSongs.size, "专辑" to starredAlbums.size, "歌单" to starredPlaylists.size, "有声书" to starredAudiobooks.size)

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 标题 + Tab栏 + 搜索框 =====
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = config!!.contentPadding, vertical = 12.dp)) {
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
                    .padding(horizontal = config?.contentPadding ?: 16.dp, vertical = 12.dp),
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
                    1 -> Text("共 ${starredAlbums.size} 张专辑", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    2 -> Text("共 ${starredPlaylists.size} 个歌单", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    3 -> Text("共 ${starredAudiobooks.size} 本有声书", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ===== 内容区域 =====
            when (selectedTab) {
                0 -> TabletSongListTab(
                    songs = starredSongs,
                    searchText = searchText,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onSongClick = onSongClick
                )
                1 -> TabletAlbumsTab(
                    albums = starredAlbums,
                    responsiveConfig = config!!,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onAlbumClick = onAlbumClick
                )
                2 -> TabletPlaylistsTab(
                    playlists = starredPlaylists,
                    responsiveConfig = config!!,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onPlaylistClick = onPlaylistClick
                )
                3 -> TabletAudiobooksTab(
                    audiobooks = starredAudiobooks,
                    responsiveConfig = config!!,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onAudiobookClick = onAudiobookClick
                )
            }
        }
    } else {
        // ═══ 手机布局 ═══
        val tabs = listOf("音乐", "专辑", "歌单", "有声书")

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("我的收藏", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Tabs
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

            when (selectedTab) {
                0 -> MusicTab(starredSongs, serverUrl, username, password, onSongClick)
                1 -> AlbumsTab(starredAlbums, serverUrl, username, password, onAlbumClick)
                2 -> PlaylistsTab(starredPlaylists, serverUrl, username, password, onPlaylistClick)
                3 -> AudiobooksTab(starredAudiobooks, serverUrl, username, password, onAudiobookClick)
            }
        }
    }
}

// ==================== 手机 Tab 组件 ====================

@Composable
private fun MusicTab(
    songs: List<Song>,
    serverUrl: String,
    username: String,
    password: String,
    onSongClick: (Song, List<Song>) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState("暂无收藏歌曲", "🎵")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item {
            Text(
                "${songs.size} 首歌曲",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        items(songs) { song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, songs) }
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    serverUrl: String,
    username: String,
    password: String,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState("暂无收藏专辑", "💿")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item {
            Text(
                "${albums.size} 张专辑",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        items(albums) { album ->
            AlbumRow(album, serverUrl, username, password) { onAlbumClick(album.id) }
        }
    }
}

@Composable
private fun AudiobooksTab(
    audiobooks: List<Audiobook>,
    serverUrl: String,
    username: String,
    password: String,
    onAudiobookClick: (String) -> Unit
) {
    if (audiobooks.isEmpty()) {
        EmptyState("暂无收藏有声书", "📖")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item {
            Text(
                "${audiobooks.size} 本有声书",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        items(audiobooks) { book ->
            AudiobookRow(book, serverUrl, username, password) { onAudiobookClick(book.id) }
        }
    }
}

@Composable
private fun EmptyState(text: String, emoji: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (album.coverArt != null) {
                AsyncImage(
                    model = ApiClient.getCoverArtUrl(serverUrl, username, password, album.coverArt),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Album,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                album.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
        Text(
            "${album.songCount}首",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AudiobookRow(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .width(48.dp)
                .height(64.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val url = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val info = buildString {
                if (book.narrator.isNotBlank()) append(book.narrator)
                else if (book.author.isNotBlank()) append(book.author)
                if (book.chapterCount > 0) append(" · ${book.chapterCount}章")
            }
            if (info.isNotEmpty()) {
                Text(
                    info,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    serverUrl: String,
    username: String,
    password: String,
    onPlaylistClick: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState("暂无歌单", "📋")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item {
            Text(
                "${playlists.size} 个歌单",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        items(playlists) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (playlist.coverArt != null) {
                        AsyncImage(
                            model = ApiClient.getCoverArtUrl(serverUrl, username, password, playlist.coverArt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlaylistPlay,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${playlist.songCount}首 · ${playlist.owner}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    if (playlist.public) Icons.Default.Public else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==================== 平板 Tab 组件 ====================

@Composable
private fun TabletSongListTab(
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
        TabletEmptyFavorites(if (searchText.isBlank()) "暂无收藏歌曲" else "未找到匹配的歌曲")
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
            Text("#", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            Text("标题", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
            Text("专辑", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
            Text("收藏时间", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
            Text("时长", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
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
                TabletFormatDuration(song.duration),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
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

@Composable
private fun TabletAlbumsTab(
    albums: List<Album>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        TabletEmptyFavorites("暂无收藏专辑")
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
        gridItems(albums) { album ->
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

@Composable
private fun TabletPlaylistsTab(
    playlists: List<Playlist>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onPlaylistClick: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        TabletEmptyFavorites("暂无收藏歌单")
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
        gridItems(playlists) { playlist ->
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

@Composable
private fun TabletAudiobooksTab(
    audiobooks: List<Audiobook>,
    responsiveConfig: ResponsiveConfig,
    serverUrl: String,
    username: String,
    password: String,
    onAudiobookClick: (String) -> Unit
) {
    if (audiobooks.isEmpty()) {
        TabletEmptyFavorites("暂无收藏有声书")
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
        gridItems(audiobooks) { book ->
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
                            contentScale = ContentScale.Crop
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

@Composable
private fun TabletEmptyFavorites(text: String) {
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

private fun TabletFormatDuration(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
