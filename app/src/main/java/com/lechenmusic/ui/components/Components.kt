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

private fun getQualityText(song: Song): String {
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

private fun getQualityColor(song: Song): Color {
    val suffix = song.suffix.uppercase()
    return when {
        suffix == "FLAC" || suffix == "DSD" || suffix == "WAV" || suffix == "AIFF" -> Color(0xFFFF6B81) // Red for lossless
        song.bitRate >= 320 -> Color(0xFF5352ED) // Purple for high bitrate
        else -> Color(0xFF2ED573) // Green for normal
    }
}

// ==================== Skip 15s Buttons ====================
// Custom SVG icons: 后退15秒 / 前进15秒

private val SkipBackward15Icon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SkipBackward15",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 1024f,
        viewportHeight = 1024f
    ).apply {
        path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(299.52f, 224.512f)
            lineTo(306.112f, 187.648f)
            horizontalLineTo(306.048f)
            arcTo(27.392f, 27.392f, 0f, false, false, 252.16f, 178.048f)
            horizontalLineTo(252.096f)
            lineTo(231.232f, 295.04f)
            arcTo(27.456f, 27.456f, 0f, false, false, 278.848f, 317.952f)
            quadTo(373.696f, 210.304f, 517.12f, 210.304f)
            quadTo(648.576f, 210.304f, 741.568f, 303.232f)
            quadTo(834.496f, 396.224f, 834.496f, 527.68f)
            quadTo(834.496f, 659.136f, 741.568f, 752.128f)
            quadTo(648.576f, 845.056f, 517.12f, 845.056f)
            quadTo(389.952f, 845.056f, 298.048f, 757.376f)
            arcTo(27.328f, 27.328f, 0f, false, false, 260.224f, 797.0f)
            quadTo(368.0f, 899.912f, 627.12f, 899.912f)
            quadTo(781.36f, 899.912f, 890.352f, 790.92f)
            quadTo(999.344f, 681.864f, 999.344f, 527.68f)
            quadTo(999.344f, 373.44f, 890.352f, 264.448f)
            quadTo(781.36f, 155.52f, 671.296f, 155.52f)
            quadTo(546.944f, 155.52f, 453.696f, 224.576f)
            lineTo(299.52f, 224.512f)
            close()
            moveTo(477.632f, 641.536f)
            horizontalLineTo(337.6f)
            verticalLineTo(607.168f)
            horizontalLineTo(389.504f)
            verticalLineTo(454.272f)
            horizontalLineTo(346.752f)
            verticalLineTo(427.968f)
            curveTo(369.792f, 423.552f, 385.856f, 417.728f, 400.512f, 408.96f)
            horizontalLineTo(431.936f)
            verticalLineTo(607.168f)
            horizontalLineTo(477.632f)
            verticalLineTo(641.536f)
            close()
            moveTo(668.224f, 565.824f)
            curveTo(668.224f, 616.64f, 629.056f, 645.952f, 586.304f, 645.952f)
            curveTo(548.608f, 645.952f, 524.096f, 630.912f, 506.944f, 613.76f)
            lineTo(526.272f, 586.688f)
            curveTo(540.224f, 599.872f, 557.376f, 611.52f, 580.8f, 611.52f)
            curveTo(606.784f, 611.52f, 625.792f, 595.072f, 625.792f, 566.912f)
            curveTo(625.792f, 539.136f, 608.576f, 523.392f, 582.976f, 523.392f)
            curveTo(568.0f, 523.392f, 559.552f, 527.424f, 545.664f, 536.576f)
            lineTo(525.568f, 523.776f)
            lineTo(532.544f, 408.96f)
            horizontalLineTo(656.832f)
            verticalLineTo(444.416f)
            horizontalLineTo(569.088f)
            lineTo(564.352f, 500.736f)
            arcTo(65.92f, 65.92f, 0f, false, true, 595.072f, 493.056f)
            curveTo(634.944f, 493.056f, 668.224f, 515.712f, 668.224f, 565.824f)
            close()
            moveTo(295.168f, 564.736f)
            horizontalLineTo(208.128f)
            verticalLineTo(534.016f)
            horizontalLineTo(295.168f)
            verticalLineTo(564.736f)
            close()
        }
    }.build()
}

