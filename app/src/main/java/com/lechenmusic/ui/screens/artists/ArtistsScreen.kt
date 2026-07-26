package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Artist
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.launch
import net.sourceforge.pinyin4j.PinyinHelper

@Composable
fun ArtistsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit = {},
    onArtistClick: (String) -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val pad = config?.contentPadding ?: 20.dp
    val artists by viewModel.artists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadArtists()
    }

    // Group artists by first letter for A-Z index
    val grouped = remember(artists) {
        artists.groupBy { artist ->
            val first = artist.name.firstOrNull()
            if (first != null && first.code in 0x4E00..0x9FFF) {
                // 中文字符转拼音首字母
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

    if (isTablet) {
        // ═══ 平板布局 ═══
        // 当前选中的字母筛选
        var selectedLetter by remember { mutableStateOf("热门") }
        var searchQuery by remember { mutableStateOf("") }

        // 筛选后的列表
        val filteredArtists = remember(artists, selectedLetter, searchQuery) {
            val letterFiltered = if (selectedLetter == "热门") {
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
            if (searchQuery.isBlank()) letterFiltered
            else letterFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val gridState = rememberLazyGridState()
        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 搜索栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("歌手", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.weight(1f))

                // 搜索框 (功能型)
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
                                        Text("搜索歌手...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // ===== 字母快捷导航 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 8.dp),
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
                columns = GridCells.Fixed(config!!.gridColumns.coerceIn(3, 6)),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = pad,
                    end = pad,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
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
    } else {
        // ═══ 手机布局 ═══
        // Flat list: each item is either a section header or an artist
        val flatList = remember(grouped) {
            val list = mutableListOf<Any>()
            for ((letter, artistList) in grouped) {
                list.add(letter) // header
                list.addAll(artistList) // artists
            }
            list
        }
        // Map letter -> index in flatList
        val letterIndexMap = remember(flatList) {
            val map = mutableMapOf<String, Int>()
            flatList.forEachIndexed { index, item ->
                if (item is String) map[item] = index
            }
            map
        }

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("歌手", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(21.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("搜索歌手", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }

                if (artists.isEmpty()) {
                    item {
                        Text(
                            "加载中...",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(flatList.size) { index ->
                    val item = flatList[index]
                    if (item is String) {
                        // Section header
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    } else if (item is Artist) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onArtistClick(item.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val coverModifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable { onArtistClick(item.id) }
                            val effectiveCoverArt = item.coverArt ?: item.id
                            if (item.artistImageUrl != null) {
                                AsyncImage(
                                    model = item.artistImageUrl,
                                    contentDescription = item.name,
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
                            Column(modifier = Modifier.padding(start = 14.dp)) {
                                Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${item.albumCount} 张专辑",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // A-Z index bar on the right side
            if (letters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    letters.forEach { letter ->
                        Text(
                            text = letter,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val targetIndex = letterIndexMap[letter] ?: return@clickable
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                                .padding(vertical = 1.dp)
                        )
                    }
                }
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
