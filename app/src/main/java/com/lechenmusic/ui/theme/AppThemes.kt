package com.lechenmusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 多套主题配色（参考 GitHub 上的开源配色项目）。
 *
 * 每套主题包含深色 / 浅色两个变体，色板取自各项目官方 palette，
 * 并按 Material 3 的角色重新映射；所有前景/背景组合都经过 WCAG 对比度校验
 * （正文文字 ≥ 4.5:1，强调色 ≥ 3:1），避免出现「文字和背景太接近看不清」。
 *
 * 色板来源：
 * - Catppuccin  https://github.com/catppuccin/catppuccin
 * - Nord        https://github.com/nordtheme/nord
 * - Dracula     https://github.com/dracula/dracula-theme
 * - Tokyo Night https://github.com/folke/tokyonight.nvim
 * - Gruvbox     https://github.com/morhetz/gruvbox
 * - Rosé Pine   https://github.com/rose-pine/rose-pine
 */
data class AppTheme(
    val id: String,
    val name: String,
    val source: String,
    val dark: Palette,
    val light: Palette
)

/** 一套主题在某个明暗模式下的颜色集合（ARGB Long，0xFFRRGGBB） */
data class Palette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceHigh: Long,
    val outline: Long,
    val outlineVariant: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long
)

private fun c(v: Long): Color = Color(v)

private fun Palette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) {
        darkColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(secondary), onTertiary = c(onSecondary),
            tertiaryContainer = c(secondaryContainer), onTertiaryContainer = c(onSecondaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            outline = c(outline), outlineVariant = c(outlineVariant),
            error = c(error), onError = c(onError),
            errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer)
        )
    } else {
        lightColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(secondary), onTertiary = c(onSecondary),
            tertiaryContainer = c(secondaryContainer), onTertiaryContainer = c(onSecondaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            outline = c(outline), outlineVariant = c(outlineVariant),
            error = c(error), onError = c(onError),
            errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer)
        )
    }
    // 卡片 / 底部栏 / 弹窗分别使用不同层级的 surfaceContainer，保证层级有区分但文字对比度足够
    return base.copy(
        surfaceContainerLowest = c(background),
        surfaceContainerLow = c(surface),
        surfaceContainer = c(surfaceVariant),
        surfaceContainerHigh = c(surfaceHigh),
        surfaceContainerHighest = c(surfaceHigh),
        inverseSurface = c(onSurface),
        inverseOnSurface = c(surface),
        inversePrimary = c(primaryContainer)
    )
}

object AppThemes {

    const val DEFAULT_ID = "aurora"

    /** 极光紫：项目原有主题（保留默认观感，次要文字加深以保证对比度） */
    private val aurora = AppTheme(
        id = "aurora",
        name = "极光紫",
        source = "LeChenMusic 默认",
        dark = Palette(
            primary = 0xFF6C4BF5, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFF35295C, onPrimaryContainer = 0xFFE6DEFF,
            secondary = 0xFFA78BFA, onSecondary = 0xFF1D1433,
            secondaryContainer = 0xFF2B2350, onSecondaryContainer = 0xFFEDE7FF,
            background = 0xFF0A0A14, onBackground = 0xFFEEEEF5,
            surface = 0xFF16162A, onSurface = 0xFFEEEEF5,
            surfaceVariant = 0xFF1E1E38, onSurfaceVariant = 0xFFA6A6C8,
            surfaceHigh = 0xFF22223E,
            outline = 0xFF3A3A66, outlineVariant = 0xFF282850,
            error = 0xFFFF6B84, onError = 0xFF2A0A12,
            errorContainer = 0xFF4A1524, onErrorContainer = 0xFFFFD9E0
        ),
        light = Palette(
            primary = 0xFF6C5CE7, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFE4DEFF, onPrimaryContainer = 0xFF2A1B6B,
            secondary = 0xFF5A4BD1, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFEDE9FF, onSecondaryContainer = 0xFF241A56,
            background = 0xFFF5F5FA, onBackground = 0xFF1A1A2E,
            surface = 0xFFFFFFFF, onSurface = 0xFF1A1A2E,
            surfaceVariant = 0xFFF0F0F8, onSurfaceVariant = 0xFF565672,
            surfaceHigh = 0xFFE9E9F3,
            outline = 0xFFC9C9DE, outlineVariant = 0xFFE0E0EA,
            error = 0xFFC0273F, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDDE2, onErrorContainer = 0xFF5A0F1C
        )
    )

