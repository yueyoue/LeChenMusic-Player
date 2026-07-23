package com.lechenmusic.ui.screens.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.LiveChannel
import com.lechenmusic.data.model.LiveChannelGroup
import com.lechenmusic.ui.VideoViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun TabletLiveScreen(
    viewModel: VideoViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit
) {
    val liveChannels by viewModel.liveChannels.collectAsState()
    val liveSources by viewModel.liveSources.collectAsState()
    val liveLoading by viewModel.liveLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLiveSources()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 面包屑
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = responsiveConfig.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("影视", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" / ", color = MaterialTheme.colorScheme.outlineVariant)
            Text("直播", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        if (liveLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (liveChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LiveTv, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无直播频道", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(responsiveConfig.gridColumns.coerceIn(3, 6)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = responsiveConfig.contentPadding,
                    end = responsiveConfig.contentPadding,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(responsiveConfig.itemSpacing)
            ) {
                liveChannels.forEach { group ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            group.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(group.channels) { channel ->
                        LiveChannelCard(channel = channel, onClick = { })
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: LiveChannel,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LiveTv, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                channel.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
