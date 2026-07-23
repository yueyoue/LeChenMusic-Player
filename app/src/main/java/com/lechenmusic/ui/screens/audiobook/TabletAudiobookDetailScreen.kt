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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookChapter
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletAudiobookDetailScreen(
    viewModel: MainViewModel,
    audiobookId: String,
    responsiveConfig: ResponsiveConfig,
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
                    Icon(Icons.Default.Error, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载失败", fontSize = 16.sp, color = Color.Gray)
                    Text("请检查网络连接后重试", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAudiobookDetail(audiobookId); loadingTimeout = false }) {
                        Text("重试")
                    }
                }
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // ===== 左侧：有声书信息 (1/3) =====
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(IntrinsicSize.Min)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 封面大图
            Surface(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(24.dp),
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
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 书名
            Text(
                book.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 作者
            if (book.author.isNotBlank()) {
                Text(
                    "作者: ${book.author}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 演播者
            if (book.narrator.isNotBlank()) {
                Text(
                    "演播: ${book.narrator}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 章节数 + 分类
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "${book.chapterCount}章",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (book.genre.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            book.genre,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 收藏
                IconButton(onClick = {
                    if (isStarred) viewModel.unstarAudiobook(book.id)
                    else viewModel.starAudiobook(book.id)
                }) {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "收藏",
                        tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 播放按钮
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (chapters.isNotEmpty()) onPlayChapter(book, chapters[0], chapters) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从头播放", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.resumeAudiobook(book) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("继续播放")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内容简介
            if (book.description.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("内容简介", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            book.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.description.length > 120) {
                            Text(
                                if (expanded) "收起" else "展开全部",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { expanded = !expanded }
                            )
                        }
                    }
                }
            }
        }

        // ===== 右侧：章节列表 =====
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // 章节标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "章节列表 (${chapters.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SwapVert, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { })
                    Icon(Icons.Default.Download, "下载", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { })
                }
            }

            // 章节列表
            if (chapters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无章节", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 160.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        ChapterListItem(
                            chapter = chapter,
                            index = index + 1,
                            onClick = { onPlayChapter(book, chapter, chapters) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: AudiobookChapter,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                "$index",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )

            // 章节信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    fontSize = 15.sp,
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

            // 时长
            if (chapter.duration > 0) {
                Text(
                    formatDuration(chapter.duration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 播放按钮
            Icon(
                Icons.Default.PlayArrow, "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
