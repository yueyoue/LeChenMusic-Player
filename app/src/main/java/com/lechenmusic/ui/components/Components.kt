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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // Quality badge
                val qualityText = getQualityText(song)
                if (qualityText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getQualityColor(song).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = qualityText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = getQualityColor(song),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = "${song.artist} · ${song.album}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
// Custom SVG icons: 后退30秒 / 前进30秒

private val SkipBackward30Icon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SkipBackward30",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 1024f,
        viewportHeight = 1024f
    ).apply {
        path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(512f, 85.333f)
            curveTo(276.352f, 85.333f, 85.333f, 276.352f, 85.333f, 512f)
            reflectiveCurveToRelative(191.019f, 426.667f, 426.667f, 426.667f)
            curveToRelative(52.267f, 0f, 102.827f, -9.472f, 150.187f, -26.752f)
            lineToRelative(139.349f, -56.528f)
            lineToRelative(-41.216f, -49.067f)
            arcTo(362.667f, 362.667f, 0f, true, true, 874.667f, 512f)
            horizontalLineToRelative(-42.667f)
            lineToRelative(80f, -128f)
            lineToRelative(80f, 128f)
            horizontalLineToRelative(-53.333f)
            curveTo(938.667f, 747.648f, 747.648f, 938.667f, 512f, 938.667f)
            curveTo(276.352f, 938.667f, 85.333f, 747.648f, 85.333f, 512f)
            curveTo(85.333f, 276.352f, 276.352f, 85.333f, 512f, 85.333f)
            close()
            moveTo(477.632f, 641.536f)
            horizontalLineTo(337.6f)
            verticalLineTo(607.168f)
            horizontalLineToRelative(51.904f)
            verticalLineTo(454.272f)
            horizontalLineTo(346.752f)
            verticalLineTo(427.968f)
            curveTo(369.792f, 423.552f, 385.856f, 417.728f, 400.512f, 408.96f)
            horizontalLineToRelative(31.424f)
            verticalLineToRelative(198.208f)
            horizontalLineToRelative(45.696f)
            verticalLineToRelative(34.368f)
            close()
            moveTo(668.224f, 565.824f)
            curveToRelative(0f, 50.816f, -39.168f, 80.128f, -81.92f, 80.128f)
            curveToRelative(-37.696f, 0f, -62.208f, -15.04f, -79.36f, -32.192f)
            lineToRelative(19.328f, -27.072f)
            curveToRelative(13.952f, 13.184f, 31.104f, 24.832f, 54.528f, 24.832f)
            curveToRelative(25.984f, 0f, 44.992f, -16.448f, 44.992f, -44.608f)
            curveToRelative(0f, -27.776f, -17.216f, -43.52f, -42.816f, -43.52f)
            curveToRelative(-14.976f, 0f, -23.424f, 4.032f, -37.312f, 13.184f)
            lineToRelative(-20.096f, -12.8f)
            lineToRelative(6.976f, -114.816f)
            horizontalLineToRelative(124.288f)
            verticalLineToRelative(35.456f)
            horizontalLineTo(569.088f)
            lineToRelative(-4.736f, 56.32f)
            curveToRelative(15.232f, -7.68f, 32.768f, -15.36f, 62.72f, -15.36f)
            curveToRelative(39.872f, 0f, 73.152f, 22.656f, 73.152f, 72.768f)
            close()
            moveTo(295.168f, 564.736f)
            horizontalLineTo(208.128f)
            verticalLineTo(534.016f)
            horizontalLineToRelative(87.04f)
            verticalLineToRelative(30.72f)
            close()
        }
    }.build()
}

private val SkipForward30Icon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SkipForward30",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 1024f,
        viewportHeight = 1024f
    ).apply {
        path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(512f, 85.333f)
            curveTo(747.648f, 85.333f, 938.667f, 276.352f, 938.667f, 512f)
            reflectiveCurveTo(747.648f, 938.667f, 512f, 938.667f)
            curveTo(459.733f, 938.667f, 409.173f, 929.195f, 361.813f, 911.915f)
            lineTo(222.464f, 955.387f)
            lineToRelative(41.216f, -49.067f)
            arcTo(362.667f, 362.667f, 0f, true, false, 149.333f, 512f)
            horizontalLineToRelative(42.667f)
            lineToRelative(-80f, 128f)
            lineToRelative(-80f, -128f)
            horizontalLineToRelative(53.333f)
            curveTo(85.333f, 276.352f, 276.352f, 85.333f, 512f, 85.333f)
            close()
            moveTo(546.368f, 641.536f)
            horizontalLineTo(686.4f)
            verticalLineTo(607.168f)
            horizontalLineToRelative(-51.904f)
            verticalLineTo(454.272f)
            horizontalLineToRelative(42.752f)
            verticalLineTo(427.968f)
            curveToRelative(-23.04f, -4.416f, -39.104f, -10.24f, -53.76f, -19.008f)
            horizontalLineToRelative(-31.424f)
            verticalLineToRelative(198.208f)
            horizontalLineToRelative(-45.696f)
            verticalLineToRelative(34.368f)
            close()
            moveTo(355.776f, 565.824f)
            curveToRelative(0f, 50.816f, 39.168f, 80.128f, 81.92f, 80.128f)
            curveToRelative(37.696f, 0f, 62.208f, -15.04f, 79.36f, -32.192f)
            lineToRelative(-19.328f, -27.072f)
            curveToRelative(-13.952f, 13.184f, -31.104f, 24.832f, -54.528f, 24.832f)
            curveToRelative(-25.984f, 0f, -44.992f, -16.448f, -44.992f, -44.608f)
            curveToRelative(0f, -27.776f, 17.216f, -43.52f, 42.816f, -43.52f)
            curveToRelative(14.976f, 0f, 23.424f, 4.032f, 37.312f, 13.184f)
            lineToRelative(20.096f, -12.8f)
            lineToRelative(-6.976f, -114.816f)
            horizontalLineTo(355.776f)
            verticalLineToRelative(35.456f)
            horizontalLineToRelative(87.8f)
            lineToRelative(4.736f, 56.32f)
            curveToRelative(-15.232f, -7.68f, -32.768f, -15.36f, -62.72f, -15.36f)
            curveToRelative(-39.872f, 0f, -73.152f, 22.656f, -73.152f, 72.768f)
            close()
            moveTo(728.832f, 564.736f)
            horizontalLineToRelative(87.04f)
            verticalLineTo(534.016f)
            horizontalLineToRelative(-87.04f)
            verticalLineToRelative(30.72f)
            close()
        }
    }.build()
}

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
            imageVector = SkipForward30Icon,
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
            imageVector = SkipBackward30Icon,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // Quality badge
                val qualityText = getQualityText(song)
                if (qualityText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getQualityColor(song).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = qualityText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = getQualityColor(song),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = "${song.artist} · ${song.album}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
