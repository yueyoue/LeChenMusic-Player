package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookWithProgress
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@Composable
fun TabletAudiobookHomeContent(
    audiobooks: List<Audiobook>,
    audiobookWithProgress: List<AudiobookWithProgress>,
    starredAudiobooks: List<Audiobook>,
    viewModel: MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    onNavigateToAudiobook: (String?) -> Unit,
    onNavigateToAudiobookDetail: (String) -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToNarratorList: () -> Unit
) {
    val narrators by viewModel.narrators.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ===== 继续收听 =====
        val booksWithProgress = audiobookWithProgress.filter { it.progress != null && !it.progress.completed }
        if (booksWithProgress.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("继续收听", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "最近播放 ›",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToAudiobook(null) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    itemsIndexed(booksWithProgress.take(5)) { index, bwp ->
                        if (index == 0) {
                            // 第一个卡片: 大尺寸封面全铺 + 渐变遮罩
                            ContinueListeningLargeCard(
                                book = bwp.toAudiobook(),
                                progress = bwp.progress,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = {
                                    viewModel.resumeAudiobook(bwp.toAudiobook())
                                    onNavigateToAudiobookDetail(bwp.id)
                                }
                            )
                        } else {
                            // 后续卡片: glass-panel 风格
                            ContinueListeningGlassCard(
                                book = bwp.toAudiobook(),
                                progress = bwp.progress,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = {
                                    viewModel.resumeAudiobook(bwp.toAudiobook())
                                    onNavigateToAudiobookDetail(bwp.id)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ===== 热门分类 (6列网格) =====
        item {
            Text("热门分类", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "📖" to "历史",
                    "🔍" to "悬疑",
                    "🚀" to "科幻",
                    "👶" to "儿童",
                    "⚡" to "玄幻",
                    "📂" to "更多"
                )
                categories.forEach { (emoji, label) ->
                    CategoryGlassCard(
                        emoji = emoji,
                        label = label,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (label == "更多") onNavigateToAudiobook(null)
                            else onNavigateToAudiobook(label)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 演播者 =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎙️ 演播者", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "全部 ›",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToNarratorList() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (narrators.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(narrators.take(8)) { narr ->
                        NarratorChip(
                            name = narr.name,
                            count = "${narr.count}部",
                            onClick = { onNavigateToNarrator(narr.name) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 最近更新 (横向滚动 192dp 卡片) =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近更新", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "发现更多 ›",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook(null) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            val recent = audiobooks.sortedByDescending { it.updatedAt }.take(8)
            if (recent.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recent) { book ->
                        AudiobookCompactCard(
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 热门榜单 =====
        item {
            Text("🏆 热门榜单", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

        // ===== 我的收藏 (横向滚动 192dp 卡片) =====
        if (starredAudiobooks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ 我的收藏", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "全部 ›",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToAudiobook("starred") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(starredAudiobooks.take(8)) { book ->
                        AudiobookCompactCard(
                            book = book,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onNavigateToAudiobookDetail(book.id) }
                        )
                    }
                }
            }
        }

        // 底部留白给 MiniPlayer
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ==================== 继续收听 - 大卡片 (500x240) ====================
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
            // 封面图全铺
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
                            colors = listOf(
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
                    fontSize = 26.sp,
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
                        fontSize = 14.sp,
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
                    Text("继续", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ==================== 继续收听 - 中卡片 (300x240) glass-panel ====================
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        book.genre.ifEmpty { book.narrator.ifEmpty { book.author } },
                        fontSize = 13.sp,
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
                        fontSize = 12.sp,
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

// ==================== 热门分类 glass-panel 卡片 ====================
@Composable
private fun CategoryGlassCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== 演播者 Chip ====================
@Composable
private fun NarratorChip(
    name: String,
    count: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        name.take(1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==================== 书籍紧凑卡片 (192dp, 正方形封面) ====================
@Composable
private fun AudiobookCompactCard(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(192.dp)
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
            if (book.chapterCount > 0) "更新至 ${book.chapterCount} 集" else "完结",
            fontSize = 12.sp,
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
                    fontSize = 12.sp,
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
