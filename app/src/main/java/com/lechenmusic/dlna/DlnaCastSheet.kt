package com.lechenmusic.dlna

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 投屏设备选择弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaCastSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onDeviceSelected: (DlnaDevice) -> Unit
) {
    if (!isVisible) return

    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchDone by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            isSearching = true
            searchDone = false
            devices = emptyList()
            scope.launch {
                devices = DlnaDiscovery.search(5000)
                isSearching = false
                searchDone = true
            }
            // 8秒后如果还在搜索，自动停止
            kotlinx.coroutines.delay(8000)
            if (isSearching) {
                isSearching = false
                searchDone = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("选择投屏设备", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在搜索局域网设备...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (searchDone && devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TvOff, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("未找到投屏设备", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("请确保设备与手机在同一WiFi网络", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }

            if (devices.isNotEmpty()) {
                Text("发现 ${devices.size} 个设备", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(devices) { device ->
                    Surface(
                        onClick = {
                            onDeviceSelected(device)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tv, null, modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.displayName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                if (device.modelName.isNotBlank()) {
                                    Text(device.modelName, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 重新搜索按钮
            if (searchDone && !isSearching) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        isSearching = true
                        searchDone = false
                        devices = emptyList()
                        scope.launch {
                            devices = DlnaDiscovery.search(5000)
                            isSearching = false
                            searchDone = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新搜索")
                }
            }
        }
    }
}

/**
 * 投屏控制迷你面板(显示在详情页底部)
 */
@Composable
fun DlnaCastControls(
    device: DlnaDevice,
    controller: DlnaController,
    onStopCasting: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var positionText by remember { mutableStateOf("00:00") }
    var durationText by remember { mutableStateOf("00:00") }

    // 定期获取播放状态
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            try {
                val state = controller.getTransportState()
                isPlaying = state == "PLAYING"
                val pos = controller.getPosition()
                if (pos != null) {
                    positionText = formatTime(pos.first)
                    durationText = formatTime(pos.second)
                }
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标+名称
            Icon(Icons.Default.Cast, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("$positionText / $durationText", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 播放/暂停
            IconButton(onClick = {
                scope.launch {
                    if (isPlaying) controller.pause() else controller.play()
                    isPlaying = !isPlaying
                }
            }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(22.dp)
                )
            }

            // 停止投屏
            IconButton(onClick = {
                scope.launch { controller.stop() }
                onStopCasting()
            }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.StopCircle, null, modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
