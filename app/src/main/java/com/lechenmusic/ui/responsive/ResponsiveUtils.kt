package com.lechenmusic.ui.responsive

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val screenWidthDp: Int,     // 屏幕宽度 dp
    // 平板自适应尺寸
    val albumCardSize: Dp,      // 专辑封面尺寸
    val playlistCardSize: Dp,   // 歌单封面尺寸
    val heroHeight: Dp,         // Hero 区域高度
    val songCoverSize: Dp,      // 歌曲列表封面尺寸
    val sectionSpacing: Dp,     // 区块间距
    val itemSpacing: Dp,        // 卡片间距
    // 平板自适应字号
    val titleFontSize: TextUnit,     // 页面标题
    val sectionTitleSize: TextUnit,  // 区块标题
    val cardTitleSize: TextUnit,     // 卡片标题
    val cardSubtitleSize: TextUnit,  // 卡片副标题
    val bodyFontSize: TextUnit,      // 正文
    val captionFontSize: TextUnit,   // 辅助文字
    // 最近播放列数
    val recentColumns: Int,
    // Hero 区域布局模式: "stack" 手机纵向堆叠 / "row" 平板横向并排
    val heroLayout: String,
)

@Composable
fun rememberResponsiveConfig(windowSizeClass: WindowSizeClass): ResponsiveConfig {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isLandscape = screenWidth > configuration.screenHeightDp

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    return remember(widthClass, heightClass, isLandscape, screenWidth) {
        when (widthClass) {
            WindowWidthSizeClass.Compact -> ResponsiveConfig(
                isCompact = true,
                isMedium = false,
                isExpanded = false,
                isLandscape = isLandscape,
                gridColumns = 2,
                useRailNav = false,
                useDrawerNav = false,
                contentPadding = 16.dp,
                cardWidth = 120.dp,
                screenWidthDp = screenWidth,
                albumCardSize = 130.dp,
                playlistCardSize = 110.dp,
                heroHeight = 170.dp,
                songCoverSize = 46.dp,
                sectionSpacing = 14.dp,
                itemSpacing = 12.dp,
                titleFontSize = 15.sp,
                sectionTitleSize = 15.sp,
                cardTitleSize = 12.sp,
                cardSubtitleSize = 11.sp,
                bodyFontSize = 13.sp,
                captionFontSize = 11.sp,
                recentColumns = 1,
                heroLayout = "stack"
            )
            WindowWidthSizeClass.Medium -> ResponsiveConfig(
                isCompact = false,
                isMedium = true,
                isExpanded = false,
                isLandscape = isLandscape,
                gridColumns = calculateAdaptiveColumns(screenWidth, 150),
                useRailNav = true,
                useDrawerNav = false,
                contentPadding = 24.dp,
                cardWidth = 140.dp,
                screenWidthDp = screenWidth,
                albumCardSize = 150.dp,
                playlistCardSize = 130.dp,
                heroHeight = 200.dp,
                songCoverSize = 52.dp,
                sectionSpacing = 18.dp,
                itemSpacing = 16.dp,
                titleFontSize = 18.sp,
                sectionTitleSize = 14.sp,
                cardTitleSize = 14.sp,
                cardSubtitleSize = 12.sp,
                bodyFontSize = 15.sp,
                captionFontSize = 12.sp,
                recentColumns = 2,
                heroLayout = "row"
            )
            else -> ResponsiveConfig( // Expanded
                isCompact = false,
                isMedium = false,
                isExpanded = true,
                isLandscape = isLandscape,
                gridColumns = calculateAdaptiveColumns(screenWidth, 170),
                useRailNav = true,
                useDrawerNav = false,
                contentPadding = 32.dp,
                cardWidth = 160.dp,
                screenWidthDp = screenWidth,
                albumCardSize = 170.dp,
                playlistCardSize = 150.dp,
                heroHeight = 240.dp,
                songCoverSize = 56.dp,
                sectionSpacing = 22.dp,
                itemSpacing = 20.dp,
                titleFontSize = 22.sp,
                sectionTitleSize = 16.sp,
                cardTitleSize = 15.sp,
                cardSubtitleSize = 13.sp,
                bodyFontSize = 16.sp,
                captionFontSize = 13.sp,
                recentColumns = if (isLandscape) 3 else 2,
                heroLayout = "row"
            )
        }
    }
}

/**
 * 根据屏幕宽度和卡片最小宽度自适应计算列数
 * 屏幕越宽，显示更多列来填充，而不是拉伸卡片
 */
private fun calculateAdaptiveColumns(screenWidthDp: Int, minCardWidthDp: Int): Int {
    val availableWidth = screenWidthDp - 80 // 减去侧边导航栏宽度
    val columns = availableWidth / (minCardWidthDp + 16) // 16dp 间距
    return columns.coerceIn(3, 8)
}

/**
 * 根据屏幕宽度计算网格列数(用于 LazyVerticalGrid)
 */
@Composable
fun rememberGridColumns(windowSizeClass: WindowSizeClass): Int {
    val config = rememberResponsiveConfig(windowSizeClass)
    return config.gridColumns
}
