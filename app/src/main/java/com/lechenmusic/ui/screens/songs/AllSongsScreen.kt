package com.lechenmusic.ui.screens.songs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItemWithMenu
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun AllSongsScreen(
    viewModel: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    responsiveConfig: ResponsiveConfig? = null
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val isLoading by viewModel.allSongsLoading.collectAsState()
    val loadError by viewModel.allSongsLoadError.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val context = LocalContext.current
    var initialLoadTriggered by remember { mutableStateOf(false) }
    val isTablet = responsiveConfig != null && (responsiveConfig.isMedium || responsiveConfig.isExpanded)

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(Unit) {
        if (!initialLoadTriggered) {
            initialLoadTriggered = true
            viewModel.loadAllSongs(showToast = true)
        }
    }

    // Search state for tablet
    var searchQuery by remember { mutableStateOf("") }
    val displayedSongs = if (isTablet && searchQuery.isNotBlank()) {
        allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    } else allSongs

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            if (isTablet) {
                // ===== 平板顶部: 返回按钮 + 标题 + 搜索框 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = responsiveConfig!!.contentPadding, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("乐库", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.weight(1f))

                    // 搜索框
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
                                            Text("搜索歌曲...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            } else {
                // 手机标题
                Text(
                    "歌曲",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        if (displayedSongs.isNotEmpty()) {
            item {
                val pad = if (isTablet) responsiveConfig!!.contentPadding else 20.dp
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${displayedSongs.size} 首歌曲",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("加载中...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            items(displayedSongs) { song ->
                SongItemWithMenu(
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    playlists = playlists,
                    onClick = { onSongClick(song, displayedSongs) },
                    onStar = { viewModel.star(song.id); Toast.makeText(context, "收藏成功", Toast.LENGTH_SHORT).show() },
                    onUnstar = { viewModel.unstar(song.id); Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show() },
                    onAddToPlaylist = { plId -> viewModel.addToPlaylist(plId, song.id) },
                    onCreatePlaylist = { name -> viewModel.createPlaylistAndAddSong(name, song.id) },
                    onAddToQueue = {
                        viewModel.playerManager.addToQueue(song)
                        Toast.makeText(context, "已添加到播放队列", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToArtist = { onArtistClick(song.artistId) },
                    onNavigateToAlbum = { onAlbumClick(song.albumId) }
                )
            }
        } else if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "加载中，请稍等...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "首次加载需要同步服务器歌曲列表",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else if (loadError != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "加载失败",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        loadError ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAllSongs() }) {
                        Text("重试")
                    }
                }
            }
        } else {
            item {
                Text(
                    "暂无歌曲",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
