package com.lechenmusic.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.SongItem
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    videoViewModel: VideoViewModel? = null,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onAudiobookClick: (String) -> Unit = {},
    onVideoClick: (String, String) -> Unit = { _, _ -> }
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()

    // 影视搜索状态
    val videoSearchResults by videoViewModel?.searchResults?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // Restore previous search state
    var query by remember { mutableStateOf(searchQuery) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            "搜索",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Search Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) {
                        viewModel.search(it)
                        // 有声书搜索 (本地过滤)
                        // 影视搜索
                        videoViewModel?.search(it)
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索音乐、有声书、影视") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable {
                            query = ""
                            viewModel.search("")
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tabs (音乐 / 有声书 / 影视)
        if (query.isNotBlank()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("音乐", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("有声书", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("影视", modifier = Modifier.padding(12.dp)) }
            }
        }

        // Results
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 160.dp)) {
            if (query.isBlank()) {
                // Hot searches
                item {
                    Text(
                        "热门搜索",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
                item {
                    val hotTags = listOf("周杰伦", "林俊杰", "陈奕迅", "薛之谦", "邓紫棋", "五月天", "Taylor Swift", "周深")
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hotTags.forEach { tag ->
                            SuggestionChip(
                                onClick = { query = tag; viewModel.search(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // 音乐搜索结果
                        val rawSongs = searchResults?.song ?: emptyList()
                        val q = query.lowercase()
                        val songs = rawSongs.filter { song ->
                            song.title.lowercase().contains(q) ||
                            song.artist.lowercase().contains(q) ||
                            song.album.lowercase().contains(q)
                        }

                        // 歌曲
                        if (songs.isNotEmpty()) {
                            item {
                                Text("歌曲 (${songs.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(songs) { song ->
                                SongItem(song = song, serverUrl = serverUrl, username = username, password = password,
                                    onClick = { onSongClick(song, songs) })
                            }
                        }

                        // 歌手
                        val artists = searchResults?.artist ?: emptyList()
                        if (artists.isNotEmpty()) {
                            item {
                                Text("歌手 (${artists.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(artists) { artist ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onArtistClick(artist.id) }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (artist.artistImageUrl != null) {
                                        AsyncImage(model = artist.artistImageUrl, contentDescription = artist.name,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)),
                                            contentScale = ContentScale.Crop)
                                    } else {
                                        Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(28.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(start = 14.dp)) {
                                        Text(artist.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("${artist.albumCount} 张专辑", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // 专辑
                        val albums = searchResults?.album ?: emptyList()
                        if (albums.isNotEmpty()) {
                            item {
                                Text("专辑 (${albums.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(albums.chunked(2)) { row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEach { album ->
                                        AlbumCard(album = album, serverUrl = serverUrl, username = username, password = password,
                                            onClick = { onAlbumClick(album.id) }, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                            item {
                                Text("未找到匹配的音乐", modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    1 -> {
                        // 有声书搜索结果 (本地过滤)
                        val q = query.lowercase()
                        val matchedBooks = audiobooks.filter {
                            it.title.contains(q, ignoreCase = true) ||
                            it.author.contains(q, ignoreCase = true) ||
                            it.narrator.contains(q, ignoreCase = true)
                        }
                        if (matchedBooks.isNotEmpty()) {
                            item {
                                Text("有声书 (${matchedBooks.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(matchedBooks) { book ->
                                val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onAudiobookClick(book.id) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(52.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant) {
                                        if (coverUrl != null) {
                                            AsyncImage(model = coverUrl, contentDescription = null,
                                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
                                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("未找到匹配的有声书", modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    2 -> {
                        // 影视搜索结果
                        if (videoSearchResults.isNotEmpty()) {
                            item {
                                Text("影视 (${videoSearchResults.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(videoSearchResults) { video ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onVideoClick(video.source, video.id) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.width(48.dp).height(68.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant) {
                                        if (video.poster.isNotBlank()) {
                                            AsyncImage(model = video.poster, contentDescription = null,
                                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(video.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(buildString {
                                            if (video.year.isNotBlank()) append(video.year)
                                            if (video.sourceName.isNotBlank()) append(" · ${video.sourceName}")
                                        }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("未找到匹配的影视", modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement) {
        content()
    }
}
