package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookWithProgress
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabletAudiobookHomeContent(
    audiobooks: List<Audiobook>,
    audiobookWithProgress: List<AudiobookWithProgress>,
    starredAudiobooks: List<Audiobook>,
    viewModel: MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    slides: List<com.lechenmusic.data.model.SlideConfig> = emptyList(),
    responsiveConfig: com.lechenmusic.ui.responsive.ResponsiveConfig? = null,
    onNavigateToAudiobook: (String?) -> Unit,
    onNavigateToAudiobookDetail: (String) -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToNarratorList: () -> Unit
) {
    val cardSize = responsiveConfig?.albumCardSize ?: 150.dp
    val narrators by viewModel.narrators.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ===== 分类导航按钮 (有声小说、相声、评书、儿童) =====
        item {
            val genreTabs = listOf(
                "有声小说" to "有声读物",
                "相声" to "相声",
                "评书" to "评书",
                "儿童" to "儿童"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                genreTabs.forEach { (label, genre) ->
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onNavigateToAudiobook(genre) }
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }

        // ===== 继续收听 (横向网格显示，和有声小说一样) =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("继续收听", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "最近播放 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook(null) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            val booksWithProgress = audiobookWithProgress.filter { it.progress != null && !it.progress.completed }
            val continueBooks = if (booksWithProgress.isNotEmpty()) booksWithProgress.map { it.toAudiobook() } else audiobooks.take(8)
            if (continueBooks.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(continueBooks.take(8)) { book ->
                        AudiobookCompactCard(
                            cardSize = cardSize,
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无收听记录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ===== 演播者 (Issue 9: 去掉背景框，上面头像下面名字，头像改大) =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("演播者", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "全部 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToNarratorList() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (narrators.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(narrators.take(8)) { narr ->
                        NarratorAvatarCard(
                            name = narr.name,
                            count = "${narr.count}部",
                            serverUrl = serverUrl,
                            onClick = { onNavigateToNarrator(narr.name) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== Issue 11: 最近更新已删除 =====

        // ===== 有声小说 =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("有声小说", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "更多 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook("有声读物") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            val novelBooks = audiobooks.filter { it.genre == "有声读物" || it.genre.isEmpty() }
            if (novelBooks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(novelBooks.take(8)) { book ->
                        AudiobookCompactCard(cardSize = cardSize,
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无有声小说", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 相声 =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("相声", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "更多 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook("相声") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            val xiangshengBooks = audiobooks.filter { it.genre == "相声" }
            if (xiangshengBooks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(xiangshengBooks.take(8)) { book ->
                        AudiobookCompactCard(cardSize = cardSize,
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无相声", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 评书 =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("评书", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "更多 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook("评书") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            val pingshuBooks = audiobooks.filter { it.genre == "评书" }
            if (pingshuBooks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(pingshuBooks.take(8)) { book ->
                        AudiobookCompactCard(cardSize = cardSize,
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无评书", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 儿童 =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("儿童", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "更多 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook("儿童") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            val childBooks = audiobooks.filter { it.genre == "儿童" }
            if (childBooks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(childBooks.take(8)) { book ->
                        AudiobookCompactCard(cardSize = cardSize,
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无儿童读物", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 热门榜单 =====
        item {
            Text("热门榜单", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            val popular = audiobooks.sortedByDescending { it.chapterCount }.take(5)
            popular.forEachIndexed { index, book ->
                PopularBookRow(
                    rank = index + 1,
                    book = book,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onNavigateToAudiobookDetail(book.id) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 底部留白给 MiniPlayer
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ==================== 继续收听 - 列表行 ====================
@Composable
private fun ContinueListeningListRow(
    book: Audiobook,
    progress: com.lechenmusic.data.model.AudiobookProgress?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                if (progress != null) {
                    Text(
                        "第 ${progress.chapterNumber} / ${book.chapterCount} 章",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 播放按钮
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, "播放", modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ==================== 继续收听 - 大卡片 (500x240) 封面全铺 ====================
@Composable
private fun ContinueListeningLargeCard(
    book: Audiobook,
    progress: com.lechenmusic.data.model.AudiobookProgress?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .width(500.dp)
            .height(240.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // 内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Text(
                        "正在收听",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    book.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (progress != null) {
                    val remainingChapter = book.chapterCount - progress.chapterNumber
                    Text(
                        "剩余约 ${remainingChapter} 章",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("继续", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==================== 继续收听 - glass-panel 卡片 (300x240) ====================
@Composable
private fun ContinueListeningGlassCard(
    book: Audiobook,
    progress: com.lechenmusic.data.model.AudiobookProgress?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
        modifier = Modifier
            .width(300.dp)
            .height(240.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部: 小封面 + 书名/分类
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
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
                                Icons.Default.MenuBook,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        book.genre.ifEmpty { book.narrator.ifEmpty { book.author } },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            // 中间: 进度条
            if (progress != null) {
                val progressFraction = (progress.chapterNumber.toFloat() / book.chapterCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                Column {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "已听 ${(progressFraction * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 底部: 播放按钮
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "播放",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ==================== 热门分类 glass-panel 卡片 (Issue 8: 修复边框) ====================
@Composable
private fun CategoryGlassCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==================== 演播者头像卡片 (Issue 9: 上面头像下面名字) ====================
@Composable
private fun NarratorAvatarCard(
    name: String,
    count: String,
    serverUrl: String,
    onClick: () -> Unit
) {
    val avatarUrl = com.lechenmusic.data.api.SubsonicApi.getNarratorAvatarUrl(serverUrl, name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        // 头像
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_menu_gallery)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 名字
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            count,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 书籍紧凑卡片 (192dp, 正方形封面) ====================
@Composable
private fun AudiobookCompactCard(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    cardSize: androidx.compose.ui.unit.Dp = 150.dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardSize)
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                        Icons.Default.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            book.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (book.chapterCount > 0) "${book.chapterCount} 集" else "完结",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ==================== 热门榜单行 ====================
@Composable
private fun PopularBookRow(
    rank: Int,
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(rank),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 封面
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
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
                            Icons.Default.MenuBook,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                Icons.Default.TrendingUp,
                "趋势",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
