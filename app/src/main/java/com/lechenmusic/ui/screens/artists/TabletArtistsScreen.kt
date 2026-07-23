package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Artist
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.launch
import net.sourceforge.pinyin4j.PinyinHelper

@Composable
fun TabletArtistsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onArtistClick: (String) -> Unit
) {
    val artists by viewModel.artists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadArtists()
    }

    // 按拼音首字母分组
    val grouped = remember(artists) {
        artists.groupBy { artist ->
            val first = artist.name.firstOrNull()
            if (first != null && first.code in 0x4E00..0x9FFF) {
                val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(first)
                if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                    pinyinArray[0][0].uppercaseChar().toString()
                } else "#"
            } else {
                val s = first?.uppercase() ?: "#"
                if (s[0] in 'A'..'Z') s else "#"
            }
        }.toSortedMap()
    }
    val letters = remember(grouped) { grouped.keys.toList() }

    // 当前选中的字母筛选
    var selectedLetter by remember { mutableStateOf("热门") }

    // 筛选后的列表
    val filteredArtists = remember(artists, selectedLetter) {
        if (selectedLetter == "热门") {
            artists.sortedByDescending { it.albumCount }
        } else {
            artists.filter { artist ->
                val first = artist.name.firstOrNull()
                if (first != null && first.code in 0x4E00..0x9FFF) {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(first)
                    val letter = if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        pinyinArray[0][0].uppercaseChar().toString()
                    } else "#"
                    letter == selectedLetter
                } else {
                    val s = first?.uppercase() ?: "#"
                    if (s[0] in 'A'..'Z') s == selectedLetter else selectedLetter == "#"
                }
            }
        }
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部面包屑 + 搜索栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 面包屑导航
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("乐库", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("歌手", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 搜索框
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.width(260.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索歌手...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 排序/筛选按钮
            Icon(Icons.Default.Sort, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.FilterList, "筛选", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
        }

        // ===== 字母快捷导航 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 热门按钮
            FilterChip(
                selected = selectedLetter == "热门",
                onClick = { selectedLetter = "热门" },
                label = { Text("热门", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            // A-Z + #
            val allLetters = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ#").map { it.toString() }
            allLetters.forEach { letter ->
                FilterChip(
                    selected = selectedLetter == letter,
                    onClick = {
                        selectedLetter = letter
                        // 滚动到对应位置
                        val idx = filteredArtists.indexOfFirst { a ->
                            val first = a.name.firstOrNull()
                            if (first != null && first.code in 0x4E00..0x9FFF) {
                                val py = PinyinHelper.toHanyuPinyinStringArray(first)
                                val l = if (py != null && py.isNotEmpty()) py[0][0].uppercaseChar().toString() else "#"
                                l == letter
                            } else {
                                val s = first?.uppercase() ?: "#"
                                if (s[0] in 'A'..'Z') s == letter else letter == "#"
                            }
                        }
                        if (idx >= 0) coroutineScope.launch { gridState.animateScrollToItem(idx) }
                    },
                    label = { Text(letter, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // ===== 歌手网格 =====
        LazyVerticalGrid(
            columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 6)),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsiveConfig.contentPadding,
                end = responsiveConfig.contentPadding,
                bottom = 160.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
            verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
        ) {
            items(filteredArtists, key = { it.id }) { artist ->
                ArtistGridCard(
                    artist = artist,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onArtistClick(artist.id) }
                )
            }
        }
    }
}

@Composable
private fun ArtistGridCard(
    artist: Artist,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isHovered) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isHovered) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圆形头像 + 播放覆盖层
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                val coverModifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                val effectiveCoverArt = artist.coverArt ?: artist.id

                if (artist.artistImageUrl != null) {
                    AsyncImage(
                        model = artist.artistImageUrl,
                        contentDescription = artist.name,
                        modifier = coverModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverImage(
                        coverArtId = effectiveCoverArt,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = coverModifier
                    )
                }

                // 播放覆盖层（hover/按压时显示）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isHovered) 0.2f else 0f))
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isHovered) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow, "播放",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 歌手名
            Text(
                artist.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 专辑数
            Text(
                "${artist.albumCount} 张专辑",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
