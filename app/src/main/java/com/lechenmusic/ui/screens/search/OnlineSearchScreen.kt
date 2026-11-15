package com.lechenmusic.ui.screens.search

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.model.OnlineDownloadTask
import com.lechenmusic.data.model.OnlineSong
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

/**
 * 在线搜歌（M6）：全网搜索 → 选音质/模糊匹配 → 下载入库（自动刮削）→ 任务进度 → 完成直接播放。
 * 复用服务端 online 系列接口（搜索/下载/任务），与 Web 管理后台同一套任务系统。
 */
@Composable
fun OnlineSearchScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit,
    onPlayStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    val pad = responsiveConfig?.contentPadding ?: 20.dp

    val searchResult by viewModel.onlineSearchResult.collectAsState()
    val searching by viewModel.onlineSearching.collectAsState()
    val searchError by viewModel.onlineSearchError.collectAsState()
    val tasks by viewModel.onlineTasks.collectAsState()
    val pendingKeyword by viewModel.pendingOnlineKeyword.collectAsState()

    var keyword by remember { mutableStateOf(pendingKeyword) }
    var selectedTab by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf("") }
    var fuzzy by remember { mutableStateOf(false) }
    // 待确认删除的任务：删除会连 NAS 上的音频文件一起删掉，必须先弹窗确认
    var pendingDelete by remember { mutableStateOf<OnlineDownloadTask?>(null) }

    // 通知权限：下载完成的「推送→直接播放」依赖系统通知，Android 13+ 需运行时授权（仅请求一次）
    var notifPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // 进入页面：消费入口关键词（或恢复上次搜索词）并刷新任务列表（有进行中任务会自动开启轮询）
    LaunchedEffect(Unit) {
        if (keyword.isBlank()) {
            keyword = viewModel.consumePendingOnlineKeyword().ifBlank { viewModel.onlineSearchQuery.value }
        }
        if (keyword.isNotBlank()) viewModel.searchOnline(keyword)
        viewModel.loadOnlineTasks()
        if (!notifPermissionRequested && android.os.Build.VERSION.SDK_INT >= 33 &&
            !androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            notifPermissionRequested = true
            try {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
            }
        }
    }

    val activeCount = tasks.count { it.isActive }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("在线搜歌", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("搜不到的歌，一键下载入库 · 自动刮削", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Tabs（搜歌下载 / 下载任务）
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Tab(selectedTab == 0, onClick = { selectedTab = 0 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("搜歌下载")
                }
            }
            Tab(selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Downloading, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("下载任务")
                    if (activeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge { Text("$activeCount") }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> OnlineSearchTab(
                keyword = keyword,
                onKeywordChange = { keyword = it },
                quality = quality,
                onQualityChange = { quality = it },
                fuzzy = fuzzy,
                onFuzzyChange = { fuzzy = it },
                songs = searchResult?.songs ?: emptyList(),
                searching = searching,
                error = searchError,
                onSearch = { viewModel.searchOnline(keyword) },
                onDownload = { song ->
                    viewModel.downloadOnline(song, quality, fuzzy) { err ->
                        android.widget.Toast.makeText(
                            context,
                            err ?: "已加入下载队列",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        if (err == null) selectedTab = 1
                    }
                },
                pad = pad
            )
            1 -> OnlineTasksTab(
                tasks = tasks,
                onRetry = { task ->
                    viewModel.retryOnlineTask(task) { err ->
                        if (err != null) {
                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDelete = { task ->
                    // 先弹确认框，确认后才真正删除（删除会连 NAS 文件一起删掉）
                    pendingDelete = task
                },
                onPlay = { task ->
                    android.widget.Toast.makeText(context, "正在查找并播放…", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.playDownloadedTask(task) { ok ->
                        if (ok) {
                            onPlayStarted()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "曲库暂未找到该歌曲，可能扫描尚未完成，稍后再试",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                pad = pad
            )
        }
    }

    // 删除确认弹窗：明确告知会同时删除 NAS 上的音频文件，避免误删
    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除下载歌曲") },
            text = {
                Text(
                    buildString {
                        append("确定删除《")
                        append(deleting.songName)
                        if (deleting.artist.isNotBlank()) append(" - ${deleting.artist}")
                        append("》？\n\n")
                        if (deleting.isDone) {
                            append("会同时删除：\n")
                            append("· NAS 上的音频文件\n")
                            append("· 曲库中的歌曲记录\n")
                            append("· 下载记录\n\n")
                            append("删除后该歌曲不在曲库，可以重新下载。此操作不可撤销。")
                        } else {
                            append("该任务尚未完成入库，删除只会移除任务记录（如已产生临时文件也会一并清理）。\n")
                            append("之后可以重新搜索下载。")
                        }
                    },
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteOnlineTask(deleting) { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

// ===== 搜歌下载 tab =====

private val QUALITY_OPTIONS = listOf(
    "" to "默认（无损优先）",
    "flac/2000" to "FLAC 2000k",
    "mp3/320" to "MP3 320k",
    "mp3/128" to "MP3 128k"
)

@Composable
private fun OnlineSearchTab(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    quality: String,
    onQualityChange: (String) -> Unit,
    fuzzy: Boolean,
    onFuzzyChange: (Boolean) -> Unit,
    songs: List<OnlineSong>,
    searching: Boolean,
    error: String?,
    onSearch: () -> Unit,
    onDownload: (OnlineSong) -> Unit,
    pad: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索表单
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("输入歌名 / 歌名 - 歌手") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (keyword.isNotEmpty()) {
                    IconButton(onClick = { onKeywordChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        // 音质选择（横向滚动，窄屏不溢出）
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QUALITY_OPTIONS.forEach { (value, label) ->
                FilterChip(
                    selected = quality == value,
                    onClick = { onQualityChange(value) },
                    label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                )
            }
        }

        // 模糊匹配 + 搜索按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = fuzzy, onCheckedChange = onFuzzyChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("模糊匹配", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("允许下载 Live / 翻唱等其他版本", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onSearch, enabled = !searching) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("搜索")
            }
        }

        if (searching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp))
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 160.dp)) {
            if (songs.isNotEmpty()) {
                item {
                    Text("搜索结果 (${songs.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
                items(songs) { song ->
                    OnlineSongItem(song = song, fuzzy = fuzzy, onDownload = { onDownload(song) })
                }
            } else if (!searching && error == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudDownload, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("输入歌名搜索全网资源\n下载后自动入库，可直接播放",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSongItem(song: OnlineSong, fuzzy: Boolean, onDownload: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(52.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.MusicNote, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(song.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(song.artist.ifBlank { "未知歌手" })
                    if (song.albumName.isNotBlank()) append(" · ${song.albumName}")
                    if (song.durationFormatted.isNotBlank()) append(" · ${song.durationFormatted}")
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (song.qualities.isNotEmpty()) {
                Text(
                    song.qualities.joinToString("  ") { it.label },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        // 已入库 / 已在下载队列 一律不可再点：避免重复下载（服务端同样会拒绝）
        if (song.downloading || song.inLibrary) {
            Column(horizontalAlignment = Alignment.End) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(if (song.downloading) "下载中" else "已在曲库", fontSize = 11.sp)
                    }
                )
                Text(
                    if (song.downloading) "去「下载任务」看进度" else if (!fuzzy) "开模糊匹配可下载" else "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            FilledTonalIconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "下载")
            }
        }
    }
}

// ===== 下载任务 tab =====

private fun fmtSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0fKB".format(kb)
    else if (kb < 1024 * 1024) "%.1fMB".format(kb / 1024)
    else "%.1fGB".format(kb / 1024 / 1024)
}

@Composable
private fun OnlineTasksTab(
    tasks: List<OnlineDownloadTask>,
    onRetry: (OnlineDownloadTask) -> Unit,
    onDelete: (OnlineDownloadTask) -> Unit,
    onPlay: (OnlineDownloadTask) -> Unit,
    pad: androidx.compose.ui.unit.Dp
) {
    val activeTasks = tasks.filter { it.isActive }
    val failedTasks = tasks.filter { it.isFailed }
    val doneTasks = tasks.filter { it.isDone }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        if (tasks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CloudDownload, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("暂无下载任务", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (activeTasks.isNotEmpty()) {
            item {
                Text("进行中 (${activeTasks.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            items(activeTasks, key = { "a${it.id}" }) { task ->
                TaskCard(task = task)
            }
        }

        if (failedTasks.isNotEmpty()) {
            item {
                Text("失败 (${failedTasks.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            items(failedTasks, key = { "f${it.id}" }) { task ->
                TaskCard(task = task, onRetry = { onRetry(task) }, onDelete = { onDelete(task) })
            }
        }

        if (doneTasks.isNotEmpty()) {
            item {
                Text("已完成 (${doneTasks.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            items(doneTasks, key = { "d${it.id}" }) { task ->
                TaskCard(task = task, onPlay = { onPlay(task) }, onDelete = { onDelete(task) })
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: OnlineDownloadTask,
    onRetry: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (task.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = task.coverUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.MusicNote, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("${task.songName} - ${task.artist}", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(task.statusLabel)
                    append(" · ${task.qualityLabel}")
                    if (task.totalBytes > 0) append(" · ${fmtSize(task.progressBytes)} / ${fmtSize(task.totalBytes)}")
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (task.isFailed && task.error.isNotBlank()) {
                Text(task.error, fontSize = 11.sp, color = MaterialTheme.colorScheme.error,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (task.isActive) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)) {
                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier.weight(1f).height(4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${task.progressPercent}%", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (onPlay != null) {
            FilledTonalIconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "播放")
            }
        }
        if (onRetry != null) {
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "重试",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