    /** Catppuccin：柔和粉彩 */
    private val catppuccin = AppTheme(
        id = "catppuccin",
        name = "猫布奇诺",
        source = "Catppuccin（Mocha / Latte）",
        dark = Palette(
            primary = 0xFFCBA6F7, onPrimary = 0xFF1E1E2E,
            primaryContainer = 0xFF4A3B63, onPrimaryContainer = 0xFFF2E7FF,
            secondary = 0xFF89B4FA, onSecondary = 0xFF10162B,
            secondaryContainer = 0xFF2E3A57, onSecondaryContainer = 0xFFE3EBFF,
            background = 0xFF1E1E2E, onBackground = 0xFFCDD6F4,
            surface = 0xFF292C3C, onSurface = 0xFFCDD6F4,
            surfaceVariant = 0xFF313244, onSurfaceVariant = 0xFFB5BDDC,
            surfaceHigh = 0xFF3A3D52,
            outline = 0xFF5A5E77, outlineVariant = 0xFF45475A,
            error = 0xFFF38BA8, onError = 0xFF2A0E17,
            errorContainer = 0xFF4E2030, onErrorContainer = 0xFFFFD9E3
        ),
        light = Palette(
            primary = 0xFF8839EF, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFEBDDFF, onPrimaryContainer = 0xFF3B1672,
            secondary = 0xFF1E66F5, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFDDE7FF, onSecondaryContainer = 0xFF0F2E6E,
            background = 0xFFEFF1F5, onBackground = 0xFF4C4F69,
            surface = 0xFFFFFFFF, onSurface = 0xFF4C4F69,
            surfaceVariant = 0xFFE6E9EF, onSurfaceVariant = 0xFF5C5F78,
            surfaceHigh = 0xFFDCE0E8,
            outline = 0xFF9CA0B0, outlineVariant = 0xFFCCD0DA,
            error = 0xFFD20F39, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDCE0, onErrorContainer = 0xFF5C0B1B
        )
    )

    /** Nord：北欧冷调蓝灰 */
    private val nord = AppTheme(
        id = "nord",
        name = "北境蓝灰",
        source = "Nord（Polar Night / Snow Storm）",
        dark = Palette(
            primary = 0xFF88C0D0, onPrimary = 0xFF2E3440,
            primaryContainer = 0xFF4C566A, onPrimaryContainer = 0xFFECEFF4,
            secondary = 0xFF81A1C1, onSecondary = 0xFF2E3440,
            secondaryContainer = 0xFF434C5E, onSecondaryContainer = 0xFFE5E9F0,
            background = 0xFF2E3440, onBackground = 0xFFECEFF4,
            surface = 0xFF3B4252, onSurface = 0xFFECEFF4,
            surfaceVariant = 0xFF434C5E, onSurfaceVariant = 0xFFD8DEE9,
            surfaceHigh = 0xFF4C566A,
            outline = 0xFF616E88, outlineVariant = 0xFF4C566A,
            error = 0xFFD96C79, onError = 0xFF2E1013,
            errorContainer = 0xFF5A2A31, onErrorContainer = 0xFFFFDDE0
        ),
        light = Palette(
            primary = 0xFF4E7199, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFD8E4F0, onPrimaryContainer = 0xFF22344A,
            secondary = 0xFF4C6A92, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFDDE6F2, onSecondaryContainer = 0xFF1F2F45,
            background = 0xFFECEFF4, onBackground = 0xFF2E3440,
            surface = 0xFFFFFFFF, onSurface = 0xFF2E3440,
            surfaceVariant = 0xFFE5E9F0, onSurfaceVariant = 0xFF465166,
            surfaceHigh = 0xFFD8DEE9,
            outline = 0xFF9AA5B5, outlineVariant = 0xFFD8DEE9,
            error = 0xFFB04049, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDDE0, onErrorContainer = 0xFF5A1418
        )
    )

