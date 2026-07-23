package com.lechenmusic.ui.screens.home

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
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletAllPlaylistsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部面包屑 + 搜索栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("歌单", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "${playlists.size} 个歌单",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 歌单网格 =====
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无歌单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 6)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = responsiveConfig.contentPadding,
                    end = responsiveConfig.contentPadding,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistGridCard(
                        playlist = playlist,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        onClick = { onPlaylistClick(playlist.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: Playlist,
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
        CoverImage(
            coverArtId = playlist.coverArt,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 歌单名
        Text(
            playlist.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 歌曲数
        Text(
            "${playlist.songCount} 首歌曲",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
