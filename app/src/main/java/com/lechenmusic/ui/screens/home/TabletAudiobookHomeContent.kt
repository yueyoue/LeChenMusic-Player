package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        // ===== 继续收听 =====
        val booksWithProgress = audiobookWithProgress.filter { it.progress != null && !it.progress.completed }
        if (booksWithProgress.isNotEmpty()) {
            Text("⏱️ 继续收听", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(booksWithProgress.take(5)) { bwp ->
                    ContinueListeningCard(
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
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ===== 分类网格 =====
        Text("📂 分类", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "📖" to "有声读物",
                "🎤" to "评书",
                "😂" to "相声",
                "👶" to "儿童"
            ).forEach { (emoji, genre) ->
                val count = audiobooks.count { it.genre == genre || (genre == "有声读物" && it.genre.isEmpty()) }
                CategoryCard(
                    emoji = emoji,
                    label = genre,
                    count = "$count 部",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToAudiobook(genre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== 演播者 =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎙️ 演播者", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("全部 ›", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToNarratorList() })
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

        // ===== 最近更新 =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🆕 最近更新", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("更多 ›", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToAudiobook(null) })
        }
        Spacer(modifier = Modifier.height(12.dp))
        val recent = audiobooks.sortedByDescending { it.updatedAt }.take(6)
        if (recent.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                recent.forEach { book ->
                    AudiobookGridItem(
                        book = book,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToAudiobookDetail(book.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== 热门榜单 =====
        Text("🏆 热门榜单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

        // ===== 收藏 =====
        if (starredAudiobooks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐ 我的收藏", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("全部 ›", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToAudiobook("starred") })
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(starredAudiobooks.take(6)) { book ->
                    AudiobookGridItem(
                        book = book,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        onClick = { onNavigateToAudiobookDetail(book.id) }
                    )
                }
            }
        }

        // 底部留白给 MiniPlayer
        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun ContinueListeningCard(
    book: Audiobook,
    progress: com.lechenmusic.data.model.AudiobookProgress?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(280.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp)) {
                if (coverUrl != null) {
                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.narrator.ifEmpty { book.author }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (progress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (progress.chapterNumber.toFloat() / book.chapterCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    emoji: String,
    label: String,
    count: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(count, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
            Surface(shape = RoundedCornerShape(50), modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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

@Composable
private fun AudiobookGridItem(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        ) {
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            if (coverUrl != null) {
                AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(book.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.narrator.ifEmpty { book.author }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 封面
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(44.dp)) {
                if (coverUrl != null) {
                    AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            Icon(Icons.Default.PlayArrow, "播放", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
    }
}
