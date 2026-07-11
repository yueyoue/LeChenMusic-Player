package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.ui.MainViewModel

@Composable
fun AudiobookScreen(
    viewModel: MainViewModel,
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

    val filteredBooks = when (genreFilter) {
        "starred" -> starredAudiobooks
        null -> audiobooks
        else -> audiobooks.filter { it.genre == genreFilter }
    }

    val title = when (genreFilter) {
        "starred" -> "⭐ 收藏的有声书"
        null -> "📖 全部有声书"
        else -> "📖 $genreFilter"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header - match AlbumDetailScreen style
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "请检查: 1)服务器地址 2)用户名密码 3)网络连接",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "请在媒体库中添加有声书目录并扫描",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks) { book ->
                    AudiobookCard(
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
fun AudiobookCard(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .width(80.dp)
                    .height(107.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.author.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        book.author,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (book.narrator.isNotBlank()) {
                    Text(
                        "演播: ${book.narrator}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${book.chapterCount}章",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (book.totalDuration > 0) {
                        Text(
                            " · ${formatDuration(book.totalDuration)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
