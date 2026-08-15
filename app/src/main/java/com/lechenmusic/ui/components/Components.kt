package com.lechenmusic.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song

@Composable
fun CoverImage(
    coverArtId: String?,
    serverUrl: String,
    username: String,
    password: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val url = ApiClient.getCoverArtUrl(serverUrl, username, password, coverArtId)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        CoverImage(
            coverArtId = album.coverArt,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = album.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = album.artist,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun SongItem(
    song: Song,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            coverArtId = song.coverArt ?: song.albumId,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quality badge
                val qualityText = getQualityText(song)
                if (qualityText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = getQualityColor(song).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = qualityText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = getQualityColor(song),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} · ${song.album}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Text(
                text = song.durationFormatted,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getQualityText(song: Song): String {
    val suffix = song.suffix.uppercase()
    val bitRate = song.bitRate
    return when {
        suffix == "FLAC" -> if (bitRate > 0) "FLAC ${bitRate}K" else "FLAC"
        suffix == "DSD" -> "DSD"
        suffix == "WAV" || suffix == "AIFF" -> suffix
        suffix == "MP3" && bitRate >= 320 -> "MP3 320K"
        suffix == "AAC" && bitRate >= 256 -> "AAC ${bitRate}K"
        suffix.isNotEmpty() && bitRate > 0 -> "$suffix ${bitRate}K"
        suffix.isNotEmpty() -> suffix
        else -> ""
    }
}

fun getQualityColor(song: Song): Color {
    val suffix = song.suffix.uppercase()
    return when {
        suffix == "FLAC" || suffix == "DSD" || suffix == "WAV" || suffix == "AIFF" -> Color(0xFFFF6B81) // Red for lossless
        song.bitRate >= 320 -> Color(0xFF5352ED) // Purple for high bitrate
        else -> Color(0xFF2ED573) // Green for normal
    }
}

// ==================== Skip 30s Buttons ====================
// Using Material Icons: Replay30 / Forward30

@Composable
fun SkipForward30Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Forward30,
            contentDescription = "前进30秒",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SkipBackward30Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Replay30,
            contentDescription = "后退30秒",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onAction?.invoke() }
            )
        }
    }
}

// ==================== Song Context Menu (Three-dot Menu) ====================

@Composable
fun SongContextMenu(
    song: Song,
    playlists: List<com.lechenmusic.data.model.Playlist>,
    isStarred: Boolean = song.isStarred,
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: ((String) -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onNavigateToArtist: (() -> Unit)? = null,
    onNavigateToAlbum: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isStarred) "取消收藏" else "收藏") },
                onClick = {
                    expanded = false
                    if (isStarred) onUnstar() else onStar()
                },
                leadingIcon = {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isStarred) Color(0xFFE94560) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("添加到歌单") },
                onClick = { expanded = false; showPlaylistDialog = true },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            // 添加到播放队列
            DropdownMenuItem(
                text = { Text("添加到播放队列") },
                onClick = { expanded = false; onAddToQueue?.invoke() },
                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            // 歌手
            DropdownMenuItem(
                text = { Text("歌手") },
                onClick = { expanded = false; onNavigateToArtist?.invoke() },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            // 专辑
            DropdownMenuItem(
                text = { Text("专辑") },
                onClick = { expanded = false; onNavigateToAlbum?.invoke() },
                leadingIcon = { Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }
    }

    // 添加到歌单弹窗
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("添加到歌单") },
            text = {
                Column {
                    // 新建歌单按钮
                    if (onCreatePlaylist != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPlaylistDialog = false; showCreateDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("新建歌单", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // 歌单列表
                    if (playlists.isEmpty()) {
                        Text("暂无歌单", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        playlists.forEach { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPlaylistDialog = false
                                        onAddToPlaylist(pl.id)
                                    },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${pl.songCount}首", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("取消") }
            }
        )
    }

    // 新建歌单弹窗
    if (showCreateDialog && onCreatePlaylist != null) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建歌单") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("歌单名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onCreatePlaylist(newName)
                        showCreateDialog = false
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun SongItemWithMenu(
    song: Song,
    serverUrl: String,
    username: String,
    password: String,
    playlists: List<com.lechenmusic.data.model.Playlist>,
    isStarred: Boolean = song.isStarred,
    onClick: () -> Unit,
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: ((String) -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onNavigateToArtist: (() -> Unit)? = null,
    onNavigateToAlbum: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            coverArtId = song.coverArt ?: song.albumId,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quality badge
                val qualityText = getQualityText(song)
                if (qualityText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = getQualityColor(song).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = qualityText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = getQualityColor(song),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} · ${song.album}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Text(
                text = song.durationFormatted,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SongContextMenu(
            song = song,
            playlists = playlists,
            isStarred = isStarred,
            onStar = onStar,
            onUnstar = onUnstar,
            onAddToPlaylist = onAddToPlaylist,
            onCreatePlaylist = onCreatePlaylist,
            onAddToQueue = onAddToQueue,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToAlbum = onNavigateToAlbum
        )
    }
}

// ==================== 共享定时器对话框 ====================

/**
 * 睡眠定时器对话框 — 音乐和有声书共用
 * @param timerMinutes 当前设定的定时分钟数，0表示未设定
 * @param onSetTimer 设置定时回调，传0表示取消
 * @param onDismiss 关闭对话框回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    timerMinutes: Int,
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Timer, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("睡眠定时", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // 当前定时状态
                if (timerMinutes > 0) {
                    Text(
                        "当前定时：${timerMinutes} 分钟",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 预设按钮（两行）
                val presets = listOf(15 to "15分钟", 30 to "30分钟", 45 to "45分钟", 60 to "60分钟", 90 to "90分钟")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    presets.take(3).forEach { (min, label) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); onDismiss() }
                        ) {
                            Text(label, fontSize = 13.sp,
                                color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    presets.drop(3).forEach { (min, label) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (timerMinutes == min) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(min); onDismiss() }
                        ) {
                            Text(label, fontSize = 13.sp,
                                color = if (timerMinutes == min) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                    // 取消按钮
                    if (timerMinutes > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            modifier = Modifier.padding(horizontal = 4.dp).clickable { onSetTimer(0); onDismiss() }
                        ) {
                            Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // 自定义时间输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("自定义分钟数", fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Button(
                        onClick = {
                            val min = customMinutes.toIntOrNull()
                            if (min != null && min > 0) {
                                onSetTimer(min)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("设置", fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

/**
 * 定时器倒计时显示条 — 底部悬浮显示
 * @param timerMinutes 当前设定的定时分钟数
 * @param onCancelTimer 取消定时回调
 */
@Composable
fun SleepTimerCountdownBar(
    timerMinutes: Int,
    onCancelTimer: () -> Unit
) {
    if (timerMinutes <= 0) return

    // 倒计时状态
    var remainingSeconds by remember(timerMinutes) { mutableStateOf(timerMinutes * 60L) }
    val targetTime = remember(timerMinutes) { System.currentTimeMillis() + timerMinutes * 60 * 1000L }

    LaunchedEffect(timerMinutes) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds = ((targetTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            if (remainingSeconds <= 0) break
        }
    }

    val hours = remainingSeconds / 3600
    val mins = (remainingSeconds % 3600) / 60
    val secs = remainingSeconds % 60
    val timeStr = if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs)
                  else "%02d:%02d".format(mins, secs)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "定时关闭 $timeStr",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                onClick = onCancelTimer,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            ) {
                Text(
                    "取消",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
