package com.lechenmusic.ui.theme
import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary, onPrimary = Color.White, primaryContainer = DarkPrimaryDark,
    background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground, onSurface = DarkOnSurface, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder, error = AccentRed
)
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary, onPrimary = Color.White, primaryContainer = LightPrimaryDark,
    background = LightBackground, surface = LightSurface, surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground, onSurface = LightOnSurface, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder, error = AccentRed
)

// 全局状态栏颜色控制（播放器页面可覆盖）
val PlayerStatusBarColor = mutableStateOf<Color?>(null)

@Composable
fun LeChenMusicTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val playerColor = PlayerStatusBarColor.value
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 如果播放器设置了自定义状态栏颜色，则使用它；否则用透明
            if (playerColor != null) {
                window.statusBarColor = playerColor.toArgb()
            } else {
                window.statusBarColor = Color.Transparent.toArgb()
            }
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false // 播放器和深色主题都是深色背景，状态栏用白色图标
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
