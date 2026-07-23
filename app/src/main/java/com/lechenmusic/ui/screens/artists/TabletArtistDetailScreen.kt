package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletArtistDetailScreen(
    viewModel: MainViewModel,
    artistId: String,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val artist by viewModel.currentArtist.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(artistId) {
        viewModel.loadArtistDetail(artistId)
    }

    val currentArtist = artist ?: return

    // Tab 状态
    var selectedTab by remember { mutableStateOf(0) } // 0=歌曲, 1=专辑

    Row(modifier = Modifier.fillMaxSize()) {
        // ===== 左侧：歌手信息 (1/3) =====
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(IntrinsicSize.Min)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 歌手大图
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                if (currentArtist.artistImageUrl != null) {
                    AsyncImage(
                        model = currentArtist.artistImageUrl,
                        contentDescription = currentArtist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverImage(
                        coverArtId = currentArtist.coverArt ?: currentArtist.id,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 歌手名
            Text(
                currentArtist.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 专辑数
            Text(
                "${currentArtist.albumCount} 张专辑",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 关注按钮
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { }
            ) {
                Text(
                    "+ 关注",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp)
                )
            }
        }

        // ===== 右侧：歌曲/专辑列表 (2/3) =====
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = responsiveConfig.contentPadding)) {
            // Tab 栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                listOf("歌曲", "专辑").forEachIndexed { index, title ->
                    Column(
                        modifier = Modifier.clickable { selectedTab = index },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            title,
                            fontSize = 18.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (selectedTab == index) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }

            // 操作栏：播放全部 + 排序/下载
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 播放全部
                val playList = if (selectedTab == 0) artistSongs else (currentArtist.album?.flatMap { emptyList<Song>() } ?: emptyList())
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable {
                        if (artistSongs.isNotEmpty()) onSongClick(artistSongs.first(), artistSongs)
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
                            "播放全部 (${if (selectedTab == 0) artistSongs.size else currentArtist.album?.size ?: 0})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 排序/下载
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SwapVert, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { })
                    Icon(Icons.Default.Download, "下载", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容区域
            when (selectedTab) {
                0 -> {
                    // 歌曲列表（两列）
                    if (artistSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // 两列网格显示歌曲
                        val chunkedSongs = artistSongs.chunked(2)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 160.dp)
                        ) {
                            itemsIndexed(chunkedSongs) { _, rowSongs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowSongs.forEach { song ->
                                        SongListItem(
                                            song = song,
                                            onClick = { onSongClick(song, artistSongs) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // 补齐空位
                                    if (rowSongs.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // 专辑列表 - 封面大图网格显示
                    if (currentArtist.album.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val albumList = currentArtist.album!!
                        val albumColumns = 3 // 固定3列让封面更大
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(albumColumns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                            verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
                        ) {
                            items(albumList.size) { idx ->
                                val album = albumList[idx]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAlbumClick(album.id) }
                                ) {
                                    CoverImage(
                                        coverArtId = album.coverArt ?: album.id,
                                        serverUrl = serverUrl,
                                        username = username,
                                        password = password,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        album.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${album.songCount} 首歌曲",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            // hover 时显示的操作按钮（简化版，始终显示）
            Icon(
                Icons.Default.MoreVert, "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
