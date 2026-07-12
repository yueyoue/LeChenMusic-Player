package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookChapter
import com.lechenmusic.ui.MainViewModel
import coil.compose.AsyncImage
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl

@Composable
fun AudiobookDetailScreen(
    viewModel: MainViewModel,
    audiobookId: String,
    onBack: () -> Unit,
    onPlayChapter: (Audiobook, AudiobookChapter, List<AudiobookChapter>) -> Unit
) {
    val audiobookDetail by viewModel.audiobookDetail.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(audiobookId) {
        viewModel.loadAudiobookDetail(audiobookId)
    }

    var loadingTimeout by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(15000)
        if (audiobookDetail == null) loadingTimeout = true
    }

    val book = audiobookDetail?.book
    val chapters = audiobookDetail?.chapters ?: emptyList()
    val isStarred = book?.isStarred ?: false

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadingTimeout) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载失败", fontSize = 16.sp, color = Color.Gray)
                    Text("请检查网络连接后重试", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAudiobookDetail(audiobookId); loadingTimeout = false }) {
                        Text("重试")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Debug info (temporary)
        item {
            val progress = audiobookDetail?.progress
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1A1A2E)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("[DEBUG]", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    Text("book.id: ${book?.id}", fontSize = 10.sp, color = Color.White)
                    Text("starred字段: ${book?.starred ?: "null"}", fontSize = 10.sp, color = Color.White)
                    Text("isStarred: ${book?.isStarred}", fontSize = 10.sp, color = Color.White)
                    Text("progress: ${if (progress != null) "ch=${progress.chapterId.take(8)}.. pos=${progress.position}s" else "null (无进度数据)"}", fontSize = 10.sp, color = Color.White)
                    Text("chapters: ${chapters.size}", fontSize = 10.sp, color = Color.White)
                    if (progress == null) {
                        Text("⚠ 进度为null: 从未保存过或服务端未返回", fontSize = 10.sp, color = Color(0xFFFF6B6B))
                    }
                }
            }
        }

        // Header bar - match AlbumDetailScreen style
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text("有声书详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (isStarred) viewModel.unstarAudiobook(book.id)
                    else viewModel.starAudiobook(book.id)
                }) {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isStarred) "取消收藏" else "收藏",
                        tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Book info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier
                        .width(120.dp)
                        .height(160.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.author.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "作者: ${book.author}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (book.narrator.isNotBlank()) {
                        Text(
                            "演播: ${book.narrator}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${book.chapterCount}章",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (book.genre.isNotBlank()) {
                        Text(
                            book.genre,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Play buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { if (chapters.isNotEmpty()) onPlayChapter(book, chapters[0], chapters) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从头播放")
                }
                OutlinedButton(
                    onClick = { viewModel.resumeAudiobook(book) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("继续播放")
                }
            }
        }

        // Chapter list header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "章节列表",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Chapters
        itemsIndexed(chapters) { index, chapter ->
            ChapterItem(
                chapter = chapter,
                index = index + 1,
                onClick = { onPlayChapter(book, chapter, chapters) }
            )
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: AudiobookChapter,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$index",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapter.fileSize > 0) {
                    Text(
                        formatFileSize(chapter.fileSize),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