private val SkipForward15Icon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SkipForward15",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 1024f,
        viewportHeight = 1024f
    ).apply {
        path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(724.48f, 221.44f)
            lineTo(717.888f, 184.576f)
            horizontalLineTo(717.952f)
            arcTo(27.392f, 27.392f, 0f, false, true, 771.84f, 174.976f)
            horizontalLineTo(771.904f)
            lineTo(792.768f, 292.032f)
            arcTo(27.456f, 27.456f, 0f, false, true, 745.152f, 314.944f)
            quadTo(650.304f, 207.232f, 506.88f, 207.232f)
            quadTo(375.424f, 207.232f, 282.432f, 300.224f)
            quadTo(189.504f, 393.152f, 189.504f, 527.616f)
            quadTo(189.504f, 658.112f, 282.432f, 752.064f)
            quadTo(375.424f, 845.0f, 506.88f, 845.0f)
            quadTo(634.048f, 845.0f, 725.952f, 757.32f)
            arcTo(27.328f, 27.328f, 0f, false, true, 763.776f, 796.936f)
            quadTo(656.0f, 899.976f, 396.88f, 899.976f)
            quadTo(242.64f, 899.976f, 133.648f, 790.92f)
            quadTo(24.656f, 681.864f, 24.656f, 527.68f)
            quadTo(24.656f, 373.504f, 133.648f, 264.448f)
            quadTo(242.64f, 155.392f, 506.88f, 155.392f)
            quadTo(631.232f, 155.392f, 724.48f, 224.448f)
            lineTo(724.48f, 221.44f)
            close()
            moveTo(457.536f, 638.464f)
            horizontalLineTo(317.44f)
            verticalLineTo(604.16f)
            horizontalLineTo(369.408f)
            verticalLineTo(451.2f)
            horizontalLineTo(326.592f)
            verticalLineTo(424.96f)
            curveTo(349.632f, 420.544f, 365.76f, 414.72f, 380.352f, 405.952f)
            horizontalLineTo(411.84f)
            verticalLineTo(604.16f)
            horizontalLineTo(457.536f)
            verticalLineTo(638.464f)
            close()
            moveTo(648.064f, 562.752f)
            curveTo(648.064f, 613.632f, 608.96f, 642.88f, 566.144f, 642.88f)
            curveTo(528.512f, 642.88f, 504.0f, 627.84f, 486.784f, 610.688f)
            lineTo(506.176f, 583.616f)
            curveTo(520.064f, 596.8f, 537.28f, 607.776f, 560.608f, 607.776f)
            curveTo(586.592f, 607.776f, 605.6f, 591.264f, 605.6f, 563.008f)
            curveTo(605.6f, 535.232f, 588.448f, 519.488f, 562.848f, 519.488f)
            curveTo(547.808f, 519.488f, 539.392f, 523.52f, 525.472f, 532.672f)
            lineTo(505.376f, 519.872f)
            lineTo(512.288f, 405.056f)
            horizontalLineTo(636.8f)
            verticalLineTo(440.512f)
            horizontalLineTo(548.992f)
            lineTo(544.192f, 496.832f)
            arcTo(65.92f, 65.92f, 0f, false, true, 574.912f, 489.152f)
            curveTo(614.784f, 489.152f, 648.064f, 511.808f, 648.064f, 562.752f)
            close()
            moveTo(775.36f, 602.24f)
            horizontalLineTo(743.552f)
            verticalLineTo(533.12f)
            horizontalLineTo(678.08f)
            verticalLineTo(502.4f)
            horizontalLineTo(743.552f)
            verticalLineTo(433.28f)
            horizontalLineTo(775.36f)
            verticalLineTo(502.4f)
            horizontalLineTo(840.832f)
            verticalLineTo(533.12f)
            horizontalLineTo(775.36f)
            verticalLineTo(602.24f)
            close()
        }
    }.build()
}

@Composable
fun SkipForward15Button(
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
            imageVector = SkipForward15Icon,
            contentDescription = "前进15秒",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SkipBackward15Button(
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
            imageVector = SkipBackward15Icon,
            contentDescription = "后退15秒",
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
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
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
            onDismissRequest = {
                expanded = false
                showPlaylistMenu = false
            }
        ) {
            DropdownMenuItem(
                text = { Text(if (song.isStarred) "取消收藏" else "收藏") },
                onClick = {
                    expanded = false
                    if (song.isStarred) onUnstar() else onStar()
                },
                leadingIcon = {
                    Icon(
                        if (song.isStarred) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (song.isStarred) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFE94560)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("添加到歌单") },
                onClick = { showPlaylistMenu = !showPlaylistMenu },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = { Icon(Icons.Default.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            if (showPlaylistMenu) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                playlists.forEach { pl ->
                    DropdownMenuItem(
                        text = { Text(pl.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            showPlaylistMenu = false
                            onAddToPlaylist(pl.id)
                        },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
                if (onCreatePlaylist != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    DropdownMenuItem(
                        text = { Text("新建歌单...") },
                        onClick = {
                            expanded = false
                            showPlaylistMenu = false
                            showCreateDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
    }

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
    onClick: () -> Unit,
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: ((String) -> Unit)? = null,
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
            onStar = onStar,
            onUnstar = onUnstar,
            onAddToPlaylist = onAddToPlaylist,
            onCreatePlaylist = onCreatePlaylist
        )
    }
}