    /** Dracula：高对比霓虹紫 */
    private val dracula = AppTheme(
        id = "dracula",
        name = "德古拉霓虹",
        source = "Dracula（Alucard 浅色）",
        dark = Palette(
            primary = 0xFFBD93F9, onPrimary = 0xFF1B1030,
            primaryContainer = 0xFF3F3357, onPrimaryContainer = 0xFFF3E9FF,
            secondary = 0xFF8BE9FD, onSecondary = 0xFF062A31,
            secondaryContainer = 0xFF23414A, onSecondaryContainer = 0xFFDDF9FF,
            background = 0xFF21222C, onBackground = 0xFFF8F8F2,
            surface = 0xFF282A36, onSurface = 0xFFF8F8F2,
            surfaceVariant = 0xFF343746, onSurfaceVariant = 0xFFB9BDCE,
            surfaceHigh = 0xFF44475A,
            outline = 0xFF5A5F73, outlineVariant = 0xFF44475A,
            error = 0xFFFF5555, onError = 0xFF2B0707,
            errorContainer = 0xFF55262A, onErrorContainer = 0xFFFFD9D9
        ),
        light = Palette(
            primary = 0xFF644AC9, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFE5DDFA, onPrimaryContainer = 0xFF2C1A5E,
            secondary = 0xFF0B6E4F, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFD6EFE4, onSecondaryContainer = 0xFF06341F,
            background = 0xFFF7F7F2, onBackground = 0xFF282A36,
            surface = 0xFFFFFFFF, onSurface = 0xFF282A36,
            surfaceVariant = 0xFFEEEDE4, onSurfaceVariant = 0xFF565866,
            surfaceHigh = 0xFFE3E2D8,
            outline = 0xFFA9A89E, outlineVariant = 0xFFDCDBD1,
            error = 0xFFB32D22, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDAD5, onErrorContainer = 0xFF5A1009
        )
    )

    /** Tokyo Night：深夜霓虹 */
    private val tokyoNight = AppTheme(
        id = "tokyo-night",
        name = "东京夜色",
        source = "Tokyo Night（Night / Day）",
        dark = Palette(
            primary = 0xFF7AA2F7, onPrimary = 0xFF0E1526,
            primaryContainer = 0xFF2C3A5E, onPrimaryContainer = 0xFFE1E9FF,
            secondary = 0xFFBB9AF7, onSecondary = 0xFF1A0E33,
            secondaryContainer = 0xFF3B3060, onSecondaryContainer = 0xFFF0E7FF,
            background = 0xFF1A1B26, onBackground = 0xFFC0CAF5,
            surface = 0xFF1F2335, onSurface = 0xFFC0CAF5,
            surfaceVariant = 0xFF292E42, onSurfaceVariant = 0xFFA9B1D6,
            surfaceHigh = 0xFF313650,
            outline = 0xFF4A516E, outlineVariant = 0xFF292E42,
            error = 0xFFF7768E, onError = 0xFF2A0B12,
            errorContainer = 0xFF55222F, onErrorContainer = 0xFFFFD9DF
        ),
        light = Palette(
            primary = 0xFF34548A, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFD7E2F7, onPrimaryContainer = 0xFF1B2C4D,
            secondary = 0xFF5A4B8C, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFE4DEFA, onSecondaryContainer = 0xFF2A1E52,
            background = 0xFFE9EAF0, onBackground = 0xFF343B58,
            surface = 0xFFFFFFFF, onSurface = 0xFF343B58,
            surfaceVariant = 0xFFE1E3EC, onSurfaceVariant = 0xFF4A5273,
            surfaceHigh = 0xFFD5D8E4,
            outline = 0xFF8C93A8, outlineVariant = 0xFFD5D8E4,
            error = 0xFF8C4351, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFD9DF, onErrorContainer = 0xFF4A1018
        )
    )

