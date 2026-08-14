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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun ArtistDetailScreen(
    viewModel: MainViewModel,
    artistId: String,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val artist by viewModel.currentArtist.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(artistId) {
        viewModel.loadArtistDetail(artistId)
    }

    val currentArtist = artist ?: return

    if (isTablet) {
        // ═══ 平板布局 ═══
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
                // 返回按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
            }

            // ===== 右侧：歌曲/专辑列表 (2/3) =====
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = config!!.contentPadding)) {
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
                                            TabletSongListItem(
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
                                horizontalArrangement = Arrangement.spacedBy(config!!.itemSpacing),
                                verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
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
    } else {
        // ═══ 手机布局 ═══
        var selectedTab by remember { mutableStateOf(0) } // 0=歌曲, 1=专辑

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
                        modifier = Modifier.padding(4.dp)
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
                            "${currentArtist.albumCount} 张专辑 · ${artistSongs.size} 首歌曲",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab 栏（歌曲 / 专辑）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    listOf("歌曲 (${artistSongs.size})", "专辑 (${currentArtist.album?.size ?: 0})").forEachIndexed { index, title ->
                        Column(
                            modifier = Modifier.clickable { selectedTab = index },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                title,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (selectedTab == index) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }

            // 播放全部按钮
            item {
                val currentList = if (selectedTab == 0) artistSongs else emptyList()
                if (currentList.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable { onSongClick(currentList.first(), currentList) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("播放全部 (${currentList.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 内容区域
            when (selectedTab) {
                0 -> {
                    // 歌曲列表
                    if (artistSongs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(artistSongs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongClick(song, artistSongs) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverImage(
                                    coverArtId = song.coverArt ?: song.albumId,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val qualityText = com.lechenmusic.ui.components.getQualityText(song)
                                        if (qualityText.isNotEmpty()) {
                                            Surface(shape = RoundedCornerShape(3.dp), color = com.lechenmusic.ui.components.getQualityColor(song).copy(alpha = 0.15f)) {
                                                Text(qualityText, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = com.lechenmusic.ui.components.getQualityColor(song), modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(song.album, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(song.durationFormatted, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                1 -> {
                    // 专辑列表
                    if (!currentArtist.album.isNullOrEmpty()) {
                        items(currentArtist.album!!.chunked(2)) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.forEach { album ->
                                    Column(
                                        modifier = Modifier.weight(1f).clickable { onAlbumClick(album.id) }
                                    ) {
                                        CoverImage(
                                            coverArtId = album.coverArt ?: album.id,
                                            serverUrl = serverUrl,
                                            username = username,
                                            password = password,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                        )
                                        Text(
                                            album.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        Text(
                                            album.artist,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
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
            }
        }
    }
}

@Composable
private fun TabletSongListItem(
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
            Icon(
                Icons.Default.MoreVert, "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
