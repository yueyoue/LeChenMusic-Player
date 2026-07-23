package com.lechenmusic.ui.screens.favorites
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Audiobook
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
    onAudiobookClick: (String) -> Unit
) {
    val starredSongs by viewModel.starredSongs.collectAsState()
    val starredAlbums by viewModel.starredAlbums.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 面包屑
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("收藏", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Tab
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = responsiveConfig.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            listOf("歌曲" to starredSongs.size, "专辑" to starredAlbums.size, "有声书" to starredAudiobooks.size).forEachIndexed { index, (label, count) ->
                Column(
                    modifier = Modifier.clickable { selectedTab = index },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$label ($count)",
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedTab == index) {
                        Box(modifier = Modifier.width(32.dp).height(2.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // 歌曲
                if (starredSongs.isEmpty()) {
                    EmptyFavorites("暂无收藏歌曲")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = responsiveConfig.contentPadding, end = responsiveConfig.contentPadding, bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(starredSongs) { song ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onSongClick(song, starredSongs) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverImage(
                                    coverArtId = song.coverArt ?: song.albumId,
                                    serverUrl = serverUrl, username = username, password = password,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // 专辑
                if (starredAlbums.isEmpty()) {
                    EmptyFavorites("暂无收藏专辑")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
                        contentPadding = PaddingValues(start = responsiveConfig.contentPadding, end = responsiveConfig.contentPadding, bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
                    ) {
                        items(starredAlbums) { album ->
                            Column(modifier = Modifier.clickable { onAlbumClick(album.id) }) {
                                CoverImage(
                                    coverArtId = album.coverArt,
                                    serverUrl = serverUrl, username = username, password = password,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(album.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(album.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
            2 -> {
                // 有声书
                if (starredAudiobooks.isEmpty()) {
                    EmptyFavorites("暂无收藏有声书")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 5)),
                        contentPadding = PaddingValues(start = responsiveConfig.contentPadding, end = responsiveConfig.contentPadding, bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                        verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
                    ) {
                        items(starredAudiobooks) { book ->
                            Column(modifier = Modifier.clickable { onAudiobookClick(book.id) }) {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
                                    if (coverUrl != null) {
                                        AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(book.narrator.ifEmpty { book.author }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
