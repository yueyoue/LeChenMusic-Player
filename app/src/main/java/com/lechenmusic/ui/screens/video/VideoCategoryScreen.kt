package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.lechenmusic.data.model.VideoInfo
import com.lechenmusic.ui.VideoViewModel

/**
 * 分类页面 - 搜索并展示某类影视内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCategoryScreen(
    viewModel: VideoViewModel,
    categoryType: String,
    onBack: () -> Unit,
    onVideoClick: (VideoInfo) -> Unit
) {
    val categoryResults by viewModel.categoryResults.collectAsState()
    val isLoading by viewModel.categoryLoading.collectAsState()

    val title = when (categoryType) {
        "movie" -> "电影"
        "tv" -> "剧集"
        "anime" -> "动漫"
        "variety" -> "综艺"
        else -> categoryType
    }

    val searchKeyword = when (categoryType) {
        "movie" -> "电影"
        "tv" -> "电视剧"
        "anime" -> "动漫"
        "variety" -> "综艺"
        else -> categoryType
    }

    LaunchedEffect(categoryType) {
        viewModel.searchCategory(searchKeyword)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (categoryResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "暂无${title}内容",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "请检查影视服务器搜索配置",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categoryResults.chunked(3)) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { video ->
                            Box(modifier = Modifier.weight(1f)) {
                                VideoCard(video = video, onClick = { onVideoClick(video) })
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
