package com.lechenmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Album
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
                            fontSize = 9.sp,
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
// Custom buttons that mimic the music player's repeat/loop icon with "15" text inside.
// Forward: loop arrow | Backward: mirrored loop arrow

/**
 * A skip-forward-15-seconds button styled like the repeat/loop icon with "15" inside.
 * The icon is two curved arrows forming a loop (like Material Icons Repeat).
 */
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
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(size * 0.8f)
        ) {
            val w = this.size.width
            val strokeW = w * 0.09f
            val arrowSize = w * 0.12f

            // Top arrow: clockwise loop (goes right, curves down-left)
            val topPath = Path().apply {
                // Start from left, go right along top
                moveTo(w * 0.2f, w * 0.32f)
                // Line to the right
                lineTo(w * 0.72f, w * 0.32f)
                // Arrow head at right end of top line
            }
            drawPath(topPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Top arrow head (pointing right)
            val topArrowTip = Offset(w * 0.72f, w * 0.32f)
            val topArrowPath = Path().apply {
                moveTo(topArrowTip.x, topArrowTip.y)
                lineTo(topArrowTip.x - arrowSize, topArrowTip.y - arrowSize * 0.7f)
                lineTo(topArrowTip.x - arrowSize, topArrowTip.y + arrowSize * 0.7f)
                close()
            }
            drawPath(topArrowPath, color = tint)

            // Right vertical connector (top to bottom)
            val rightConn = Path().apply {
                moveTo(w * 0.72f, w * 0.32f)
                lineTo(w * 0.72f, w * 0.68f)
            }
            drawPath(rightConn, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom arrow: counter-clockwise loop (goes left, curves up-right)
            val bottomPath = Path().apply {
                moveTo(w * 0.8f, w * 0.68f)
                lineTo(w * 0.28f, w * 0.68f)
            }
            drawPath(bottomPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom arrow head (pointing left)
            val bottomArrowTip = Offset(w * 0.28f, w * 0.68f)
            val bottomArrowPath = Path().apply {
                moveTo(bottomArrowTip.x, bottomArrowTip.y)
                lineTo(bottomArrowTip.x + arrowSize, bottomArrowTip.y - arrowSize * 0.7f)
                lineTo(bottomArrowTip.x + arrowSize, bottomArrowTip.y + arrowSize * 0.7f)
                close()
            }
            drawPath(bottomArrowPath, color = tint)

            // Left vertical connector (bottom to top)
            val leftConn = Path().apply {
                moveTo(w * 0.28f, w * 0.68f)
                lineTo(w * 0.28f, w * 0.32f)
            }
            drawPath(leftConn, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }

        // Draw "15" text in the center
        Text(
            text = "15",
            fontSize = (size * 0.24f).value.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A skip-backward-15-seconds button styled like the mirrored repeat/loop icon with "15" inside.
 * The icon is mirrored horizontally (arrows point in opposite direction).
 */
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
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(size * 0.8f)
        ) {
            val w = this.size.width
            val strokeW = w * 0.09f
            val arrowSize = w * 0.12f

            // Top arrow: goes LEFT (mirrored)
            val topPath = Path().apply {
                moveTo(w * 0.8f, w * 0.32f)
                lineTo(w * 0.28f, w * 0.32f)
            }
            drawPath(topPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Top arrow head (pointing left)
            val topArrowTip = Offset(w * 0.28f, w * 0.32f)
            val topArrowPath = Path().apply {
                moveTo(topArrowTip.x, topArrowTip.y)
                lineTo(topArrowTip.x + arrowSize, topArrowTip.y - arrowSize * 0.7f)
                lineTo(topArrowTip.x + arrowSize, topArrowTip.y + arrowSize * 0.7f)
                close()
            }
            drawPath(topArrowPath, color = tint)

            // Left vertical connector (top to bottom)
            val leftConn = Path().apply {
                moveTo(w * 0.28f, w * 0.32f)
                lineTo(w * 0.28f, w * 0.68f)
            }
            drawPath(leftConn, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom arrow: goes RIGHT (mirrored)
            val bottomPath = Path().apply {
                moveTo(w * 0.2f, w * 0.68f)
                lineTo(w * 0.72f, w * 0.68f)
            }
            drawPath(bottomPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom arrow head (pointing right)
            val bottomArrowTip = Offset(w * 0.72f, w * 0.68f)
            val bottomArrowPath = Path().apply {
                moveTo(bottomArrowTip.x, bottomArrowTip.y)
                lineTo(bottomArrowTip.x - arrowSize, bottomArrowTip.y - arrowSize * 0.7f)
                lineTo(bottomArrowTip.x - arrowSize, bottomArrowTip.y + arrowSize * 0.7f)
                close()
            }
            drawPath(bottomArrowPath, color = tint)

            // Right vertical connector (bottom to top)
            val rightConn = Path().apply {
                moveTo(w * 0.72f, w * 0.68f)
                lineTo(w * 0.72f, w * 0.32f)
            }
            drawPath(rightConn, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }

        // Draw "15" text in the center
        Text(
            text = "15",
            fontSize = (size * 0.24f).value.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            textAlign = TextAlign.Center
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
