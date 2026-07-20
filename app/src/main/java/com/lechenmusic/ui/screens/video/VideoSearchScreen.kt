package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.VideoInfo

@Composable
fun VideoSearchScreen(
    onBack: () -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    val searchHistory = remember { mutableStateListOf("三体", "流浪地球", "庆余年", "狂飙") }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) {
                        // TODO: 搜索API
                        hasSearched = true
                        searchResults = listOf(
                            VideoInfo(id = "s1", title = "${it}相关电影", year = "2025", type = "movie", rate = "8.0"),
                            VideoInfo(id = "s2", title = "${it}相关剧集", year = "2024", type = "tv", totalEpisodes = 24, rate = "7.5"),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索电影、电视剧、动漫") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; hasSearched = false }) {
                            Icon(Icons.Default.Close, "清除")
                        }
                    }
                }
            )
        }

        if (!hasSearched) {
            // 搜索历史
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("搜索历史", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "清除",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { searchHistory.clear() }
                    )
                }
                LazyRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHistory) { item ->
                        SuggestionChip(
                            onClick = { query = item; hasSearched = true },
                            label = { Text(item, fontSize = 13.sp) }
                        )
                    }
                }
            }

            // 热门搜索
            Text(
                "🔥 热门搜索",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            val hotTags = listOf("哪吒", "三体", "庆余年", "流浪地球", "狂飙", "繁花", "漫长的季节", "封神")
            LazyRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hotTags) { tag ->
                    SuggestionChip(
                        onClick = { query = tag; hasSearched = true },
                        label = { Text(tag, fontSize = 13.sp) }
                    )
                }
            }
        } else {
            // 搜索结果
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到相关影视", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(searchResults) { video ->
                        VideoSearchResultItem(video = video, onClick = { onVideoClick(video) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSearchResultItem(
    video: VideoInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面占位
        Surface(
            modifier = Modifier
                .width(60.dp)
                .height(80.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (video.cover.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = video.cover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        video.title.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (video.rate != null) {
                    Text(
                        "⭐ ${video.rate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    buildString {
                        append(video.year)
                        append(" · ")
                        append(
                            when (video.type) {
                                "movie" -> "电影"
                                "tv" -> "电视剧"
                                "show" -> "综艺"
                                "anime" -> "动漫"
                                else -> video.type
                            }
                        )
                        if (video.totalEpisodes > 1) append(" · ${video.totalEpisodes}集")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
