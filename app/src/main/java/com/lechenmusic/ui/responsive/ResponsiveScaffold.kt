package com.lechenmusic.ui.responsive

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 导航项定义
 */
data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * 响应式 Scaffold
 * - Compact(手机): 底部导航栏
 * - Medium/Expanded(平板): 侧边导航栏(NavigationRail)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveScaffold(
    config: ResponsiveConfig,
    navItems: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    showBottomBar: Boolean = true,
    topBar: @Composable () -> Unit = {},
    miniPlayer: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (config.useRailNav) {
        // 平板/车机: 侧边导航栏
        Row(modifier = Modifier.fillMaxSize()) {
            // MiniPlayer + NavigationRail
            Column(modifier = Modifier.fillMaxHeight()) {
                miniPlayer()
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(modifier = Modifier.weight(0.3f))
                    navItems.forEach { item ->
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    if (currentRoute == item.route) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, fontSize = 10.sp) },
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.7f))
                }
            }
            // 主内容区
            Scaffold(
                topBar = topBar,
                content = content
            )
        }
    } else {
        // 手机: 底部导航栏
        Scaffold(
            topBar = topBar,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Column {
                        miniPlayer()
                        NavigationBar {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            if (currentRoute == item.route) item.selectedIcon else item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label, fontSize = 10.sp) },
                                    selected = currentRoute == item.route,
                                    onClick = { onNavigate(item.route) }
                                )
                            }
                        }
                    }
                }
            },
            content = content
        )
    }
}
