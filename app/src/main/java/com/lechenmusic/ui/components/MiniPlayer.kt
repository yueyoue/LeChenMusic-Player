package com.lechenmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.player.MusicPlayerManager

@Composable
fun MiniPlayer(
    playerManager: MusicPlayerManager,
    serverUrl: String,
    username: String,
    password: String,
    audiobookCoverUrl: String? = null,
    isAudiobook: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    val song = currentSong ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            if (audiobookCoverUrl != null) {
                coil.compose.AsyncImage(
                    model = audiobookCoverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                CoverImage(
                    coverArtId = song.coverArt ?: song.albumId,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // 歌曲信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 控制按钮
            if (!isAudiobook) {
                // 音乐模式: 随机 | 上一曲 | 播放/暂停 | 下一曲 | 循环
                IconButton(onClick = { playerManager.toggleShuffle() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Shuffle, "随机",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = { playerManager.skipPrevious() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一曲", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }

            // 播放/暂停 - 主按钮
            FilledIconButton(
                onClick = { playerManager.togglePlayPause() },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = { playerManager.skipNext() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一曲", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }

            if (!isAudiobook) {
                // 音乐模式: 循环按钮
                IconButton(onClick = { playerManager.toggleRepeat() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Repeat, "循环",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
