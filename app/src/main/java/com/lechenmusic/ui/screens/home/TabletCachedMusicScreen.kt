package com.lechenmusic.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletCachedMusicScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {}
) {
    val cachedSongs by viewModel.cachedSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCachedSongs()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部面包屑 + 歌曲数 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("首页", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" / ", color = MaterialTheme.colorScheme.outlineVariant)
                Text("缓存音乐", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 歌曲数
            Text(
                "${cachedSongs.size} 首歌曲",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 操作按钮
            Icon(Icons.Default.SwapVert, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.FilterList, "筛选", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 播放全部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 8.dp),
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

            // 下载/管理
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Download, "下载管理", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { })
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
                    Text("暂无缓存音乐", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("播放过的歌曲会自动缓存到本地", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = responsiveConfig.contentPadding,
                    end = responsiveConfig.contentPadding,
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
                formatDuration(song.duration),
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

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
