package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@Composable
fun AudiobookScreen(
    viewModel: MainViewModel,
    genreFilter: String? = null,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit,
    onAudiobookClick: (String) -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
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
        "starred" -> if (isTablet) "收藏的有声书" else "⭐ 收藏的有声书"
        null -> if (isTablet) "全部有声书" else "📖 全部有声书"
        else -> if (isTablet) (genreFilter ?: "全部有声书") else "📖 $genreFilter"
    }

    if (isTablet) {
        // ═══ 平板布局 ═══
        val pad = config!!.contentPadding

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 搜索栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

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
                    columns = GridCells.Fixed(config.gridColumns.coerceIn(3, 6)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = pad,
                        end = pad,
                        bottom = 160.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
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
    } else {
        // ═══ 手机布局 ═══
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Search field
            if (genreFilter == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = { Text("搜索有声书名称、作者、演播者...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (genreFilter != null) "暂无$genreFilter" else "暂无有声书",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        if (audiobookError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                audiobookError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                // Album grid style - 2 columns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredBooks) { book ->
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
}

@Composable
fun AudiobookGridCard(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Cover image - square with rounded corners
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Title
        Text(
            text = book.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Subtitle: narrator or author
        Text(
            text = book.narrator.ifEmpty { book.author },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
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

fun getAudiobookCoverUrl(serverUrl: String, username: String, password: String, bookId: String): String? {
    val normalizedUrl = serverUrl.trimEnd('/')
    val encodedPass = if (password.startsWith("enc:")) password
                      else "enc:${password.toByteArray().joinToString("") { "%02x".format(it) }}"
    return "$normalizedUrl/api/audiobook/$bookId/cover?u=$username&p=$encodedPass"
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
