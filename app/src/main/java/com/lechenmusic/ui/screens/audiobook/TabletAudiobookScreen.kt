package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    var selectedTab by remember { mutableIntStateOf(
        when (actualGenre) {
            "有声读物" -> 1
            "评书" -> 2
            "相声" -> 3
            "儿童" -> 4
            "starred" -> 5
            else -> 0
        }
    ) }

    val tabs = listOf(
        "全部" to null,
        "有声小说" to "有声读物",
        "评书" to "评书",
        "相声" to "相声",
        "儿童" to "儿童",
        "收藏" to "starred"
    )

    val baseBooks = when (val genre = tabs[selectedTab].second) {
        "starred" -> starredAudiobooks
        null -> audiobooks
        else -> audiobooks.filter { it.genre == genre }
    }
    val filteredBooks = if (searchQuery.isBlank()) baseBooks
        else baseBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.narrator.contains(searchQuery, ignoreCase = true)
        }

    val pad = responsiveConfig.contentPadding
    val cardSize = responsiveConfig.albumCardSize

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 顶部: 返回按钮 + 标题 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = pad, end = pad, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("有声书", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        // ===== 搜索栏 =====
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pad, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("搜索有声书名称、作者、演播者...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ===== 分类标签 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = pad, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEachIndexed { index, (label, _) ->
                val isSelected = selectedTab == index
                Surface(
                    onClick = { selectedTab = index },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        if (genreFilter != null) "暂无${tabs[selectedTab].first}" else "暂无有声书",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                    )
                    if (audiobookError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(audiobookError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // 根据屏幕宽度自适应列数，确保封面不会太大
            val columns = when {
                responsiveConfig.isExpanded -> 5
                else -> 4
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = pad,
                    end = pad,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    TabletAudiobookGridCard(
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
            shape = RoundedCornerShape(14.dp),
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
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF3CAC)))
                ), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook, null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 书名
        Text(
            book.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 演播者/作者
        Text(
            book.narrator.ifEmpty { book.author },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 章节数
        if (book.chapterCount > 0) {
            Text(
                "${book.chapterCount}集",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
