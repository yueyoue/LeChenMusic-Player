package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.components.SongItem

@Composable
fun ArtistDetailScreen(
    viewModel: MainViewModel,
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val artist by viewModel.currentArtist.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(artistId) {
        viewModel.loadArtistDetail(artistId)
    }

    val currentArtist = artist ?: return

    // Collect all songs from all albums
    val allSongs = remember(currentArtist) {
        currentArtist.album?.flatMap { it.song ?: emptyList() } ?: emptyList()
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        // Gradient background header with artist info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Background image with gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                ) {
                    if (currentArtist.artistImageUrl != null) {
                        AsyncImage(
                            model = currentArtist.artistImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }
                }
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                // Artist info centered
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentArtist.artistImageUrl != null) {
                        AsyncImage(
                            model = currentArtist.artistImageUrl,
                            contentDescription = currentArtist.name,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(60.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CoverImage(
                            coverArtId = currentArtist.coverArt,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(60.dp))
                        )
                    }
                    Text(
                        currentArtist.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "${currentArtist.albumCount} 张专辑",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tabs: 专辑 / 歌曲
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("专辑" to 0, "歌曲" to 1).forEach { (label, index) ->
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }

        when (selectedTab) {
            0 -> {
                // Albums tab
                if (!currentArtist.album.isNullOrEmpty()) {
                    items(currentArtist.album!!.chunked(2)) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { album ->
                                AlbumCard(
                                    album = album,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    onClick = { onAlbumClick(album.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无专辑",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            1 -> {
                // Songs tab
                if (allSongs.isNotEmpty()) {
                    item {
                        Text(
                            "${allSongs.size} 首歌曲",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                    items(allSongs) { song ->
                        SongItem(
                            song = song,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onSongClick(song, allSongs) }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无歌曲",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
