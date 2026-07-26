package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel

@Composable
fun VideoSearchScreen(
    viewModel: VideoViewModel,
    responsiveConfig: com.lechenmusic.ui.responsive.ResponsiveConfig? = null,
    onBack: () -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.searchLoading.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    if (isTablet) {
        // ═══ 平板布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // 搜索栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = config!!.contentPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("搜索影视...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) viewModel.search(query) })
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { if (query.isNotBlank()) viewModel.search(query) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("搜索")
                }
            }

            // 结果
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (searchResults.isEmpty() && query.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("未找到相关影视", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.gridColumns.coerceIn(3, 6)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = config.contentPadding,
                        end = config.contentPadding,
                        bottom = 160.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
                ) {
                    items(searchResults) { video ->
                        VideoGridCard(video = video, onClick = { onVideoClick(video) })
                    }
                }
            }
        }
    } else {
        // ═══ 手机布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // 搜索栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.clearSearchResults()
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索电影、电视剧、动漫") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                viewModel.clearSearchResults()
                            }) {
                                Icon(Icons.Default.Close, "清除")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) {
                                viewModel.search(query)
                            }
                        }
                    )
                )
            }

            if (searchResults.isEmpty() && !isLoading) {
                // 搜索历史
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("搜索历史", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "清除",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        searchHistory.forEach { item ->
                            SuggestionChip(
                                onClick = {
                                    query = item
                                    viewModel.search(item)
                                },
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    hotTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {
                                query = tag
                                viewModel.search(tag)
                            },
                            label = { Text(tag, fontSize = 13.sp) }
                        )
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 搜索结果
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
        // 封面
        Surface(
            modifier = Modifier
                .width(60.dp)
                .height(80.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (video.displayCover.isNotBlank()) {
                AsyncImage(
                    model = video.displayCover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        video.title.take(1),
                        fontSize = 16.sp,
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
                        append(categoryName(video.type))
                        if (video.displaySourceName.isNotBlank()) append(" · ${video.displaySourceName}")
                        if (video.displayTotalEpisodes > 1) append(" · ${video.displayTotalEpisodes}集")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (video.desc.isNotBlank()) {
                Text(
                    video.desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: VideoInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
        ) {
            if (video.poster.isNotBlank()) {
                AsyncImage(
                    model = video.poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(video.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (video.year.isNotBlank()) {
            Text(video.year, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
