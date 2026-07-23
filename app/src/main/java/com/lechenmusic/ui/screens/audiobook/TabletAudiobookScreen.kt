package com.lechenmusic.ui.screens.audiobook

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletAudiobookScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    genreFilter: String? = null,
    onBack: () -> Unit,
    onAudiobookClick: (String) -> Unit
) {
    val audiobooks by viewModel.audiobooks.collectAsState()
    val audiobookError by viewModel.audiobookError.collectAsState()
    val starredAudiobooks by viewModel.starredAudiobooks.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAudiobooks()
        viewModel.loadStarredAudiobooks()
    }

    val actualGenre = when (genreFilter) {
        "有声小说" -> "有声读物"
        "儿童读物" -> "儿童"
        else -> genreFilter
    }

    var searchQuery by remember { mutableStateOf("") }

    val baseBooks = when (actualGenre) {
        "starred" -> starredAudiobooks
        null -> audiobooks
        else -> audiobooks.filter { it.genre == actualGenre }
    }
    val filteredBooks = if (searchQuery.isBlank()) baseBooks
        else baseBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.narrator.contains(searchQuery, ignoreCase = true)
    }

    val title = when (genreFilter) {
        "starred" -> "收藏的有声书"
        null -> "全部有声书"
        else -> genreFilter ?: "全部有声书"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部面包屑 + 搜索栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("有声书", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" / ", color = MaterialTheme.colorScheme.outlineVariant)
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 搜索框
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.width(280.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索有声书名称、作者、演播者...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(Icons.Default.Sort, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.FilterList, "筛选", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 有声书网格 =====
        if (filteredBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MenuBook, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (genreFilter != null) "暂无$genreFilter" else "暂无有声书",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                    )
                    if (audiobookError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(audiobookError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
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
                items(filteredBooks, key = { it.id }) { book ->
                    AudiobookGridCard(
                        book = book,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        onClick = { onAudiobookClick(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletAudiobookGridCard(
    book: Audiobook,
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 书名
        Text(
            book.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 演播者/作者
        Text(
            book.narrator.ifEmpty { book.author },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
