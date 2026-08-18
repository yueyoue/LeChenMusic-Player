package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.InternetRadioStation
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun RadioScreen(
    responsiveConfig: ResponsiveConfig? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val radioStations by viewModel.radioStations.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val starredRadioIds by viewModel.starredRadioIds.collectAsState()

    val radioColors = listOf(
        Color(0xFFFF4757),
        Color(0xFFA55EEA),
        Color(0xFF5352ED),
        Color(0xFF2ED573),
        Color(0xFF1E90FF),
        Color(0xFFFF6348)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar - consistent with other screens, no extra padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("电台", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Sort: starred (pinned) stations first
        val sortedStations = remember(radioStations, starredRadioIds) {
            radioStations.sortedByDescending { starredRadioIds.contains(it.id) }
        }

        if (radioStations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无电台",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedStations) { station ->
                    val color = radioColors[sortedStations.indexOf(station) % radioColors.size]
                    val isStarred = starredRadioIds.contains(station.id)
                    RadioListItem(
                        station = station,
                        color = color,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        isStarred = isStarred,
                        onClick = { viewModel.playerManager.playRadioStation(station) },
                        onStarToggle = {
                            if (isStarred) viewModel.unstarRadio(station.id)
                            else viewModel.starRadio(station.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioListItem(
    station: InternetRadioStation,
    color: Color,
    serverUrl: String = "",
    username: String = "",
    password: String = "",
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onStarToggle: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // 优先显示电台封面图
                    if (!station.coverArt.isNullOrEmpty() && serverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ApiClient.getCoverArtUrl(serverUrl, username, password, station.coverArt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    station.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "网络电台",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 置顶按钮
            IconButton(onClick = onStarToggle) {
                Icon(
                    if (isStarred) Icons.Default.PushPin else Icons.Default.PushPin,
                    contentDescription = if (isStarred) "取消置顶" else "置顶",
                    tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
