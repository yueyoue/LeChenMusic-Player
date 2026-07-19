package com.lechenmusic.ui.screens.artists

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.components.SongMoreMenuButton

@Composable
fun ArtistDetailScreen(
    viewModel: MainViewModel,
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit = {}
) {
    val artist by viewModel.currentArtist.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=专辑, 1=歌曲

    LaunchedEffect(artistId) { viewModel.loadArtistDetail(artistId) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val currentArtist = artist ?: return

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        // #10: Background image with gradient fade
        item {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                // Background image
                if (currentArtist.artistImageUrl != null) {
                    AsyncImage(
                        model = currentArtist.artistImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Gradient overlay: visible at top, transparent at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                // Back button
                IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(4.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                // Artist info at bottom
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentArtist.artistImageUrl != null) {
                        AsyncImage(
                            model = currentArtist.artistImageUrl,
                            contentDescription = currentArtist.name,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(50.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CoverImage(
                            coverArtId = currentArtist.coverArt,
                            serverUrl = serverUrl, username = username, password = password,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(50.dp))
                        )
                    }
                    Text(currentArtist.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text("${currentArtist.albumCount} 张专辑", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Tab row: 专辑 / 歌曲
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("专辑 (${currentArtist.album?.size ?: 0})", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("歌曲 (${artistSongs.size})", modifier = Modifier.padding(12.dp))
                }
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                // Albums
                if (!currentArtist.album.isNullOrEmpty()) {
                    items(currentArtist.album!!.chunked(2)) { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { album ->
                                AlbumCard(album = album, serverUrl = serverUrl, username = username, password = password, onClick = { onAlbumClick(album.id) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            1 -> {
                // Songs
                if (artistSongs.isNotEmpty()) {
                    items(artistSongs) { song ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSongClick(song, artistSongs) }.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverImage(coverArtId = song.coverArt ?: song.albumId, serverUrl = serverUrl, username = username, password = password, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.album, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            SongMoreMenuButton(
                                song = song, playlists = playlists, isStarred = song.isStarred,
                                onFavorite = { if (song.isStarred) viewModel.unstar(song.id) else viewModel.star(song.id) },
                                onAddToPlaylist = { pid: String -> viewModel.addToPlaylist(pid, song.id) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onGoToArtist = { if (song.artistId.isNotBlank()) onArtistClick(song.artistId) },
                                onGoToAlbum = { if (song.albumId.isNotBlank()) onAlbumClick(song.albumId) },
                                onCreatePlaylist = { name: String -> viewModel.createPlaylistAndAddSong(name, song.id) }
                            )
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
