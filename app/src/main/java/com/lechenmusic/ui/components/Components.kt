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
// Forward: clockwise arrow | Backward: counter-clockwise arrow (mirrored)

/**
 * A skip-forward-15-seconds button styled like the repeat icon with "15" inside.
 * The arrow rotates clockwise.
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
            modifier = Modifier.size(size * 0.85f)
        ) {
            val canvasSize = this.size.width
            val strokeWidth = canvasSize * 0.1f
            val arcRadius = canvasSize * 0.38f
            val center = Offset(canvasSize / 2f, canvasSize / 2f)

            // Draw circular arc (about 300 degrees, clockwise)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2, arcRadius * 2)

            drawArc(
                color = tint,
                startAngle = -60f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw arrowhead at the end of the arc (at angle -60 + 300 = 240 degrees)
            val arrowAngle = Math.toRadians(240.0)
            val arrowTipX = center.x + arcRadius * kotlin.math.cos(arrowAngle).toFloat()
            val arrowTipY = center.y + arcRadius * kotlin.math.sin(arrowAngle).toFloat()

            val arrowSize = canvasSize * 0.14f
            // Arrow tip pointing in the direction of the arc (perpendicular to radius at 240 degrees)
            val tangentAngle = arrowAngle + Math.PI / 2 // perpendicular to radius
            val arrowLeft = Offset(
                arrowTipX - arrowSize * kotlin.math.cos(tangentAngle - 0.5).toFloat(),
                arrowTipY - arrowSize * kotlin.math.sin(tangentAngle - 0.5).toFloat()
            )
            val arrowRight = Offset(
                arrowTipX - arrowSize * kotlin.math.cos(tangentAngle + 0.5).toFloat(),
                arrowTipY - arrowSize * kotlin.math.sin(tangentAngle + 0.5).toFloat()
            )

            val arrowPath = Path().apply {
                moveTo(arrowTipX, arrowTipY)
                lineTo(arrowLeft.x, arrowLeft.y)
                lineTo(arrowRight.x, arrowRight.y)
                close()
            }
            drawPath(arrowPath, color = tint)
        }

        // Draw "15" text in the center
        Text(
            text = "15",
            fontSize = (size * 0.26f).value.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A skip-backward-15-seconds button styled like the mirrored repeat icon with "15" inside.
 * The arrow rotates counter-clockwise (horizontal mirror of forward).
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
            modifier = Modifier.size(size * 0.85f)
        ) {
            val canvasSize = this.size.width
            val strokeWidth = canvasSize * 0.1f
            val arcRadius = canvasSize * 0.38f
            val center = Offset(canvasSize / 2f, canvasSize / 2f)

            // Draw circular arc (about 300 degrees, counter-clockwise = mirrored)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2, arcRadius * 2)

            drawArc(
                color = tint,
                startAngle = 240f,
                sweepAngle = -300f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw arrowhead at the end of the arc (at angle 240 - 300 = -60 degrees)
            val arrowAngle = Math.toRadians(-60.0)
            val arrowTipX = center.x + arcRadius * kotlin.math.cos(arrowAngle).toFloat()
            val arrowTipY = center.y + arcRadius * kotlin.math.sin(arrowAngle).toFloat()

            val arrowSize = canvasSize * 0.14f
            // Arrow tip pointing in the direction of the arc (perpendicular to radius at -60 degrees)
            // For counter-clockwise, the tangent is flipped
            val tangentAngle = arrowAngle - Math.PI / 2
            val arrowLeft = Offset(
                arrowTipX - arrowSize * kotlin.math.cos(tangentAngle - 0.5).toFloat(),
                arrowTipY - arrowSize * kotlin.math.sin(tangentAngle - 0.5).toFloat()
            )
            val arrowRight = Offset(
                arrowTipX - arrowSize * kotlin.math.cos(tangentAngle + 0.5).toFloat(),
                arrowTipY - arrowSize * kotlin.math.sin(tangentAngle + 0.5).toFloat()
            )

            val arrowPath = Path().apply {
                moveTo(arrowTipX, arrowTipY)
                lineTo(arrowLeft.x, arrowLeft.y)
                lineTo(arrowRight.x, arrowRight.y)
                close()
            }
            drawPath(arrowPath, color = tint)
        }

        // Draw "15" text in the center
        Text(
            text = "15",
            fontSize = (size * 0.26f).value.sp,
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
