package com.lechenmusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig
import com.lechenmusic.ui.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletSettingsScreen(
    viewModel: MainViewModel,
    videoViewModel: VideoViewModel?,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = responsiveConfig.contentPadding, vertical = 16.dp)
    ) {
        // 面包屑
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("首页", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" / ", color = MaterialTheme.colorScheme.outlineVariant)
            Text("设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 用户信息卡片
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

        // 设置项列表
        Text("外观", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "深色模式",
                subtitle = if (themeMode == "dark") "已开启" else "已关闭",
                onClick = { viewModel.setThemeMode(if (themeMode == "dark") "light" else "dark") }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "主题颜色",
                subtitle = "紫色",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("播放", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Default.MusicNote,
                title = "音质设置",
                subtitle = "自动",
                onClick = { }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Download,
                title = "缓存大小",
                subtitle = "2GB",
                onClick = { }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "清除缓存",
                subtitle = "释放存储空间",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("关于", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "版本",
                subtitle = "v1.5.0",
                onClick = { }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Update,
                title = "检查更新",
                subtitle = "当前已是最新版本",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 退出登录
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth().clickable { showLogoutDialog = true }
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

        Spacer(modifier = Modifier.height(160.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
