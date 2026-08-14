package com.lechenmusic.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    videoViewModel: com.lechenmusic.ui.VideoViewModel? = null,
    responsiveConfig: ResponsiveConfig? = null,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val config = responsiveConfig
    val isTablet = config != null && (config.isMedium || config.isExpanded)
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val serverStats by viewModel.serverStats.collectAsState()
    val context = LocalContext.current
    var showCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateInfo by viewModel.updateInfo.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    var userTriggeredCheck by remember { mutableStateOf(false) }

    // Show toast messages (including "already latest")
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 检查到更新时弹窗（支持手动多次触发）
    LaunchedEffect(updateInfo, userTriggeredCheck) {
        if (updateInfo != null && userTriggeredCheck) {
            showUpdateDialog = true
            userTriggeredCheck = false
        }
    }
    var musicCacheSize by remember { mutableStateOf("计算中...") }
    var otherDataSize by remember { mutableStateOf("计算中...") }

    LaunchedEffect(Unit) {
        val cacheBytes = viewModel.playerManager.getCacheBytes()
        musicCacheSize = if (cacheBytes > 0) PhoneFormatSize(cacheBytes) else PhoneCalculateCacheSize(context, "music_cache")
        otherDataSize = PhoneCalculateOtherDataSize(context)
    }

    if (isTablet) {
        // ═══ 平板布局 ═══
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = config!!.contentPadding, vertical = 16.dp)
        ) {
            // 面包屑
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("首页", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" / ", color = MaterialTheme.colorScheme.outlineVariant)
                Text("设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 用户信息卡片 =====
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                username.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(serverUrl, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 听歌等级卡片 =====
            TabletUserLevelCard(viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 服务器信息 =====
            TabletSectionTitle("服务器信息")
            TabletSettingsCard {
                val statsLoaded = serverStats.songCount > 0 || serverStats.albumCount > 0
                TabletSettingsInfoRow("服务器地址", serverUrl)
                TabletSettingsInfoRow("用户名", username)
                TabletSettingsInfoRow("歌曲数量", if (statsLoaded) "${serverStats.songCount}" else "加载中...")
                TabletSettingsInfoRow("专辑数量", if (statsLoaded) "${serverStats.albumCount}" else "加载中...")
                TabletSettingsInfoRow("歌手数量", if (statsLoaded) "${serverStats.artistCount}" else "加载中...")
                TabletSettingsInfoRow("歌单数量", if (statsLoaded) "${serverStats.playlistCount}" else "加载中...")
                TabletSettingsInfoRow("有声读物", if (statsLoaded) "${serverStats.audiobookCount}" else "加载中...")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 外观设置 =====
            TabletSectionTitle("外观设置")
            TabletSettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Settings,
                    iconBg = Color(0xFFA55EEA).copy(alpha = 0.15f),
                    label = "深色模式",
                    checked = themeMode == "dark",
                    onCheckedChange = { viewModel.setThemeMode(if (it) "dark" else "light") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 缓存设置 =====
            TabletSectionTitle("缓存设置")
            TabletSettingsCard {
                SettingsClickItem(
                    icon = Icons.Default.Storage,
                    iconBg = Color(0xFFFF4757).copy(alpha = 0.15f),
                    label = "音乐缓存大小",
                    value = "${cacheSize} GB",
                    onClick = { showCacheDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 存储空间 =====
            TabletSectionTitle("存储空间")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val exoCacheBytes = viewModel.playerManager.getCacheBytes()
                    val musicBytes = if (exoCacheBytes > 0) exoCacheBytes else PhoneGetCacheSizeBytes(context, "music_cache")
                    val otherBytes = PhoneGetOtherDataSizeBytes(context)
                    val totalUsed = musicBytes + otherBytes
                    val maxBytes = cacheSize.toLong() * 1024 * 1024 * 1024
                    val progress = if (maxBytes > 0) (totalUsed.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("已使用 ${PhoneFormatSize(totalUsed)}", fontSize = 13.sp)
                        Text("共 ${cacheSize} GB", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎵 音乐缓存 $musicCacheSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📦 其他数据 $otherDataSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.playerManager.clearMusicCache()
                            musicCacheSize = "0 B"
                            otherDataSize = PhoneCalculateOtherDataSize(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除缓存", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 账号 =====
            TabletSectionTitle("账号")
            TabletSettingsCard {
                SettingsClickItem(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFF5352ED).copy(alpha = 0.15f),
                    label = "当前账号",
                    value = username,
                    onClick = { }
                )
                SettingsClickItem(
                    icon = Icons.Default.Link,
                    iconBg = Color(0xFFFFA502).copy(alpha = 0.15f),
                    label = "服务器地址",
                    value = serverUrl,
                    onClick = { }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("切换服务器", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 影视服务器设置 =====
            if (videoViewModel != null) {
                TabletVideoServerSettings(videoViewModel)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== 版本更新 =====
            TabletSectionTitle("版本更新")
            TabletSettingsCard {
                SettingsClickItem(
                    icon = Icons.Default.Refresh,
                    iconBg = Color(0xFF1E90FF).copy(alpha = 0.15f),
                    label = "检查更新",
                    value = if (isCheckingUpdate) "检查中..."
                    else if (updateInfo != null) "有新版本 v${updateInfo!!.versionName}"
                    else if (updateStatus.isNotEmpty()) updateStatus
                    else "当前 ${PhoneGetCurrentVersionName(context)}",
                    onClick = {
                        viewModel.dismissUpdate()
                        userTriggeredCheck = true
                        viewModel.checkForUpdate(silent = false)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 关于 =====
            TabletSectionTitle("关于")
            TabletSettingsCard {
                SettingsClickItem(
                    icon = Icons.Default.Info,
                    iconBg = Color(0xFF2ED573).copy(alpha = 0.15f),
                    label = "关于悦音",
                    value = "",
                    onClick = { showAboutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 退出登录 =====
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            }

            // 底部留白
            Spacer(modifier = Modifier.height(160.dp))
        }
    } else {
        // ═══ 手机布局 ═══
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                    Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                // ── 听歌等级卡片（同步服务端 + 本地缓存）──
                val totalSeconds = remember {
                    mutableStateOf(
                        context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                            .getLong("total_play_seconds", 0L)
                    )
                }
                // 启动时从服务端同步，解决重装丢失问题
                LaunchedEffect(Unit) {
                    val serverStats = viewModel.fetchUserPlayStats()
                    if (serverStats != null) {
                        val serverSeconds = serverStats.totalDuration.toLong()
                        val localSeconds = context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                            .getLong("total_play_seconds", 0L)
                        // 取服务端和本地的较大值，避免本地上报延迟导致数据倒退
                        val maxSeconds = maxOf(serverSeconds, localSeconds)
                        context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                            .edit().putLong("total_play_seconds", maxSeconds).apply()
                        totalSeconds.value = maxSeconds
                    }
                    // 定期刷新本地统计
                    while (true) {
                        kotlinx.coroutines.delay(5000)
                        totalSeconds.value = context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                            .getLong("total_play_seconds", 0L)
                    }
                }
                val totalMinutes = totalSeconds.value / 60
                val level = com.lechenmusic.data.model.UserLevelSystem.getCurrentLevel(totalMinutes)
                val nextLevel = com.lechenmusic.data.model.UserLevelSystem.getNextLevel(totalMinutes)
                val minutesToNext = com.lechenmusic.data.model.UserLevelSystem.getMinutesToNextLevel(totalMinutes)
                val progress = com.lechenmusic.data.model.UserLevelSystem.getLevelProgress(totalMinutes)
                val isMaxLevel = nextLevel == null

                val gradientColors = when (level.level) {
                    1 -> listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
                    2 -> listOf(Color(0xFF66BB6A), Color(0xFFA5D6A7))
                    3 -> listOf(Color(0xFF42A5F5), Color(0xFF90CAF9))
                    4 -> listOf(Color(0xFF7E57C2), Color(0xFFB39DDB))
                    5 -> listOf(Color(0xFFFF7043), Color(0xFFFFAB91))
                    6 -> listOf(Color(0xFFFFA726), Color(0xFFFFCC80))
                    7 -> listOf(Color(0xFFEF5350), Color(0xFFEF9A9A))
                    8 -> listOf(Color(0xFFAB47BC), Color(0xFFCE93D8))
                    9 -> listOf(Color(0xFFFFD700), Color(0xFFFFF176))
                    10 -> listOf(Color(0xFFE91E63), Color(0xFFFF80AB))
                    else -> listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(gradientColors))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            // 第一行：等级称号大字
                            Text(
                                level.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 第二行：等级 + 距下一级
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 等级徽章
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        "Lv${level.level}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isMaxLevel) "已满级 🎉"
                                    else "距下一级「${nextLevel!!.title}」还有 ${minutesToNext} 分钟",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // 进度条
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 累计时长
                            val hours = totalMinutes / 60
                            val mins = totalMinutes % 60
                            Text(
                                "累计听歌 ${hours} 小时 ${mins} 分钟",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("服务器信息")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        InfoRow("服务器地址", serverUrl)
                        InfoRow("用户名", username)
                        val statsLoaded = serverStats.songCount > 0 || serverStats.albumCount > 0
                        InfoRow("歌曲数量", if (statsLoaded) "${serverStats.songCount}" else "加载中...")
                        InfoRow("专辑数量", if (statsLoaded) "${serverStats.albumCount}" else "加载中...")
                        InfoRow("歌手数量", if (statsLoaded) "${serverStats.artistCount}" else "加载中...")
                        InfoRow("歌单数量", if (statsLoaded) "${serverStats.playlistCount}" else "加载中...")
                        InfoRow("有声读物", if (statsLoaded) "${serverStats.audiobookCount}" else "加载中...")
                    }
                }
            }

            item {
                SectionTitle("外观设置")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsToggleItem(icon = Icons.Default.Settings, iconBg = Color(0xFFA55EEA).copy(alpha = 0.15f), label = "深色模式", checked = themeMode == "dark", onCheckedChange = { viewModel.setThemeMode(if (it) "dark" else "light") })
                    }
                }
            }

            item {
                SectionTitle("缓存设置")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsClickItem(icon = Icons.Default.Storage, iconBg = Color(0xFFFF4757).copy(alpha = 0.15f), label = "音乐缓存大小", value = "${cacheSize} GB", onClick = { showCacheDialog = true })
                    }
                }
            }

            item {
                SectionTitle("存储空间")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val exoCacheBytes = viewModel.playerManager.getCacheBytes()
                        val musicBytes = if (exoCacheBytes > 0) exoCacheBytes else PhoneGetCacheSizeBytes(context, "music_cache")
                        val otherBytes = PhoneGetOtherDataSizeBytes(context)
                        val totalUsed = musicBytes + otherBytes
                        val maxBytes = cacheSize.toLong() * 1024 * 1024 * 1024
                        val progress = if (maxBytes > 0) (totalUsed.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("已使用 ${PhoneFormatSize(totalUsed)}", fontSize = 13.sp)
                            Text("共 ${cacheSize} GB", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎵 音乐缓存 $musicCacheSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("📦 其他数据 $otherDataSize", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.playerManager.clearMusicCache(); musicCacheSize = "0 B"; otherDataSize = PhoneCalculateOtherDataSize(context) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("清除缓存", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                SectionTitle("账号")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsClickItem(icon = Icons.Default.Person, iconBg = Color(0xFF5352ED).copy(alpha = 0.15f), label = "当前账号", value = username, onClick = { })
                        SettingsClickItem(icon = Icons.Default.Link, iconBg = Color(0xFFFFA502).copy(alpha = 0.15f), label = "服务器地址", value = serverUrl, onClick = { })
                        Box(modifier = Modifier.fillMaxWidth().clickable { showLogoutDialog = true }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            Text("切换服务器", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 影视服务器设置
            if (videoViewModel != null) {
                item {
                    PhoneVideoServerSettings(videoViewModel)
                }
            }

            // 版本更新
            item {
                SectionTitle("版本更新")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        val isChecking by viewModel.isCheckingUpdate.collectAsState()
                        SettingsClickItem(
                            icon = Icons.Default.Refresh,
                            iconBg = Color(0xFF1E90FF).copy(alpha = 0.15f),
                            label = "检查更新",
                            value = if (isChecking) "检查中..."
                                    else if (updateInfo != null) "有新版本 v${updateInfo!!.versionName}"
                                    else if (updateStatus.isNotEmpty()) updateStatus
                                    else "当前 ${PhoneGetCurrentVersionName(context)}",
                            onClick = { viewModel.dismissUpdate(); userTriggeredCheck = true; viewModel.checkForUpdate(silent = false) }
                        )
                    }
                }
            }

            item {
                SectionTitle("关于")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsClickItem(icon = Icons.Default.Info, iconBg = Color(0xFF2ED573).copy(alpha = 0.15f), label = "关于悦音", value = "", onClick = { showAboutDialog = true })
                    }
                }
            }
        }
    }

    // ===== 共享弹窗 =====

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于悦音", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("官网", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("http://yy.tthsdd.top", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("邮箱", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("10711306@qq.com", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("确定") } }
        )
    }

    // 更新弹窗
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 v${info.versionName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("更新内容：", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(info.updateLog, fontSize = 14.sp)
                    if (updateStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateStatus, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.downloadUpdate() }) {
                    Text("立即更新", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipUpdate(); showUpdateDialog = false }) { Text("跳过该版本") }
            }
        )
    }

    if (showCacheDialog) {
        AlertDialog(onDismissRequest = { showCacheDialog = false }, title = { Text("选择缓存大小") }, text = {
            Column {
                listOf(2, 4, 8, 16).forEach { size ->
                    Text("${size} GB", modifier = Modifier.fillMaxWidth().clickable { viewModel.setCacheSize(size); showCacheDialog = false }.padding(vertical = 14.dp), fontSize = 15.sp, fontWeight = if (size == cacheSize) FontWeight.Bold else FontWeight.Normal, color = if (size == cacheSize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }, confirmButton = { TextButton(onClick = { showCacheDialog = false }) { Text("取消") } })
    }

    if (showLogoutDialog) {
        AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("切换服务器") }, text = { Text("确定要退出当前服务器吗？退出后需要重新输入服务器地址登录。") }, confirmButton = {
            TextButton(onClick = { viewModel.logout(); showLogoutDialog = false; onLogout() }) { Text("确定", color = MaterialTheme.colorScheme.primary) }
        }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } })
    }
}

// ==================== 手机组件 ====================

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

// ==================== 平板组件 ====================

@Composable
private fun TabletSectionTitle(title: String) {
    Text(
        title,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun TabletSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun TabletSettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

// ==================== 平板等级卡片 ====================

@Composable
private fun TabletUserLevelCard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val totalSeconds = remember {
        mutableStateOf(
            context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                .getLong("total_play_seconds", 0L)
        )
    }
    LaunchedEffect(Unit) {
        val serverStats = viewModel.fetchUserPlayStats()
        if (serverStats != null) {
            val serverSeconds = serverStats.totalDuration.toLong()
            val localSeconds = context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                .getLong("total_play_seconds", 0L)
            val maxSeconds = maxOf(serverSeconds, localSeconds)
            context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                .edit().putLong("total_play_seconds", maxSeconds).apply()
            totalSeconds.value = maxSeconds
        }
        while (true) {
            kotlinx.coroutines.delay(5000)
            totalSeconds.value = context.getSharedPreferences("play_stats", Context.MODE_PRIVATE)
                .getLong("total_play_seconds", 0L)
        }
    }
    val totalMinutes = totalSeconds.value / 60
    val level = com.lechenmusic.data.model.UserLevelSystem.getCurrentLevel(totalMinutes)
    val nextLevel = com.lechenmusic.data.model.UserLevelSystem.getNextLevel(totalMinutes)
    val minutesToNext = com.lechenmusic.data.model.UserLevelSystem.getMinutesToNextLevel(totalMinutes)
    val progress = com.lechenmusic.data.model.UserLevelSystem.getLevelProgress(totalMinutes)
    val isMaxLevel = nextLevel == null

    val gradientColors = when (level.level) {
        1 -> listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
        2 -> listOf(Color(0xFF66BB6A), Color(0xFFA5D6A7))
        3 -> listOf(Color(0xFF42A5F5), Color(0xFF90CAF9))
        4 -> listOf(Color(0xFF7E57C2), Color(0xFFB39DDB))
        5 -> listOf(Color(0xFFFF7043), Color(0xFFFFAB91))
        6 -> listOf(Color(0xFFFFA726), Color(0xFFFFCC80))
        7 -> listOf(Color(0xFFEF5350), Color(0xFFEF9A9A))
        8 -> listOf(Color(0xFFAB47BC), Color(0xFFCE93D8))
        9 -> listOf(Color(0xFFFFD700), Color(0xFFFFF176))
        10 -> listOf(Color(0xFFE91E63), Color(0xFFFF80AB))
        else -> listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：等级徽章
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Lv${level.level}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                // 右侧：等级信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        level.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isMaxLevel) "已满级 🎉"
                        else "距下一级「${nextLevel!!.title}」还有 ${minutesToNext} 分钟",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val hours = totalMinutes / 60
                    val mins = totalMinutes % 60
                    Text(
                        "累计听歌 ${hours} 小时 ${mins} 分钟",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ==================== 共享组件 ====================

@Composable
private fun SettingsToggleItem(icon: ImageVector, iconBg: Color, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = iconBg) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } }
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickItem(icon: ImageVector, iconBg: Color, label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = iconBg) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } }
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

// ==================== 影视服务器设置 ====================

@Composable
private fun PhoneVideoServerSettings(viewModel: com.lechenmusic.ui.VideoViewModel) {
    val videoServerUrl by viewModel.videoServerUrl.collectAsState()
    val videoUsername by viewModel.videoUsername.collectAsState()
    val videoPassword by viewModel.videoPassword.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var showVideoServerDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    SectionTitle("影视服务器")
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            SettingsClickItem(
                icon = Icons.Default.Tv,
                iconBg = Color(0xFFE94560).copy(alpha = 0.15f),
                label = "影视服务器",
                value = if (isLoggedIn) "已连接" else if (videoServerUrl.isNotBlank()) "未连接" else "未配置",
                onClick = { showVideoServerDialog = true }
            )
            if (videoServerUrl.isNotBlank()) {
                InfoRow("服务器地址", videoServerUrl)
                InfoRow("用户名", videoUsername)
            }
            if (isLoggedIn) {
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.logout() }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("断开影视服务器", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        }
    }

    if (showVideoServerDialog) {
        var url by remember { mutableStateOf(videoServerUrl.ifBlank { "http://j.tthsdd.top:3000" }) }
        var user by remember { mutableStateOf(videoUsername) }
        var pass by remember { mutableStateOf(videoPassword) }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showVideoServerDialog = false; testResult = null },
            title = { Text("影视服务器配置", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://j.tthsdd.top:3000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 测试结果
                    testResult?.let { (success, message) ->
                        Text(
                            message,
                            fontSize = 13.sp,
                            color = if (success) Color(0xFF2ED573) else MaterialTheme.colorScheme.error
                        )
                    }
                    // 测试连接按钮
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testResult = null
                            viewModel.testConnection(url, user, pass) { success, msg ->
                                isTesting = false
                                testResult = success to msg
                            }
                        },
                        enabled = !isTesting && url.isNotBlank() && user.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isTesting) "测试中..." else "测试连接")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.login(url, user, pass)
                    showVideoServerDialog = false
                    testResult = null
                }) {
                    Text("保存并连接", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoServerDialog = false; testResult = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TabletVideoServerSettings(viewModel: com.lechenmusic.ui.VideoViewModel) {
    val videoServerUrl by viewModel.videoServerUrl.collectAsState()
    val videoUsername by viewModel.videoUsername.collectAsState()
    val videoPassword by viewModel.videoPassword.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var showVideoServerDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    TabletSectionTitle("影视服务器")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            SettingsClickItem(
                icon = Icons.Default.Tv,
                iconBg = Color(0xFFE94560).copy(alpha = 0.15f),
                label = "影视服务器",
                value = if (isLoggedIn) "已连接" else if (videoServerUrl.isNotBlank()) "未连接" else "未配置",
                onClick = { showVideoServerDialog = true }
            )
            if (videoServerUrl.isNotBlank()) {
                TabletSettingsInfoRow("服务器地址", videoServerUrl)
                TabletSettingsInfoRow("用户名", videoUsername)
            }
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("断开影视服务器", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        }
    }

    if (showVideoServerDialog) {
        var url by remember { mutableStateOf(videoServerUrl.ifBlank { "http://j.tthsdd.top:3000" }) }
        var user by remember { mutableStateOf(videoUsername) }
        var pass by remember { mutableStateOf(videoPassword) }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showVideoServerDialog = false; testResult = null },
            title = { Text("影视服务器配置", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://j.tthsdd.top:3000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    testResult?.let { (success, message) ->
                        Text(
                            message,
                            fontSize = 13.sp,
                            color = if (success) Color(0xFF2ED573) else MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testResult = null
                            viewModel.testConnection(url, user, pass) { success, msg ->
                                isTesting = false
                                testResult = success to msg
                            }
                        },
                        enabled = !isTesting && url.isNotBlank() && user.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isTesting) "测试中..." else "测试连接")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.login(url, user, pass)
                    showVideoServerDialog = false
                    testResult = null
                }) {
                    Text("保存并连接", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoServerDialog = false; testResult = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 工具函数 ====================

private fun PhoneGetCacheSizeBytes(context: Context, dirName: String): Long {
    val dir = java.io.File(context.cacheDir, dirName)
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun PhoneGetOtherDataSizeBytes(context: Context): Long {
    var size = 0L
    val datastoreDir = java.io.File(context.filesDir, "datastore")
    if (datastoreDir.exists()) size += datastoreDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    context.cacheDir.listFiles()?.forEach { f ->
        if (f.name != "music_cache") {
            size += if (f.isDirectory) f.walkTopDown().filter { it.isFile }.sumOf { it.length() } else f.length()
        }
    }
    return size
}

private fun PhoneCalculateCacheSize(context: Context, dirName: String): String = PhoneFormatSize(PhoneGetCacheSizeBytes(context, dirName))
private fun PhoneCalculateOtherDataSize(context: Context): String = PhoneFormatSize(PhoneGetOtherDataSizeBytes(context))

private fun PhoneFormatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun PhoneGetCurrentVersionName(context: Context): String {
    return try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (_: Exception) { "1.0.0" }
}
