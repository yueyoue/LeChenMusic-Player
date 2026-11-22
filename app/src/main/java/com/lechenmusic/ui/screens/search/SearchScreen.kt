package com.lechenmusic.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.SongItem
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    videoViewModel: VideoViewModel? = null,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onAudiobookClick: (String) -> Unit = {},
    onVideoClick: (String, String) -> Unit = { _, _ -> },
    onOnlineSearch: (String) -> Unit = {}
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val pad = config?.contentPadding ?: 20.dp
    val searchResults by viewModel.searchResults.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    // 热门搜索：来自服务端真实数据（最常播放专辑的歌手 / 有声书热门演播者），非硬编码演示词条
    val hotTags by viewModel.hotSearches.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHotSearches() }

    // 影视搜索状态
    val videoSearchResults by videoViewModel?.searchResults?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // 搜索框状态：rememberSaveable 跨“进详情页后返回”保留，但重新进入搜索页是新条目、从空开始
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    // 新进入搜索页时清掉上次的结果残留，避免“看起来又搜了一次”
    LaunchedEffect(Unit) {
        if (query.isBlank()) viewModel.search("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.search("")
                query = ""
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("搜索", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Search Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery
                    if (canStartSearch(newQuery)) {
                        viewModel.search(newQuery)
                        // 有声书搜索 (本地过滤)
                        // 影视搜索
                        videoViewModel?.search(newQuery)
                    } else {
                        // 空输入或未达到搜索门槛（如单个英文字母）：清掉上一次的结果，
                        // 避免输入框与列表内容不一致
                        viewModel.search("")
                        videoViewModel?.search("")
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索歌曲、有声书、影视") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable {
                            query = ""
                            viewModel.search("")
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tabs (音乐 / 有声书 / 影视)
        if (query.isNotBlank()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("音乐", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("有声书", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("影视", modifier = Modifier.padding(12.dp)) }
            }
        }

        // Results
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 160.dp)) {
            if (query.isBlank()) {
                // 搜全网入口：曲库缺歌时一键全网搜索下载入库（M6）
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { onOnlineSearch("") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("搜全网下载", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("曲库没有的歌，全网搜索自动下载入库", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Hot searches（真实数据，无数据时不展示，不再用硬编码演示词条）
                if (hotTags.isNotEmpty()) {
                    item {
                        Text(
                            "热门搜索",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }
                    item {
                        // 真正的流式布局：放不下的标签自动换行，避免被压缩成空白细条按钮
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hotTags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { query = tag; viewModel.search(tag) },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // 歌手搜索结果（横向头像列表，显示在歌曲顶部）
                        val artists = searchResults?.artist ?: emptyList()
                        if (artists.isNotEmpty()) {
                            item {
                                Text("歌手 (${artists.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(artists) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(72.dp).clickable { onArtistClick(artist.id) }
                                        ) {
                                            if (artist.artistImageUrl != null) {
                                                AsyncImage(model = artist.artistImageUrl, contentDescription = artist.name,
                                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)),
                                                    contentScale = ContentScale.Crop)
                                            } else {
                                                Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(32.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(artist.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }

                        // 音乐搜索结果：直接采用服务端 search3 的匹配结果。
                        // 服务端会先做大小写/标点归一化，再按词匹配标题/专辑/歌手/专辑歌手；
                        // 客户端若再按字面 contains(query) 过滤，会把服务端已匹配的歌误删，
                        // 例如库里「谁 (live)」搜「谁 live」、「谁(Live)」搜「谁 live」都会命中服务端却被丢掉。
                        val songs = searchResults?.song ?: emptyList()

                        // 歌曲
                        if (songs.isNotEmpty()) {
                            item {
                                Text("歌曲 (${songs.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(songs) { song ->
                                SongItem(song = song, serverUrl = serverUrl, username = username, password = password,
                                    onClick = { onSongClick(song, songs) })
                            }
                        }



                        // 专辑
                        val albums = searchResults?.album ?: emptyList()
                        if (albums.isNotEmpty()) {
                            item {
                                Text("专辑 (${albums.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(albums.chunked(2)) { row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEach { album ->
                                        AlbumCard(album = album, serverUrl = serverUrl, username = username, password = password,
                                            onClick = { onAlbumClick(album.id) }, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (!canStartSearch(query)) {
                                        // 单个英文字母等过短输入：提示继续输入，而不是误报「未找到」
                                        Text("继续输入以开始搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("未找到匹配的音乐", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(onClick = { onOnlineSearch(query) }) {
                                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("曲库没有？去全网搜")
                                        }
                                        Text("全网搜索 · 一键下载 · 自动刮削入库", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 有声书搜索结果 (本地过滤)
                        val q = query.lowercase()
                        val matchedBooks = audiobooks.filter {
                            it.title.contains(q, ignoreCase = true) ||
                            it.author.contains(q, ignoreCase = true) ||
                            it.narrator.contains(q, ignoreCase = true)
                        }
                        if (matchedBooks.isNotEmpty()) {
                            item {
                                Text("有声书 (${matchedBooks.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(matchedBooks) { book ->
                                val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id, book.updatedAt)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onAudiobookClick(book.id) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(52.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant) {
                                        if (coverUrl != null) {
                                            AsyncImage(model = coverUrl, contentDescription = null,
                                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            com.lechenmusic.ui.components.SourceBadge(path = book.libraryPath)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
                                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                                modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("未找到匹配的有声书", modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    2 -> {
                        // 影视搜索结果
                        if (videoSearchResults.isNotEmpty()) {
                            item {
                                Text("影视 (${videoSearchResults.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                            items(videoSearchResults) { video ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onVideoClick(video.source, video.id) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.width(48.dp).height(68.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant) {
                                        if (video.poster.isNotBlank()) {
                                            AsyncImage(model = video.poster, contentDescription = null,
                                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(video.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(buildString {
                                            if (video.year.isNotBlank()) append(video.year)
                                            if (video.sourceName.isNotBlank()) append(" · ${video.sourceName}")
                                        }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("未找到匹配的影视", modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * 是否已达到发起音乐/影视搜索的输入门槛。
 *
 * 单个汉字（含日文假名、韩文）本身就可能是完整歌名，必须允许 1 个字就搜索，
 * 否则「谁」「爱」这类一字歌名永远发不出请求；其他文字仍要求至少 2 个字符，
 * 与服务端 search3 对单字符查询的保护一致，避免整库级的过宽查询。
 */
private fun canStartSearch(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.length >= 2) return true
    return isCjkChar(trimmed[0])
}

/** 判断是否为 CJK 字符（汉字、日文假名、韩文音节），按码位区间判断，不依赖额外依赖库 */
private fun isCjkChar(c: Char): Boolean {
    val code = c.code
    return code in 0x4E00..0x9FFF ||   // CJK 统一表意文字
        code in 0x3400..0x4DBF ||      // CJK 扩展 A
        code in 0xF900..0xFAFF ||      // CJK 兼容表意文字
        code in 0x3040..0x30FF ||      // 日文平假名 / 片假名
        code in 0xAC00..0xD7AF         // 韩文音节
}
