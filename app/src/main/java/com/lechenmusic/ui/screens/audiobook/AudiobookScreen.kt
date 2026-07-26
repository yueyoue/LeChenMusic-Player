package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
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
        viewModel.loadNarrators()
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
        // ═══ 平板布局：按照模板设计 ═══
        val pad = config!!.contentPadding
        val gap = config.itemSpacing
        val narrators by viewModel.narrators.collectAsState()
        val audiobooksWithProgress by viewModel.audiobookWithProgress.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 顶部: 返回按钮 + 搜索栏 =====
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(280.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("搜索有声书、演播者...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (genreFilter != null) {
                // 有分类筛选时直接显示网格
                AudiobookGridSection(filteredBooks, config, serverUrl, username, password, audiobookError, genreFilter, onAudiobookClick)
            } else {
                // 无分类筛选时显示完整首页
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = pad, end = pad, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(config.sectionSpacing)
                ) {
                    // ── 热门推荐 Hero ──
                    val featured = audiobooks.maxByOrNull { it.chapterCount }
                    if (featured != null) {
                        item {
                            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, featured.id)
                            Box(
                                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(
                                    Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
                                )
                            ) {
                                if (coverUrl != null) {
                                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.3f)
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                Column(modifier = Modifier.align(Alignment.CenterStart).padding(24.dp)) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.2f)) {
                                        Text("🔥 热门", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(featured.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${featured.author} · ${featured.narrator}演播 · ${featured.chapterCount}集", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    // ── 继续收听（只显示有进度的） ──
                    val booksWithProgress = audiobooksWithProgress.filter { it.progress != null }
                    if (booksWithProgress.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("继续收听", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                booksWithProgress.take(4).forEach { bwp ->
                                    ContinueListeningCard(bwp, serverUrl, username, password) { onAudiobookClick(bwp.id) }
                                }
                            }
                        }
                    }

                    // ── 分类 ──
                    item {
                        Text("分类", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                    }
                    item {
                        TabletGenreGrid(onAudiobookClick)
                    }

                    // ── 热门演播者 ──
                    if (narrators.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("热门演播者", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                                Text("更多 ›", fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                narrators.take(6).forEach { narr ->
                                    NarratorCard(narr, serverUrl, username, password) { onAudiobookClick("narrator:${narr.name}") }
                                }
                            }
                        }
                    }

                    // ── 最近更新 ──
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("最近更新", fontSize = config.sectionTitleSize, fontWeight = FontWeight.Bold)
                            Text("更多 ›", fontSize = config.captionFontSize, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            audiobooks.sortedByDescending { it.createdAt }.take(6).forEach { book ->
                                AudiobookCard(book, serverUrl, username, password, 130.dp) { onAudiobookClick(book.id) }
                            }
                        }
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
        // Cover image - 18dp radius + chapter badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        .background(Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF3CAC)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
                }
            }
            // Chapter count badge
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    "${book.chapterCount}集",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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

// ═══ 平板有声书页面辅助组件 ═══

@Composable
private fun AudiobookGridSection(
    filteredBooks: List<com.lechenmusic.data.model.Audiobook>,
    config: ResponsiveConfig,
    serverUrl: String, username: String, password: String,
    audiobookError: String?, genreFilter: String?,
    onAudiobookClick: (String) -> Unit
) {
    val pad = config.contentPadding
    if (filteredBooks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (genreFilter != null) "暂无$genreFilter" else "暂无有声书", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                if (audiobookError != null) { Spacer(modifier = Modifier.height(8.dp)); Text(audiobookError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(config.gridColumns.coerceIn(3, 6)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = pad, end = pad, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
            verticalArrangement = Arrangement.spacedBy(config.itemSpacing)
        ) {
            items(filteredBooks, key = { it.id }) { book ->
                TabletAudiobookGridCard(book, serverUrl, username, password) { onAudiobookClick(book.id) }
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    bwp: com.lechenmusic.data.model.AudiobookWithProgress,
    serverUrl: String, username: String, password: String,
    onClick: () -> Unit
) {
    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
        Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
        Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))),
        Brush.linearGradient(listOf(Color(0xFF00E68A), Color(0xFF00B8D4)))
    )
    val gradient = gradients[bwp.id.hashCode().and(0x7FFFFFFF) % gradients.size]
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, bwp.id)
    val progressFraction = if (bwp.totalDuration > 0) bwp.progress?.position?.toFloat()?.div(bwp.totalDuration) ?: 0f else 0f
    val currentChapter = bwp.progress?.chapterNumber ?: 0

    Surface(
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(gradient),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MenuBook, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                }
                // 播放按钮
                Surface(modifier = Modifier.size(20.dp).align(Alignment.Center), shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bwp.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("第${currentChapter + 1}集 · 已听${(progressFraction * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                // 进度条
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TabletGenreGrid(onGenreClick: (String) -> Unit) {
    data class GenreItem(val emoji: String, val label: String, val genre: String, val color1: Color, val color2: Color, val count: Int)
    // count 会在运行时从实际数据计算
    val genres = listOf(
        GenreItem("📚", "有声书", "有声读物", Color(0xFFFF6B35), Color(0xFFFF3CAC), 0),
        GenreItem("🎤", "评书", "评书", Color(0xFF667eea), Color(0xFF764ba2), 0),
        GenreItem("😂", "相声", "相声", Color(0xFFFBBF24), Color(0xFFF59E0B), 0),
        GenreItem("🧸", "儿童", "儿童", Color(0xFF00E68A), Color(0xFF00B8D4), 0)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        genres.forEach { genre ->
            Surface(
                modifier = Modifier.weight(1f).height(80.dp).clickable { onGenreClick(genre.genre) },
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(genre.color1, genre.color2)))) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(genre.emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(genre.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NarratorCard(
    narr: com.lechenmusic.data.repository.MusicRepository.NarratorInfo,
    serverUrl: String, username: String, password: String,
    onClick: () -> Unit
) {
    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF3CAC))),
        Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))),
        Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
        Brush.linearGradient(listOf(Color(0xFF00E68A), Color(0xFF00B8D4)))
    )
    val gradient = gradients[narr.name.hashCode().and(0x7FFFFFFF) % gradients.size]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).width(80.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(narr.name.take(1), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(narr.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${narr.count}部作品", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudiobookCard(
    book: com.lechenmusic.data.model.Audiobook,
    serverUrl: String, username: String, password: String,
    coverSize: Dp,
    onClick: () -> Unit
) {
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
    Column(modifier = Modifier.clickable(onClick = onClick).width(coverSize)) {
        Box(
            modifier = Modifier.size(coverSize).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUrl != null) {
                AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))))) {
                    Icon(Icons.Default.MenuBook, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
                }
            }
            // 章数角标
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text("${book.chapterCount}集", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(book.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(book.author, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
