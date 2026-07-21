package com.lechenmusic.ui.responsive

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 响应式布局工具类
 *
 * 参考: Jetpack Compose WindowSizeClass 最佳实践
 * - Compact: 手机 (< 600dp)
 * - Medium: 小平板 (600dp - 840dp)
 * - Expanded: 大平板/车机 (> 840dp)
 */

@Immutable
data class ResponsiveConfig(
    val isCompact: Boolean,      // 手机
    val isMedium: Boolean,       // 小平板
    val isExpanded: Boolean,     // 大平板/车机
    val isLandscape: Boolean,    // 横屏
    val gridColumns: Int,        // 网格列数
    val useRailNav: Boolean,     // 使用侧边导航栏
    val useDrawerNav: Boolean,   // 使用抽屉导航
    val contentPadding: Dp,     // 内容边距
    val cardWidth: Dp,          // 卡片推荐宽度
)

@Composable
fun rememberResponsiveConfig(windowSizeClass: WindowSizeClass): ResponsiveConfig {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    return remember(widthClass, heightClass, isLandscape) {
        when (widthClass) {
            WindowWidthSizeClass.Compact -> ResponsiveConfig(
                isCompact = true,
                isMedium = false,
                isExpanded = false,
                isLandscape = isLandscape,
                gridColumns = 3,
                useRailNav = false,
                useDrawerNav = false,
                contentPadding = 16.dp,
                cardWidth = 120.dp
            )
            WindowWidthSizeClass.Medium -> ResponsiveConfig(
                isCompact = false,
                isMedium = true,
                isExpanded = false,
                isLandscape = isLandscape,
                gridColumns = if (isLandscape) 5 else 4,
                useRailNav = true,
                useDrawerNav = false,
                contentPadding = 24.dp,
                cardWidth = 140.dp
            )
            else -> ResponsiveConfig( // Expanded
                isCompact = false,
                isMedium = false,
                isExpanded = true,
                isLandscape = isLandscape,
                gridColumns = if (isLandscape) 7 else 5,
                useRailNav = true,
                useDrawerNav = false,
                contentPadding = 32.dp,
                cardWidth = 160.dp
            )
        }
    }
}

/**
 * 根据屏幕宽度计算网格列数(用于 LazyVerticalGrid)
 */
@Composable
fun rememberGridColumns(windowSizeClass: WindowSizeClass): Int {
    val config = rememberResponsiveConfig(windowSizeClass)
    return config.gridColumns
}