    /** Gruvbox：复古暖色 */
    private val gruvbox = AppTheme(
        id = "gruvbox",
        name = "复古暖阳",
        source = "Gruvbox（Dark / Light）",
        dark = Palette(
            primary = 0xFFFE8019, onPrimary = 0xFF282828,
            primaryContainer = 0xFF5A3A1E, onPrimaryContainer = 0xFFFFDFC2,
            secondary = 0xFF83A598, onSecondary = 0xFF1B2B26,
            secondaryContainer = 0xFF33473F, onSecondaryContainer = 0xFFD8EFE7,
            background = 0xFF282828, onBackground = 0xFFEBDBB2,
            surface = 0xFF32302F, onSurface = 0xFFEBDBB2,
            surfaceVariant = 0xFF3C3836, onSurfaceVariant = 0xFFD5C4A1,
            surfaceHigh = 0xFF504945,
            outline = 0xFF665C54, outlineVariant = 0xFF504945,
            error = 0xFFFB4934, onError = 0xFF2A0A06,
            errorContainer = 0xFF5A211B, onErrorContainer = 0xFFFFD9D2
        ),
        light = Palette(
            primary = 0xFFAF3A03, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFF6D9B8, onPrimaryContainer = 0xFF4A1500,
            secondary = 0xFF076678, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFCFE5E0, onSecondaryContainer = 0xFF04323A,
            background = 0xFFFBF1C7, onBackground = 0xFF282828,
            surface = 0xFFFFFCF2, onSurface = 0xFF282828,
            surfaceVariant = 0xFFF2E5BC, onSurfaceVariant = 0xFF5F5648,
            surfaceHigh = 0xFFEBDBB2,
            outline = 0xFFA99E8C, outlineVariant = 0xFFE5D5A9,
            error = 0xFF9D0006, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDAD5, onErrorContainer = 0xFF5A0A06
        )
    )

    /** Rosé Pine：低饱和玫瑰护眼 */
    private val rosePine = AppTheme(
        id = "rose-pine",
        name = "玫瑰松林",
        source = "Rosé Pine（Main / Dawn）",
        dark = Palette(
            primary = 0xFFC4A7E7, onPrimary = 0xFF241A33,
            primaryContainer = 0xFF3D3356, onPrimaryContainer = 0xFFF1E8FF,
            secondary = 0xFF9CCFD8, onSecondary = 0xFF0F2A2E,
            secondaryContainer = 0xFF2F4A50, onSecondaryContainer = 0xFFDDF4F8,
            background = 0xFF191724, onBackground = 0xFFE0DEF4,
            surface = 0xFF1F1D2E, onSurface = 0xFFE0DEF4,
            surfaceVariant = 0xFF26233A, onSurfaceVariant = 0xFFB8B4CE,
            surfaceHigh = 0xFF312F44,
            outline = 0xFF524F67, outlineVariant = 0xFF403D52,
            error = 0xFFEB6F92, onError = 0xFF2A0D16,
            errorContainer = 0xFF55222F, onErrorContainer = 0xFFFFD9E1
        ),
        light = Palette(
            primary = 0xFF6B5A85, onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFFE8DFF3, onPrimaryContainer = 0xFF3B2E52,
            secondary = 0xFF286983, onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFD8E9EE, onSecondaryContainer = 0xFF103B48,
            background = 0xFFFAF4ED, onBackground = 0xFF575279,
            surface = 0xFFFFFFFF, onSurface = 0xFF575279,
            surfaceVariant = 0xFFF2E9E1, onSurfaceVariant = 0xFF605C7A,
            surfaceHigh = 0xFFEBE0D8,
            outline = 0xFFB5AFA8, outlineVariant = 0xFFEBE0D8,
            error = 0xFFA64F66, onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDDE3, onErrorContainer = 0xFF5A1B29
        )
    )

    val all: List<AppTheme> = listOf(aurora, catppuccin, nord, dracula, tokyoNight, gruvbox, rosePine)

    fun find(id: String?): AppTheme = all.firstOrNull { it.id == id } ?: aurora

    fun colorScheme(id: String?, dark: Boolean): ColorScheme {
        val theme = find(id)
        return (if (dark) theme.dark else theme.light).toColorScheme(dark)
    }

    /** 主题预览用的色块（主色 / 次色 / 背景 / 卡片） */
    fun previewColors(theme: AppTheme, dark: Boolean): List<Color> {
        val p = if (dark) theme.dark else theme.light
        return listOf(Color(p.primary), Color(p.secondary), Color(p.background), Color(p.surface))
    }
}