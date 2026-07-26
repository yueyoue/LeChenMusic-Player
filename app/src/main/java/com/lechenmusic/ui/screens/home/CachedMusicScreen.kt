package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.components.SongItemWithMenu
import com.lechenmusic.ui.components.CoverImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CachedMusicScreen(
    responsiveConfig: ResponsiveConfig? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {}
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val cachedSongs by viewModel.cachedSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    // Refresh cached songs when entering this screen
    LaunchedEffect(Unit) {
        viewModel.loadCachedSongs()
    }

    if (isTablet) {
        // ═══ 平板布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 标题 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = config!!.contentPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("缓存音乐", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.weight(1f))

                // 歌曲数
                Text(
                    "${cachedSongs.size} 首歌曲",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 播放全部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = config.contentPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable {
                        if (cachedSongs.isNotEmpty()) onSongClick(cachedSongs.first(), cachedSongs)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "播放全部 (${cachedSongs.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 歌曲列表 =====
            if (cachedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Download, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无缓存音乐", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("播放过的歌曲会自动缓存到本地", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = config.contentPadding,
                        end = config.contentPadding,
                        bottom = 160.dp
                    )
                ) {
                    items(cachedSongs) { song ->
                        CachedSongListItem(
                            song = song,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onSongClick(song, cachedSongs) }
                        )
                    }
                }
            }
        }
    } else {
        // ═══ 手机布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar - no extra padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text("缓存音乐", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${cachedSongs.size}首",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            if (cachedSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "暂无缓存音乐",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "播放过的歌曲会自动缓存到本地",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(cachedSongs) { song ->
                        SongItemWithMenu(
                            song = song,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            playlists = playlists,
                            onClick = { onSongClick(song, cachedSongs) },
                            onStar = { viewModel.star(song.id) },
                            onUnstar = { viewModel.unstar(song.id) },
                            onAddToPlaylist = { plId -> viewModel.addToPlaylist(plId, song.id) },
                            onCreatePlaylist = { name -> viewModel.createPlaylistAndAddSong(name, song.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CachedSongListItem(
    song: Song,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            CoverImage(
                coverArtId = song.coverArt ?: song.albumId,
                serverUrl = serverUrl,
                username = username,
                password = password,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp)
            )

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${song.artist} - ${song.album}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 时长
            Text(
                CachedFormatDuration(song.duration),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 更多操作
            Icon(
                Icons.Default.MoreVert, "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp).clickable { }
            )
        }
    }
}

private fun CachedFormatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
