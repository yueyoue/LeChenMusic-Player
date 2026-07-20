package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Artist
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import kotlinx.coroutines.launch
import net.sourceforge.pinyin4j.PinyinHelper

@Composable
fun ArtistsScreen(
    viewModel: MainViewModel,
    onArtistClick: (String) -> Unit
) {
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
                Text(
                    "歌手",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
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
