package com.lechenmusic.ui.theme
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 全局状态栏颜色控制（播放器页面可覆盖）
val PlayerStatusBarColor = mutableStateOf<Color?>(null)

/**
 * 应用主题：支持多套配色（见 AppThemes.kt）。
 * [themeId] 为设置页选择的配色 id，[darkTheme] 决定使用深色还是浅色变体。
 */
@Composable
fun LeChenMusicTheme(
    darkTheme: Boolean = true,
    themeId: String = AppThemes.DEFAULT_ID,
    content: @Composable () -> Unit
) {
    val colorScheme = AppThemes.colorScheme(themeId, darkTheme)
    val view = LocalView.current
    val playerColor = PlayerStatusBarColor.value
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置窗口背景色与当前主题背景色一致，避免系统栏区域出现异色
            window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            // 如果播放器设置了自定义状态栏颜色，则使用它；否则用透明
            window.statusBarColor = playerColor?.toArgb() ?: Color.Transparent.toArgb()
            window.navigationBarColor = playerColor?.toArgb() ?: Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = if (playerColor != null) false else !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}