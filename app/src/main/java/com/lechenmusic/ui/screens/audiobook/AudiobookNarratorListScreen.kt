package com.lechenmusic.ui.screens.audiobook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.SubsonicApi
import com.lechenmusic.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookNarratorListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNarratorClick: (String) -> Unit
) {
    val narrators by viewModel.narrators.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNarrators()
    }

    val colors = listOf(
        Color(0xFFE94560), Color(0xFF3498DB), Color(0xFF2ECC71),
        Color(0xFFF39C12), Color(0xFF8E44AD), Color(0xFF00B894)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header - no TopAppBar, just a simple row like AudiobookScreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("🎙️ 演播者", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (narrators.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无演播者信息", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("请重新扫描有声书媒体库", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            // Grid style - 2 columns, like album display
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(narrators) { narr ->
                    val colorIdx = narr.name.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }
                    NarratorGridCard(
                        name = narr.name,
                        count = narr.count,
                        color = colors[colorIdx],
                        serverUrl = serverUrl,
                        onClick = { onNarratorClick(narr.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun NarratorGridCard(
    name: String,
    count: Int,
    color: Color,
    serverUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar - circular
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = color
        ) {
            val avatarUrl = SubsonicApi.getNarratorAvatarUrl(serverUrl, name)
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Name
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Count
        Text(
            text = "${count}部作品",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
